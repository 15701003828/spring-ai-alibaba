///*
// * Copyright 2024-2025 the original author or authors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.alibaba.cloud.ai.examples.bossfeedback.service;
//
//import com.alibaba.cloud.ai.examples.bossfeedback.model.TicketCategory;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.time.Duration;
//import java.util.Optional;
//
///**
// * 工单缓存服务
// */
//@Service
//public class TicketCacheService {
//
//    private final RedisTemplate<String, String> redisTemplate;
//    private static final String CACHE_PREFIX = "ticket:cache:";
//
//    public TicketCacheService(RedisTemplate<String, String> redisTemplate) {
//        this.redisTemplate = redisTemplate;
//    }
//
//    /**
//     * 获取缓存值
//     */
//    public String get(String key) {
//        return redisTemplate.opsForValue().get(CACHE_PREFIX + key);
//    }
//
//    /**
//     * 设置缓存值（带过期时间）
//     */
//    public void put(String key, String value, Duration ttl) {
//        redisTemplate.opsForValue().set(
//            CACHE_PREFIX + key,
//            value,
//            ttl
//        );
//    }
//
//    /**
//     * 缓存工单分类结果
//     */
//    public void cacheClassification(String ticketContent, TicketCategory category) {
//        String key = "classification:" + ticketContent.hashCode();
//        put(key, category.name(), Duration.ofHours(24));
//    }
//
//    /**
//     * 获取缓存的分类结果
//     */
//    public Optional<TicketCategory> getCachedClassification(String ticketContent) {
//        String key = "classification:" + ticketContent.hashCode();
//        String cached = get(key);
//        if (cached != null) {
//            try {
//                return Optional.of(TicketCategory.valueOf(cached));
//            } catch (IllegalArgumentException e) {
//                return Optional.empty();
//            }
//        }
//        return Optional.empty();
//    }
//
//    /**
//     * 清除缓存
//     */
//    public void evict(String key) {
//        redisTemplate.delete(CACHE_PREFIX + key);
//    }
//}
//
