import { ApiError, api, tokenStorage } from '@/api/client'
import type { ApiResponse } from '@/types/api'
import type { PageResult } from '@/api/catalog'

export type AiModelType = 'CHAT' | 'EMBEDDING'
export type AiProviderCode = 'OPENAI' | 'DEEPSEEK' | 'QWEN' | 'MOONSHOT' | 'ZHIPU' | 'OLLAMA' | 'OPENAI_COMPATIBLE'

export interface AiProviderOption {
  code: AiProviderCode
  name: string
  defaultBaseUrl: string
  supportedTypes: AiModelType[]
  chatModels: string[]
  embeddingModels: string[]
  credentialRequired: boolean
}

export interface AiModelConfig {
  id: number
  name: string
  modelType: AiModelType
  provider: AiProviderCode
  baseUrl: string
  modelName: string
  temperature: number
  maxTokens: number
  embeddingDimensions: number | null
  active: boolean
  enabled: boolean
  credentialConfigured: boolean
  credentialRequired: boolean
  createdAt: string
  updatedAt: string
}

export interface AiModelConfigInput {
  name: string
  modelType: AiModelType
  provider: AiProviderCode
  baseUrl: string
  modelName: string
  apiKey: string
  temperature: number
  maxTokens: number
  embeddingDimensions: number | null
}

export interface AiChatMessage {
  role: 'USER' | 'ASSISTANT'
  content: string
}

export interface AiChatReply {
  content: string
  modelName: string
  latencyMs: number
  sources: AiChatSource[]
  tools: AiAgentTool[]
  pendingAction: AiPendingAction | null
  suggestions: AiSuggestion[]
  responseType: AiResponseType
  missingFields: string[]
}

export type AiResponseType = 'ANSWER' | 'TOOL_RESULT' | 'ACTION_CONFIRMATION' | 'CLARIFICATION' | 'UNSUPPORTED' | 'SYSTEM_ERROR'

export type AiCapabilityType = 'DIRECT' | 'READ_ONLY' | 'CONFIRM_REQUIRED'
export type AiCapabilityCategory = 'GENERAL' | 'KNOWLEDGE' | 'BUSINESS' | 'ANALYTICS'

export interface AiCapability {
  code: string
  displayName: string
  description: string
  category: AiCapabilityCategory
  type: AiCapabilityType
  examples: string[]
}

export interface AiSuggestion {
  label: string
  prompt: string
  capabilityCode: string
}

export interface AiAgentTool {
  toolName: string
  displayName: string
  summary: string
  latencyMs: number
  navigation?: AiAgentNavigation | null
}

export interface AiAgentNavigation {
  path: '/orders' | '/refunds' | '/tickets' | '/knowledge' | '/data-analysis'
  query: Record<string, string>
}

export interface AiPendingAction {
  token: string
  actionName: string
  displayName: string
  summary: string
  expiresAt: string
}

export type AiAgentActionStatus = 'PENDING' | 'EXECUTING' | 'SUCCEEDED' | 'CANCELLED' | 'EXPIRED'

export interface AiAgentActionResult {
  actionName: string
  displayName: string
  summary: string
  status: AiAgentActionStatus
  latencyMs: number
}

export interface AiChatSource {
  reference: number
  documentId: number
  documentName: string
  chunkIndex: number
  excerpt: string
  score: number
}

export type AiConversationStatus = 'ACTIVE' | 'ARCHIVED'

export interface AiConversation {
  id: number
  title: string
  status: AiConversationStatus
  awaitingParameters: boolean
  lastMessageAt: string
  createdAt: string
  updatedAt: string
}

export interface AiConversationMessage extends AiChatMessage {
  id: number
  responseType: AiResponseType | null
  modelName: string | null
  latencyMs: number | null
  tools: AiAgentTool[]
  suggestions: AiSuggestion[]
  missingFields: string[]
  createdAt: string
}

export interface AiConversationTurn {
  conversation: AiConversation
  userMessageId: number
  assistantMessageId: number
  reply: AiChatReply
}

export interface AiAgentAudit {
  id: number
  userId: number | null
  username: string
  toolName: string
  argumentKeys: string[]
  status: 'SUCCESS' | 'FAILED'
  latencyMs: number
  errorCode: string | null
  traceId: string | null
  conversationId: number | null
  createdAt: string
}

export type AiCallScene = 'ASSISTANT_CHAT' | 'KNOWLEDGE_RAG' | 'INTERNAL_ORCHESTRATION'
  | 'KNOWLEDGE_EMBEDDING' | 'MODEL_CONNECTIVITY_TEST'

export interface AiCallLog {
  id: number
  modelConfigId: number | null
  modelType: AiModelType
  provider: AiProviderCode
  modelName: string
  scene: AiCallScene
  status: 'SUCCESS' | 'FAILED'
  latencyMs: number
  inputCount: number
  inputCharacterCount: number
  outputCount: number
  outputUnitCount: number | null
  errorCode: string | null
  traceId: string | null
  conversationId: number | null
  createdAt: string
}

export interface AiCallLogFilters {
  modelType?: string
  provider?: string
  scene?: string
  status?: string
  traceId?: string
}

export interface AiStreamHandlers {
  onDelta: (content: string) => void
  onResult: (turn: AiConversationTurn) => void
}

async function streamConversationMessage(
  conversationId: number,
  content: string,
  handlers: AiStreamHandlers,
  signal?: AbortSignal,
) {
  const token = tokenStorage.get()
  const response = await fetch(`/api/ai/conversations/${conversationId}/messages/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ content }),
    signal,
  })
  if (!response.ok || !response.body) {
    const body = await response.json().catch(() => null) as ApiResponse<unknown> | null
    throw new ApiError(body?.message || '无法建立 AI 流式连接', body?.code || 'STREAM_ERROR', response.status)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  while (true) {
    const { done, value } = await reader.read()
    buffer += decoder.decode(value, { stream: !done })
    buffer = buffer.replace(/\r\n/g, '\n')
    let boundary = buffer.indexOf('\n\n')
    while (boundary >= 0) {
      const block = buffer.slice(0, boundary)
      buffer = buffer.slice(boundary + 2)
      const event = block.split('\n').find(line => line.startsWith('event:'))?.slice(6).trim()
      const dataText = block.split('\n').filter(line => line.startsWith('data:'))
        .map(line => line.slice(5).trimStart()).join('\n')
      if (event && dataText) {
        const data = JSON.parse(dataText) as Record<string, unknown>
        if (event === 'delta') handlers.onDelta(String(data.content || ''))
        if (event === 'result') handlers.onResult(data as unknown as AiConversationTurn)
        if (event === 'error') throw new ApiError(String(data.message || 'AI 请求失败'), String(data.code || 'STREAM_ERROR'))
      }
      boundary = buffer.indexOf('\n\n')
    }
    if (done) break
  }
}

export const aiApi = {
  listCapabilities: async () =>
    (await api.get<ApiResponse<AiCapability[]>>('/ai/capabilities')).data.data,
  listProviders: async () =>
    (await api.get<ApiResponse<AiProviderOption[]>>('/ai/models/providers')).data.data,
  listModels: async () =>
    (await api.get<ApiResponse<AiModelConfig[]>>('/ai/models')).data.data,
  createModel: async (input: AiModelConfigInput) =>
    (await api.post<ApiResponse<AiModelConfig>>('/ai/models', input)).data.data,
  updateModel: async (id: number, input: AiModelConfigInput) =>
    (await api.put<ApiResponse<AiModelConfig>>(`/ai/models/${id}`, input)).data.data,
  activateModel: async (id: number) =>
    (await api.patch<ApiResponse<AiModelConfig>>(`/ai/models/${id}/active`)).data.data,
  changeModelEnabled: async (id: number, enabled: boolean) =>
    (await api.patch<ApiResponse<AiModelConfig>>(`/ai/models/${id}/enabled`, { enabled })).data.data,
  testModel: async (id: number) =>
    (await api.post<ApiResponse<{ success: boolean; reply: string }>>(`/ai/models/${id}/test`, undefined, { timeout: 130000 })).data.data,
  listSystemProviders: async () =>
    (await api.get<ApiResponse<AiProviderOption[]>>('/admin/ai/models/providers')).data.data,
  listSystemModels: async () =>
    (await api.get<ApiResponse<AiModelConfig[]>>('/admin/ai/models')).data.data,
  createSystemModel: async (input: AiModelConfigInput) =>
    (await api.post<ApiResponse<AiModelConfig>>('/admin/ai/models', input)).data.data,
  updateSystemModel: async (id: number, input: AiModelConfigInput) =>
    (await api.put<ApiResponse<AiModelConfig>>(`/admin/ai/models/${id}`, input)).data.data,
  activateSystemModel: async (id: number) =>
    (await api.patch<ApiResponse<AiModelConfig>>(`/admin/ai/models/${id}/active`)).data.data,
  changeSystemModelEnabled: async (id: number, enabled: boolean) =>
    (await api.patch<ApiResponse<AiModelConfig>>(`/admin/ai/models/${id}/enabled`, { enabled })).data.data,
  testSystemModel: async (id: number) =>
    (await api.post<ApiResponse<{ success: boolean; reply: string }>>(`/admin/ai/models/${id}/test`, undefined, { timeout: 130000 })).data.data,
  chat: async (messages: AiChatMessage[], knowledgeEnabled = false, agentEnabled = false) =>
    (await api.post<ApiResponse<AiChatReply>>('/ai/chat', {
      messages,
      knowledgeEnabled,
      agentEnabled,
    }, { timeout: 130000 })).data.data,
  confirmAgentAction: async (token: string) =>
    (await api.post<ApiResponse<AiAgentActionResult>>('/ai/agent/confirm', { token }, { timeout: 130000 })).data.data,
  cancelAgentAction: async (token: string) =>
    (await api.post<ApiResponse<AiAgentActionResult>>('/ai/agent/cancel', { token })).data.data,
  createConversation: async () =>
    (await api.post<ApiResponse<AiConversation>>('/ai/conversations')).data.data,
  listConversations: async (page = 1, size = 30) =>
    (await api.get<ApiResponse<PageResult<AiConversation>>>('/ai/conversations', { params: { page, size } })).data.data,
  listConversationMessages: async (conversationId: number) =>
    (await api.get<ApiResponse<AiConversationMessage[]>>(`/ai/conversations/${conversationId}/messages`)).data.data,
  archiveConversation: async (conversationId: number) =>
    api.delete(`/ai/conversations/${conversationId}`),
  streamConversationMessage,
  listAgentAudits: async (page = 1, size = 20, toolName = '', status = '', traceId = '') =>
    (await api.get<ApiResponse<PageResult<AiAgentAudit>>>('/ai/agent/audits', {
      params: { page, size, toolName: toolName || undefined, status: status || undefined, traceId: traceId || undefined },
    })).data.data,
  getAgentAudit: async (id: number) =>
    (await api.get<ApiResponse<AiAgentAudit>>(`/ai/agent/audits/${id}`)).data.data,
  listCallLogs: async (page = 1, size = 20, filters: AiCallLogFilters = {}) =>
    (await api.get<ApiResponse<PageResult<AiCallLog>>>('/ai/call-logs', {
      params: { page, size, ...filters },
    })).data.data,
  getCallLog: async (id: number) =>
    (await api.get<ApiResponse<AiCallLog>>(`/ai/call-logs/${id}`)).data.data,
}
