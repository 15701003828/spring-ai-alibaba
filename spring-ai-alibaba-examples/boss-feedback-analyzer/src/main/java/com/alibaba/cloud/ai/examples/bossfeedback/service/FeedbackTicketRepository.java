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
import com.alibaba.cloud.ai.examples.bossfeedback.model.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单Repository接口
 */
@Repository
public interface FeedbackTicketRepository extends JpaRepository<FeedbackTicket, Long> {
    
    /**
     * 根据工单ID查找
     */
    FeedbackTicket findByTicketId(String ticketId);
    
    /**
     * 根据用户ID查找
     */
    List<FeedbackTicket> findByUserId(String userId);
    
    /**
     * 根据分类查找
     */
    List<FeedbackTicket> findByCategory(TicketCategory category);
    
    /**
     * 根据时间范围查找
     */
    List<FeedbackTicket> findByFeedbackTimeBetween(LocalDateTime start, LocalDateTime end);
}

