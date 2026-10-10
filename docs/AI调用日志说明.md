# AI 调用日志说明

## 目标

阶段 8.1 在不记录敏感内容的前提下，为 CHAT 与 EMBEDDING 模型调用提供统一、可筛选的运行日志。日志用于管理员排查供应商错误、模型配置问题、调用耗时和调用规模，不承担会话归档或业务审计职责。

## 调用链

- `AiChatService`：记录助手对话、知识库 RAG 和内部编排调用。
- `AiEmbeddingService`：按一次批量向量化任务记录输入条数、已输出向量数和维度单元总数。
- `AiModelConfigService`：记录 CHAT、EMBEDDING 模型连通性测试。
- `AiCallLogService`：统一构造安全日志、校验筛选条件，并通过仓储端口写入或分页查询。
- `MybatisAiCallLogRepository`：在 infrastructure 模块实现 `ai_call_log` 的 MyBatis 持久化。

业务调用完成后写日志；日志持久化异常会写入服务端错误日志，但不会改变原模型调用的成功或失败结果。

## 数据与脱敏边界

`ai_call_log` 只保存以下元数据：

- 模型类型、供应商、模型名称和模型配置 ID；
- 调用场景、成功/失败状态、耗时和安全错误码；
- 输入项目数、输入字符总数、输出项目数和输出规模。

明确不保存：

- API Key、加密密文或请求鉴权头；
- 完整 Prompt、系统提示词和用户输入；
- 完整模型回答、向量内容或敏感业务查询结果；
- 原始异常消息和供应商响应正文。

计数语义如下：

| 模型类型 | input_count | input_character_count | output_count | output_unit_count |
| --- | --- | --- | --- | --- |
| CHAT | 用户/历史消息条数 | 系统提示词与消息的字符总数 | 成功回答数 | 回答字符数 |
| EMBEDDING | 输入文本条数 | 输入文本字符总数 | 已返回向量数 | 各向量维度之和 |

## 调用场景

- `ASSISTANT_CHAT`：普通 AI 助手对话。
- `KNOWLEDGE_RAG`：带知识库上下文的回答。
- `INTERNAL_ORCHESTRATION`：Agent 规划、数据分析规划与解释等内部编排。
- `KNOWLEDGE_EMBEDDING`：知识文档或检索文本的向量化。
- `MODEL_CONNECTIVITY_TEST`：管理员执行的模型连通性测试。

## 管理接口与权限

Flyway `V15__create_ai_call_log.sql` 创建日志表、索引和 `ai:call_log_view` 权限，并只将该权限授予内置 `ADMIN` 角色。

分页接口：

```text
GET /api/ai/call-logs?page=1&size=20&modelType=CHAT&provider=OPENAI&scene=ASSISTANT_CHAT&status=SUCCESS
```

所有筛选项均可省略；`page` 从 1 开始，`size` 范围为 1～100。非法枚举值返回统一的 `BAD_REQUEST`。接口需要 `ai:call_log_view` 权限。

前端“系统设置 / 调用日志”页面同样按此权限显示，提供模型类型、供应商、调用场景和状态筛选，以及分页、加载、空状态和失败重试。

## 审计详情与安全关联

阶段 8.3 增加单条安全详情接口：

```text
GET /api/ai/call-logs/{id}
```

详情接口与列表接口都需要 `ai:call_log_view`，返回内容仍只有上述运行元数据。一次持久化会话回复会生成 UUID `traceId`，并把明确的 `conversationId` 同时写入该回复产生的模型调用和 Agent 工具审计。管理员可按 `traceId` 精确筛选关联记录；没有关联字段的历史数据保持为空，不使用时间或相似内容猜测。

Flyway `V16__link_ai_audit_records.sql` 为两类审计表增加可空关联字段和索引。前端仅在记录具备 `traceId` 且当前账号拥有目标页面权限时显示跨页入口。

## 验证

后端应使用 JDK 21 与 Maven 3.6.3 以上版本执行：

```powershell
mvn test
```

前端执行：

```powershell
npm run build
```
