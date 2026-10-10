<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Ban, CreditCard, Eye, Plus, RefreshCw, RotateCcw, Search, ShoppingCart, Trash2 } from 'lucide-vue-next'
import { ApiError } from '@/api/client'
import { customerApi, productApi } from '@/api/catalog'
import type { Customer, Product } from '@/api/catalog'
import { orderApi } from '@/api/orders'
import type { CreateOrderInput, Order, OrderStatus } from '@/api/orders'
import { refundApi } from '@/api/refunds'
import { useAuthStore } from '@/stores/auth'
import { readPositiveQueryId, readQueryEnum, readQueryText } from '@/utils/routeQuery'

interface DraftItem {
  key: number
  productId: number | null
  quantity: number
}

const auth = useAuthStore()
const route = useRoute()
const rows = ref<Order[]>([])
const page = ref(1)
const total = ref(0)
const keyword = ref('')
const statusFilter = ref<OrderStatus | ''>('')
const loading = ref(false)
const loadError = ref('')
const actingId = ref<number | null>(null)
const createOpen = ref(false)
const saving = ref(false)
const formError = ref('')
const optionsLoading = ref(false)
const customers = ref<Customer[]>([])
const products = ref<Product[]>([])
const detailOpen = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detailId = ref<number | null>(null)
const detail = ref<Order | null>(null)
const refundOpen = ref(false)
const refundTarget = ref<Order | null>(null)
const refundReason = ref('')
const refundSaving = ref(false)
const createForm = reactive<{ customerId: number | null; items: DraftItem[] }>({
  customerId: null,
  items: [],
})
let lineKey = 0
let requestId = 0
let detailRequestId = 0

const statuses: Array<{ value: OrderStatus; label: string }> = [
  { value: 'PENDING_PAYMENT', label: '待支付' },
  { value: 'PAID', label: '已支付' },
  { value: 'CANCELLED', label: '已取消' },
  { value: 'REFUNDING', label: '退款审核中' },
  { value: 'REFUNDED', label: '已退款' },
]

const draftTotal = computed(() => createForm.items.reduce((sum, item) => {
  const product = findProduct(item.productId)
  return sum + (product ? Number(product.price) * item.quantity : 0)
}, 0))

function statusLabel(status: OrderStatus) {
  return statuses.find(item => item.value === status)?.label || status
}

function statusClass(status: OrderStatus) {
  return {
    PENDING_PAYMENT: 'order-status--pending',
    PAID: 'order-status--paid',
    CANCELLED: 'order-status--cancelled',
    REFUNDING: 'order-status--refunding',
    REFUNDED: 'order-status--refunded',
  }[status]
}

function findProduct(productId: number | null) {
  return productId === null ? undefined : products.value.find(product => product.id === productId)
}

function productDisabled(product: Product, currentLine: DraftItem) {
  const selectedElsewhere = createForm.items.some(item => item.key !== currentLine.key && item.productId === product.id)
  return !product.active || product.stock <= 0 || selectedElsewhere
}

function addLine() {
  createForm.items.push({ key: ++lineKey, productId: null, quantity: 1 })
}

function removeLine(index: number) {
  createForm.items.splice(index, 1)
}

async function loadPage(nextPage = page.value) {
  const current = ++requestId
  page.value = nextPage
  loading.value = true
  loadError.value = ''
  try {
    const result = await orderApi.list(nextPage, keyword.value.trim(), statusFilter.value)
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
    loadError.value = error instanceof ApiError ? error.message : '订单加载失败'
  } finally {
    if (current === requestId) loading.value = false
  }
}

function resetFilters() {
  keyword.value = ''
  statusFilter.value = ''
  void loadPage(1)
}

async function loadOptions() {
  optionsLoading.value = true
  formError.value = ''
  try {
    const [customerResult, productResult] = await Promise.all([
      customerApi.list(1, '', 100),
      productApi.list(1, '', 100),
    ])
    customers.value = customerResult.items
    products.value = productResult.items
  } catch (error) {
    formError.value = error instanceof ApiError ? error.message : '客户和商品加载失败'
  } finally {
    optionsLoading.value = false
  }
}

function openCreate() {
  createForm.customerId = null
  createForm.items.splice(0)
  addLine()
  formError.value = ''
  createOpen.value = true
  void loadOptions()
}

function validateDraft(): CreateOrderInput | null {
  if (createForm.customerId === null) {
    formError.value = '请选择客户'
    return null
  }
  if (!createForm.items.length) {
    formError.value = '订单至少需要一件商品'
    return null
  }
  for (const item of createForm.items) {
    const product = findProduct(item.productId)
    if (!product) {
      formError.value = '请为每一行选择商品'
      return null
    }
    if (!Number.isInteger(item.quantity) || item.quantity <= 0) {
      formError.value = `商品「${product.name}」的数量不正确`
      return null
    }
    if (item.quantity > product.stock) {
      formError.value = `商品「${product.name}」最多可购买 ${product.stock} 件`
      return null
    }
  }
  return {
    customerId: createForm.customerId,
    items: createForm.items.map(item => ({ productId: item.productId as number, quantity: item.quantity })),
  }
}

async function createOrder() {
  const input = validateDraft()
  if (!input) return
  saving.value = true
  formError.value = ''
  try {
    const created = await orderApi.create(input)
    createOpen.value = false
    ElMessage.success(`订单 ${created.orderNo} 已创建`)
    await loadPage(1)
    await openDetail(created.id)
  } catch (error) {
    formError.value = error instanceof ApiError ? error.message : '订单创建失败'
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
  try {
    const response = await orderApi.get(id)
    if (currentRequest === detailRequestId) detail.value = response
  } catch (error) {
    if (currentRequest !== detailRequestId) return
    detailError.value = error instanceof ApiError ? error.message : '订单详情加载失败'
  } finally {
    if (currentRequest === detailRequestId) detailLoading.value = false
  }
}

async function runAction(order: Order, action: 'pay' | 'cancel') {
  const paying = action === 'pay'
  try {
    await ElMessageBox.confirm(
      paying
        ? `确认将订单 ${order.orderNo} 标记为已支付？本项目不会调用真实支付渠道。`
        : `确认取消订单 ${order.orderNo}？取消后系统会恢复商品库存。`,
      paying ? '模拟支付' : '取消订单',
      { type: 'warning', confirmButtonText: paying ? '确认支付' : '确认取消', cancelButtonText: '返回' },
    )
  } catch {
    return
  }

  actingId.value = order.id
  try {
    const updated = paying ? await orderApi.pay(order.id) : await orderApi.cancel(order.id)
    ElMessage.success(paying ? '模拟支付成功' : '订单已取消，库存已恢复')
    if (detail.value?.id === updated.id) detail.value = updated
    await loadPage(page.value)
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : '操作失败，请刷新后重试')
  } finally {
    actingId.value = null
  }
}

function openRefund(order: Order) {
  refundTarget.value = order
  refundReason.value = ''
  refundOpen.value = true
}

async function submitRefund() {
  const order = refundTarget.value
  if (!order) return
  if (!refundReason.value.trim()) {
    ElMessage.warning('请填写退款原因')
    return
  }
  refundSaving.value = true
  try {
    const created = await refundApi.create(order.id, refundReason.value.trim())
    refundOpen.value = false
    ElMessage.success(`退款申请 ${created.refundNo} 已提交`)
    if (detail.value?.id === order.id) detail.value = await orderApi.get(order.id)
    await loadPage(page.value)
  } catch (error) {
    ElMessage.error(error instanceof ApiError ? error.message : '退款申请提交失败')
  } finally {
    refundSaving.value = false
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
  <div class="view-shell catalog-view order-view">
    <div class="page-heading">
      <div><p class="eyebrow">OPERATIONS / 订单</p><h1>订单管理<span class="heading-spark">.</span></h1></div>
      <button v-if="auth.can('order:create')" type="button" class="primary-button catalog-add" @click="openCreate"><ShoppingCart :size="18" />创建订单</button>
    </div>

    <div class="catalog-toolbar order-toolbar">
      <el-input v-model="keyword" placeholder="搜索订单号或客户名称" clearable aria-label="搜索订单" :disabled="loading" @keyup.enter="loadPage(1)" @clear="loadPage(1)">
        <template #prefix><Search :size="16" /></template>
      </el-input>
      <el-select v-model="statusFilter" class="order-status-filter" aria-label="筛选订单状态" :disabled="loading" @change="loadPage(1)">
        <el-option label="全部状态" value="" />
        <el-option v-for="status in statuses" :key="status.value" :label="status.label" :value="status.value" />
      </el-select>
      <button type="button" class="outline-button" :disabled="loading" @click="loadPage(1)"><Search :size="16" />搜索</button>
      <button v-if="keyword || statusFilter" type="button" class="text-link" :disabled="loading" @click="resetFilters">重置</button>
      <button type="button" class="icon-button catalog-refresh" title="刷新列表" aria-label="刷新列表" :disabled="loading" @click="loadPage()"><RefreshCw :size="17" :class="{ spinning: loading }" /></button>
      <span class="catalog-count">共 {{ total }} 笔订单</span>
    </div>

    <div class="catalog-table-wrap">
      <table class="catalog-table order-table">
        <thead><tr><th>订单号</th><th>客户</th><th>金额</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="order in loading || loadError ? [] : rows" :key="order.id">
            <td><strong class="mono">{{ order.orderNo }}</strong><small>#{{ order.id }}</small></td>
            <td>{{ order.customerName }}</td>
            <td class="catalog-money">¥ {{ Number(order.totalAmount).toFixed(2) }}</td>
            <td><span class="order-status" :class="statusClass(order.status)"><i />{{ statusLabel(order.status) }}</span></td>
            <td class="catalog-muted">{{ new Date(order.createdAt).toLocaleString('zh-CN') }}</td>
            <td>
              <div class="order-actions">
                <button type="button" class="icon-button catalog-edit" title="查看详情" aria-label="查看订单详情" @click="openDetail(order.id)"><Eye :size="17" /></button>
                <button v-if="auth.can('order:pay') && order.status === 'PENDING_PAYMENT'" type="button" class="icon-button catalog-edit order-action--pay" title="模拟支付" aria-label="模拟支付" :disabled="actingId === order.id" @click="runAction(order, 'pay')"><CreditCard :size="17" /></button>
                <button v-if="auth.can('order:cancel') && order.status === 'PENDING_PAYMENT'" type="button" class="icon-button catalog-edit order-action--cancel" title="取消订单" aria-label="取消订单" :disabled="actingId === order.id" @click="runAction(order, 'cancel')"><Ban :size="17" /></button>
                <button v-if="auth.can('refund:apply') && order.status === 'PAID'" type="button" class="icon-button catalog-edit order-action--refund" title="申请退款" aria-label="申请全额退款" @click="openRefund(order)"><RotateCcw :size="17" /></button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="loading" class="catalog-feedback" role="status">正在加载订单...</div>
      <div v-else-if="loadError" class="catalog-feedback catalog-feedback--error" role="alert">{{ loadError }} <button type="button" class="text-link" @click="loadPage()">重试</button></div>
      <div v-else-if="!rows.length" class="catalog-feedback">{{ keyword || statusFilter ? '没有匹配的订单' : '暂无订单' }}</div>
    </div>
    <el-pagination v-if="total > 20" class="catalog-pagination" background :current-page="page" :page-size="20" :total="total" layout="prev, pager, next, total" @current-change="loadPage" />

    <el-dialog v-model="createOpen" title="创建订单" width="min(720px, 94vw)" :close-on-click-modal="!saving" :show-close="!saving">
      <div class="order-form" v-loading="optionsLoading">
        <label class="order-field-label" for="order-customer">客户</label>
        <el-select id="order-customer" v-model="createForm.customerId" filterable placeholder="请选择下单客户" class="order-customer-select">
          <el-option v-for="customer in customers" :key="customer.id" :label="`${customer.name}${customer.phone ? ` · ${customer.phone}` : ''}`" :value="customer.id" />
        </el-select>

        <div class="order-lines-heading"><span>商品明细</span><button type="button" class="text-link" @click="addLine"><Plus :size="15" />添加商品</button></div>
        <div class="order-lines">
          <div v-for="(item, index) in createForm.items" :key="item.key" class="order-line">
            <el-select v-model="item.productId" filterable placeholder="选择商品" class="order-product-select">
              <el-option v-for="product in products" :key="product.id" :label="`${product.name} · ${product.sku} · 库存 ${product.stock}`" :value="product.id" :disabled="productDisabled(product, item)" />
            </el-select>
            <el-input-number v-model="item.quantity" :min="1" :max="findProduct(item.productId)?.stock || 999999" :precision="0" controls-position="right" aria-label="购买数量" />
            <span class="order-line-price">¥ {{ (Number(findProduct(item.productId)?.price || 0) * item.quantity).toFixed(2) }}</span>
            <button type="button" class="icon-button order-line-remove" title="移除商品" aria-label="移除商品" :disabled="createForm.items.length === 1" @click="removeLine(index)"><Trash2 :size="16" /></button>
          </div>
        </div>

        <div class="order-total"><span>预计订单金额</span><strong>¥ {{ draftTotal.toFixed(2) }}</strong></div>
        <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>
        <div class="catalog-form__actions">
          <button type="button" class="outline-button" :disabled="saving" @click="createOpen = false">取消</button>
          <button type="button" class="primary-button" :disabled="saving || optionsLoading" @click="createOrder">{{ saving ? '创建中...' : '创建订单' }}</button>
        </div>
      </div>
    </el-dialog>

    <el-drawer v-model="detailOpen" title="订单详情" size="520px" class="order-detail-drawer">
      <div v-if="detailLoading" class="catalog-feedback" role="status">正在加载订单详情...</div>
      <div v-else-if="detailError" class="catalog-feedback catalog-feedback--error" role="alert">{{ detailError }} <button v-if="detailId" type="button" class="text-link" @click="openDetail(detailId)">重试</button></div>
      <div v-else-if="detail" class="order-detail">
        <div class="order-detail__header">
          <div><p class="eyebrow">ORDER RECORD</p><h2 class="mono">{{ detail.orderNo }}</h2></div>
          <span class="order-status" :class="statusClass(detail.status)"><i />{{ statusLabel(detail.status) }}</span>
        </div>
        <dl class="order-detail__meta">
          <div><dt>客户</dt><dd>{{ detail.customerName }}</dd></div>
          <div><dt>创建时间</dt><dd>{{ new Date(detail.createdAt).toLocaleString('zh-CN') }}</dd></div>
          <div v-if="detail.paidAt"><dt>支付时间</dt><dd>{{ new Date(detail.paidAt).toLocaleString('zh-CN') }}</dd></div>
          <div v-if="detail.cancelledAt"><dt>取消时间</dt><dd>{{ new Date(detail.cancelledAt).toLocaleString('zh-CN') }}</dd></div>
          <div v-if="detail.refundedAt"><dt>退款时间</dt><dd>{{ new Date(detail.refundedAt).toLocaleString('zh-CN') }}</dd></div>
        </dl>
        <div class="order-detail__items">
          <div v-for="item in detail.items" :key="item.id" class="order-detail__item">
            <div><strong>{{ item.productName }}</strong><small class="mono">{{ item.productSku }}</small></div>
            <span>¥ {{ Number(item.unitPrice).toFixed(2) }} × {{ item.quantity }}</span>
            <strong>¥ {{ Number(item.subtotal).toFixed(2) }}</strong>
          </div>
        </div>
        <div class="order-detail__total"><span>订单总额</span><strong>¥ {{ Number(detail.totalAmount).toFixed(2) }}</strong></div>
        <div v-if="detail.status === 'PENDING_PAYMENT' && (auth.can('order:pay') || auth.can('order:cancel'))" class="order-detail__actions">
          <button v-if="auth.can('order:cancel')" type="button" class="outline-button order-cancel-button" :disabled="actingId === detail.id" @click="runAction(detail, 'cancel')"><Ban :size="16" />取消订单</button>
          <button v-if="auth.can('order:pay')" type="button" class="primary-button" :disabled="actingId === detail.id" @click="runAction(detail, 'pay')"><CreditCard :size="16" />模拟支付</button>
        </div>
        <div v-else-if="detail.status === 'PAID' && auth.can('refund:apply')" class="order-detail__actions">
          <button type="button" class="outline-button order-refund-button" @click="openRefund(detail)"><RotateCcw :size="16" />申请全额退款</button>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="refundOpen" title="申请全额退款" width="min(500px, 94vw)" :close-on-click-modal="!refundSaving" :show-close="!refundSaving">
      <div class="review-dialog">
        <p v-if="refundTarget" class="review-target"><span>{{ refundTarget.orderNo }}</span><strong>¥ {{ Number(refundTarget.totalAmount).toFixed(2) }}</strong></p>
        <label for="refund-reason">退款原因</label>
        <el-input id="refund-reason" v-model="refundReason" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="请说明退款原因" />
        <p class="dialog-hint">提交后订单进入退款审核中；管理员通过后恢复全部商品库存。</p>
        <div class="catalog-form__actions">
          <button type="button" class="outline-button" :disabled="refundSaving" @click="refundOpen = false">取消</button>
          <button type="button" class="primary-button" :disabled="refundSaving" @click="submitRefund">{{ refundSaving ? '提交中...' : '提交申请' }}</button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>
