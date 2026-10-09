# 只读 Agent 工具调用说明（阶段 7.1）

## 1. 当前范围

Assistant 的“业务 Agent”模式可以由模型选择并调用一个只读工具：

| 工具 | 用途 | 所需权限 |
|---|---|---|
| `knowledgeBaseSearch` | 检索知识库片段 | `knowledge:view` |
| `queryOrder` | 查询订单列表或详情 | `order:view` |
| `queryRefund` | 查询退款列表或详情 | `refund:view` |
| `queryTicket` | 查询售后工单列表或详情 | `ticket:view` |
| `topRefundProducts` | 统计退款商品排行 | `analytics:view` |
| `refundReasonDistribution` | 统计退款原因分布 | `analytics:view` |

本文件描述阶段 7.1 的只读能力。阶段 7.2 已额外支持“创建售后工单”和“追加工单回复”，但必须经过用户二次确认，详见《受控Agent写操作说明.md》。支付、订单取消、退款审核、删除和权限调整仍不向 Agent 开放。

## 2. 执行链路

```text
用户消息
  -> CHAT 模型输出严格 JSON 计划
  -> ReadOnlyAgentService 校验 action、工具名和参数字段
  -> 从当前 JWT 登录用户读取权限
  -> 调用现有业务 Service / Analytics / Knowledge Search
  -> 写入 agent_tool_audit
  -> CHAT 模型只基于工具结果生成中文回答
  -> 前端展示回答与工具调用轨迹
```

模型不能提供或覆盖用户 ID，也不能直接选择数据库表、字段或 SQL。模型返回未知工具、多余参数、错误类型或越界参数时，请求会被拒绝。

## 3. 审计

Flyway `V12` 创建 `agent_tool_audit` 表。每次真正进入工具执行阶段后，会记录：

- 当前登录用户；
- 工具名称和受控参数；
- 成功或失败状态；
- 工具执行耗时；
- 失败错误码；
- 调用时间。

审计不保存模型 API Key、完整系统提示词或完整工具结果。审计写入失败时会记录后端错误日志，但不会覆盖原始业务结果或异常。

## 4. 页面测试

1. 重启后端，让 Flyway 执行 `V12`。
2. 重新加载前端并进入“AI 助手”，页面默认使用智能模式，不再需要手动打开业务 Agent。
3. 输入“查询最近的订单”“退款原因有哪些”或“最近 30 天退款金额最高的商品”。
4. 回答下方出现 `READ ONLY` 工具轨迹即表示工具已实际执行。

若当前账号缺少对应业务权限，后端返回 `FORBIDDEN`，工具不会执行，但失败尝试仍会进入审计表。
