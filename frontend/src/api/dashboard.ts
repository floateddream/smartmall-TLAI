import { api } from '@/api/client'
import type { OrderStatus } from '@/api/orders'
import type { ApiResponse } from '@/types/api'

export interface DashboardTrendPoint {
  date: string
  orderCount: number
  refundCount: number
}

export interface DashboardRecentOrder {
  id: number
  orderNo: string
  productSummary: string
  customerName: string
  totalAmount: number
  status: OrderStatus
  createdAt: string
}

export interface DashboardOverview {
  weekOrders: number
  weekOrderChangePercent: number
  weekRefunds: number
  weekRefundChangePercent: number
  pendingTickets: number
  weekPaymentRate: number
  weekPaymentRateChangePoints: number
  trend: DashboardTrendPoint[]
  recentOrders: DashboardRecentOrder[]
  generatedAt: string
}

export const dashboardApi = {
  overview: async () =>
    (await api.get<ApiResponse<DashboardOverview>>('/dashboard/overview')).data.data,
}
