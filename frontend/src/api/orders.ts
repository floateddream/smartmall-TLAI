import { api } from '@/api/client'
import type { PageResult } from '@/api/catalog'
import type { ApiResponse } from '@/types/api'

export type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'CANCELLED' | 'REFUNDING' | 'REFUNDED'

export interface OrderItem {
  id: number
  productId: number
  productSku: string
  productName: string
  unitPrice: number
  quantity: number
  subtotal: number
}

export interface Order {
  id: number
  orderNo: string
  customerId: number
  customerName: string
  totalAmount: number
  status: OrderStatus
  items: OrderItem[]
  paidAt: string | null
  cancelledAt: string | null
  refundedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateOrderInput {
  customerId: number
  items: Array<{ productId: number; quantity: number }>
}

export const orderApi = {
  list: async (page: number, keyword: string, status: OrderStatus | '', size = 20) =>
    (await api.get<ApiResponse<PageResult<Order>>>('/orders', {
      params: { page, size, keyword, status: status || undefined },
    })).data.data,
  get: async (id: number) =>
    (await api.get<ApiResponse<Order>>(`/orders/${id}`)).data.data,
  create: async (input: CreateOrderInput) =>
    (await api.post<ApiResponse<Order>>('/orders', input)).data.data,
  pay: async (id: number) =>
    (await api.post<ApiResponse<Order>>(`/orders/${id}/pay`)).data.data,
  cancel: async (id: number) =>
    (await api.post<ApiResponse<Order>>(`/orders/${id}/cancel`)).data.data,
}
