<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { BarChart3, CalendarRange, CircleAlert, Database, Hash, History, MessageSquareText, RefreshCw, Sparkles, Trophy } from 'lucide-vue-next'
import AnalyticsChart from '@/components/AnalyticsChart.vue'
import MarkdownContent from '@/components/MarkdownContent.vue'
import { ApiError } from '@/api/client'
import {
  analyticsApi,
  type AnalyticsAudit,
  type AnalyticsAuditStatus,
  type AnalyticsDimension,
  type AnalyticsGranularity,
  type AnalyticsMetric,
  type AnalyticsQueryMode,
  type AnalyticsResult,
} from '@/api/analytics'
import { useAuthStore } from '@/stores/auth'
import { readIsoDate, readQueryEnum, readQueryInteger, validDateRange } from '@/utils/routeQuery'

type ReportCode =
  | 'ORDER_TREND'
  | 'REFUND_TREND'
  | 'REFUND_PRODUCT'
  | 'REFUND_REASON'
  | 'TICKET_TREND'
  | 'TICKET_STATUS'
  | 'TICKET_CATEGORY'

interface ReportPreset {
  code: ReportCode
  label: string
  description: string
  metric: AnalyticsMetric
  dimension: AnalyticsDimension
}

const reportPresets: ReportPreset[] = [
  { code: 'ORDER_TREND', label: '订单趋势', description: '按时间观察订单数量变化', metric: 'ORDER_COUNT', dimension: 'DATE' },
  { code: 'REFUND_TREND', label: '退款金额趋势', description: '按时间观察退款金额变化', metric: 'REFUND_AMOUNT', dimension: 'DATE' },
  { code: 'REFUND_PRODUCT', label: '商品退款排行', description: '定位退款金额较高的商品', metric: 'REFUND_AMOUNT', dimension: 'PRODUCT' },
  { code: 'REFUND_REASON', label: '退款原因分布', description: '统计各类退款原因出现次数', metric: 'REFUND_COUNT', dimension: 'REFUND_REASON' },
  { code: 'TICKET_TREND', label: '工单趋势', description: '按时间观察售后工单变化', metric: 'TICKET_COUNT', dimension: 'DATE' },
  { code: 'TICKET_STATUS', label: '工单状态分布', description: '查看各处理状态的工单数量', metric: 'TICKET_COUNT', dimension: 'TICKET_STATUS' },
  { code: 'TICKET_CATEGORY', label: '工单分类分布', description: '查看各售后问题类型占比', metric: 'TICKET_COUNT', dimension: 'TICKET_CATEGORY' },
]

const auth = useAuthStore()
const route = useRoute()

const labelTranslations: Record<string, string> = {
  OPEN: '待处理',
  PROCESSING: '处理中',
  RESOLVED: '已解决',
  CLOSED: '已关闭',
  RETURN_EXCHANGE: '退换货',
  LOGISTICS: '物流',
  PRODUCT_QUALITY: '商品质量',
  OTHER: '其他',
}

function formatDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const today = new Date()
const ninetyDaysAgo = new Date(today)
ninetyDaysAgo.setDate(today.getDate() - 89)

const selectedCode = ref<ReportCode>('REFUND_PRODUCT')
const dateRange = ref<[string, string]>([formatDate(ninetyDaysAgo), formatDate(today)])
const granularity = ref<AnalyticsGranularity>('MONTH')
const limit = ref(10)
const loading = ref(false)
const naturalLoading = ref(false)
const errorMessage = ref('')
const result = ref<AnalyticsResult | null>(null)
const question = ref('')
const explanation = ref('')
const explanationModel = ref('')
const explanationLatency = ref(0)
const lastRequestMode = ref<'structured' | 'natural'>('structured')
const audits = ref<AnalyticsAudit[]>([])
const auditPage = ref(1)
const auditTotal = ref(0)
const auditMode = ref<AnalyticsQueryMode | ''>('')
const auditStatus = ref<AnalyticsAuditStatus | ''>('')
const auditLoading = ref(false)
const auditError = ref('')
let analysisRequestId = 0
let auditRequestId = 0

const activePreset = computed(() => reportPresets.find(item => item.code === selectedCode.value) ?? reportPresets[0])
const isTimeline = computed(() => activePreset.value.dimension === 'DATE')
const busy = computed(() => loading.value || naturalLoading.value)
const points = computed(() => result.value?.points ?? [])
const total = computed(() => points.value.reduce((sum, point) => sum + Number(point.value), 0))
const highest = computed(() => points.value.reduce<(typeof points.value)[number] | null>((current, point) => {
  if (!current || Number(point.value) > Number(current.value)) return point
  return current
}, null))
const canViewAudits = computed(() => auth.can('analytics:audit_view'))

function displayLabel(label: string) {
  return labelTranslations[label] ?? label
}

function displayValue(value: number) {
  if (activePreset.value.metric === 'REFUND_AMOUNT') {
    return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(value)
  }
  return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 0 }).format(value)
}

function generatedTime() {
  if (!result.value) return '尚未生成'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false,
  }).format(new Date(result.value.generatedAt))
}

function formatAuditTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false,
  }).format(new Date(value))
}

function auditQueryLabel(audit: AnalyticsAudit) {
  if (audit.question) return audit.question
  const preset = reportPresets.find(item => item.metric === audit.metric && item.dimension === audit.dimension)
  return preset?.label ?? ([audit.metric, audit.dimension].filter(Boolean).join(' / ') || '结构化分析')
}

async function loadAudits(nextPage = auditPage.value) {
  if (!canViewAudits.value) return
  const currentRequest = ++auditRequestId
  auditLoading.value = true
  auditError.value = ''
  auditPage.value = nextPage
  try {
    const response = await analyticsApi.listAudits(nextPage, auditMode.value, auditStatus.value)
    if (currentRequest !== auditRequestId) return
    const lastPage = Math.max(1, Math.ceil(response.total / 10))
    if (response.total > 0 && nextPage > lastPage) {
      await loadAudits(lastPage)
      return
    }
    audits.value = response.items
    auditTotal.value = response.total
  } catch (error) {
    if (currentRequest !== auditRequestId) return
    audits.value = []
    auditTotal.value = 0
    auditError.value = error instanceof ApiError ? error.message : '查询审计加载失败'
  } finally {
    if (currentRequest === auditRequestId) auditLoading.value = false
  }
}

function resetAuditFilters() {
  auditMode.value = ''
  auditStatus.value = ''
  void loadAudits(1)
}

function estimatedBucketCount() {
  const [startText, endText] = dateRange.value
  const start = new Date(`${startText}T00:00:00`)
  const end = new Date(`${endText}T00:00:00`)
  if (granularity.value === 'DAY') return Math.floor((end.getTime() - start.getTime()) / 86400000) + 1
  if (granularity.value === 'WEEK') {
    const startMonday = new Date(start)
    const endMonday = new Date(end)
    startMonday.setDate(start.getDate() - ((start.getDay() + 6) % 7))
    endMonday.setDate(end.getDate() - ((end.getDay() + 6) % 7))
    return Math.floor((endMonday.getTime() - startMonday.getTime()) / (7 * 86400000)) + 1
  }
  return (end.getFullYear() - start.getFullYear()) * 12 + end.getMonth() - start.getMonth() + 1
}

async function runAnalysis() {
  const currentRequest = ++analysisRequestId
  lastRequestMode.value = 'structured'
  errorMessage.value = ''
  if (!dateRange.value?.[0] || !dateRange.value?.[1]) {
    errorMessage.value = '请选择完整的分析日期范围'
    return
  }
  if (isTimeline.value && estimatedBucketCount() > limit.value) {
    errorMessage.value = `当前时间粒度会产生 ${estimatedBucketCount()} 个分组，请提高返回条数或选择更大的时间粒度`
    return
  }

  loading.value = true
  naturalLoading.value = false
  try {
    const response = await analyticsApi.query({
      metric: activePreset.value.metric,
      dimension: activePreset.value.dimension,
      granularity: granularity.value,
      startDate: dateRange.value[0],
      endDate: dateRange.value[1],
      limit: limit.value,
    })
    if (currentRequest !== analysisRequestId) return
    result.value = response
    explanation.value = ''
    explanationModel.value = ''
    explanationLatency.value = 0
  } catch (error) {
    if (currentRequest !== analysisRequestId) return
    errorMessage.value = error instanceof ApiError ? error.message : '分析请求失败，请稍后重试'
  } finally {
    if (currentRequest === analysisRequestId) {
      loading.value = false
      void loadAudits(1)
    }
  }
}

function syncQueryControls(analytics: AnalyticsResult) {
  const preset = reportPresets.find(item => item.metric === analytics.metric && item.dimension === analytics.dimension)
  if (preset) selectedCode.value = preset.code
  dateRange.value = [analytics.startDate, analytics.endDate]
  granularity.value = analytics.granularity
  limit.value = analytics.limit
}

async function askData() {
  const content = question.value.trim()
  if (!content || naturalLoading.value) return
  const currentRequest = ++analysisRequestId
  lastRequestMode.value = 'natural'
  errorMessage.value = ''
  naturalLoading.value = true
  loading.value = false
  try {
    const response = await analyticsApi.naturalLanguage(content)
    if (currentRequest !== analysisRequestId) return
    result.value = response
    syncQueryControls(response)
    explanation.value = response.explanation
    explanationModel.value = response.modelName
    explanationLatency.value = response.latencyMs
  } catch (error) {
    if (currentRequest !== analysisRequestId) return
    errorMessage.value = error instanceof ApiError ? error.message : '智能分析失败，请换一种说法后重试'
  } finally {
    if (currentRequest === analysisRequestId) {
      naturalLoading.value = false
      void loadAudits(1)
    }
  }
}

function useSuggestion(value: string) {
  question.value = value
  void askData()
}

function applyRouteFilters() {
  const report = readQueryEnum(route.query.report, reportPresets.map(item => item.code))
  if (report) selectedCode.value = report
  const startDate = readIsoDate(route.query.startDate)
  const endDate = readIsoDate(route.query.endDate)
  if (validDateRange(startDate, endDate)) dateRange.value = [startDate, endDate]
  const routeGranularity = readQueryEnum(route.query.granularity,
    ['DAY', 'WEEK', 'MONTH'] as AnalyticsGranularity[])
  if (routeGranularity) granularity.value = routeGranularity
  const routeLimit = readQueryInteger(route.query.limit, 1, 50)
  if (routeLimit !== null) limit.value = routeLimit
}

onMounted(() => {
  applyRouteFilters()
  void runAnalysis()
})
</script>

<template>
  <div class="analytics-view view-shell">
    <div class="page-heading">
      <div>
        <p class="eyebrow">ANALYTICS / 业务洞察</p>
        <h1>数据分析<span class="heading-spark">.</span></h1>
        <p class="page-heading__subtitle">组合预设指标与时间范围，直接查看真实业务数据。</p>
      </div>
      <div class="page-heading__actions">
        <span class="analytics-updated"><i />生成于 {{ generatedTime() }}</span>
        <button type="button" class="icon-button dashboard-refresh" :disabled="busy" title="重新运行" aria-label="重新运行分析" @click="runAnalysis">
          <RefreshCw :size="16" :class="{ spinning: busy }" />
        </button>
      </div>
    </div>

    <section class="analytics-question" aria-label="自然语言数据分析">
      <span class="analytics-question__icon"><MessageSquareText :size="20" /></span>
      <div class="analytics-question__body">
        <label for="analytics-question">问数据</label>
        <input id="analytics-question" v-model="question" maxlength="500" type="text" placeholder="例如：最近三个月哪些商品退款金额最高？" :disabled="naturalLoading" @keydown.enter.prevent="askData">
        <div class="analytics-suggestions">
          <button type="button" :disabled="busy" @click="useSuggestion('最近三个月哪些商品退款金额最高？')">商品退款排行</button>
          <button type="button" :disabled="busy" @click="useSuggestion('最近三十天的订单趋势')">订单趋势</button>
          <button type="button" :disabled="busy" @click="useSuggestion('今年各类售后工单有多少？')">工单分类</button>
        </div>
      </div>
      <button type="button" class="primary-button analytics-ask" :disabled="!question.trim() || busy" @click="askData">
        <Sparkles :size="17" />{{ naturalLoading ? '正在理解...' : '智能分析' }}
      </button>
    </section>

    <section class="analytics-query" aria-label="分析条件">
      <div class="analytics-field analytics-field--report">
        <label for="analytics-report">分析主题</label>
        <el-select id="analytics-report" v-model="selectedCode" placeholder="选择分析主题">
          <el-option v-for="preset in reportPresets" :key="preset.code" :value="preset.code" :label="preset.label">
            <div class="analytics-option"><strong>{{ preset.label }}</strong><small>{{ preset.description }}</small></div>
          </el-option>
        </el-select>
      </div>
      <div class="analytics-field analytics-field--date">
        <label for="analytics-date">日期范围</label>
        <el-date-picker id="analytics-date" v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" :clearable="false" :disabled-date="(date: Date) => date.getTime() > Date.now()" />
      </div>
      <div class="analytics-field">
        <label for="analytics-granularity">时间粒度</label>
        <el-select id="analytics-granularity" v-model="granularity" :disabled="!isTimeline">
          <el-option label="按天" value="DAY" />
          <el-option label="按周" value="WEEK" />
          <el-option label="按月" value="MONTH" />
        </el-select>
      </div>
      <div class="analytics-field analytics-field--limit">
        <label for="analytics-limit">返回条数</label>
        <el-input-number id="analytics-limit" v-model="limit" :min="1" :max="50" controls-position="right" />
      </div>
      <button type="button" class="primary-button analytics-run" :disabled="busy" @click="runAnalysis">
        <BarChart3 :size="17" />{{ loading ? '分析中...' : '运行分析' }}
      </button>
    </section>

    <div v-if="errorMessage" class="analytics-feedback" role="alert">
      <CircleAlert :size="17" />
      <span>{{ errorMessage }}</span>
      <button type="button" class="text-link" :disabled="busy" @click="lastRequestMode === 'natural' ? askData() : runAnalysis()">重试</button>
    </div>

    <section class="analytics-summary" :class="{ 'analytics-loading': busy }" aria-label="分析摘要">
      <article class="analytics-stat analytics-stat--teal">
        <span class="analytics-stat__icon"><Sparkles :size="19" /></span>
        <div><small>统计合计</small><strong>{{ result ? displayValue(total) : '--' }}</strong></div>
      </article>
      <article class="analytics-stat analytics-stat--orange">
        <span class="analytics-stat__icon"><Trophy :size="19" /></span>
        <div><small>最高项</small><strong>{{ highest ? displayLabel(highest.label) : '--' }}</strong><em>{{ highest ? displayValue(Number(highest.value)) : '等待分析' }}</em></div>
      </article>
      <article class="analytics-stat analytics-stat--purple">
        <span class="analytics-stat__icon"><Hash :size="19" /></span>
        <div><small>结果条目</small><strong>{{ result ? points.length : '--' }}</strong></div>
      </article>
      <article class="analytics-stat analytics-stat--green">
        <span class="analytics-stat__icon"><CalendarRange :size="19" /></span>
        <div><small>分析区间</small><strong class="analytics-stat__range">{{ dateRange[0] }}<br>{{ dateRange[1] }}</strong></div>
      </article>
    </section>

    <section v-if="explanation" class="data-section analytics-insight">
      <div class="analytics-insight__head">
        <div><p class="eyebrow">AI INSIGHT / 模型解读</p><h2>分析结论</h2></div>
        <span>{{ explanationModel }} · {{ explanationLatency }}ms</span>
      </div>
      <MarkdownContent class="analytics-insight__content" :content="explanation" />
    </section>

    <section class="data-section analytics-result" :class="{ 'analytics-loading': busy }">
      <div class="section-heading">
        <div><p class="eyebrow">VISUALIZATION / 实时查询</p><h2>{{ activePreset.label }}</h2></div>
        <div class="analytics-result__status">
          <span v-if="result" class="analytics-source" :class="{ 'analytics-source--cache': result.cached }">
            <Database :size="13" />{{ result.cached ? 'REDIS CACHE' : 'POSTGRESQL' }}
          </span>
          <span class="analytics-result__meta">{{ activePreset.description }}</span>
        </div>
      </div>

      <div v-if="loading && !result" class="analytics-empty" role="status">
        <span class="analytics-loader" />正在聚合业务数据...
      </div>
      <div v-else-if="result && points.length === 0" class="analytics-empty">
        <BarChart3 :size="32" />所选范围内暂无可分析数据
      </div>
      <template v-else-if="result">
        <AnalyticsChart :points="points" :metric="result.metric" :dimension="result.dimension" />
        <div class="table-scroll analytics-table-wrap">
          <table class="orders-table analytics-table">
            <thead><tr><th>排名</th><th>{{ isTimeline ? '时间分组' : '分析维度' }}</th><th>{{ result.metric === 'REFUND_AMOUNT' ? '退款金额' : '数量' }}</th><th>占比</th></tr></thead>
            <tbody>
              <tr v-for="(point, index) in points" :key="`${point.label}-${index}`">
                <td class="mono">{{ String(index + 1).padStart(2, '0') }}</td>
                <td><strong>{{ displayLabel(point.label) }}</strong></td>
                <td class="analytics-table__value">{{ displayValue(Number(point.value)) }}</td>
                <td><span class="analytics-share"><i :style="{ width: `${total > 0 ? (Number(point.value) / total) * 100 : 0}%` }" /></span><small>{{ total > 0 ? `${((Number(point.value) / total) * 100).toFixed(1)}%` : '0.0%' }}</small></td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>
      <div v-else class="analytics-empty"><BarChart3 :size="32" />设置条件后运行分析</div>
    </section>

    <section v-if="canViewAudits" class="data-section analytics-audit">
      <div class="section-heading analytics-audit__heading">
        <div><p class="eyebrow">AUDIT TRAIL / 管理员可见</p><h2>查询与模型调用审计</h2></div>
        <button type="button" class="icon-button" :disabled="auditLoading" title="刷新审计" aria-label="刷新查询审计" @click="loadAudits()">
          <RefreshCw :size="16" :class="{ spinning: auditLoading }" />
        </button>
      </div>

      <div class="analytics-audit__filters">
        <el-select v-model="auditMode" placeholder="全部查询模式" :disabled="auditLoading" aria-label="筛选查询模式" @change="loadAudits(1)">
          <el-option label="全部查询模式" value="" />
          <el-option label="结构化查询" value="STRUCTURED" />
          <el-option label="自然语言" value="NATURAL_LANGUAGE" />
        </el-select>
        <el-select v-model="auditStatus" placeholder="全部状态" :disabled="auditLoading" aria-label="筛选审计状态" @change="loadAudits(1)">
          <el-option label="全部状态" value="" />
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
        </el-select>
        <span>共 {{ auditTotal }} 条记录</span>
        <button v-if="auditMode || auditStatus" type="button" class="text-link" :disabled="auditLoading" @click="resetAuditFilters">重置</button>
      </div>

      <div v-if="auditError" class="analytics-feedback" role="alert">
        <CircleAlert :size="17" /><span>{{ auditError }}</span>
        <button type="button" class="text-link" @click="loadAudits()">重试</button>
      </div>
      <div v-else-if="auditLoading && audits.length === 0" class="analytics-audit__empty" role="status">
        <span class="analytics-loader" />正在读取审计记录...
      </div>
      <div v-else-if="audits.length === 0" class="analytics-audit__empty"><History :size="28" />暂无查询审计</div>
      <template v-else>
        <div class="table-scroll analytics-audit__table-wrap" :class="{ 'analytics-loading': auditLoading }">
          <table class="orders-table analytics-audit__table">
            <thead><tr><th>时间 / 用户</th><th>查询内容</th><th>执行状态</th><th>结果来源</th><th>模型 / 耗时</th></tr></thead>
            <tbody>
              <tr v-for="audit in audits" :key="audit.id">
                <td><strong>{{ formatAuditTime(audit.createdAt) }}</strong><small>{{ audit.username }}</small></td>
                <td><strong>{{ auditQueryLabel(audit) }}</strong><small>{{ audit.queryMode === 'NATURAL_LANGUAGE' ? '自然语言' : '结构化查询' }} · {{ audit.startDate || '--' }} 至 {{ audit.endDate || '--' }}</small></td>
                <td><span class="analytics-audit__state" :class="`analytics-audit__state--${audit.status.toLowerCase()}`">{{ audit.status === 'SUCCESS' ? '成功' : '失败' }}</span><small>{{ audit.errorCode || `${audit.resultCount ?? 0} 条结果` }}</small></td>
                <td><span class="analytics-source" :class="{ 'analytics-source--cache': audit.cacheHit }"><Database :size="12" />{{ audit.cacheHit ? 'Redis' : 'PostgreSQL' }}</span></td>
                <td><strong>{{ audit.modelName || '未调用模型' }}</strong><small class="mono">{{ audit.latencyMs }} ms</small></td>
              </tr>
            </tbody>
          </table>
        </div>
        <el-pagination v-if="auditTotal > 10" class="analytics-audit__pagination" background layout="prev, pager, next" :page-size="10" :total="auditTotal" :current-page="auditPage" @current-change="loadAudits" />
      </template>
    </section>
  </div>
</template>
