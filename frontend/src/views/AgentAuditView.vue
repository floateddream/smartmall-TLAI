<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Activity, CircleAlert, Clock3, Database, Eye, Link2, RefreshCw, ScrollText, Search, ShieldCheck, UserRound } from 'lucide-vue-next'
import { aiApi, type AiAgentAudit } from '@/api/ai'
import { useAuthStore } from '@/stores/auth'
import { readTraceId } from '@/utils/routeQuery'
import { auditConversationNavigation, auditCorrelationNavigation } from '@/utils/auditNavigation'
import { LatestRequestGate, requestErrorMessage } from '@/utils/requestState'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const audits = ref<AiAgentAudit[]>([])
const page = ref(1)
const pageSize = 20
const total = ref(0)
const toolName = ref('')
const status = ref('')
const loading = ref(false)
const errorMessage = ref('')
const traceId = ref('')
const detailOpen = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detailId = ref<number | null>(null)
const detail = ref<AiAgentAudit | null>(null)
const listRequests = new LatestRequestGate()
const detailRequests = new LatestRequestGate()

const successCount = computed(() => audits.value.filter(item => item.status === 'SUCCESS').length)
const averageLatency = computed(() => {
  if (!audits.value.length) return 0
  return Math.round(audits.value.reduce((sum, item) => sum + item.latencyMs, 0) / audits.value.length)
})

const toolOptions = [
  { value: 'knowledgeBaseSearch', label: '知识库检索' },
  { value: 'queryOrder', label: '订单查询' },
  { value: 'queryRefund', label: '退款查询' },
  { value: 'queryTicket', label: '工单查询' },
  { value: 'analyticsQuery', label: '数据分析' },
]

const toolLabels = Object.fromEntries(toolOptions.map(item => [item.value, item.label]))

async function loadAudits(targetPage = page.value) {
  const currentRequest = listRequests.begin()
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await aiApi.listAgentAudits(targetPage, pageSize, toolName.value, status.value, traceId.value)
    if (!listRequests.isCurrent(currentRequest)) return
    const lastPage = Math.max(1, Math.ceil(result.total / pageSize))
    if (result.total > 0 && targetPage > lastPage) {
      await loadAudits(lastPage)
      return
    }
    audits.value = result.items
    total.value = result.total
    page.value = result.page
  } catch (error) {
    if (!listRequests.isCurrent(currentRequest)) return
    console.error('[Agent审计] 加载记录失败', error)
    audits.value = []
    total.value = 0
    errorMessage.value = requestErrorMessage(error, '审计记录加载失败')
  } finally {
    if (listRequests.isCurrent(currentRequest)) loading.value = false
  }
}

function applyFilters() {
  void loadAudits(1)
}

function resetFilters() {
  toolName.value = ''
  status.value = ''
  traceId.value = ''
  void router.replace({ query: {} })
  void loadAudits(1)
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false,
  }).format(new Date(value))
}

function argumentSummary(value: string[]) {
  return value.length ? `字段：${value.join('、')}` : '无受控参数字段'
}

async function openDetail(id: number) {
  const currentRequest = detailRequests.begin()
  detailId.value = id
  detailOpen.value = true
  detailLoading.value = true
  detailError.value = ''
  detail.value = null
  try {
    const response = await aiApi.getAgentAudit(id)
    if (detailRequests.isCurrent(currentRequest)) detail.value = response
  } catch (error) {
    if (!detailRequests.isCurrent(currentRequest)) return
    detailError.value = requestErrorMessage(error, '审计详情加载失败')
  } finally {
    if (detailRequests.isCurrent(currentRequest)) detailLoading.value = false
  }
}

function openCallLogs(value: string) {
  const target = auditCorrelationNavigation('ai-call-logs', value, auth.can('ai:call_log_view'))
  if (target) void router.push(target)
}

function openConversation(id: number) {
  const target = auditConversationNavigation(id, auth.can('ai:chat'))
  if (target) void router.push(target)
}

onMounted(() => {
  traceId.value = readTraceId(route.query.traceId)
  void loadAudits(1)
})
</script>

<template>
  <div class="agent-audit-view">
    <header class="page-heading">
      <div><p class="eyebrow">AGENT / AUDIT TRAIL</p><h1>Agent 审计<span class="heading-spark">.</span></h1><p>查看业务工具的调用参数、执行结果与耗时</p></div>
      <button type="button" class="icon-button" :disabled="loading" title="刷新审计记录" aria-label="刷新审计记录" @click="loadAudits()">
        <RefreshCw :size="17" :class="{ spinning: loading }" />
      </button>
    </header>

    <section class="agent-audit-runtime" aria-label="当前页审计概览">
      <div><span>记录总数</span><strong>{{ total }}</strong></div>
      <div><span>当前页成功</span><strong class="agent-audit-runtime__success">{{ successCount }}</strong></div>
      <div><span>平均耗时</span><strong>{{ averageLatency }} <small>ms</small></strong></div>
      <div class="agent-audit-runtime__state"><Activity :size="15" /><span>AUDIT ONLINE</span></div>
    </section>

    <section class="data-section agent-audit-panel">
      <div class="agent-audit-toolbar">
        <el-select v-model="toolName" placeholder="全部工具" clearable :disabled="loading" aria-label="筛选业务工具" @change="applyFilters">
          <el-option label="全部工具" value="" />
          <el-option v-for="item in toolOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="status" placeholder="全部状态" clearable :disabled="loading" aria-label="筛选执行状态" @change="applyFilters">
          <el-option label="全部状态" value="" />
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
        </el-select>
        <span v-if="traceId" class="audit-trace-filter"><Link2 :size="12" />关联追踪 {{ traceId.slice(0, 8) }}</span>
        <span><Search :size="13" />筛选结果 {{ total }} 条</span>
        <button v-if="toolName || status || traceId" type="button" class="text-link" :disabled="loading" @click="resetFilters">清除筛选</button>
      </div>

      <div v-if="errorMessage" class="audit-feedback" role="alert">
        <CircleAlert :size="18" /><div><strong>审计记录读取失败</strong><span>{{ errorMessage }}</span></div>
        <button type="button" class="outline-button" :disabled="loading" @click="loadAudits()">重试</button>
      </div>
      <div v-else-if="loading && audits.length === 0" class="audit-empty" role="status">
        <span class="audit-empty__icon audit-empty__icon--loading"><Activity :size="24" /></span><strong>正在读取审计记录...</strong><small>只加载安全元数据</small>
      </div>
      <div v-else-if="audits.length === 0" class="audit-empty">
        <span class="audit-empty__icon"><ScrollText :size="29" /></span><strong>暂无符合条件的工具调用记录</strong><small>执行只读业务工具后，安全摘要会显示在这里</small>
      </div>
      <template v-else>
        <div class="table-scroll agent-audit-table-wrap" :class="{ 'analytics-loading': loading }">
          <table class="orders-table agent-audit-table">
            <thead><tr><th>调用时间</th><th>用户</th><th>业务工具</th><th>安全参数摘要</th><th>执行结果</th><th>耗时</th><th>详情</th></tr></thead>
            <tbody>
              <tr v-for="audit in audits" :key="audit.id">
                <td><span class="agent-audit-time"><Clock3 :size="12" />{{ formatTime(audit.createdAt) }}</span></td>
                <td><span class="agent-audit-user"><UserRound :size="13" /><strong>{{ audit.username || '已删除用户' }}</strong><small>{{ audit.userId ? `#${audit.userId}` : '账号已删除' }}</small></span></td>
                <td><span class="agent-audit-tool"><Database :size="13" /><strong>{{ toolLabels[audit.toolName] || audit.toolName }}</strong><small>{{ audit.toolName }}</small></span></td>
                <td><span class="agent-audit-arguments" :title="argumentSummary(audit.argumentKeys)">{{ argumentSummary(audit.argumentKeys) }}</span></td>
                <td><span class="audit-status" :class="`audit-status--${audit.status.toLowerCase()}`"><i />{{ audit.status === 'SUCCESS' ? '成功' : '失败' }}</span><small class="agent-audit-error">{{ audit.errorCode || 'NORMAL' }}</small></td>
                <td><strong class="mono agent-audit-latency">{{ audit.latencyMs }} ms</strong></td>
                <td><button type="button" class="audit-detail-button" @click="openDetail(audit.id)"><Eye :size="13" />查看</button></td>
              </tr>
            </tbody>
          </table>
        </div>
        <el-pagination v-if="total > pageSize" class="agent-audit-pagination" background layout="prev, pager, next, total" :page-size="pageSize" :total="total" :current-page="page" @current-change="loadAudits" />
      </template>
    </section>

    <el-drawer v-model="detailOpen" title="Agent 审计详情" size="min(520px, 96vw)" class="audit-detail-drawer">
      <div v-if="detailLoading" class="audit-detail-state"><span class="analytics-loader" />正在读取安全元数据...</div>
      <div v-else-if="detailError" class="audit-detail-state audit-detail-state--error"><CircleAlert :size="18" />{{ detailError }}<button v-if="detailId" type="button" class="text-link" @click="openDetail(detailId)">重试</button></div>
      <div v-else-if="detail" class="audit-detail">
        <div class="audit-detail__banner"><ShieldCheck :size="18" /><div><strong>仅展示脱敏审计元数据</strong><small>不返回参数值、业务结果、Prompt 或模型回答</small></div></div>
        <dl>
          <div><dt>记录标识</dt><dd>#{{ detail.id }}</dd></div>
          <div><dt>执行状态</dt><dd><span class="audit-status" :class="`audit-status--${detail.status.toLowerCase()}`"><i />{{ detail.status === 'SUCCESS' ? '成功' : '失败' }}</span></dd></div>
          <div><dt>业务工具</dt><dd>{{ toolLabels[detail.toolName] || detail.toolName }}<small>{{ detail.toolName }}</small></dd></div>
          <div><dt>操作账号</dt><dd>{{ detail.username || '已删除用户' }}<small>{{ detail.userId ? `用户 #${detail.userId}` : '账号已删除' }}</small></dd></div>
          <div><dt>参数字段</dt><dd>{{ argumentSummary(detail.argumentKeys) }}</dd></div>
          <div><dt>执行耗时</dt><dd>{{ detail.latencyMs }} ms</dd></div>
          <div><dt>失败原因</dt><dd>{{ detail.errorCode || '无' }}</dd></div>
          <div><dt>发生时间</dt><dd>{{ formatTime(detail.createdAt) }}</dd></div>
          <div><dt>会话标识</dt><dd>{{ detail.conversationId ? `#${detail.conversationId}` : '历史记录未关联' }}</dd></div>
          <div><dt>追踪标识</dt><dd class="audit-detail__trace">{{ detail.traceId || '历史记录未关联' }}</dd></div>
        </dl>
        <button v-if="detail.conversationId && detail.userId === auth.user?.id && auth.can('ai:chat')" type="button" class="outline-button audit-detail__link" @click="openConversation(detail.conversationId)"><Link2 :size="14" />打开我的关联会话</button>
        <button v-if="detail.traceId && auth.can('ai:call_log_view')" type="button" class="outline-button audit-detail__link" @click="openCallLogs(detail.traceId)"><Link2 :size="14" />查看同次请求的 AI 调用</button>
      </div>
    </el-drawer>
  </div>
</template>
