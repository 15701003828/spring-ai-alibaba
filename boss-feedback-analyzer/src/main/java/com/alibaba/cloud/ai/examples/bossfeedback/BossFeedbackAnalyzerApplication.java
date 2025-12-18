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
package com.alibaba.cloud.ai.examples.bossfeedback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * BOSS直聘用户反馈工单分析助手主应用
 */
@Slf4j
@SpringBootApplication(scanBasePackages = {"com.alibaba"})
public class BossFeedbackAnalyzerApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(BossFeedbackAnalyzerApplication.class, args);
            log.info("<--- BossFeedbackAnalyzerApplication Application Startup Success --->");
        } catch (Exception ex) {
            log.error("BossFeedbackAnalyzerApplication Application Startup Error", ex);
        }
    }
}

