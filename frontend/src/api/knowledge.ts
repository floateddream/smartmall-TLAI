import type { AxiosProgressEvent } from 'axios'
import { api } from '@/api/client'
import type { ApiResponse } from '@/types/api'
import type { PageResult } from '@/api/catalog'

export type KnowledgeDocumentStatus = 'UPLOADED' | 'PROCESSING' | 'READY' | 'FAILED'

export interface KnowledgeDocument {
  id: number
  originalName: string
  contentType: string
  sizeBytes: number
  fileExtension: string
  status: KnowledgeDocumentStatus
  chunkCount: number
  errorMessage: string | null
  uploadedBy: number
  uploaderName: string
  createdAt: string
  updatedAt: string
}

export const knowledgeApi = {
  list: async (page: number, keyword: string, status: KnowledgeDocumentStatus | '', size = 20) =>
    (await api.get<ApiResponse<PageResult<KnowledgeDocument>>>('/knowledge/documents', {
      params: { page, size, keyword, status: status || undefined },
    })).data.data,

  upload: async (file: File, onProgress: (progress: number) => void) => {
    const form = new FormData()
    form.append('file', file)
    return (await api.post<ApiResponse<KnowledgeDocument>>('/knowledge/documents', form, {
      timeout: 120_000,
      onUploadProgress: (event: AxiosProgressEvent) => {
        if (event.total) onProgress(Math.round((event.loaded * 100) / event.total))
      },
    })).data.data
  },

  download: async (id: number) =>
    (await api.get<Blob>(`/knowledge/documents/${id}/download`, {
      responseType: 'blob',
      timeout: 120_000,
    })).data,

  delete: async (id: number) => {
    await api.delete(`/knowledge/documents/${id}`)
  },

  retry: async (id: number) => {
    await api.post(`/knowledge/documents/${id}/retry`)
  },
}
