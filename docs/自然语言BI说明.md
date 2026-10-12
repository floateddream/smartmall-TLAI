# 自然语言 BI 说明

## 1. 当前能力

当前实现已经形成从自然语言问题到图表展示的完整链路。结构化接口适合前端筛选器直接查询；自然语言接口会先让活动对话模型生成受控 Analytics DSL，再执行同一套校验和固定 SQL，最后由模型解读真实统计结果。

```text
结构化 DSL
  -> AnalyticsQueryService 白名单与范围校验
  -> AnalyticsRepository 固定查询端口
  -> MybatisAnalyticsRepository
  -> AnalyticsMapper 预定义 PostgreSQL 聚合 SQL
  -> 指标点列表
```

```text
自然语言问题
  -> 对话模型只生成六字段 JSON 查询计划
  -> Jackson 严格解析（拒绝多余字段和 SQL）
  -> AnalyticsQueryService 再次执行白名单与范围校验
  -> 固定 SQL 返回统计结果
  -> 对话模型基于统计 JSON 生成中文解读
  -> 前端同步筛选条件并展示图表、表格和结论
```

系统不会接收、拼接或执行前端和模型提供的 SQL。

## 2. 模块职责

```text
smartmall-analytics
  model/        指标、维度、时间粒度、查询计划和结果
  application/  组合白名单、日期范围、返回行数校验、查询路由、自然语言编排和审计
  repository/   聚合查询、结果缓存和审计持久化端口

smartmall-infrastructure
  persistence/analytics/  固定 MyBatis SQL、审计表与端口实现
  cache/                  Redis 分析结果缓存适配器

smartmall-web
  analytics/  HTTP 请求、响应 DTO 和权限检查
```

## 3. DSL 字段

| 字段 | 可选值或限制 |
|---|---|
| `metric` | `ORDER_COUNT`、`REFUND_AMOUNT`、`REFUND_COUNT`、`TICKET_COUNT` |
| `dimension` | `DATE`、`PRODUCT`、`REFUND_REASON`、`TICKET_STATUS`、`TICKET_CATEGORY` |
| `granularity` | `DAY`、`WEEK`、`MONTH`，不传时默认 `DAY` |
| `startDate` / `endDate` | 上海时区自然日，包含首尾日期，最长 366 天，不能查询未来 |
| `limit` | 1 到 50，不传时默认 20 |

允许的指标与维度组合：

| 指标 | 维度 |
|---|---|
| `ORDER_COUNT` | `DATE` |
| `REFUND_AMOUNT` | `DATE`、`PRODUCT` |
| `REFUND_COUNT` | `REFUND_REASON` |
| `TICKET_COUNT` | `DATE`、`TICKET_STATUS`、`TICKET_CATEGORY` |

按日期分组时，时间桶数量不能超过 `limit`，避免趋势结果被静默截断。调用方应提高 `limit` 或把粒度从天调整为周/月。

## 4. 结构化查询接口

```http
POST /api/analytics/query
Authorization: Bearer <token>
Content-Type: application/json
```

权限：`analytics:view`。Flyway `V10` 会创建权限并授予内置 `ADMIN` 角色。

示例：最近三个月已完成退款的商品金额排行。

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

退款金额趋势和商品排行只统计状态为 `COMPLETED` 的全额退款；商品金额使用订单明细小计汇总。退款原因分布统计申请次数，工单趋势按创建时间统计。

## 5. 自然语言分析接口

```http
POST /api/analytics/natural-language
Authorization: Bearer <token>
Content-Type: application/json
```

权限同样为 `analytics:view`。请求示例：

```json
{
  "question": "最近三个月哪些商品退款金额最高？"
}
```

返回内容包含模型生成并通过后端校验的 DSL、真实统计点、AI 解读、模型名称和整条链路耗时。模型输出的查询计划只能包含 `metric`、`dimension`、`granularity`、`startDate`、`endDate`、`limit` 六个字段；字段缺失、多余、枚举错误、日期越界或非法指标组合都会被拒绝。

自然语言分析会调用活动的 `CHAT` 模型两次：第一次负责问题到 DSL 的规划，第二次只基于真实统计结果生成解读。因此使用该功能前，需要先在“模型配置”中启用一个可用的对话模型。

## 6. 前端页面

页面地址为 `/data-analysis`，位于“智能工作台 / 数据分析”。页面同时提供：

- “问数据”自然语言输入和常用问题入口；
- 七种固定分析主题、日期范围、时间粒度和返回条数；
- 时间折线图、分类横向柱状图、摘要指标和结果明细表；
- 自然语言查询后的模型解读及查询条件自动回填。

## 7. 安全边界

- 模型和前端都不能提供 SQL，也不能选择数据库表名或列名；
- 模型查询计划必须通过与结构化接口完全相同的后端校验；
- 商品名等数据库标签按不可信文本处理，不能覆盖模型系统指令；
- 当前 BI 只读，不执行订单、退款、工单或库存写操作；
- 问题最长 500 字，日期范围最长 366 天，结果最多 50 行。

## 8. Redis 结果缓存

通过校验后的 Analytics DSL 会生成稳定缓存键。系统先查询 Redis，命中时直接返回结果并把响应中的 `cached` 标记为 `true`；未命中时查询 PostgreSQL，再把结果写入 Redis。默认有效期为 5 分钟，可通过 `SMARTMALL_ANALYTICS_CACHE_TTL` 调整。

缓存只用于加速，不作为业务数据源。Redis 读取、反序列化或写入失败时，后端会记录警告日志并自动回源 PostgreSQL，分析请求不会因缓存故障而失败。

## 9. 查询与模型调用审计

Flyway `V11` 创建 `analytics_query_audit` 表以及 `analytics:audit_view` 权限，并默认授予内置 `ADMIN` 角色。每次结构化或自然语言分析都会记录用户、DSL 摘要、结果数、缓存命中、模型、耗时、状态和错误码，不记录模型密钥或完整提示词。

```http
GET /api/analytics/audits?page=1&size=20&queryMode=NATURAL_LANGUAGE&status=SUCCESS
Authorization: Bearer <token>
```

`queryMode` 可选 `STRUCTURED`、`NATURAL_LANGUAGE`，`status` 可选 `SUCCESS`、`FAILED`。接口需要 `analytics:audit_view` 权限；前端仅向拥有该权限的用户展示审计区。
