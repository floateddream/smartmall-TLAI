import { api } from '@/api/client'
import type { PageResult } from '@/api/catalog'
import type { ApiResponse } from '@/types/api'

export type AnalyticsMetric = 'ORDER_COUNT' | 'REFUND_AMOUNT' | 'REFUND_COUNT' | 'TICKET_COUNT'
export type AnalyticsDimension = 'DATE' | 'PRODUCT' | 'REFUND_REASON' | 'TICKET_STATUS' | 'TICKET_CATEGORY'
export type AnalyticsGranularity = 'DAY' | 'WEEK' | 'MONTH'

export interface AnalyticsQuery {
  metric: AnalyticsMetric
  dimension: AnalyticsDimension
  granularity: AnalyticsGranularity
  startDate: string
  endDate: string
  limit: number
}

export interface AnalyticsPoint {
  label: string
  value: number
}

export interface AnalyticsResult extends AnalyticsQuery {
  points: AnalyticsPoint[]
  generatedAt: string
  cached: boolean
}

export interface NaturalLanguageAnalyticsResult extends AnalyticsResult {
  question: string
  explanation: string
  modelName: string
  latencyMs: number
}

export type AnalyticsQueryMode = 'STRUCTURED' | 'NATURAL_LANGUAGE'
export type AnalyticsAuditStatus = 'SUCCESS' | 'FAILED'

export interface AnalyticsAudit {
  id: number
  userId: number | null
  username: string
  queryMode: AnalyticsQueryMode
  question: string | null
  metric: AnalyticsMetric | null
  dimension: AnalyticsDimension | null
  granularity: AnalyticsGranularity | null
  startDate: string | null
  endDate: string | null
  resultLimit: number | null
  resultCount: number | null
  cacheHit: boolean
  modelName: string | null
  latencyMs: number
  status: AnalyticsAuditStatus
  errorCode: string | null
  createdAt: string
}

export const analyticsApi = {
  query: async (input: AnalyticsQuery) =>
    (await api.post<ApiResponse<AnalyticsResult>>('/analytics/query', input)).data.data,
  naturalLanguage: async (question: string) =>
    (await api.post<ApiResponse<NaturalLanguageAnalyticsResult>>(
      '/analytics/natural-language', { question }, { timeout: 130_000 },
    )).data.data,
  listAudits: async (page = 1, queryMode: AnalyticsQueryMode | '' = '', status: AnalyticsAuditStatus | '' = '') =>
    (await api.get<ApiResponse<PageResult<AnalyticsAudit>>>('/analytics/audits', {
      params: { page, size: 10, queryMode: queryMode || undefined, status: status || undefined },
    })).data.data,
}
