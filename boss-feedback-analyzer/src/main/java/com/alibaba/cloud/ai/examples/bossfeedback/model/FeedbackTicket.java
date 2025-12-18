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
package com.alibaba.cloud.ai.examples.bossfeedback.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户反馈工单实体
 */
@Entity
@Table(name = "feedback_tickets")
public class FeedbackTicket {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String ticketId;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(length = 2000)
    private String userRequest;        // 用户诉求
    
    @Column(length = 5000)
    private String problemDescription; // 问题描述
    
    @ElementCollection
    @CollectionTable(name = "ticket_screenshots", joinColumns = @JoinColumn(name = "ticket_id"))
    @Column(name = "screenshot_url")
    private List<String> screenshots = new ArrayList<>();   // 截图URL列表
    
    @Column(nullable = false)
    private LocalDateTime feedbackTime; // 反馈时间
    
    private String phoneModel;         // 手机型号
    
    private String appVersion;         // BOSS直聘版本
    
    @Enumerated(EnumType.STRING)
    private TicketCategory category;   // 工单分类（自动分析得出）
    
    @Column(length = 10000)
    private String analysisResult;     // 分析结果
    
    @ElementCollection
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    @CollectionTable(name = "ticket_metadata", joinColumns = @JoinColumn(name = "ticket_id"))
    private Map<String, String> metadata = new HashMap<>(); // 元数据
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt;
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTicketId() {
        return ticketId;
    }
    
    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUserRequest() {
        return userRequest;
    }
    
    public void setUserRequest(String userRequest) {
        this.userRequest = userRequest;
    }
    
    public String getProblemDescription() {
        return problemDescription;
    }
    
    public void setProblemDescription(String problemDescription) {
        this.problemDescription = problemDescription;
    }
    
    public List<String> getScreenshots() {
        return screenshots;
    }
    
    public void setScreenshots(List<String> screenshots) {
        this.screenshots = screenshots;
    }
    
    public LocalDateTime getFeedbackTime() {
        return feedbackTime;
    }
    
    public void setFeedbackTime(LocalDateTime feedbackTime) {
        this.feedbackTime = feedbackTime;
    }
    
    public String getPhoneModel() {
        return phoneModel;
    }
    
    public void setPhoneModel(String phoneModel) {
        this.phoneModel = phoneModel;
    }
    
    public String getAppVersion() {
        return appVersion;
    }
    
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }
    
    public TicketCategory getCategory() {
        return category;
    }
    
    public void setCategory(TicketCategory category) {
        this.category = category;
    }
    
    public String getAnalysisResult() {
        return analysisResult;
    }
    
    public void setAnalysisResult(String analysisResult) {
        this.analysisResult = analysisResult;
    }
    
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

