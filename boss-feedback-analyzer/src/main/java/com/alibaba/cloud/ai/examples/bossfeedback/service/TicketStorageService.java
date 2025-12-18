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
package com.alibaba.cloud.ai.examples.bossfeedback.service;

import com.alibaba.cloud.ai.examples.bossfeedback.model.FeedbackTicket;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 工单存储服务
 */
@Service
public class TicketStorageService {

    @Autowired
    private VectorStore vectorStore;
    
    /**
     * 存储工单到向量数据库
     */
    public void storeTicket(FeedbackTicket ticket) {
        // 1. 构建文档内容用于向量化
        String documentText = buildDocumentText(ticket);
        
        // 2. 创建Document对象
        Document document = new Document(documentText);
        
        // 3. 添加元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("ticketId", ticket.getTicketId());
        metadata.put("userId", ticket.getUserId());
        metadata.put("userRequest", ticket.getUserRequest());
        metadata.put("feedbackTime", ticket.getFeedbackTime().toString());
        metadata.put("phoneModel", ticket.getPhoneModel());
        metadata.put("appVersion", ticket.getAppVersion());
        if (ticket.getCategory() != null) {
            metadata.put("category", ticket.getCategory().name());
        }
        document.getMetadata().putAll(metadata);
        
        // 4. 存储到向量数据库
        vectorStore.add(List.of(document));
    }
    
    /**
     * 构建用于向量化的文档文本
     */
    private String buildDocumentText(FeedbackTicket ticket) {
        return String.format("""
            用户诉求：%s
            问题描述：%s
            手机型号：%s
            APP版本：%s
            反馈时间：%s
            """,
            ticket.getUserRequest() != null ? ticket.getUserRequest() : "",
            ticket.getProblemDescription() != null ? ticket.getProblemDescription() : "",
            ticket.getPhoneModel() != null ? ticket.getPhoneModel() : "",
            ticket.getAppVersion() != null ? ticket.getAppVersion() : "",
            ticket.getFeedbackTime() != null ? ticket.getFeedbackTime().toString() : ""
        );
    }
    
    /**
     * 检索相似工单
     */
    public List<Document> searchSimilarTickets(String query, int topK) {
        return vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build()
        );
    }
    
    /**
     * 生成工单ID
     */
    public String generateTicketId() {
        return "TICKET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

