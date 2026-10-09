import { api } from '@/api/client'
import type { ApiResponse } from '@/types/api'

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

export const aiApi = {
  listCapabilities: async () =>
    (await api.get<ApiResponse<AiCapability[]>>('/ai/capabilities')).data.data,
  listProviders: async () =>
    (await api.get<ApiResponse<AiProviderOption[]>>('/admin/ai/models/providers')).data.data,
  listModels: async () =>
    (await api.get<ApiResponse<AiModelConfig[]>>('/admin/ai/models')).data.data,
  createModel: async (input: AiModelConfigInput) =>
    (await api.post<ApiResponse<AiModelConfig>>('/admin/ai/models', input)).data.data,
  updateModel: async (id: number, input: AiModelConfigInput) =>
    (await api.put<ApiResponse<AiModelConfig>>(`/admin/ai/models/${id}`, input)).data.data,
  activateModel: async (id: number) =>
    (await api.patch<ApiResponse<AiModelConfig>>(`/admin/ai/models/${id}/active`)).data.data,
  changeModelEnabled: async (id: number, enabled: boolean) =>
    (await api.patch<ApiResponse<AiModelConfig>>(`/admin/ai/models/${id}/enabled`, { enabled })).data.data,
  testModel: async (id: number) =>
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
}
