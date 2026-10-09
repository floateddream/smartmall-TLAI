<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import axios from 'axios'
import {
  Activity, ArrowDownRight, ArrowRight, ArrowUpRight, Boxes, CircleAlert,
  CircleCheck, Clock3, RefreshCw, RotateCcw, Ticket,
} from 'lucide-vue-next'
import TrendChart from '@/components/TrendChart.vue'
import { ApiError } from '@/api/client'
import { dashboardApi, type DashboardOverview } from '@/api/dashboard'
import type { OrderStatus } from '@/api/orders'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const health = ref<'loading' | 'online' | 'offline'>('loading')
const healthChecking = ref(false)
const overview = ref<DashboardOverview | null>(null)
const dashboardLoading = ref(false)
const dashboardError = ref('')

const statusPresentation: Record<OrderStatus, { label: string; kind: string }> = {
  PENDING_PAYMENT: { label: '待支付', kind: 'pending' },
  PAID: { label: '已支付', kind: 'success' },
  CANCELLED: { label: '已取消', kind: 'muted' },
  REFUNDING: { label: '退款中', kind: 'warning' },
  REFUNDED: { label: '已退款', kind: 'purple' },
}

function signed(value: number, suffix = '%') {
  return `${value > 0 ? '+' : ''}${value.toFixed(1)}${suffix}`
}

function direction(value: number) {
  return value > 0 ? 'up' : value < 0 ? 'down' : 'flat'
}

const metrics = computed(() => [
  { label: '本周订单', value: overview.value?.weekOrders.toLocaleString('zh-CN') ?? '--', change: signed(overview.value?.weekOrderChangePercent ?? 0), direction: direction(overview.value?.weekOrderChangePercent ?? 0), icon: Boxes, tone: 'teal', compare: '较上周同期' },
  { label: '本周退款申请', value: overview.value?.weekRefunds.toLocaleString('zh-CN') ?? '--', change: signed(overview.value?.weekRefundChangePercent ?? 0), direction: direction(overview.value?.weekRefundChangePercent ?? 0), icon: RotateCcw, tone: 'orange', compare: '较上周同期' },
  { label: '待处理工单', value: overview.value?.pendingTickets.toLocaleString('zh-CN') ?? '--', change: '实时', direction: 'flat', icon: Ticket, tone: 'purple', compare: '当前待办' },
  { label: '支付转化率', value: overview.value ? `${overview.value.weekPaymentRate.toFixed(1)}%` : '--', change: signed(overview.value?.weekPaymentRateChangePoints ?? 0, ' 点'), direction: direction(overview.value?.weekPaymentRateChangePoints ?? 0), icon: CircleCheck, tone: 'green', compare: '较上周同期' },
])

async function checkHealth() {
  healthChecking.value = true
  try {
    const response = await axios.get<{ status: string }>('/actuator/health', { timeout: 5000 })
    health.value = response.data.status === 'UP' ? 'online' : 'offline'
  } catch {
    health.value = 'offline'
  } finally {
    healthChecking.value = false
  }
}

async function loadDashboard() {
  dashboardLoading.value = true
  dashboardError.value = ''
  try {
    overview.value = await dashboardApi.overview()
  } catch (error) {
    dashboardError.value = error instanceof ApiError ? error.message : '仪表盘数据加载失败'
  } finally {
    dashboardLoading.value = false
  }
}

function refreshAll() {
  void Promise.all([loadDashboard(), checkHealth()])
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(value)
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false,
  }).format(new Date(value))
}

function generatedTime() {
  if (!overview.value) return new Date().toLocaleDateString('zh-CN')
  return `更新于 ${new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date(overview.value.generatedAt))}`
}

onMounted(refreshAll)
</script>

<template>
  <div class="dashboard view-shell">
    <div class="page-heading">
      <div>
        <p class="eyebrow">OVERVIEW / 运营总览</p>
        <h1>工作台<span class="heading-spark">.</span></h1>
        <p class="page-heading__subtitle">欢迎回来，{{ auth.user?.username }}。这是当前运营概况。</p>
      </div>
      <div class="page-heading__actions">
        <span class="date-label">{{ generatedTime() }}</span>
        <button type="button" class="icon-button dashboard-refresh" :disabled="dashboardLoading || healthChecking" title="刷新仪表盘" aria-label="刷新仪表盘" @click="refreshAll">
          <RefreshCw :size="16" :class="{ spinning: dashboardLoading || healthChecking }" />
        </button>
      </div>
    </div>

    <div v-if="dashboardError" class="dashboard-feedback" role="alert">
      <CircleAlert :size="16" />{{ dashboardError }}
      <button type="button" class="text-link" @click="loadDashboard">重试</button>
    </div>

    <div class="metrics-grid" :class="{ 'dashboard-data--loading': dashboardLoading }">
      <article v-for="(metric, index) in metrics" :key="metric.label" class="metric" :class="`metric--${metric.tone}`" :style="{ '--delay': `${index * 70}ms` }">
        <div class="metric__head"><span>{{ metric.label }}</span><component :is="metric.icon" :size="20" :stroke-width="1.7" /></div>
        <strong>{{ metric.value }}</strong>
        <div class="metric__bottom">
          <span class="metric__change">
            <ArrowUpRight v-if="metric.direction === 'up'" :size="15" />
            <ArrowDownRight v-else-if="metric.direction === 'down'" :size="15" />
            {{ metric.change }}
          </span>
          <span>{{ metric.compare }}</span>
        </div>
      </article>
    </div>

    <div class="dashboard__middle">
      <section class="data-section trend-section">
        <div class="section-heading"><div><p class="eyebrow">ANALYTICS / 真实数据</p><h2>业务趋势</h2></div><span class="period-label">近 7 天</span></div>
        <div class="chart-legend"><span><i class="legend-dot legend-dot--teal" />订单</span><span><i class="legend-dot legend-dot--orange" />退款</span></div>
        <TrendChart :points="overview?.trend ?? []" />
      </section>
      <section class="data-section health-section">
        <div class="section-heading"><div><p class="eyebrow">SYSTEM / 实时状态</p><h2>服务监测</h2></div><Activity :size="19" /></div>
        <div class="health-status" :class="`health-status--${health}`">
          <span class="health-status__icon"><CircleCheck v-if="health === 'online'" :size="25" /><CircleAlert v-else-if="health === 'offline'" :size="25" /><Clock3 v-else :size="25" /></span>
          <div><strong>{{ health === 'online' ? '服务运行正常' : health === 'offline' ? '服务暂不可用' : '正在检查服务' }}</strong><p>Spring Boot · /actuator/health</p></div>
        </div>
        <div class="health-detail"><span>API 服务</span><span :class="health === 'online' ? 'text-green' : 'text-muted'">{{ health === 'online' ? '在线' : health === 'loading' ? '检查中' : '离线' }}</span></div>
        <div class="health-detail"><span>检查方式</span><span>实时请求</span></div>
        <button type="button" class="outline-button health-refresh" :disabled="healthChecking" @click="checkHealth"><RefreshCw :size="15" :class="{ spinning: healthChecking }" />重新检测</button>
      </section>
    </div>

    <section class="data-section orders-section">
      <div class="section-heading"><div><p class="eyebrow">ACTIVITY / 实时数据</p><h2>近期订单</h2></div><router-link to="/orders" class="text-link">订单管理 <ArrowRight :size="16" /></router-link></div>
      <div class="table-scroll">
        <table class="orders-table">
          <thead><tr><th>订单编号</th><th>商品</th><th>客户</th><th>金额</th><th>状态</th><th>创建时间</th></tr></thead>
          <tbody>
            <tr v-for="order in overview?.recentOrders ?? []" :key="order.id">
              <td class="mono">{{ order.orderNo }}</td><td>{{ order.productSummary }}</td><td>{{ order.customerName }}</td>
              <td class="amount">{{ formatCurrency(order.totalAmount) }}</td>
              <td><span class="status-tag" :class="`status-tag--${statusPresentation[order.status].kind}`"><i />{{ statusPresentation[order.status].label }}</span></td>
              <td class="mono">{{ formatTime(order.createdAt) }}</td>
            </tr>
            <tr v-if="!dashboardLoading && overview && overview.recentOrders.length === 0"><td colspan="6" class="dashboard-empty">暂无订单数据</td></tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>
