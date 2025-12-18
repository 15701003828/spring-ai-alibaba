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

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 简单的内存VectorStore实现
 * 注意：这是一个简化版本，仅用于示例。生产环境应使用专业的向量数据库如Milvus
 */
public class SimpleMemoryVectorStore implements VectorStore {
    
    private final EmbeddingModel embeddingModel;
    private final Map<String, Document> documents = new ConcurrentHashMap<>();
    private final Map<String, float[]> embeddings = new ConcurrentHashMap<>();
    
    public SimpleMemoryVectorStore(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }
    
    @Override
    public void add(List<Document> documents) {
        for (Document doc : documents) {
            String id = doc.getId() != null ? doc.getId() : java.util.UUID.randomUUID().toString();
            this.documents.put(id, doc);
            
            // 生成嵌入向量
            EmbeddingResponse embeddingResponse = embeddingModel.embedForResponse(List.of(doc.getText()));
            // EmbeddingResponse可能包含多个结果，取第一个
            float[] embedding = embeddingResponse.getResult().getOutput();
            float[] embeddingArray = new float[embedding.length];
            for (int i = 0; i < embedding.length; i++) {
                embeddingArray[i] = embedding[i];
            }
            this.embeddings.put(id, embeddingArray);
        }
    }
    
    @Override
    public void delete(List<String> idList) {
        for (String id : idList) {
            documents.remove(id);
            embeddings.remove(id);
        }
    }

    @Override
    public void delete(Filter.Expression filterExpression) {

    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        if (documents.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 生成查询向量
        String query = request.getQuery();
        EmbeddingResponse queryEmbeddingResponse = embeddingModel.embedForResponse(List.of(query));
        float[] queryEmbedding = queryEmbeddingResponse.getResult().getOutput();
        float[] queryArray = new float[queryEmbedding.length];
        for (int i = 0; i < queryEmbedding.length; i++) {
            queryArray[i] = queryEmbedding[i];
        }
        
        // 计算相似度并排序
        int topK = request.getTopK() != 0 ? request.getTopK() : 4;
        
        List<Document> results = embeddings.entrySet().stream()
            .map(entry -> {
                String id = entry.getKey();
                float[] docEmbedding = entry.getValue();
                double similarity = cosineSimilarity(queryArray, docEmbedding);
                Document doc = documents.get(id);
                // 添加相似度到元数据
                if (doc != null) {
                    doc.getMetadata().put("similarity", similarity);
                }
                return new DocumentWithSimilarity(doc, similarity);
            })
            .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
            .limit(topK)
            .map(dws -> dws.document)
            .collect(Collectors.toList());
        
        return results;
    }
    
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    private static class DocumentWithSimilarity {
        final Document document;
        final double similarity;
        
        DocumentWithSimilarity(Document document, double similarity) {
            this.document = document;
            this.similarity = similarity;
        }
    }
}

