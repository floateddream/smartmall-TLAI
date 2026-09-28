import { api } from '@/api/client'
import type { PageResult } from '@/api/catalog'
import type { ApiResponse } from '@/types/api'

export type TicketCategory = 'RETURN_EXCHANGE' | 'LOGISTICS' | 'PRODUCT_QUALITY' | 'OTHER'
export type TicketPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'
export type TicketStatus = 'OPEN' | 'PROCESSING' | 'RESOLVED' | 'CLOSED'
export type TicketMessageType = 'REPLY' | 'STATUS_CHANGE'

export interface TicketMessage {
  id: number
  type: TicketMessageType
  senderId: number
  senderName: string
  content: string | null
  fromStatus: TicketStatus | null
  toStatus: TicketStatus | null
  createdAt: string
}

export interface Ticket {
  id: number
  ticketNo: string
  orderId: number
  orderNo: string
  customerId: number
  customerName: string
  category: TicketCategory
  priority: TicketPriority
  status: TicketStatus
  subject: string
  description: string
  creatorId: number
  creatorName: string
  resolvedAt: string | null
  closedAt: string | null
  createdAt: string
  updatedAt: string
  messages: TicketMessage[]
}

export interface CreateTicketInput {
  orderId: number
  category: TicketCategory
  priority: TicketPriority
  subject: string
  description: string
}

export const ticketApi = {
  list: async (page: number, keyword: string, status: TicketStatus | '', category: TicketCategory | '') =>
    (await api.get<ApiResponse<PageResult<Ticket>>>('/tickets', {
      params: { page, size: 20, keyword, status: status || undefined, category: category || undefined },
    })).data.data,
  get: async (id: number) =>
    (await api.get<ApiResponse<Ticket>>(`/tickets/${id}`)).data.data,
  create: async (input: CreateTicketInput) =>
    (await api.post<ApiResponse<Ticket>>('/tickets', input)).data.data,
  addMessage: async (id: number, content: string) =>
    (await api.post<ApiResponse<Ticket>>(`/tickets/${id}/messages`, { content })).data.data,
  updateStatus: async (id: number, status: TicketStatus, remark: string) =>
    (await api.patch<ApiResponse<Ticket>>(`/tickets/${id}/status`, { status, remark: remark || null })).data.data,
}
