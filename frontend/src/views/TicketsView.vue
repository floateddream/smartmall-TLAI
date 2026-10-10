<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, Eye, MessageSquareReply, Plus, RefreshCw, Search, Send, TicketCheck } from 'lucide-vue-next'
import { ApiError } from '@/api/client'
import { orderApi } from '@/api/orders'
import type { Order } from '@/api/orders'
import { ticketApi } from '@/api/tickets'
import type { CreateTicketInput, Ticket, TicketCategory, TicketPriority, TicketStatus } from '@/api/tickets'
import { useAuthStore } from '@/stores/auth'
import { readPositiveQueryId, readQueryEnum, readQueryText } from '@/utils/routeQuery'

const auth = useAuthStore()
const route = useRoute()
const rows = ref<Ticket[]>([])
const page = ref(1)
const total = ref(0)
const keyword = ref('')
const statusFilter = ref<TicketStatus | ''>('')
const categoryFilter = ref<TicketCategory | ''>('')
const loading = ref(false)
const loadError = ref('')
const createOpen = ref(false)
const saving = ref(false)
const formError = ref('')
const orderOptions = ref<Order[]>([])
const orderSearchLoading = ref(false)
const detailOpen = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detailId = ref<number | null>(null)
const detail = ref<Ticket | null>(null)
const replyContent = ref('')
const statusRemark = ref('')
const acting = ref(false)
let requestId = 0
let orderRequestId = 0
let detailRequestId = 0

const createForm = reactive<{
  orderId: number | null
  category: TicketCategory
  priority: TicketPriority
  subject: string
  description: string
}>({ orderId: null, category: 'OTHER', priority: 'NORMAL', subject: '', description: '' })

const statuses: Array<{ value: TicketStatus; label: string }> = [
  { value: 'OPEN', label: '待处理' },
  { value: 'PROCESSING', label: '处理中' },
  { value: 'RESOLVED', label: '已解决' },
  { value: 'CLOSED', label: '已关闭' },
]
const categories: Array<{ value: TicketCategory; label: string }> = [
  { value: 'RETURN_EXCHANGE', label: '退换货' },
  { value: 'LOGISTICS', label: '物流问题' },
  { value: 'PRODUCT_QUALITY', label: '商品质量' },
  { value: 'OTHER', label: '其他' },
]
const priorities: Array<{ value: TicketPriority; label: string }> = [
  { value: 'LOW', label: '低' },
  { value: 'NORMAL', label: '普通' },
  { value: 'HIGH', label: '高' },
  { value: 'URGENT', label: '紧急' },
]
const nextStatus = computed<TicketStatus | null>(() => ({
  OPEN: 'PROCESSING', PROCESSING: 'RESOLVED', RESOLVED: 'CLOSED', CLOSED: null,
})[detail.value?.status || 'CLOSED'] as TicketStatus | null)

function labelOf<T extends string>(options: Array<{ value: T; label: string }>, value: T) {
  return options.find(item => item.value === value)?.label || value
}
function statusLabel(value: TicketStatus) { return labelOf(statuses, value) }
function categoryLabel(value: TicketCategory) { return labelOf(categories, value) }
function priorityLabel(value: TicketPriority) { return labelOf(priorities, value) }
function nullableStatusLabel(value: TicketStatus | null) { return value ? statusLabel(value) : '-' }
function statusClass(status: TicketStatus) { return `ticket-status--${status.toLowerCase()}` }
function priorityClass(priority: TicketPriority) { return `ticket-priority--${priority.toLowerCase()}` }
function formatTime(value: string | null) { return value ? new Date(value).toLocaleString('zh-CN') : '-' }

async function loadPage(nextPage = page.value) {
  const current = ++requestId
  page.value = nextPage
  loading.value = true
  loadError.value = ''
  try {
    const result = await ticketApi.list(nextPage, keyword.value.trim(), statusFilter.value, categoryFilter.value)
    if (current !== requestId) return
    const lastPage = Math.max(1, Math.ceil(result.total / 20))
    if (result.total > 0 && nextPage > lastPage) {
      await loadPage(lastPage)
      return
    }
    rows.value = result.items
    total.value = result.total
  } catch (error) {
    if (current !== requestId) return
    rows.value = []
    total.value = 0
    loadError.value = error instanceof ApiError ? error.message : '售后工单加载失败'
  } finally {
    if (current === requestId) loading.value = false
  }
}

function resetFilters() {
  keyword.value = ''
  statusFilter.value = ''
  categoryFilter.value = ''
  void loadPage(1)
}

async function searchOrders(query: string) {
  const current = ++orderRequestId
  orderSearchLoading.value = true
  try {
    const result = await orderApi.list(1, query.trim(), '', 20)
    if (current === orderRequestId) orderOptions.value = result.items
  } catch (error) {
    if (current === orderRequestId) formError.value = error instanceof ApiError ? error.message : '订单搜索失败'
  } finally {
    if (current === orderRequestId) orderSearchLoading.value = false
  }
}

function openCreate() {
  Object.assign(createForm, { orderId: null, category: 'OTHER', priority: 'NORMAL', subject: '', description: '' })
  formError.value = ''
  createOpen.value = true
  void searchOrders('')
}

async function createTicket() {
  if (createForm.orderId === null) { formError.value = '请选择关联订单'; return }
  if (!createForm.subject.trim()) { formError.value = '请填写工单主题'; return }
  if (!createForm.description.trim()) { formError.value = '请填写问题描述'; return }
  saving.value = true
  formError.value = ''
  try {
    const input: CreateTicketInput = {
      orderId: createForm.orderId,
      category: createForm.category,
      priority: createForm.priority,
      subject: createForm.subject.trim(),
      description: createForm.description.trim(),
    }
    const created = await ticketApi.create(input)
    createOpen.value = false
    ElMessage.success(`工单 ${created.ticketNo} 已创建`)
    await loadPage(1)
    await openDetail(created.id)
  } catch (error) {
    formError.value = error instanceof ApiError ? error.message : '工单创建失败'
  } finally {
    saving.value = false
  }
}

async function openDetail(id: number) {
  const currentRequest = ++detailRequestId
  detailId.value = id
  detailOpen.value = true
  detailLoading.value = true
  detailError.value = ''
  detail.value = null
  replyContent.value = ''
  statusRemark.value = ''
  try {
    const response = await ticketApi.get(id)
    if (currentRequest === detailRequestId) detail.value = response
  } catch (error) {
    if (currentRequest !== detailRequestId) return
    detailError.value = error instanceof ApiError ? error.message : '工单详情加载失败'
  } finally {
    if (currentRequest === detailRequestId) detailLoading.value = false
  }
}

async function submitReply() {
  if (!detail.value || !replyContent.value.trim()) {
    ElMessage.warning('请先填写回复内容')
    return
  }
  acting.value = true
  try {
    detail.value = await ticketApi.addMessage(detail.value.id, replyContent.value.trim())
    replyContent.value = ''
    ElMessage.success('回复已记录')
    await loadPage(page.value)
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : '回复失败')
  } finally {
    acting.value = false
  }
}

async function advanceStatus() {
  if (!detail.value || !nextStatus.value) return
  const target = nextStatus.value
  try {
    await ElMessageBox.confirm(
      `确认将工单推进至“${statusLabel(target)}”？该流程不能回退。`,
      '推进工单状态',
      { type: 'warning', confirmButtonText: '确认推进', cancelButtonText: '返回' },
    )
  } catch {
    return
  }
  acting.value = true
  try {
    detail.value = await ticketApi.updateStatus(detail.value.id, target, statusRemark.value.trim())
    statusRemark.value = ''
    ElMessage.success(`工单已进入“${statusLabel(target)}”`)
    await loadPage(page.value)
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : '状态更新失败')
  } finally {
    acting.value = false
  }
}

onMounted(() => {
  keyword.value = readQueryText(route.query.keyword)
  statusFilter.value = readQueryEnum(route.query.status, statuses.map(item => item.value))
  categoryFilter.value = readQueryEnum(route.query.category, categories.map(item => item.value))
  const detailId = readPositiveQueryId(route.query.id)
  void loadPage(1)
  if (detailId !== null) void openDetail(detailId)
})
</script>

<template>
  <div class="view-shell catalog-view ticket-view">
    <div class="page-heading">
      <div><p class="eyebrow">OPERATIONS / 售后</p><h1>售后工单<span class="heading-spark">.</span></h1></div>
      <button v-if="auth.can('ticket:create')" type="button" class="primary-button catalog-add" @click="openCreate"><Plus :size="18" />新建工单</button>
    </div>

    <div class="catalog-toolbar ticket-toolbar">
      <el-input v-model="keyword" placeholder="搜索工单、订单、客户或主题" clearable aria-label="搜索工单" :disabled="loading" @keyup.enter="loadPage(1)" @clear="loadPage(1)">
        <template #prefix><Search :size="16" /></template>
      </el-input>
      <el-select v-model="statusFilter" class="ticket-filter" aria-label="筛选工单状态" :disabled="loading" @change="loadPage(1)">
        <el-option label="全部状态" value="" /><el-option v-for="item in statuses" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <el-select v-model="categoryFilter" class="ticket-filter" aria-label="筛选工单分类" :disabled="loading" @change="loadPage(1)">
        <el-option label="全部分类" value="" /><el-option v-for="item in categories" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>
      <button v-if="keyword || statusFilter || categoryFilter" type="button" class="text-link" :disabled="loading" @click="resetFilters">重置</button>
      <button type="button" class="icon-button catalog-refresh" title="刷新列表" aria-label="刷新列表" :disabled="loading" @click="loadPage()"><RefreshCw :size="17" :class="{ spinning: loading }" /></button>
      <span class="catalog-count">共 {{ total }} 笔工单</span>
    </div>

    <div class="catalog-table-wrap">
      <table class="catalog-table ticket-table">
        <thead><tr><th>工单</th><th>主题</th><th>客户 / 订单</th><th>分类</th><th>优先级</th><th>状态</th><th>更新时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="ticket in loading || loadError ? [] : rows" :key="ticket.id">
            <td><strong class="mono">{{ ticket.ticketNo }}</strong><small>#{{ ticket.id }}</small></td>
            <td class="ticket-subject">{{ ticket.subject }}</td>
            <td><strong>{{ ticket.customerName }}</strong><small class="mono">{{ ticket.orderNo }}</small></td>
            <td class="catalog-muted">{{ categoryLabel(ticket.category) }}</td>
            <td><span class="ticket-priority" :class="priorityClass(ticket.priority)">{{ priorityLabel(ticket.priority) }}</span></td>
            <td><span class="ticket-status" :class="statusClass(ticket.status)"><i />{{ statusLabel(ticket.status) }}</span></td>
            <td class="catalog-muted">{{ formatTime(ticket.updatedAt) }}</td>
            <td><button type="button" class="icon-button catalog-edit" title="查看与处理" aria-label="查看工单" @click="openDetail(ticket.id)"><Eye :size="17" /></button></td>
          </tr>
        </tbody>
      </table>
      <div v-if="loading" class="catalog-feedback" role="status">正在加载售后工单...</div>
      <div v-else-if="loadError" class="catalog-feedback catalog-feedback--error" role="alert">{{ loadError }} <button type="button" class="text-link" @click="loadPage()">重试</button></div>
      <div v-else-if="!rows.length" class="catalog-feedback">{{ keyword || statusFilter || categoryFilter ? '没有匹配的工单' : '暂无售后工单' }}</div>
    </div>
    <el-pagination v-if="total > 20" class="catalog-pagination" background :current-page="page" :page-size="20" :total="total" layout="prev, pager, next, total" @current-change="loadPage" />

    <el-dialog v-model="createOpen" title="新建售后工单" width="min(620px, 94vw)" :close-on-click-modal="!saving" :show-close="!saving">
      <div class="ticket-form">
        <label class="order-field-label" for="ticket-order">关联订单</label>
        <el-select id="ticket-order" v-model="createForm.orderId" filterable remote reserve-keyword :remote-method="searchOrders" :loading="orderSearchLoading" placeholder="输入订单号或客户名称搜索" class="order-customer-select">
          <el-option v-for="order in orderOptions" :key="order.id" :label="`${order.orderNo} · ${order.customerName} · ¥${Number(order.totalAmount).toFixed(2)}`" :value="order.id" />
        </el-select>
        <div class="catalog-form__row ticket-form__row">
          <div><label class="order-field-label">问题分类</label><el-select v-model="createForm.category"><el-option v-for="item in categories" :key="item.value" :label="item.label" :value="item.value" /></el-select></div>
          <div><label class="order-field-label">优先级</label><el-select v-model="createForm.priority"><el-option v-for="item in priorities" :key="item.value" :label="item.label" :value="item.value" /></el-select></div>
        </div>
        <label class="order-field-label" for="ticket-subject">工单主题</label>
        <el-input id="ticket-subject" v-model="createForm.subject" maxlength="120" show-word-limit placeholder="简要描述售后问题" />
        <label class="order-field-label ticket-description-label" for="ticket-description">问题描述</label>
        <el-input id="ticket-description" v-model="createForm.description" type="textarea" :rows="5" maxlength="2000" show-word-limit placeholder="记录问题现象、客户诉求和已知信息" />
        <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>
        <div class="catalog-form__actions"><button type="button" class="outline-button" :disabled="saving" @click="createOpen = false">取消</button><button type="button" class="primary-button" :disabled="saving" @click="createTicket">{{ saving ? '创建中...' : '创建工单' }}</button></div>
      </div>
    </el-dialog>

    <el-drawer v-model="detailOpen" title="工单处理" size="min(650px, 96vw)" class="ticket-detail-drawer">
      <div v-if="detailLoading" class="catalog-feedback" role="status">正在加载工单详情...</div>
      <div v-else-if="detailError" class="catalog-feedback catalog-feedback--error" role="alert">{{ detailError }} <button v-if="detailId" type="button" class="text-link" @click="openDetail(detailId)">重试</button></div>
      <div v-else-if="detail" class="ticket-detail">
        <header class="ticket-detail__header">
          <div><p class="eyebrow">SERVICE TICKET</p><h2>{{ detail.subject }}</h2><span class="mono">{{ detail.ticketNo }}</span></div>
          <span class="ticket-status" :class="statusClass(detail.status)"><i />{{ statusLabel(detail.status) }}</span>
        </header>
        <div class="ticket-detail__facts">
          <span><small>关联订单</small><strong class="mono">{{ detail.orderNo }}</strong></span>
          <span><small>客户</small><strong>{{ detail.customerName }}</strong></span>
          <span><small>分类</small><strong>{{ categoryLabel(detail.category) }}</strong></span>
          <span><small>优先级</small><strong :class="priorityClass(detail.priority)">{{ priorityLabel(detail.priority) }}</strong></span>
        </div>
        <section class="record-block ticket-description"><span>问题描述</span><p>{{ detail.description }}</p><small>{{ detail.creatorName }} · {{ formatTime(detail.createdAt) }}</small></section>

        <section class="ticket-timeline">
          <h3>处理记录</h3>
          <div v-if="!detail.messages.length" class="ticket-timeline__empty">尚无回复或状态变更</div>
          <article v-for="message in detail.messages" :key="message.id" class="ticket-event" :class="{ 'ticket-event--status': message.type === 'STATUS_CHANGE' }">
            <span class="ticket-event__dot"><ArrowRight v-if="message.type === 'STATUS_CHANGE'" :size="12" /><MessageSquareReply v-else :size="12" /></span>
            <div><header><strong>{{ message.senderName }}</strong><time>{{ formatTime(message.createdAt) }}</time></header><p v-if="message.type === 'STATUS_CHANGE'">状态从“{{ nullableStatusLabel(message.fromStatus) }}”推进至“{{ nullableStatusLabel(message.toStatus) }}”</p><p v-if="message.content">{{ message.content }}</p></div>
          </article>
        </section>

        <div v-if="detail.status !== 'CLOSED' && (auth.can('ticket:reply') || auth.can('ticket:status'))" class="ticket-actions-panel">
          <template v-if="auth.can('ticket:reply')">
          <label class="order-field-label" for="ticket-reply">添加回复</label>
          <div class="ticket-reply"><el-input id="ticket-reply" v-model="replyContent" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="记录沟通内容或处理结果" /><button type="button" class="icon-button ticket-send" title="发送回复" aria-label="发送回复" :disabled="acting || !replyContent.trim()" @click="submitReply"><Send :size="18" /></button></div>
          </template>
          <template v-if="nextStatus && auth.can('ticket:status')">
            <label class="order-field-label ticket-status-remark" for="status-remark">状态处理说明（选填）</label>
            <div class="ticket-advance"><el-input id="status-remark" v-model="statusRemark" maxlength="1000" placeholder="本次状态变更说明" /><button type="button" class="primary-button" :disabled="acting" @click="advanceStatus"><TicketCheck :size="17" />推进至{{ statusLabel(nextStatus) }}</button></div>
          </template>
        </div>
      </div>
    </el-drawer>
  </div>
</template>
