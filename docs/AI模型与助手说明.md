# AI 模型、助手与 RAG 说明

## 1. 功能边界

当前实现提供模型调用的完整基础链路：管理员维护对话和 Embedding 模型配置，分别选择活动模型，
普通后台用户通过统一聊天接口调用对话模型，知识库处理任务通过 Embedding 模型生成文档检索向量。

```text
AssistantView
  -> POST /api/ai/chat
  -> 普通模式：AiChatService
  -> 知识库增强：RagChatService
       -> KnowledgeSearchService
       -> bge-m3 生成问题向量
       -> pgvector 余弦相似度检索
       -> AiChatService 注入资料并生成引用式回答
  -> AiChatGateway
  -> SpringAiChatGateway
  -> OpenAI Compatible API

Embedding test / knowledge indexing
  -> AiModelConfigService / AiEmbeddingService
  -> AiEmbeddingGateway
  -> SpringAiEmbeddingGateway
  -> POST {baseUrl}/embeddings
```

文档上传后会使用活动的 Embedding 模型生成向量并写入 `knowledge_chunk`。AI 助手会根据用户意图选择通用问答、知识库检索或受控业务工具，并把会话、消息和多轮参数状态持久化。详细流程见《AI助手会话持久化与SSE说明.md》和《只读Agent工具调用说明.md》。

## 2. 模块职责

```text
smartmall-ai
  application/  模型配置与聊天用例
  model/        AI 领域对象
  repository/   模型配置持久化端口
  gateway/      模型供应商调用端口和 Spring AI 实现

smartmall-infrastructure
  persistence/ai/  MyBatis 模型配置持久化实现

smartmall-knowledge
  问题向量化和 pgvector 相似度检索

smartmall-rag
  检索上下文组装、来源编号和引用式回答编排

smartmall-web
  ai/  模型管理与聊天 HTTP 接口

smartmall-application
  V6__create_ai_model_config.sql
  V7__add_ai_provider_credentials.sql
  V8__add_embedding_model_support.sql
```

## 3. API Key 加密

每个模型的 API Key 由管理员在模型配置页面录入。后端使用 AES-256-GCM 加密后保存到
`ai_model_config.api_key_ciphertext`，查询接口只返回 `credentialConfigured`，不会返回明文或密文。

后端仍需要一个主加密密钥。它只用于加解密供应商 API Key，不会发送给模型供应商。
本地学习环境可以直接设置一段不少于 32 个 ASCII 字符的文本：

```yaml
smartmall:
  ai:
    master-key: "smartmall-tlai-ai-master-key-2026"
    request-timeout: 60s
```

如果用于正式环境，建议在 PowerShell 中生成随机密钥：

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$masterKey = [Convert]::ToBase64String($bytes)
$rng.Dispose()
$masterKey
```

把文本或生成结果配置到被 Git 忽略的 `application-local.yml`，注意合并到已有的 `smartmall:` 节点：

```yaml
smartmall:
  ai:
    master-key: 上一步生成的Base64文本
    request-timeout: 60s
```

生产环境使用环境变量：

```text
SMARTMALL_AI_MASTER_KEY
SMARTMALL_AI_REQUEST_TIMEOUT
```

主密钥配置一次后不要随意更换，否则数据库中的旧 API Key 将无法解密。确需轮换时，应先实现密钥轮换流程，或在更换后由管理员重新录入每个模型的 API Key。

## 4. 模型配置

模型配置页面：

```text
http://127.0.0.1:5173/models
```

配置项：

| 字段 | 示例 | 说明 |
|---|---|---|
| 配置名称 | DeepSeek Chat | 后台识别名称，必须唯一 |
| 模型类型 | `CHAT` | 对话模型生成文本，`EMBEDDING` 模型生成检索向量 |
| 供应商 | DeepSeek | 内置 OpenAI、DeepSeek、通义千问、Moonshot、智谱和自定义兼容接口 |
| 接口地址 | `https://api.deepseek.com/v1` | 包含版本前缀，不包含 `/chat/completions` |
| 模型名称 | `deepseek-chat` | 可从供应商预设中选择，也可手工输入模型 ID |
| API Key | `sk-...` | 新增或切换供应商时必填；同供应商编辑时留空表示保留原密钥 |
| 温度 | `0.7` | 取值 0 到 2 |
| 最大输出 | `2048` | 取值 128 到 32768 |
| 向量维度 | `1024` | 仅 Embedding 使用，必须与模型实际返回维度一致 |

数据库使用按模型类型划分的部分唯一索引，保证 `CHAT` 和 `EMBEDDING` 各自最多只有一个活动模型。
激活 Embedding 模型不会取消当前 DeepSeek 对话模型。激活操作还使用 PostgreSQL 事务级 advisory lock，
避免并发切换产生冲突。没有 API Key 的历史远程模型会保留，但必须补录密钥后才能测试或激活。

### Ollama bge-m3

本地或自建 Ollama 使用 OpenAI 兼容的 Embedding 接口，不需要真实 API Key：

| 字段 | 开发环境示例 |
|---|---|
| 模型类型 | `EMBEDDING` |
| 供应商 | `Ollama` |
| 接口地址 | `http://<服务器公网IP>:11434/v1` |
| 模型名称 | `bge-m3` |
| 向量维度 | `1024` |

源码中的默认地址使用 `http://localhost:11434/v1`，不会硬编码具体服务器 IP。开发时在后台填写实际地址；
后端部署进同一个 Docker 网络后，地址可改为 `http://ollama:11434/v1`。Ollama 的 OpenAI 兼容客户端仍要求
Authorization 字段非空，后端会在内存中使用固定占位值 `ollama`，不会写入数据库。

## 5. 接口与权限

| 方法 | 地址 | 权限 | 用途 |
|---|---|---|---|
| `GET` | `/api/admin/ai/models/providers` | `system:model_manage` | 查询供应商和模型预设 |
| `GET` | `/api/admin/ai/models` | `system:model_manage` | 查询模型配置 |
| `POST` | `/api/admin/ai/models` | `system:model_manage` | 新增模型配置 |
| `PUT` | `/api/admin/ai/models/{id}` | `system:model_manage` | 编辑模型配置 |
| `PATCH` | `/api/admin/ai/models/{id}/active` | `system:model_manage` | 切换当前模型 |
| `PATCH` | `/api/admin/ai/models/{id}/enabled` | `system:model_manage` | 启停模型配置 |
| `POST` | `/api/admin/ai/models/{id}/test` | `system:model_manage` | 发起真实连通性测试 |
| `POST` | `/api/ai/chat` | `ai:chat`；增强模式另需 `knowledge:view` | 普通对话或知识库增强对话 |

V6 会把 `ai:chat` 授予内置 `ADMIN` 和 `OPERATOR`。前端最多发送最近 30 条消息，每条不超过 4000 个字符。

## 6. 页面

- `/models`：对话/向量模型选择、API Key 录入、配置编辑、分类激活和真实连通性测试。
- `/assistant`：可切换普通对话与知识库增强模式；增强回答展示文档、片段编号和相似度。
- `/knowledge`：MinIO 文档管理、解析、分段、向量化和处理状态。

AI 助手会话已经在服务端持久化，刷新页面后可恢复历史消息；具体流程见《AI助手会话持久化与SSE说明.md》。

## 7. 排错日志

- 页面操作失败会显示 Element Plus 错误消息。
- 浏览器按 `F12` 打开 Console，可以看到请求方法、接口地址、HTTP 状态、业务错误码和操作位置。
- 后端控制台会记录模型新增、编辑、启停、激活、连通性测试及聊天的开始和结果。
- 供应商调用失败会记录配置 ID、供应商、模型、接口地址、耗时和异常堆栈。
- 日志不会打印 API Key、加密密文或完整对话内容。

常见日志前缀：

```text
AI model connectivity test started
AI provider call failed
AI embedding call failed
AI embedding connectivity test succeeded
AI chat started
Knowledge search completed
RAG chat succeeded
Business request rejected
```
