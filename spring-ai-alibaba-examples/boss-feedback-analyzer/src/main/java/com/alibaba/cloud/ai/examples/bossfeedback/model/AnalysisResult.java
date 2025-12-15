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
 * 分析结果
 */
public class AnalysisResult {
    
    private boolean success;
    private boolean fromCache;
    private String errorMessage;
    private TicketCategory category;
    private String rootCauseAnalysis;
    private String impactAssessment;
    private String solutionProposal;
    private String finalReport;
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
        private String ticketId;
        private String userRequest;
        private String problemDescription;
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

