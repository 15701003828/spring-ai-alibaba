# BOSS直聘用户反馈工单分析助手

基于Spring AI Alibaba构建的智能工单分析系统，使用多智能体架构和RAG技术，能够自动分析用户反馈工单，提供问题分类、根因分析、影响评估和解决方案建议。

## 功能特性

### 核心功能
- **智能工单分析**：自动解析用户反馈，提取关键信息
- **多维度分析**：并行执行根因分析、影响评估和解决方案生成
- **相似工单检索**：基于RAG技术检索历史相似工单，识别重复问题
- **图片分析**：支持多模态分析，能够理解用户上传的截图
- **智能分类**：自动将工单分类为Bug、功能建议、性能问题等

### 优化特性
- **并行处理**：使用`ParallelAgent`并行分析多个工单，提升处理效率
- **缓存机制**：对分类结果和相似工单检索结果进行缓存，减少重复计算
- **流式输出**：支持实时流式推送分析结果，提升用户体验
- **人工审核**：集成`HumanInTheLoopHook`，对重要操作进行人工审核
- **上下文压缩**：使用`SummarizationHook`自动压缩长文本工单的上下文

## 快速开始

### 前置要求

- JDK 17+
- Maven 3.6+
- Redis（用于缓存）
- MySQL（可选，用于关系数据库存储）
- DashScope API Key

### 环境变量配置

```bash
# DashScope API Key（必需）
export AI_DASHSCOPE_API_KEY=your_dashscope_api_key

# Redis配置（可选，默认localhost:6379）
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=

# MySQL配置（可选）
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=boss_feedback
export DB_USERNAME=root
export DB_PASSWORD=
```

### 运行应用

1. **进入项目目录**
   ```bash
   cd spring-ai-alibaba-examples/boss-feedback-analyzer
   ```

2. **配置环境变量**
   ```bash
   export AI_DASHSCOPE_API_KEY=your_api_key
   ```

3. **启动应用**
   ```bash
   mvn spring-boot:run
   ```

4. **访问应用**
   - API地址：http://localhost:8080
   - 健康检查：http://localhost:8080/actuator/health

## API使用示例

### 1. 同步分析工单

```bash
curl -X POST http://localhost:8080/api/tickets/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "userRequest": "登录失败",
    "problemDescription": "使用手机号登录时提示密码错误，但密码确认无误",
    "phoneModel": "iPhone 14 Pro",
    "appVersion": "8.5.0",
    "feedbackTime": "2024-01-15T10:30:00",
    "screenshots": []
  }'
```

### 2. 流式分析工单

```bash
curl -X POST http://localhost:8080/api/tickets/analyze/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "userId": "user123",
    "userRequest": "登录失败",
    "problemDescription": "使用手机号登录时提示密码错误",
    "phoneModel": "iPhone 14 Pro",
    "appVersion": "8.5.0",
    "feedbackTime": "2024-01-15T10:30:00",
    "screenshots": []
  }'
```

### 3. 批量分析工单

```bash
curl -X POST http://localhost:8080/api/tickets/analyze/batch \
  -H "Content-Type: application/json" \
  -d '[
    {
      "userId": "user123",
      "userRequest": "登录失败",
      "problemDescription": "使用手机号登录时提示密码错误",
      "phoneModel": "iPhone 14 Pro",
      "appVersion": "8.5.0",
      "feedbackTime": "2024-01-15T10:30:00"
    }
  ]'
```

## 技术栈

- **Spring AI Alibaba**: 多智能体框架
- **DashScope**: 大语言模型和嵌入模型
- **Redis**: 缓存服务
- **MySQL**: 关系数据库（可选）
- **VectorStore**: 向量数据库（当前使用SimpleMemoryVectorStore，生产环境建议使用Milvus）

## 多智能体架构

### 1. 工单接收Agent
- 解析用户提交的工单信息
- 提取关键信息
- 分析截图（如果提供）

### 2. 工单分类Agent
- 分析工单内容，确定类别
- 检索历史相似工单（RAG）
- 识别重复工单

### 3. 深度分析Agent（并行）
- **根因分析Agent**：分析问题根本原因
- **影响评估Agent**：评估问题影响范围
- **解决方案Agent**：提供解决方案建议

### 4. 结果生成Agent
- 生成完整分析报告
- 整合所有分析结果
- 提供处理建议

## 许可证

Apache License 2.0

