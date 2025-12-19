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
package com.alibaba.cloud.ai.examples.bossfeedback.agent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
import com.alibaba.cloud.ai.graph.agent.hook.toolcalllimit.ToolCallLimitHook;
//import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * BOSS直聘工单分析多智能体系统
 */
@Component
public class TicketAnalysisAgentSystem {

    @Autowired
    private VectorStore vectorStore;
    
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;
    
    private ChatModel chatModel;
    private SummarizationHook summarizationHook;
    private HumanInTheLoopHook humanInTheLoopHook;
    private ToolCallLimitHook toolCallLimitHook;
    
    @PostConstruct
    public void init() {
		// 初始化ChatModel，显式指定通义千问-Plus模型（qwen-plus）
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(apiKey)
				.build();

		DashScopeChatOptions options = DashScopeChatOptions.builder()
				.withModel("qwen-plus")
				.build();

		this.chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.defaultOptions(options)
				.build();
        
        // 初始化上下文压缩Hook - 处理长文本工单
        this.summarizationHook = SummarizationHook.builder()
            .model(chatModel)
            .maxTokensBeforeSummary(8000) // 当上下文超过8000 tokens时触发压缩
            .messagesToKeep(10) // 保留最近10条消息
            .build();
        
         // 初始化人工审核Hook - 对重要操作进行人工审核（暂时禁用）
         this.humanInTheLoopHook = HumanInTheLoopHook.builder()
             .approvalOn("search_similar_tickets",
                 "请审核是否检索相似工单。对于高优先级工单，需要人工确认。")
             .build();
        
        // 初始化工具调用限制Hook - 防止无限循环
        this.toolCallLimitHook = ToolCallLimitHook.builder()
            .runLimit(30) // 最多30轮工具调用
            .build();
    }
    
    // 1. 工单接收与预处理Agent（带上下文压缩）
    public ReactAgent createTicketReceiverAgent() {
        return ReactAgent.builder()
            .name("ticket_receiver")
            .model(chatModel)
            .instruction("""
                你是一个工单接收助手。你的任务是：
                1. 解析用户提交的工单信息
                2. 提取关键信息：用户诉求、问题描述、设备信息等
                3. 对截图进行描述（如果有）
                4. 将结构化信息传递给下一个Agent
                """)
            .tools(createImageAnalysisTool())
            .outputKey("structured_ticket")
            // .hooks(summarizationHook, toolCallLimitHook) // 暂时禁用Hook排查问题
            // .saver(new MemorySaver()) // 暂时禁用，避免Duplicate @class问题
            .build();
    }
    
    // 2. 工单分类Agent（带缓存优化）
    public ReactAgent createTicketClassifierAgent() {
        return ReactAgent.builder()
            .name("ticket_classifier")
            .model(chatModel)
            .instruction("""
                你是一个工单分类专家。你的任务是：
                1. 分析工单内容，确定工单类别（Bug、功能建议、性能问题等）
                2. 使用search_similar_tickets工具检索历史相似工单
                3. 分析是否为重复工单或已知问题
                4. 提取关键问题点
                """)
            .tools(createCachedSimilarTicketSearchTool())
            .outputKey("classification_result")
            // .hooks(summarizationHook, /*humanInTheLoopHook,*/ toolCallLimitHook) // 暂时禁用Hook排查问题
            // .saver(new MemorySaver()) // 暂时禁用，避免Duplicate @class问题
            .build();
    }
    
    // 3. 深度分析Agent（并行分析多个维度）
    public ParallelAgent createDeepAnalysisAgent() throws GraphStateException {
        // 创建多个专业分析子Agent
        ReactAgent rootCauseAgent = ReactAgent.builder()
            .name("root_cause_analyst")
            .model(chatModel)
            .instruction("""
                你是一个根因分析专家。专注于：
                1. 分析问题的根本原因
                2. 识别技术层面的问题
                3. 评估问题的影响范围
                """)
            .outputKey("root_cause_analysis")
            // .hooks(summarizationHook, toolCallLimitHook) // 暂时禁用Hook排查问题
            // .saver(new MemorySaver()) // 暂时禁用，避免Duplicate @class问题
            .build();
        
        ReactAgent impactAssessmentAgent = ReactAgent.builder()
            .name("impact_assessor")
            .model(chatModel)
            .instruction("""
                你是一个影响评估专家。专注于：
                1. 评估问题对用户的影响程度
                2. 评估问题对业务的影响
                3. 确定问题优先级
                """)
            .outputKey("impact_assessment")
            // .hooks(summarizationHook, toolCallLimitHook) // 暂时禁用Hook排查问题
            // .saver(new MemorySaver()) // 暂时禁用，避免Duplicate @class问题
            .build();
        
        ReactAgent solutionAgent = ReactAgent.builder()
            .name("solution_provider")
            .model(chatModel)
            .instruction("""
                你是一个解决方案专家。专注于：
                1. 提供技术解决方案
                2. 提供临时缓解措施
                3. 提供长期改进建议
                """)
            .outputKey("solution_proposal")
            // .hooks(summarizationHook, toolCallLimitHook) // 暂时禁用Hook排查问题
            // .saver(new MemorySaver()) // 暂时禁用，避免Duplicate @class问题
            .build();
        
        // 使用ParallelAgent并行执行多个分析任务
        return ParallelAgent.builder()
            .name("deep_analysis_parallel")
            .description("并行执行根因分析、影响评估和解决方案生成")
            .subAgents(List.of(rootCauseAgent, impactAssessmentAgent, solutionAgent))
            .mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
            .build();
    }
    
    // 4. 结果生成Agent（带人工审核）
    public ReactAgent createResultGeneratorAgent() {
        return ReactAgent.builder()
            .name("result_generator")
            .model(chatModel)
            .instruction("""
                你是一个专业的报告生成助手。基于前面的分析结果，生成：
                1. 工单摘要
                2. 问题分类和标签
                3. 相似历史工单参考
                4. 问题分析报告
                5. 处理建议
                6. 优先级评估
                """)
            .outputKey("final_report")
            // .hooks(summarizationHook, /*humanInTheLoopHook,*/ toolCallLimitHook) // 暂时禁用Hook排查问题
            // .saver(new MemorySaver()) // 暂时禁用，避免Duplicate @class问题
            .build();
    }
    
    // 主编排Agent：使用SequentialAgent串联所有子Agent
    public SequentialAgent createMainAnalysisAgent() throws GraphStateException {
        return SequentialAgent.builder()
            .name("ticket_analysis_main")
            .description("BOSS直聘用户反馈工单分析助手（优化版）")
            .subAgents(List.of(
                createTicketReceiverAgent(),
                createTicketClassifierAgent(),
                createDeepAnalysisAgent(),
                createResultGeneratorAgent()
            ))
            .build();
    }
    
    // ========== 带缓存的相似工单检索工具 ==========
    
    /**
     * 格式化相似工单信息
     * @param doc 文档对象
     * @return 格式化后的工单信息字符串
     */
    private String formatSimilarTicket(Document doc) {
        Map<String, Object> metadata = doc.getMetadata();
        StringBuilder sb = new StringBuilder();
        
        // 添加工单ID
        String ticketId = (String) metadata.getOrDefault("ticketId", "N/A");
        sb.append("工单ID: ").append(ticketId);
        
        // 添加用户诉求
        String userRequest = (String) metadata.getOrDefault("userRequest", "N/A");
        if (!"N/A".equals(userRequest) && !userRequest.isBlank()) {
            sb.append("\n用户诉求: ").append(userRequest);
        }
        
        // 添加问题描述
        String problemDescription = doc.getText();
        if (problemDescription != null && !problemDescription.isBlank()) {
            sb.append("\n问题描述: ").append(problemDescription);
        }
        
        // 添加相似度信息（如果存在）
        Object similarity = metadata.get("similarity");
        if (similarity != null) {
            double sim = similarity instanceof Number ? 
                ((Number) similarity).doubleValue() : 0.0;
            sb.append(String.format("\n相似度: %.2f%%", sim * 100));
        }
        
        // 添加反馈时间
        String feedbackTime = (String) metadata.getOrDefault("feedbackTime", "N/A");
        if (!"N/A".equals(feedbackTime)) {
            sb.append("\n反馈时间: ").append(feedbackTime);
        }
        
        // 添加其他有用信息
        String phoneModel = (String) metadata.getOrDefault("phoneModel", null);
        if (phoneModel != null && !phoneModel.isBlank()) {
            sb.append("\n设备信息: ").append(phoneModel);
        }
        
        return sb.toString();
    }
    
    private ToolCallback createCachedSimilarTicketSearchTool() {
        class CachedSimilarTicketSearchTool {
            public Response search(Request request) {
                // 1. 检查缓存
//                String cacheKey = "similar_tickets:" + request.query().hashCode();
//                String cachedResult = cacheService.get(cacheKey);
//                if (cachedResult != null) {
//                    return new Response(cachedResult, true);
//                }
                
                // 2. 从向量数据库检索
                List<Document> similarTickets = vectorStore.similaritySearch(
                    SearchRequest.builder()
                        .query(request.query())
                        .topK(5)
                        .build()
                );
                
                // 3. 格式化返回结果
                if (similarTickets.isEmpty()) {
                    return new Response("未找到相似工单", false);
                }
                
                // 使用StringBuilder优化字符串拼接，提取分隔符为常量
                final String SEPARATOR = "\n---\n";
                String results = similarTickets.stream()
                    .map(doc -> formatSimilarTicket(doc))
                    .filter(formatted -> !formatted.isBlank()) // 过滤空结果
                    .collect(Collectors.joining(SEPARATOR));
                
//                // 4. 缓存结果（缓存1小时）
//                cacheService.put(cacheKey, results, java.time.Duration.ofHours(1));
                
                return new Response(results, false);
            }
            
            public record Request(String query) {}
            public record Response(String content, boolean fromCache) {}
        }
        
        CachedSimilarTicketSearchTool tool = new CachedSimilarTicketSearchTool();
        return FunctionToolCallback.builder("search_similar_tickets",
                (Function<CachedSimilarTicketSearchTool.Request, CachedSimilarTicketSearchTool.Response>)
                    request -> tool.search(request))
            .description("从历史工单库中检索相似工单，结果会被缓存以提高性能")
            .inputType(CachedSimilarTicketSearchTool.Request.class)
            .build();
    }
    
    // 图片分析工具（多模态支持）
    private ToolCallback createImageAnalysisTool() {
        class ImageAnalysisTool {
            public Response analyze(Request request) {
                try {
                    ChatClient chatClient = ChatClient.builder(chatModel).build();
                    
                    // 注意：这里简化处理，实际应该支持base64图片
                    String analysis = chatClient.prompt()
                        .user("请分析用户反馈的截图。如果提供了图片，请描述其中的问题。")
                        .call()
                        .content();
                    
                    return new Response(analysis);
                } catch (Exception e) {
                    return new Response("图片分析失败: " + e.getMessage());
                }
            }
            
            public record Request(String imageBase64) {}
            public record Response(String description) {}
        }
        
        ImageAnalysisTool tool = new ImageAnalysisTool();
        return FunctionToolCallback.builder("analyze_screenshot",
                (Function<ImageAnalysisTool.Request, ImageAnalysisTool.Response>)
                    request -> tool.analyze(request))
            .description("分析用户反馈的截图，提取其中的问题信息")
            .inputType(ImageAnalysisTool.Request.class)
            .build();
    }
}

