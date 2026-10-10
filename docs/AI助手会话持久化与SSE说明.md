# AI 助手会话持久化与 SSE 说明

## 1. 本阶段解决的问题

阶段 7 收尾把原来只存在于浏览器内存中的聊天升级为可恢复的业务会话，并增加四项能力：

- 会话和消息持久化：刷新页面或重新登录后仍可查看历史对话。
- SSE 流式响应：前端逐段显示回答，不必等到整段文本渲染完再出现。
- 多轮参数状态：创建工单等操作缺参数时，下一轮可以继续补充。
- Agent 审计：管理员可以查看只读工具调用的参数、结果、错误码和耗时。

## 2. 数据表

Flyway 启动时执行 `V14__create_ai_conversation_and_agent_audit_permission.sql`。

### `ai_conversation`

保存会话归属、标题、状态和多轮参数状态。`user_id` 是会话所有者，所有查询都同时校验会话 ID 和当前用户 ID。`state_json` 只保存受控意图、已知参数和缺失字段。

### `ai_conversation_message`

保存用户消息和助手消息，并记录模型名、耗时、工具轨迹、推荐问题和缺失字段。前端最多读取最近 200 条消息，发送给模型的上下文最多取最近 30 条。

确认操作的 token 不写入消息表。它是短时、高权限凭证，刷新页面后必须重新生成，避免历史消息被重复执行。

## 3. 接口与调用流程

```text
前端创建会话
  -> POST /api/ai/conversations
  -> 返回 conversationId

前端发送消息
  -> POST /api/ai/conversations/{id}/messages/stream
  -> 后端校验会话所有者和 ACTIVE 状态
  -> 保存用户消息
  -> 读取最近 30 条上下文和 state_json
  -> ReadOnlyAgentService 规划并执行
  -> 保存助手消息并更新/清空 state_json
  -> 通过 SSE 返回回答和完整结果
```

历史接口：

```text
GET    /api/ai/conversations
GET    /api/ai/conversations/{id}/messages
DELETE /api/ai/conversations/{id}
```

`DELETE` 是逻辑归档，不会删除数据库中的会话和消息。

## 4. SSE 事件

| 事件 | 含义 |
| --- | --- |
| `accepted` | 服务端已接收请求 |
| `delta` | 一段可追加到聊天气泡的文本 |
| `result` | 完整回合，包括持久化消息 ID、工具轨迹和会话状态 |
| `done` | 本轮正常结束 |
| `error` | 业务错误或系统错误，包含 `code` 和友好提示 |

当前实现是在模型完成后由后端按片段发送 `delta`，已经具备标准 SSE 传输和渐进渲染。它还不是模型供应商原生 token 流；后续若供应商适配层支持流式接口，可以保持前端事件协议不变，仅替换后端内容来源。

浏览器点击“停止接收”会中断当前 HTTP 流，但不能保证已经开始的模型请求立即取消。服务端仍可能完成并保存消息，重新打开该会话即可看到最终结果。

## 5. 多轮参数状态

当规划器返回 `CLARIFY` 时，系统只保存：

```json
{
  "intent": "createAfterSalesTicket",
  "argumentsJson": "{\"orderNo\":\"SM20260001\",\"category\":\"LOGISTICS\"}",
  "missingFields": ["subject", "description"]
}
```

下一轮规划时，这段状态会作为系统维护的上下文附加到最新用户消息。若仍缺参数则继续更新状态；正常回答、工具查询或生成完整待确认操作后，状态会被清空，避免污染后续问题。

## 6. Agent 审计

管理员菜单 `智能工作台 -> Agent 审计` 使用权限 `agent:audit_view`。页面请求：

```text
GET /api/ai/agent/audits?page=1&size=20&toolName=queryOrder&status=SUCCESS
```

审计表只保存受控工具参数，不保存模型 API Key，也不保存完整工具结果。管理接口只返回参数字段名，不向浏览器发送参数值。支持按工具和 `SUCCESS`、`FAILED` 状态筛选。用户被删除后，历史记录仍可展示用户名快照，`userId` 允许为空。

单条详情接口 `GET /api/ai/agent/audits/{id}` 继续要求 `agent:audit_view`。详情展示工具、操作账号、状态、耗时、安全错误码、参数字段名、会话标识和追踪标识，不展示 Prompt、模型回答或敏感业务数据。

每次会话回复生成一个不可由模型控制的 UUID `traceId`，并与明确的 `conversationId` 一起贯穿 Agent 审计和 AI 调用日志。管理员可从详情跳到相同 `traceId` 的另一类审计记录；只有会话所有者本人同时具备 `ai:chat` 时才显示会话入口，后端仍执行所有权校验。历史数据没有关联字段时只显示“未关联”，不会按时间猜测。

## 7. 手工验证建议

1. 新建会话，发送普通问题，刷新页面后重新打开该会话。
2. 发起缺少描述的创建工单请求，确认左侧显示“待补充”，再补充描述。
3. 完成一轮后再问无关问题，确认旧参数没有被带入。
4. 发起订单或工单查询，在 Agent 审计页查看成功记录、参数和耗时。
5. 用另一个账号访问原会话 ID，确认接口返回“AI 会话不存在”。
6. 归档会话，确认它从列表消失且不能继续发送消息。

## 8. 业务页面联动

只读工具成功后，后端会在 `AgentToolExecution.navigation` 中附加受控导航元数据，并随消息的 `tools_json` 一起持久化。历史 JSON 没有 `navigation` 字段时按 `null` 读取，不影响旧会话。

固定映射如下：

| 工具 | 页面 |
| --- | --- |
| `queryOrder` | `/orders` |
| `queryRefund` | `/refunds` |
| `queryTicket` | `/tickets` |
| `knowledgeBaseSearch` | `/knowledge` |
| `analyticsQuery` 及现有分析能力 | `/data-analysis` |

路径不能由模型参数指定。后端只从已校验的工具参数中挑选允许的 `id`、`keyword`、`status`、`category`、分析主题和日期等字段；前端跳转前会再次应用同一组路径白名单，目标页面只读取本页面认可的参数。未知键、非法枚举、超长关键字、非正整数 ID 和无效日期范围都会被忽略。

手工验证时，可让助手查询一个具体订单、退款或工单，点击工具卡片的“查看”按钮，确认目标页面自动筛选或打开详情；也可以手工在 URL 中加入 `redirect`、未知状态或非法 ID，确认页面不会跳转到其他地址，也不会应用非法条件。
