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
}

export const aiApi = {
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
  chat: async (messages: AiChatMessage[]) =>
    (await api.post<ApiResponse<AiChatReply>>('/ai/chat', { messages }, { timeout: 70000 })).data.data,
}
