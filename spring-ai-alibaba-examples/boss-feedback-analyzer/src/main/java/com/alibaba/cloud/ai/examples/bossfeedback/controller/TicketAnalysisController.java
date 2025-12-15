/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.bossfeedback.controller;

import com.alibaba.cloud.ai.examples.bossfeedback.agent.TicketAnalysisAgentSystem;
import com.alibaba.cloud.ai.examples.bossfeedback.model.AnalysisResult;
import com.alibaba.cloud.ai.examples.bossfeedback.model.FeedbackTicket;
import com.alibaba.cloud.ai.examples.bossfeedback.model.TicketCategory;
import com.alibaba.cloud.ai.examples.bossfeedback.model.ToolApprovalRequest;
import com.alibaba.cloud.ai.examples.bossfeedback.service.TicketCacheService;
import com.alibaba.cloud.ai.examples.bossfeedback.service.TicketStorageService;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工单分析REST API控制器
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketAnalysisController {

    private final SequentialAgent mainAnalysisAgent;
    private final TicketStorageService ticketStorageService;
    private final TicketCacheService cacheService;
    private final ObjectMapper objectMapper;

    public TicketAnalysisController(
            TicketAnalysisAgentSystem agentSystem,
            TicketStorageService ticketStorageService,
            TicketCacheService cacheService,
            ObjectMapper objectMapper) throws GraphStateException {
        this.mainAnalysisAgent = agentSystem.createMainAnalysisAgent();
        this.ticketStorageService = ticketStorageService;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
    }

    /**
     * 同步分析接口（适用于简单工单）
     */
    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResult> analyzeTicket(@RequestBody FeedbackTicket ticket) {
        // 1. 检查缓存
        Optional<TicketCategory> cachedCategory = cacheService.getCachedClassification(
                ticket.getUserRequest() + ticket.getProblemDescription()
        );

        if (cachedCategory.isPresent()) {
            // 如果缓存命中，可以快速返回基础分类结果
            AnalysisResult quickResult = AnalysisResult.builder()
                    .success(true)
                    .fromCache(true)
                    .category(cachedCategory.get())
                    .build();
            return ResponseEntity.ok(quickResult);
        }

        // 2. 生成工单ID（如果没有）
        if (ticket.getTicketId() == null || ticket.getTicketId().isEmpty()) {
            ticket.setTicketId(ticketStorageService.generateTicketId());
        }

        // 3. 构建分析输入
        String analysisInput = buildAnalysisInput(ticket);

        // 4. 调用多智能体系统进行分析
        Optional<OverAllState> result;
        try {
            result = mainAnalysisAgent.invoke(analysisInput);
        } catch (Exception e) {
            AnalysisResult errorResult = AnalysisResult.builder()
                    .success(false)
                    .errorMessage("分析失败: " + e.getMessage())
                    .build();
            return ResponseEntity.ok(errorResult);
        }

        // 5. 提取分析结果
        AnalysisResult analysisResult = extractAnalysisResult(result);

        // 6. 缓存分类结果
        if (analysisResult.getCategory() != null) {
            cacheService.cacheClassification(
                    ticket.getUserRequest() + ticket.getProblemDescription(),
                    analysisResult.getCategory()
            );
        }

        // 7. 保存工单（异步）
        CompletableFuture.runAsync(() -> {
            try {
                ticketStorageService.storeTicket(ticket);
            } catch (Exception e) {
                // 记录错误但不影响主流程
                System.err.println("保存工单失败: " + e.getMessage());
            }
        });

        return ResponseEntity.ok(analysisResult);
    }

    /**
     * 流式分析接口（实时推送分析结果）
     */
    @PostMapping(value = "/analyze/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<ServerSentEvent<String>>> analyzeTicketStream(@RequestBody FeedbackTicket ticket) throws GraphRunnerException {
        // 生成工单ID（如果没有）
        if (ticket.getTicketId() == null || ticket.getTicketId().isEmpty()) {
            ticket.setTicketId(ticketStorageService.generateTicketId());
        }

        String analysisInput = buildAnalysisInput(ticket);

        // 使用stream()方法实现实时流式输出
        Flux<NodeOutput> stream = mainAnalysisAgent.stream(analysisInput);

        // 转换为SSE格式
        Flux<ServerSentEvent<String>> sseStream = stream
                .map(output -> {
                    try {
                        // 提取节点输出信息
                        String nodeName = output.node();
                        String agentName = output.agent();
                        OverAllState state = output.state();

                        // 构建SSE消息
                        Map<String, Object> messageData = new HashMap<>();
                        messageData.put("node", nodeName);
                        messageData.put("agent", agentName);
                        messageData.put("state", extractStateData(state));

                        // 如果是流式输出，提取消息内容
                        if (output instanceof StreamingOutput streamingOutput) {
                            var message = streamingOutput.message();
                            if (message instanceof AssistantMessage assistantMessage) {
                                messageData.put("content", assistantMessage.getText());
                            } else if (message != null) {
                                // 对于其他类型的Message，尝试获取文本内容
                                messageData.put("content", message.toString());
                            }
                        }

                        String json = objectMapper.writeValueAsString(messageData);
                        return ServerSentEvent.<String>builder()
                                .event("analysis_update")
                                .data(json)
                                .build();
                    } catch (Exception e) {
                        return ServerSentEvent.<String>builder()
                                .event("error")
                                .data("{\"error\": \"" + e.getMessage() + "\"}")
                                .build();
                    }
                })
                .onErrorResume(error -> {
                    // 错误处理
                    ServerSentEvent<String> errorEvent = ServerSentEvent.<String>builder()
                            .event("error")
                            .data("{\"error\": \"" + error.getMessage() + "\"}")
                            .build();
                    return Flux.just(errorEvent);
                })
                .doOnComplete(() -> {
                    // 流式处理完成后，异步保存工单
                    CompletableFuture.runAsync(() -> {
                        try {
                            ticketStorageService.storeTicket(ticket);
                        } catch (Exception e) {
                            System.err.println("保存工单失败: " + e.getMessage());
                        }
                    });
                });

        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .body(sseStream);
    }

    /**
     * 批量并行分析接口 - 使用ParallelAgent并行处理多个工单
     */
    @PostMapping("/analyze/batch")
    public ResponseEntity<Flux<AnalysisResult>> analyzeTicketsBatch(@RequestBody List<FeedbackTicket> tickets) {
        // 为每个工单创建分析任务
        List<Mono<AnalysisResult>> analysisTasks = (List<Mono<AnalysisResult>>) tickets.stream()
                .map((Function<? super FeedbackTicket, ?>) ticket -> {
                    // 生成工单ID
                    if (ticket.getTicketId() == null || ticket.getTicketId().isEmpty()) {
                        ticket.setTicketId(ticketStorageService.generateTicketId());
                    }

                    String analysisInput = buildAnalysisInput(ticket);
                    return Mono.fromCallable(() -> {
                        try {
                            Optional<OverAllState> result = mainAnalysisAgent.invoke(analysisInput);
                            AnalysisResult analysisResult = extractAnalysisResult(result);
                            // 异步保存
                            CompletableFuture.runAsync(() -> {
                                try {
                                    ticketStorageService.storeTicket(ticket);
                                } catch (Exception e) {
                                    System.err.println("保存工单失败: " + e.getMessage());
                                }
                            });
                            return analysisResult;
                        } catch (Exception e) {
                            return AnalysisResult.builder()
                                    .success(false)
                                    .errorMessage("分析失败: " + e.getMessage())
                                    .build();
                        }
                    }).subscribeOn(Schedulers.parallel());
                })
                .collect(Collectors.toList());

        // 并行执行所有分析任务
        Flux<AnalysisResult> results = Flux.merge(analysisTasks);

        return ResponseEntity.ok(results);
    }

    /**
     * 人工审核接口 - 用于HumanInTheLoopHook的回调
     * 
     * @param threadId 线程ID，用于恢复被中断的Agent执行
     * @param request 工具审批请求，包含工具ID、名称、参数、审批结果等信息
     * @return 200 OK 如果审批成功，500 如果执行失败
     */
    @PostMapping("/approve/{threadId}")
    public ResponseEntity<Void> approveToolCall(
            @PathVariable String threadId,
            @RequestBody ToolApprovalRequest request) {
        
        // 参数验证
        if (threadId == null || threadId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (request == null || request.getId() == null || request.getToolName() == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // 构建人工反馈
            // ToolFeedback构造函数参数顺序：id, name, arguments, result, description
            InterruptionMetadata.ToolFeedback toolFeedback =
                    new InterruptionMetadata.ToolFeedback(
                            request.getId(),
                            request.getToolName(),
                            request.getArguments() != null ? request.getArguments() : "",
                            request.getResult() != null ? request.getResult() : 
                                InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED,
                            request.getDescription() != null ? request.getDescription() : ""
                    );

            // 使用Builder模式创建InterruptionMetadata
            InterruptionMetadata interruptionMetadata = InterruptionMetadata.builder()
                    .addToolFeedback(toolFeedback)
                    .build();

            // 将反馈添加到RunnableConfig中
            // 使用专门的addHumanFeedback方法，更语义化且符合框架设计
            RunnableConfig config = RunnableConfig.builder()
                    .threadId(threadId)
                    .addHumanFeedback(interruptionMetadata)
                    .build();

            // 恢复Agent执行
            mainAnalysisAgent.invoke("", config);
            
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            // 参数错误
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            // 执行失败，记录错误但不暴露内部细节
            return ResponseEntity.internalServerError().build();
        }
    }

    private String buildAnalysisInput(FeedbackTicket ticket) {
        return String.format("""
                        请分析以下用户反馈工单：
                        
                        用户ID：%s
                        用户诉求：%s
                        问题描述：%s
                        手机型号：%s
                        APP版本：%s
                        反馈时间：%s
                        截图数量：%d
                        """,
                ticket.getUserId() != null ? ticket.getUserId() : "N/A",
                ticket.getUserRequest() != null ? ticket.getUserRequest() : "",
                ticket.getProblemDescription() != null ? ticket.getProblemDescription() : "",
                ticket.getPhoneModel() != null ? ticket.getPhoneModel() : "N/A",
                ticket.getAppVersion() != null ? ticket.getAppVersion() : "N/A",
                ticket.getFeedbackTime() != null ? ticket.getFeedbackTime().toString() : "N/A",
                ticket.getScreenshots() != null ? ticket.getScreenshots().size() : 0
        );
    }

    private Map<String, Object> extractStateData(OverAllState state) {
        Map<String, Object> data = new HashMap<>();
        // 提取关键状态信息
        state.value("structured_ticket").ifPresent(v -> data.put("structuredTicket", v.toString()));
        state.value("classification_result").ifPresent(v -> data.put("classification", v.toString()));
        state.value("root_cause_analysis").ifPresent(v -> data.put("rootCause", v.toString()));
        state.value("impact_assessment").ifPresent(v -> data.put("impact", v.toString()));
        state.value("solution_proposal").ifPresent(v -> data.put("solution", v.toString()));
        state.value("final_report").ifPresent(v -> data.put("finalReport", v.toString()));
        return data;
    }

    private AnalysisResult extractAnalysisResult(Optional<OverAllState> result) {
        if (result.isEmpty()) {
            return AnalysisResult.builder()
                    .success(false)
                    .errorMessage("分析失败：未返回结果")
                    .build();
        }

        OverAllState state = result.get();
        return AnalysisResult.builder()
                .success(true)
                .category(extractCategory(state))
                .rootCauseAnalysis(extractValue(state, "root_cause_analysis"))
                .impactAssessment(extractValue(state, "impact_assessment"))
                .solutionProposal(extractValue(state, "solution_proposal"))
                .finalReport(extractValue(state, "final_report"))
                .build();
    }

    private TicketCategory extractCategory(OverAllState state) {
        // 从分类结果中提取类别
        return state.value("classification_result")
                .map(v -> {
                    // 解析分类结果，提取类别
                    String resultText = v instanceof AssistantMessage ?
                            ((AssistantMessage) v).getText() : v.toString();
                    // 简单的关键词匹配（实际应该更智能）
                    if (resultText.contains("BUG") || resultText.contains("Bug") || resultText.contains("bug")) {
                        return TicketCategory.BUG_REPORT;
                    } else if (resultText.contains("功能") || resultText.contains("建议")) {
                        return TicketCategory.FEATURE_REQUEST;
                    } else if (resultText.contains("性能")) {
                        return TicketCategory.PERFORMANCE_ISSUE;
                    } else if (resultText.contains("UI") || resultText.contains("界面")) {
                        return TicketCategory.UI_UX_ISSUE;
                    } else if (resultText.contains("账号")) {
                        return TicketCategory.ACCOUNT_ISSUE;
                    }
                    return TicketCategory.OTHER;
                })
                .orElse(TicketCategory.OTHER);
    }

    private String extractValue(OverAllState state, String key) {
        return state.value(key)
                .map(v -> v instanceof AssistantMessage ?
                        ((AssistantMessage) v).getText() : v.toString())
                .orElse("");
    }
}

