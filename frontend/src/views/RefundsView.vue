<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Eye, RefreshCw, RotateCcw, Search, X } from 'lucide-vue-next'
import { ApiError } from '@/api/client'
import { refundApi } from '@/api/refunds'
import type { RefundRequest, RefundStatus } from '@/api/refunds'
import { useAuthStore } from '@/stores/auth'
import { readPositiveQueryId, readQueryEnum, readQueryText } from '@/utils/routeQuery'

const auth = useAuthStore()
const route = useRoute()
const rows = ref<RefundRequest[]>([])
const page = ref(1)
const total = ref(0)
const keyword = ref('')
const statusFilter = ref<RefundStatus | ''>('')
const loading = ref(false)
const loadError = ref('')
const detailOpen = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detailId = ref<number | null>(null)
const detail = ref<RefundRequest | null>(null)
const reviewOpen = ref(false)
const reviewAction = ref<'approve' | 'reject'>('approve')
const reviewTarget = ref<RefundRequest | null>(null)
const reviewRemark = ref('')
const reviewing = ref(false)
let requestId = 0
let detailRequestId = 0

const statuses: Array<{ value: RefundStatus; label: string }> = [
  { value: 'PENDING_REVIEW', label: '待审核' },
  { value: 'COMPLETED', label: '已退款' },
  { value: 'REJECTED', label: '已驳回' },
]

function statusLabel(status: RefundStatus) {
  return statuses.find(item => item.value === status)?.label || status
}

function statusClass(status: RefundStatus) {
  return {
    PENDING_REVIEW: 'refund-status--pending',
    COMPLETED: 'refund-status--completed',
    REJECTED: 'refund-status--rejected',
  }[status]
}

function formatTime(value: string | null) {
  return value ? new Date(value).toLocaleString('zh-CN') : '-'
}

async function loadPage(nextPage = page.value) {
  const current = ++requestId
  page.value = nextPage
  loading.value = true
  loadError.value = ''
  try {
    const result = await refundApi.list(nextPage, keyword.value.trim(), statusFilter.value)
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
    loadError.value = error instanceof ApiError ? error.message : '退款申请加载失败'
  } finally {
    if (current === requestId) loading.value = false
  }
}

function resetFilters() {
  keyword.value = ''
  statusFilter.value = ''
  void loadPage(1)
}

async function openDetail(id: number) {
  const currentRequest = ++detailRequestId
  detailId.value = id
  detailOpen.value = true
  detailLoading.value = true
  detailError.value = ''
  detail.value = null
  try {
    const response = await refundApi.get(id)
    if (currentRequest === detailRequestId) detail.value = response
  } catch (error) {
    if (currentRequest !== detailRequestId) return
    detailError.value = error instanceof ApiError ? error.message : '退款详情加载失败'
  } finally {
    if (currentRequest === detailRequestId) detailLoading.value = false
  }
}

function openReview(refund: RefundRequest, action: 'approve' | 'reject') {
  reviewTarget.value = refund
  reviewAction.value = action
  reviewRemark.value = ''
  reviewOpen.value = true
}

async function submitReview() {
  const target = reviewTarget.value
  if (!target) return
  if (reviewAction.value === 'reject' && !reviewRemark.value.trim()) {
    ElMessage.warning('驳回退款时请填写原因')
    return
  }
  try {
    await ElMessageBox.confirm(
      reviewAction.value === 'approve'
        ? `确认通过 ${target.refundNo}？通过后将恢复该订单全部商品库存。`
        : `确认驳回 ${target.refundNo}？订单将恢复为已支付状态。`,
      reviewAction.value === 'approve' ? '确认退款' : '确认驳回',
      { type: 'warning', confirmButtonText: '确认执行', cancelButtonText: '返回' },
    )
  } catch {
    return
  }

  reviewing.value = true
  try {
    const updated = reviewAction.value === 'approve'
      ? await refundApi.approve(target.id, reviewRemark.value.trim())
      : await refundApi.reject(target.id, reviewRemark.value.trim())
    reviewOpen.value = false
    ElMessage.success(reviewAction.value === 'approve' ? '退款已完成，库存已恢复' : '退款申请已驳回')
    if (detail.value?.id === updated.id) detail.value = updated
    await loadPage(page.value)
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : '退款审核失败')
  } finally {
    reviewing.value = false
  }
}

onMounted(() => {
  keyword.value = readQueryText(route.query.keyword)
  statusFilter.value = readQueryEnum(route.query.status, statuses.map(item => item.value))
  const detailId = readPositiveQueryId(route.query.id)
  void loadPage(1)
  if (detailId !== null) void openDetail(detailId)
})
</script>

<template>
  <div class="view-shell catalog-view refund-view">
    <div class="page-heading">
      <div><p class="eyebrow">OPERATIONS / 退款</p><h1>退款管理<span class="heading-spark">.</span></h1></div>
      <span class="workflow-note"><RotateCcw :size="16" />全额退款流程</span>
    </div>

    <div class="catalog-toolbar order-toolbar">
      <el-input v-model="keyword" placeholder="搜索退款单、订单号或申请人" clearable aria-label="搜索退款申请" :disabled="loading" @keyup.enter="loadPage(1)" @clear="loadPage(1)">
        <template #prefix><Search :size="16" /></template>
      </el-input>
      <el-select v-model="statusFilter" class="order-status-filter" aria-label="筛选退款状态" :disabled="loading" @change="loadPage(1)">
        <el-option label="全部状态" value="" />
        <el-option v-for="status in statuses" :key="status.value" :label="status.label" :value="status.value" />
      </el-select>
      <button type="button" class="outline-button" :disabled="loading" @click="loadPage(1)"><Search :size="16" />搜索</button>
      <button v-if="keyword || statusFilter" type="button" class="text-link" :disabled="loading" @click="resetFilters">重置</button>
      <button type="button" class="icon-button catalog-refresh" title="刷新列表" aria-label="刷新列表" :disabled="loading" @click="loadPage()"><RefreshCw :size="17" :class="{ spinning: loading }" /></button>
      <span class="catalog-count">共 {{ total }} 笔申请</span>
    </div>

    <div class="catalog-table-wrap">
      <table class="catalog-table refund-table">
        <thead><tr><th>退款单</th><th>关联订单</th><th>申请金额</th><th>申请人</th><th>状态</th><th>申请时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="refund in loading || loadError ? [] : rows" :key="refund.id">
            <td><strong class="mono">{{ refund.refundNo }}</strong><small>#{{ refund.id }}</small></td>
            <td class="mono catalog-muted">{{ refund.orderNo }}</td>
            <td class="catalog-money">¥ {{ Number(refund.amount).toFixed(2) }}</td>
            <td>{{ refund.applicantName }}</td>
            <td><span class="refund-status" :class="statusClass(refund.status)"><i />{{ statusLabel(refund.status) }}</span></td>
            <td class="catalog-muted">{{ formatTime(refund.appliedAt) }}</td>
            <td>
              <div class="order-actions">
                <button type="button" class="icon-button catalog-edit" title="查看详情" aria-label="查看退款详情" @click="openDetail(refund.id)"><Eye :size="17" /></button>
                <button v-if="auth.can('refund:review') && refund.status === 'PENDING_REVIEW'" type="button" class="icon-button catalog-edit refund-approve" title="通过" aria-label="通过退款" @click="openReview(refund, 'approve')"><Check :size="17" /></button>
                <button v-if="auth.can('refund:review') && refund.status === 'PENDING_REVIEW'" type="button" class="icon-button catalog-edit refund-reject" title="驳回" aria-label="驳回退款" @click="openReview(refund, 'reject')"><X :size="17" /></button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="loading" class="catalog-feedback" role="status">正在加载退款申请...</div>
      <div v-else-if="loadError" class="catalog-feedback catalog-feedback--error" role="alert">{{ loadError }} <button type="button" class="text-link" @click="loadPage()">重试</button></div>
      <div v-else-if="!rows.length" class="catalog-feedback">{{ keyword || statusFilter ? '没有匹配的退款申请' : '暂无退款申请' }}</div>
    </div>
    <el-pagination v-if="total > 20" class="catalog-pagination" background :current-page="page" :page-size="20" :total="total" layout="prev, pager, next, total" @current-change="loadPage" />

    <el-drawer v-model="detailOpen" title="退款详情" size="500px" class="order-detail-drawer">
      <div v-if="detailLoading" class="catalog-feedback" role="status">正在加载退款详情...</div>
      <div v-else-if="detailError" class="catalog-feedback catalog-feedback--error" role="alert">{{ detailError }} <button v-if="detailId" type="button" class="text-link" @click="openDetail(detailId)">重试</button></div>
      <div v-else-if="detail" class="order-detail">
        <div class="order-detail__header">
          <div><p class="eyebrow">REFUND RECORD</p><h2 class="mono">{{ detail.refundNo }}</h2></div>
          <span class="refund-status" :class="statusClass(detail.status)"><i />{{ statusLabel(detail.status) }}</span>
        </div>
        <dl class="order-detail__meta">
          <div><dt>关联订单</dt><dd class="mono">{{ detail.orderNo }}</dd></div>
          <div><dt>退款金额</dt><dd class="refund-amount">¥ {{ Number(detail.amount).toFixed(2) }}</dd></div>
          <div><dt>申请人</dt><dd>{{ detail.applicantName }}</dd></div>
          <div><dt>申请时间</dt><dd>{{ formatTime(detail.appliedAt) }}</dd></div>
          <div v-if="detail.reviewerName"><dt>审核人</dt><dd>{{ detail.reviewerName }}</dd></div>
          <div v-if="detail.reviewedAt"><dt>审核时间</dt><dd>{{ formatTime(detail.reviewedAt) }}</dd></div>
        </dl>
        <section class="record-block"><span>退款原因</span><p>{{ detail.reason }}</p></section>
        <section v-if="detail.reviewRemark" class="record-block"><span>审核备注</span><p>{{ detail.reviewRemark }}</p></section>
        <div v-if="auth.can('refund:review') && detail.status === 'PENDING_REVIEW'" class="order-detail__actions">
          <button type="button" class="outline-button refund-reject" @click="openReview(detail, 'reject')"><X :size="16" />驳回</button>
          <button type="button" class="primary-button" @click="openReview(detail, 'approve')"><Check :size="16" />通过退款</button>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="reviewOpen" :title="reviewAction === 'approve' ? '通过退款申请' : '驳回退款申请'" width="min(500px, 94vw)" :close-on-click-modal="!reviewing" :show-close="!reviewing">
      <div class="review-dialog">
        <p v-if="reviewTarget" class="review-target"><span>{{ reviewTarget.refundNo }}</span><strong>¥ {{ Number(reviewTarget.amount).toFixed(2) }}</strong></p>
        <label for="review-remark">{{ reviewAction === 'reject' ? '驳回原因' : '审核备注（选填）' }}</label>
        <el-input id="review-remark" v-model="reviewRemark" type="textarea" :rows="4" maxlength="500" show-word-limit :placeholder="reviewAction === 'reject' ? '请填写驳回原因' : '可填写退款审核说明'" />
        <div class="catalog-form__actions">
          <button type="button" class="outline-button" :disabled="reviewing" @click="reviewOpen = false">取消</button>
          <button type="button" class="primary-button" :class="{ 'danger-button': reviewAction === 'reject' }" :disabled="reviewing" @click="submitReview">{{ reviewing ? '处理中...' : '确认提交' }}</button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>
