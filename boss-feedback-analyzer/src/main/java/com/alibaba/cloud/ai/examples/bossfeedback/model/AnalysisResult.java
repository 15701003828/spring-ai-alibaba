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

import java.util.List;

/**
 * 工单分析结果
 */
public class AnalysisResult {
    
    /** 分析是否成功 */
    private boolean success;
    
    /** 结果是否来自缓存 */
    private boolean fromCache;
    
    /** 错误信息（分析失败时返回） */
    private String errorMessage;
    
    /** 工单分类（Bug、功能建议、性能问题等） */
    private TicketCategory category;
    
    /** 根因分析结果 */
    private String rootCauseAnalysis;
    
    /** 影响评估（问题影响范围、严重程度等） */
    private String impactAssessment;
    
    /** 解决方案建议 */
    private String solutionProposal;
    
    /** 最终分析报告（综合所有分析结果的完整报告） */
    private String finalReport;
    
    /** 相似历史工单列表（用于参考历史解决方案） */
    private List<SimilarTicket> similarTickets;
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private AnalysisResult result = new AnalysisResult();
        
        public Builder success(boolean success) {
            result.success = success;
            return this;
        }
        
        public Builder fromCache(boolean fromCache) {
            result.fromCache = fromCache;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            result.errorMessage = errorMessage;
            return this;
        }
        
        public Builder category(TicketCategory category) {
            result.category = category;
            return this;
        }
        
        public Builder rootCauseAnalysis(String rootCauseAnalysis) {
            result.rootCauseAnalysis = rootCauseAnalysis;
            return this;
        }
        
        public Builder impactAssessment(String impactAssessment) {
            result.impactAssessment = impactAssessment;
            return this;
        }
        
        public Builder solutionProposal(String solutionProposal) {
            result.solutionProposal = solutionProposal;
            return this;
        }
        
        public Builder finalReport(String finalReport) {
            result.finalReport = finalReport;
            return this;
        }
        
        public Builder similarTickets(List<SimilarTicket> similarTickets) {
            result.similarTickets = similarTickets;
            return this;
        }
        
        public AnalysisResult build() {
            return result;
        }
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public boolean isFromCache() {
        return fromCache;
    }
    
    public void setFromCache(boolean fromCache) {
        this.fromCache = fromCache;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public TicketCategory getCategory() {
        return category;
    }
    
    public void setCategory(TicketCategory category) {
        this.category = category;
    }
    
    public String getRootCauseAnalysis() {
        return rootCauseAnalysis;
    }
    
    public void setRootCauseAnalysis(String rootCauseAnalysis) {
        this.rootCauseAnalysis = rootCauseAnalysis;
    }
    
    public String getImpactAssessment() {
        return impactAssessment;
    }
    
    public void setImpactAssessment(String impactAssessment) {
        this.impactAssessment = impactAssessment;
    }
    
    public String getSolutionProposal() {
        return solutionProposal;
    }
    
    public void setSolutionProposal(String solutionProposal) {
        this.solutionProposal = solutionProposal;
    }
    
    public String getFinalReport() {
        return finalReport;
    }
    
    public void setFinalReport(String finalReport) {
        this.finalReport = finalReport;
    }
    
    public List<SimilarTicket> getSimilarTickets() {
        return similarTickets;
    }
    
    public void setSimilarTickets(List<SimilarTicket> similarTickets) {
        this.similarTickets = similarTickets;
    }
    
    /**
     * 相似工单
     */
    public static class SimilarTicket {
        /** 工单ID */
        private String ticketId;
        
        /** 用户诉求 */
        private String userRequest;
        
        /** 问题描述 */
        private String problemDescription;
        
        /** 相似度（0-1之间，越大越相似） */
        private double similarity;
        
        public SimilarTicket(String ticketId, String userRequest, String problemDescription, double similarity) {
            this.ticketId = ticketId;
            this.userRequest = userRequest;
            this.problemDescription = problemDescription;
            this.similarity = similarity;
        }
        
        // Getters
        public String getTicketId() {
            return ticketId;
        }
        
        public String getUserRequest() {
            return userRequest;
        }
        
        public String getProblemDescription() {
            return problemDescription;
        }
        
        public double getSimilarity() {
            return similarity;
        }
    }
}

