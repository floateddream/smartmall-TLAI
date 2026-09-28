<script setup lang="ts">
import { onMounted, ref } from 'vue'
import axios from 'axios'
import {
  Activity, ArrowDownRight, ArrowRight, ArrowUpRight, Boxes, CircleAlert,
  CircleCheck, Clock3, RefreshCw, RotateCcw, Ticket,
} from 'lucide-vue-next'
import TrendChart from '@/components/TrendChart.vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const health = ref<'loading' | 'online' | 'offline'>('loading')
const healthChecking = ref(false)

// 健康状态来自真实后端；下面的经营数据仍明确标注为演示数据。
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
onMounted(checkHealth)

const metrics = [
  { label: '本周订单', value: '2,674', change: '+12.8%', direction: 'up', icon: Boxes, tone: 'teal' },
  { label: '退款申请', value: '407', change: '-4.2%', direction: 'down', icon: RotateCcw, tone: 'orange' },
  { label: '待处理工单', value: '28', change: '+6', direction: 'up', icon: Ticket, tone: 'purple' },
  { label: '订单完成率', value: '96.4%', change: '+2.1%', direction: 'up', icon: CircleCheck, tone: 'green' },
]
const orders = [
  { id: 'SM-20260921-0482', product: '智能家居组合套装', customer: '陈先生', amount: '¥ 1,299.00', status: '已完成', kind: 'success', time: '10:42' },
  { id: 'SM-20260921-0479', product: '无线降噪耳机', customer: '李女士', amount: '¥ 499.00', status: '处理中', kind: 'pending', time: '10:28' },
  { id: 'SM-20260921-0473', product: '便携投影仪', customer: '王先生', amount: '¥ 2,199.00', status: '已完成', kind: 'success', time: '09:56' },
  { id: 'SM-20260921-0468', product: '智能空气净化器', customer: '赵女士', amount: '¥ 899.00', status: '退款中', kind: 'warning', time: '09:21' },
]
</script>

<template>
  <div class="dashboard view-shell">
    <div class="page-heading">
      <div>
        <p class="eyebrow">OVERVIEW / 运营总览</p>
        <h1>工作台<span class="heading-spark">.</span></h1>
        <p class="page-heading__subtitle">欢迎回来，{{ auth.user?.username }}。这是当前运营概况。</p>
      </div>
      <div class="page-heading__actions"><span class="demo-badge">演示数据</span><span class="date-label">{{ new Date().toLocaleDateString('zh-CN') }}</span></div>
    </div>

    <div class="metrics-grid">
      <article v-for="(metric, index) in metrics" :key="metric.label" class="metric" :class="`metric--${metric.tone}`" :style="{ '--delay': `${index * 70}ms` }">
        <div class="metric__head"><span>{{ metric.label }}</span><component :is="metric.icon" :size="20" :stroke-width="1.7" /></div>
        <strong>{{ metric.value }}</strong>
        <div class="metric__bottom"><span class="metric__change"><ArrowUpRight v-if="metric.direction === 'up'" :size="15" /><ArrowDownRight v-else :size="15" /> {{ metric.change }}</span><span>较上周</span></div>
      </article>
    </div>

    <div class="dashboard__middle">
      <section class="data-section trend-section">
        <div class="section-heading"><div><p class="eyebrow">ANALYTICS / 演示数据</p><h2>业务趋势</h2></div><span class="period-label">近 7 天</span></div>
        <div class="chart-legend"><span><i class="legend-dot legend-dot--teal" />订单</span><span><i class="legend-dot legend-dot--orange" />退款</span></div>
        <TrendChart />
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
      <div class="section-heading"><div><p class="eyebrow">ACTIVITY / 演示数据</p><h2>近期订单</h2></div><router-link to="/orders" class="text-link">订单管理 <ArrowRight :size="16" /></router-link></div>
      <div class="table-scroll">
        <table class="orders-table">
          <thead><tr><th>订单编号</th><th>商品</th><th>客户</th><th>金额</th><th>状态</th><th>时间</th></tr></thead>
          <tbody><tr v-for="order in orders" :key="order.id"><td class="mono">{{ order.id }}</td><td>{{ order.product }}</td><td>{{ order.customer }}</td><td class="amount">{{ order.amount }}</td><td><span class="status-tag" :class="`status-tag--${order.kind}`"><i />{{ order.status }}</span></td><td class="mono">{{ order.time }}</td></tr></tbody>
        </table>
      </div>
    </section>
  </div>
</template>
