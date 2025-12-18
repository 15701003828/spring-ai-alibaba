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
package com.alibaba.cloud.ai.examples.bossfeedback.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量数据库配置
 * 注意：这里使用SimpleMemoryVectorStore作为示例，实际生产环境应使用Milvus Java SDK实现
 * 
 * EmbeddingModel会由spring-ai-alibaba-starter-dashscope自动配置
 * 如果自动配置失败，请检查依赖是否正确添加
 */
@Configuration
public class MilvusConfig {
    
    @Bean
    @ConditionalOnMissingBean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        // 使用自定义的SimpleMemoryVectorStore
        // 生产环境应替换为Milvus实现
        return new SimpleMemoryVectorStore(embeddingModel);
    }
}

