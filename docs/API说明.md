# API 说明

所有接口前缀为 `/api`，返回统一结构 `{ "success": true, "data": ... }`；失败返回稳定错误码和面向用户的消息。除注册、登录外均需 `Authorization: Bearer <token>`。下表列出主要入口，实际 DTO 约束以代码为准。

| 模块 | 方法与路径 | 主要权限 | 说明 |
|---|---|---|---|
| 认证 | `POST /auth/register`、`POST /auth/login` | 公开 | 注册普通运营账号、登录 |
| 认证 | `GET /auth/me` | 已认证 | 当前用户、角色和权限 |
| 商品 | `GET/POST/PUT /products...` | `product:view/manage` | 商品查询与维护 |
| 客户 | `GET/POST/PUT /customers...` | `customer:view/manage` | 客户查询与维护 |
| 订单 | `GET/POST /orders...` | `order:*` | 创建、支付、取消、查询 |
| 退款 | `/orders/{id}/refunds`、`/refunds/{id}/approve|reject` | `refund:*` | 全额退款申请与审核 |
| 工单 | `/tickets...` | `ticket:*` | 创建、回复和状态更新 |
| 知识库 | `/knowledge/documents...` | `knowledge:view/manage` | 上传、下载、删除、重试 |
| 模型 | `/ai/models...` | `system:model_manage` | 配置、启停、激活、连通测试 |
| 对话 | `POST /ai/chat` | `ai:chat` | 普通、RAG 或 Agent 模式 |
| 会话 | `/ai/conversations...` | `ai:chat` | 会话列表、消息和 SSE |
| 确认 | `POST /ai/agent/confirm|cancel` | `ai:chat` + 工具权限 | 高风险写操作二次确认 |
| 分析 | `POST /analytics/query` | `analytics:view` | 固定 DSL 查询 |
| 分析 | `POST /analytics/natural-language` | `analytics:view` | 自然语言规划与解释 |
| 审计 | `GET /ai/agent-audits...` | `agent:audit_view` | Agent 安全摘要 |
| 调用日志 | `GET /ai/call-logs...` | `ai:call_log_view` | 模型调用安全元数据 |

## 示例：受控 Analytics DSL

```json
{
  "metric": "REFUND_AMOUNT",
  "dimension": "PRODUCT",
  "granularity": "MONTH",
  "startDate": "2026-07-01",
  "endDate": "2026-09-30",
  "limit": 10
}
```

服务端只接受已定义枚举与合法组合，日期跨度不超过 366 天，`limit` 为 1～50。任何额外字段（包括 `sql`）都会被拒绝。

## 示例：AI 对话

```json
{
  "messages": [{ "role": "USER", "content": "查询订单号 SM202609240001" }],
  "knowledgeEnabled": false,
  "agentEnabled": true
}
```

Agent 响应可能是 `ANSWER`、`TOOL_RESULT`、`ACTION_CONFIRMATION`、`CLARIFICATION`、`UNSUPPORTED` 或 `SYSTEM_ERROR`。确认令牌应当作短时敏感值，只提交给确认/取消接口，不写日志。

## 分页和错误

分页通常使用 `page`（从 1 开始）与 `size`（最大 100）。常见错误包括 `BAD_REQUEST`、`INVALID_CREDENTIALS`、`FORBIDDEN`、资源不存在、状态冲突、Agent 确认过期或进行中。客户端不得根据中文消息分支，应使用错误码。
