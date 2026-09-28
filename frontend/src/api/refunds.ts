import { api } from '@/api/client'
import type { PageResult } from '@/api/catalog'
import type { ApiResponse } from '@/types/api'

export type RefundStatus = 'PENDING_REVIEW' | 'COMPLETED' | 'REJECTED'

export interface RefundRequest {
  id: number
  refundNo: string
  orderId: number
  orderNo: string
  amount: number
  reason: string
  status: RefundStatus
  applicantId: number
  applicantName: string
  reviewerId: number | null
  reviewerName: string | null
  reviewRemark: string | null
  appliedAt: string
  reviewedAt: string | null
  createdAt: string
  updatedAt: string
}

export const refundApi = {
  list: async (page: number, keyword: string, status: RefundStatus | '') =>
    (await api.get<ApiResponse<PageResult<RefundRequest>>>('/refunds', {
      params: { page, size: 20, keyword, status: status || undefined },
    })).data.data,
  get: async (id: number) =>
    (await api.get<ApiResponse<RefundRequest>>(`/refunds/${id}`)).data.data,
  create: async (orderId: number, reason: string) =>
    (await api.post<ApiResponse<RefundRequest>>(`/orders/${orderId}/refunds`, { reason })).data.data,
  approve: async (id: number, remark: string) =>
    (await api.post<ApiResponse<RefundRequest>>(`/refunds/${id}/approve`, { remark: remark || null })).data.data,
  reject: async (id: number, remark: string) =>
    (await api.post<ApiResponse<RefundRequest>>(`/refunds/${id}/reject`, { remark: remark || null })).data.data,
}
