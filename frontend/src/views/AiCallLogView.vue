<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Activity, Bot, Braces, CircleAlert, Clock3, DatabaseZap,
  Eye, Gauge, Layers3, Link2, RadioTower, RefreshCw, Search, ShieldCheck,
} from 'lucide-vue-next'
import { aiApi, type AiCallLog } from '@/api/ai'
import { useAuthStore } from '@/stores/auth'
import { readTraceId } from '@/utils/routeQuery'
import { auditCorrelationNavigation } from '@/utils/auditNavigation'
import { LatestRequestGate, requestErrorMessage } from '@/utils/requestState'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const logs = ref<AiCallLog[]>([])
const page = ref(1)
const pageSize = 20
const total = ref(0)
const modelType = ref('')
const provider = ref('')
const scene = ref('')
const status = ref('')
const loading = ref(false)
const errorMessage = ref('')
const traceId = ref('')
const detailOpen = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detailId = ref<number | null>(null)
const detail = ref<AiCallLog | null>(null)
const listRequests = new LatestRequestGate()
const detailRequests = new LatestRequestGate()

const successCount = computed(() => logs.value.filter(item => item.status === 'SUCCESS').length)
const successRate = computed(() => logs.value.length
  ? Math.round((successCount.value / logs.value.length) * 100)
  : 0)
const averageLatency = computed(() => logs.value.length
  ? Math.round(logs.value.reduce((sum, item) => sum + item.latencyMs, 0) / logs.value.length)
  : 0)
const inputCharacters = computed(() => logs.value.reduce((sum, item) => sum + item.inputCharacterCount, 0))

const providerOptions = [
  ['OPENAI', 'OpenAI'], ['DEEPSEEK', 'DeepSeek'], ['QWEN', '通义千问'],
  ['MOONSHOT', 'Moonshot'], ['ZHIPU', '智谱 AI'], ['OLLAMA', 'Ollama'],
  ['OPENAI_COMPATIBLE', 'OpenAI 兼容'],
]
const providerLabels = Object.fromEntries(providerOptions)
const sceneOptions = [
  ['ASSISTANT_CHAT', '助手对话'], ['KNOWLEDGE_RAG', '知识库问答'],
  ['INTERNAL_ORCHESTRATION', '内部编排'], ['KNOWLEDGE_EMBEDDING', '知识库向量化'],
  ['MODEL_CONNECTIVITY_TEST', '模型连通测试'],
]
const sceneLabels = Object.fromEntries(sceneOptions)

async function loadLogs(targetPage = page.value) {
  const currentRequest = listRequests.begin()
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await aiApi.listCallLogs(targetPage, pageSize, {
      modelType: modelType.value || undefined,
      provider: provider.value || undefined,
      scene: scene.value || undefined,
      status: status.value || undefined,
      traceId: traceId.value || undefined,
    })
    if (!listRequests.isCurrent(currentRequest)) return
    const lastPage = Math.max(1, Math.ceil(result.total / pageSize))
    if (result.total > 0 && targetPage > lastPage) {
      await loadLogs(lastPage)
      return
    }
    logs.value = result.items
    total.value = result.total
    page.value = result.page
  } catch (error) {
    if (!listRequests.isCurrent(currentRequest)) return
    logs.value = []
    total.value = 0
    errorMessage.value = requestErrorMessage(error, 'AI 调用日志加载失败')
  } finally {
    if (listRequests.isCurrent(currentRequest)) loading.value = false
  }
}

function applyFilters() {
  void loadLogs(1)
}

function resetFilters() {
  modelType.value = ''
  provider.value = ''
  scene.value = ''
  status.value = ''
  traceId.value = ''
  void router.replace({ query: {} })
  void loadLogs(1)
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false,
  }).format(new Date(value))
}

function outputLabel(log: AiCallLog) {
  if (log.outputUnitCount === null) return `${log.outputCount} 项`
  return log.modelType === 'CHAT'
    ? `${log.outputCount} 条 / ${log.outputUnitCount} 字符`
    : `${log.outputCount} 向量 / ${log.outputUnitCount} 维度单元`
}

async function openDetail(id: number) {
  const currentRequest = detailRequests.begin()
  detailId.value = id
  detailOpen.value = true
  detailLoading.value = true
  detailError.value = ''
  detail.value = null
  try {
    const response = await aiApi.getCallLog(id)
    if (detailRequests.isCurrent(currentRequest)) detail.value = response
  } catch (error) {
    if (!detailRequests.isCurrent(currentRequest)) return
    detailError.value = requestErrorMessage(error, '调用详情加载失败')
  } finally {
    if (detailRequests.isCurrent(currentRequest)) detailLoading.value = false
  }
}

function openAgentAudits(value: string) {
  const target = auditCorrelationNavigation('agent-audits', value, auth.can('agent:audit_view'))
  if (target) void router.push(target)
}

onMounted(() => {
  traceId.value = readTraceId(route.query.traceId)
  void loadLogs(1)
})
</script>

<template>
  <div class="call-log-view">
    <header class="page-heading call-log-heading">
      <div>
        <p class="eyebrow">AI OBSERVABILITY / SAFE METADATA</p>
        <h1>调用日志<span class="heading-spark">.</span></h1>
        <p>追踪模型运行状态与规模指标，不展示提示词、回答或密钥</p>
      </div>
      <button type="button" class="icon-button" :disabled="loading" title="刷新调用日志" aria-label="刷新调用日志" @click="loadLogs()">
        <RefreshCw :size="17" :class="{ spinning: loading }" />
      </button>
    </header>

    <section class="call-log-aurora" aria-label="当前页调用概览">
      <div class="call-log-orbit call-log-orbit--one" />
      <div class="call-log-orbit call-log-orbit--two" />
      <article><span><RadioTower :size="14" />日志总量</span><strong>{{ total }}</strong><small>ADMIN VIEW</small></article>
      <article><span><ShieldCheck :size="14" />当前页成功率</span><strong>{{ successRate }}<em>%</em></strong><small>{{ successCount }} / {{ logs.length }} SUCCESS</small></article>
      <article><span><Gauge :size="14" />平均耗时</span><strong>{{ averageLatency }}<em>ms</em></strong><small>CURRENT PAGE</small></article>
      <article><span><Braces :size="14" />输入规模</span><strong>{{ inputCharacters.toLocaleString() }}</strong><small>CHARACTERS / PAGE</small></article>
    </section>

    <section class="call-log-filter" aria-label="调用日志筛选">
      <el-select v-model="modelType" placeholder="全部模型类型" clearable :disabled="loading" aria-label="筛选模型类型" @change="applyFilters">
        <el-option label="Chat" value="CHAT" /><el-option label="Embedding" value="EMBEDDING" />
      </el-select>
      <el-select v-model="provider" placeholder="全部供应商" clearable :disabled="loading" aria-label="筛选模型供应商" @change="applyFilters">
        <el-option v-for="item in providerOptions" :key="item[0]" :label="item[1]" :value="item[0]" />
      </el-select>
      <el-select v-model="scene" placeholder="全部调用场景" clearable :disabled="loading" aria-label="筛选调用场景" @change="applyFilters">
        <el-option v-for="item in sceneOptions" :key="item[0]" :label="item[1]" :value="item[0]" />
      </el-select>
      <el-select v-model="status" placeholder="全部状态" clearable :disabled="loading" aria-label="筛选调用状态" @change="applyFilters">
        <el-option label="成功" value="SUCCESS" /><el-option label="失败" value="FAILED" />
      </el-select>
      <span v-if="traceId" class="audit-trace-filter"><Link2 :size="12" />关联追踪 {{ traceId.slice(0, 8) }}</span>
      <span><Search :size="13" />命中 {{ total }} 条</span>
      <button v-if="modelType || provider || scene || status || traceId" type="button" class="text-link" :disabled="loading" @click="resetFilters">清除筛选</button>
    </section>

    <section class="data-section call-log-panel">
      <div class="section-heading">
        <div><p class="eyebrow">MODEL TRAFFIC / 实时记录</p><h2>模型调用明细</h2></div>
        <span class="call-log-safe"><ShieldCheck :size="13" />敏感内容不落库</span>
      </div>

      <div v-if="errorMessage" class="audit-feedback" role="alert">
        <CircleAlert :size="18" /><div><strong>日志读取失败</strong><span>{{ errorMessage }}</span></div>
        <button type="button" class="outline-button" @click="loadLogs()">重试</button>
      </div>
      <div v-else-if="loading && logs.length === 0" class="audit-empty" role="status">
        <span class="audit-empty__icon audit-empty__icon--loading"><Activity :size="24" /></span><strong>正在读取调用日志...</strong><small>只加载安全元数据</small>
      </div>
      <div v-else-if="logs.length === 0" class="audit-empty">
        <span class="audit-empty__icon"><DatabaseZap :size="29" /></span><strong>暂无符合条件的调用日志</strong><small>模型产生调用后，安全摘要会显示在这里</small>
      </div>
      <template v-else>
        <div class="table-scroll call-log-table-wrap" :class="{ 'call-log-loading': loading }">
          <table class="orders-table call-log-table">
            <thead><tr><th>调用时间</th><th>模型</th><th>场景</th><th>状态</th><th>输入规模</th><th>输出规模</th><th>耗时</th><th>详情</th></tr></thead>
            <tbody>
              <tr v-for="log in logs" :key="log.id">
                <td><span class="call-log-time"><Clock3 :size="12" />{{ formatTime(log.createdAt) }}</span><small>#{{ log.id }}</small></td>
                <td><span class="call-log-model"><Bot v-if="log.modelType === 'CHAT'" :size="15" /><Layers3 v-else :size="15" /><strong>{{ log.modelName }}</strong><small>{{ providerLabels[log.provider] || log.provider }} · {{ log.modelType }}</small></span></td>
                <td><strong class="call-log-scene">{{ sceneLabels[log.scene] || log.scene }}</strong><small>{{ log.scene }}</small></td>
                <td><span class="audit-status" :class="`audit-status--${log.status.toLowerCase()}`"><i />{{ log.status === 'SUCCESS' ? '成功' : '失败' }}</span><small>{{ log.errorCode || 'NORMAL' }}</small></td>
                <td><strong class="mono">{{ log.inputCount }} 项</strong><small>{{ log.inputCharacterCount.toLocaleString() }} 字符</small></td>
                <td><strong class="mono call-log-output">{{ outputLabel(log) }}</strong></td>
                <td><strong class="mono call-log-latency">{{ log.latencyMs }} ms</strong></td>
                <td><button type="button" class="audit-detail-button" @click="openDetail(log.id)"><Eye :size="13" />查看</button></td>
              </tr>
            </tbody>
          </table>
        </div>
        <el-pagination v-if="total > pageSize" class="call-log-pagination" background layout="prev, pager, next, total" :page-size="pageSize" :total="total" :current-page="page" @current-change="loadLogs" />
      </template>
    </section>

    <el-drawer v-model="detailOpen" title="AI 调用详情" size="min(520px, 96vw)" class="audit-detail-drawer">
      <div v-if="detailLoading" class="audit-detail-state"><span class="analytics-loader" />正在读取安全元数据...</div>
      <div v-else-if="detailError" class="audit-detail-state audit-detail-state--error"><CircleAlert :size="18" />{{ detailError }}<button v-if="detailId" type="button" class="text-link" @click="openDetail(detailId)">重试</button></div>
      <div v-else-if="detail" class="audit-detail">
        <div class="audit-detail__banner"><ShieldCheck :size="18" /><div><strong>仅展示模型运行元数据</strong><small>不保存或返回 API Key、Prompt、回答全文及向量</small></div></div>
        <dl>
          <div><dt>记录标识</dt><dd>#{{ detail.id }}</dd></div>
          <div><dt>调用状态</dt><dd><span class="audit-status" :class="`audit-status--${detail.status.toLowerCase()}`"><i />{{ detail.status === 'SUCCESS' ? '成功' : '失败' }}</span></dd></div>
          <div><dt>模型信息</dt><dd>{{ detail.modelName }}<small>{{ providerLabels[detail.provider] || detail.provider }} · {{ detail.modelType }}</small></dd></div>
          <div><dt>调用场景</dt><dd>{{ sceneLabels[detail.scene] || detail.scene }}<small>{{ detail.scene }}</small></dd></div>
          <div><dt>输入规模</dt><dd>{{ detail.inputCount }} 项 / {{ detail.inputCharacterCount.toLocaleString() }} 字符</dd></div>
          <div><dt>输出规模</dt><dd>{{ outputLabel(detail) }}</dd></div>
          <div><dt>执行耗时</dt><dd>{{ detail.latencyMs }} ms</dd></div>
          <div><dt>失败原因</dt><dd>{{ detail.errorCode || '无' }}</dd></div>
          <div><dt>模型配置</dt><dd>{{ detail.modelConfigId ? `#${detail.modelConfigId}` : '历史配置已删除' }}</dd></div>
          <div><dt>发生时间</dt><dd>{{ formatTime(detail.createdAt) }}</dd></div>
          <div><dt>会话标识</dt><dd>{{ detail.conversationId ? `#${detail.conversationId}` : '非会话调用或历史记录' }}</dd></div>
          <div><dt>追踪标识</dt><dd class="audit-detail__trace">{{ detail.traceId || '历史记录未关联' }}</dd></div>
        </dl>
        <button v-if="detail.traceId && auth.can('agent:audit_view')" type="button" class="outline-button audit-detail__link" @click="openAgentAudits(detail.traceId)"><Link2 :size="14" />查看同次请求的 Agent 审计</button>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.call-log-view { position: relative; }
.call-log-heading { position: relative; z-index: 1; }
.call-log-aurora { position: relative; min-height: 126px; display: grid; grid-template-columns: repeat(4,minmax(0,1fr)); gap: 1px; margin-bottom: 16px; overflow: hidden; border: 1px solid rgba(94,234,212,.17); border-radius: 8px; background: #0d1721; isolation: isolate; }
.call-log-aurora::before { content: ''; position: absolute; z-index: -2; inset: -130% -10% auto; height: 310px; background: conic-gradient(from 220deg,transparent,rgba(45,212,191,.2),rgba(76,110,245,.12),rgba(168,85,247,.2),transparent 72%); filter: blur(48px); animation: aurora-drift 12s ease-in-out infinite alternate; }
.call-log-aurora::after { content: ''; position: absolute; z-index: -1; inset: 0; opacity: .32; background-image: linear-gradient(rgba(103,232,249,.045) 1px,transparent 1px),linear-gradient(90deg,rgba(103,232,249,.045) 1px,transparent 1px); background-size: 28px 28px; mask-image: linear-gradient(to bottom,black,transparent); }
.call-log-aurora article { min-width: 0; display: flex; flex-direction: column; justify-content: center; padding: 22px; border-right: 1px solid rgba(105,136,151,.14); background: rgba(10,18,28,.47); }
.call-log-aurora article:last-of-type { border-right: 0; }
.call-log-aurora article > span { display: flex; align-items: center; gap: 7px; color: #7b98a4; font-size: 9px; }
.call-log-aurora article > span svg { color: #63d9cb; }
.call-log-aurora strong { margin-top: 10px; color: #e7f5f5; font: 650 24px Consolas,monospace; text-shadow: 0 0 18px rgba(94,234,212,.12); }
.call-log-aurora strong em { margin-left: 4px; color: #78939d; font-size: 10px; font-style: normal; }
.call-log-aurora small { margin-top: 5px; color: #526c78; font: 8px Consolas,monospace; letter-spacing: .08em; }
.call-log-orbit { position: absolute; z-index: -1; border: 1px solid rgba(94,234,212,.12); border-radius: 50%; }
.call-log-orbit--one { width: 170px; height: 170px; right: 8%; top: -120px; }
.call-log-orbit--two { width: 240px; height: 240px; right: 4%; top: -170px; border-color: rgba(168,85,247,.1); }
.call-log-filter { min-height: 68px; display: flex; align-items: center; gap: 10px; padding: 12px 0; border-top: 1px solid rgba(45,212,191,.2); border-bottom: 1px solid #283944; }
.call-log-filter :deep(.el-select) { width: 158px; }
.call-log-filter :deep(.el-select__wrapper) { min-height: 36px; border: 1px solid #344853; border-radius: 5px; background: #101b25; box-shadow: none !important; }
.call-log-filter :deep(.el-select__wrapper.is-focused) { border-color: #47bfb2; box-shadow: 0 0 0 2px rgba(45,212,191,.07) !important; }
.call-log-filter > span { display: inline-flex; align-items: center; gap: 6px; margin-left: auto; color: #6d8791; font: 9px Consolas,monospace; white-space: nowrap; }
.call-log-filter > .audit-trace-filter { margin-left: 0; }
.call-log-filter .text-link { border: 0; background: transparent; white-space: nowrap; }
.call-log-panel { margin-top: 27px; }
.call-log-safe { display: inline-flex; align-items: center; gap: 6px; color: #63cbbb; font: 9px Consolas,monospace; }
.call-log-feedback { min-height: 112px; display: flex; align-items: center; gap: 13px; margin-top: 15px; padding: 18px; border: 1px solid rgba(249,115,22,.25); border-left: 2px solid #f97316; border-radius: 5px; color: #ee9b63; background: rgba(249,115,22,.045); }
.call-log-feedback div { display: flex; flex: 1; flex-direction: column; gap: 5px; }
.call-log-feedback strong { color: #f0b28a; font-size: 11px; }.call-log-feedback span { color: #8e776d; font-size: 10px; }
.call-log-empty { min-height: 330px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px; color: #728b96; }
.call-log-empty strong { color: #9db0b8; font-size: 11px; }.call-log-empty small { color: #536c78; font-size: 9px; }
.call-log-empty__icon,.call-log-pulse { width: 56px; height: 56px; display: grid; place-items: center; border: 1px solid rgba(45,212,191,.2); border-radius: 50%; color: #5bc8ba; background: radial-gradient(circle,rgba(45,212,191,.11),transparent 68%); }
.call-log-pulse { animation: telemetry-pulse 1.4s ease-in-out infinite; }
.call-log-table-wrap { margin-top: 8px; border-top: 1px solid #31444e; transition: opacity .2s; }
.call-log-loading { opacity: .55; }
.call-log-table { min-width: 1130px; }.call-log-table th:first-child { width: 170px; }.call-log-table th:nth-child(2) { width: 210px; }.call-log-table th:nth-child(3) { width: 175px; }.call-log-table th:nth-child(4) { width: 105px; }.call-log-table th:nth-last-child(2) { width: 90px; }.call-log-table th:last-child { width: 72px; }
.call-log-table td strong,.call-log-table td small { display: block; }.call-log-table td small { margin-top: 5px; color: #617983; font-size: 8px; }
.call-log-time { display: inline-flex; align-items: center; gap: 6px; color: #8399a2; font: 9px Consolas,monospace; white-space: nowrap; }
.call-log-model { display: grid; grid-template-columns: 24px minmax(0,1fr); align-items: center; gap: 2px 7px; }.call-log-model > svg { grid-row: 1 / span 2; color: #70d7cb; }.call-log-model strong { overflow: hidden; color: #d7e4e8; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.call-log-scene { color: #b5a0ea; font-size: 10px; }.call-log-status { display: inline-flex; align-items: center; gap: 7px; min-height: 23px; padding: 0 8px; border: 1px solid rgba(16,185,129,.28); border-radius: 4px; color: #65d9af; background: rgba(16,185,129,.07); font-size: 9px; }.call-log-status i { width: 5px; height: 5px; border-radius: 50%; background: currentColor; box-shadow: 0 0 7px currentColor; }.call-log-status--failed { border-color: rgba(249,115,22,.3); color: #f5a369; background: rgba(249,115,22,.07); }
.call-log-output { max-width: 185px; overflow: hidden; color: #8da5ae; font-size: 9px; text-overflow: ellipsis; white-space: nowrap; }.call-log-latency { color: #6bdacc; font-size: 10px; }.call-log-pagination { justify-content: flex-end; margin-top: 17px; }
@keyframes aurora-drift { to { transform: translate3d(-12%,18%,0) rotate(18deg); } }
@keyframes telemetry-pulse { 50% { transform: scale(1.05); border-color: rgba(45,212,191,.5); box-shadow: 0 0 24px rgba(45,212,191,.1); } }
@media (max-width: 980px) { .call-log-aurora { grid-template-columns: repeat(2,minmax(0,1fr)); }.call-log-aurora article:nth-of-type(2) { border-right: 0; }.call-log-filter { flex-wrap: wrap; }.call-log-filter :deep(.el-select) { width: calc(50% - 5px); }.call-log-filter > span { margin-left: 0; }.call-log-filter .text-link { margin-left: auto; } }
@media (max-width: 560px) { .call-log-aurora { grid-template-columns: 1fr; }.call-log-aurora article { min-height: 92px; border-right: 0; border-bottom: 1px solid rgba(105,136,151,.14); }.call-log-filter :deep(.el-select) { width: 100%; } }
</style>
