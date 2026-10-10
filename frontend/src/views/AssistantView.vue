<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Archive, ArrowUpRight, BookOpenCheck, Bot, CheckCircle2, CircleAlert, CircleHelp, Clock3, MessageSquarePlus, RefreshCw, Send, ShieldCheck, Sparkles, Square, UserRound, Wrench, X } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  aiApi,
  type AiAgentActionResult,
  type AiAgentTool,
  type AiCapability,
  type AiChatMessage,
  type AiChatSource,
  type AiConversation,
  type AiConversationMessage,
  type AiPendingAction,
  type AiResponseType,
  type AiSuggestion,
} from '@/api/ai'
import { sanitizeAgentNavigation } from '@/utils/routeQuery'
import { readPositiveQueryId } from '@/utils/routeQuery'
import { LatestRequestGate, requestErrorMessage } from '@/utils/requestState'
import { useAuthStore } from '@/stores/auth'

interface DisplayMessage extends AiChatMessage {
  id: number
  modelName?: string | null
  latencyMs?: number | null
  sources?: AiChatSource[]
  tools?: AiAgentTool[]
  pendingAction?: AiPendingAction | null
  actionResult?: AiAgentActionResult
  suggestions?: AiSuggestion[]
  responseType?: AiResponseType | null
  missingFields?: string[]
  createdAt?: string
}

const messages = ref<DisplayMessage[]>([])
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const conversations = ref<AiConversation[]>([])
const activeConversationId = ref<number | null>(null)
const draft = ref('')
const sending = ref(false)
const loadingHistory = ref(false)
const conversationListLoading = ref(false)
const conversationListError = ref('')
const historyError = ref('')
const capabilityError = ref('')
const archiving = ref(false)
const actingToken = ref('')
const capabilities = ref<AiCapability[]>([])
const sequence = ref(0)
const timeline = ref<HTMLElement>()
const composerInput = ref<HTMLTextAreaElement>()
let streamController: AbortController | null = null
const conversationListRequests = new LatestRequestGate()
const historyRequests = new LatestRequestGate()

const navigationPermissions: Record<string, string> = {
  '/orders': 'order:view',
  '/refunds': 'refund:view',
  '/tickets': 'ticket:view',
  '/knowledge': 'knowledge:view',
  '/data-analysis': 'analytics:view',
}

const remaining = computed(() => 4000 - draft.value.length)
const activeConversation = computed(() => conversations.value.find(item => item.id === activeConversationId.value))
const starterPrompts = computed(() => capabilities.value
  .flatMap(capability => capability.examples.map(prompt => ({
    label: capability.displayName,
    prompt,
    capabilityCode: capability.code,
  })))
  .slice(0, 8))

async function scrollToBottom() {
  await nextTick()
  timeline.value?.scrollTo({ top: timeline.value.scrollHeight, behavior: 'smooth' })
}

async function loadConversations(selectLatest = false) {
  const currentRequest = conversationListRequests.begin()
  conversationListLoading.value = true
  conversationListError.value = ''
  try {
    const page = await aiApi.listConversations()
    if (!conversationListRequests.isCurrent(currentRequest)) return
    conversations.value = page.items
    if (selectLatest && !activeConversationId.value && page.items.length) {
      await openConversation(page.items[0].id)
    }
  } catch (error) {
    if (!conversationListRequests.isCurrent(currentRequest)) return
    conversationListError.value = requestErrorMessage(error, '会话列表加载失败')
  } finally {
    if (conversationListRequests.isCurrent(currentRequest)) conversationListLoading.value = false
  }
}

async function openConversation(id: number) {
  if (sending.value || (id === activeConversationId.value && !historyError.value)) return
  const currentRequest = historyRequests.begin()
  activeConversationId.value = id
  loadingHistory.value = true
  historyError.value = ''
  messages.value = []
  try {
    const history = await aiApi.listConversationMessages(id)
    if (!historyRequests.isCurrent(currentRequest)) return
    messages.value = history.map(toDisplayMessage)
    sequence.value = Math.max(0, ...history.map(item => item.id))
    await scrollToBottom()
  } catch (error) {
    if (!historyRequests.isCurrent(currentRequest)) return
    console.error('[AI助手] 加载会话失败', error)
    historyError.value = requestErrorMessage(error, '会话加载失败')
  } finally {
    if (historyRequests.isCurrent(currentRequest)) loadingHistory.value = false
  }
}

function startNewConversation() {
  if (sending.value) return
  historyRequests.invalidate()
  activeConversationId.value = null
  messages.value = []
  historyError.value = ''
  loadingHistory.value = false
  draft.value = ''
  void nextTick(() => composerInput.value?.focus())
}

async function archiveCurrentConversation() {
  if (!activeConversationId.value || sending.value || archiving.value) return
  const conversationId = activeConversationId.value
  try {
    await ElMessageBox.confirm('归档后会话不会继续显示，但历史消息仍保留在数据库中。', '归档当前会话', {
      confirmButtonText: '确认归档', cancelButtonText: '取消', type: 'warning',
    })
    archiving.value = true
    await aiApi.archiveConversation(conversationId)
    startNewConversation()
    await loadConversations()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    console.error('[AI助手] 归档会话失败', error)
    ElMessage.error(error instanceof Error ? error.message : '会话归档失败')
  } finally {
    archiving.value = false
  }
}

async function ensureConversation() {
  if (activeConversationId.value) return activeConversationId.value
  const conversation = await aiApi.createConversation()
  conversations.value.unshift(conversation)
  activeConversationId.value = conversation.id
  return conversation.id
}

async function send() {
  const content = draft.value.trim()
  if (!content || sending.value) return
  let conversationId: number
  try {
    conversationId = await ensureConversation()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '无法创建 AI 会话')
    return
  }

  const userMessage: DisplayMessage = { id: -(++sequence.value), role: 'USER', content }
  const assistantMessage: DisplayMessage = {
    id: -(++sequence.value), role: 'ASSISTANT', content: '', responseType: 'ANSWER',
    tools: [], suggestions: [], missingFields: [], pendingAction: null,
  }
  messages.value.push(userMessage, assistantMessage)
  draft.value = ''
  sending.value = true
  streamController = new AbortController()
  await scrollToBottom()
  try {
    await aiApi.streamConversationMessage(conversationId, content, {
      onDelta: (chunk) => {
        assistantMessage.content += chunk
        void scrollToBottom()
      },
      onResult: (turn) => {
        userMessage.id = turn.userMessageId
        assistantMessage.id = turn.assistantMessageId
        Object.assign(assistantMessage, turn.reply)
        const index = conversations.value.findIndex(item => item.id === turn.conversation.id)
        if (index >= 0) conversations.value.splice(index, 1)
        conversations.value.unshift(turn.conversation)
      },
    }, streamController.signal)
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      assistantMessage.content ||= '已停止接收本次回答。服务器可能已经完成处理，可重新打开会话查看最终结果。'
    } else {
      console.error('[AI助手] 流式消息失败', error)
      assistantMessage.responseType = 'SYSTEM_ERROR'
      assistantMessage.content ||= error instanceof Error ? error.message : 'AI 请求失败'
      ElMessage.error(assistantMessage.content)
    }
  } finally {
    sending.value = false
    streamController = null
    await loadConversations()
    await scrollToBottom()
  }
}

function stopStream() {
  streamController?.abort()
}

async function confirmAction(message: DisplayMessage) {
  const action = message.pendingAction
  if (!action || actingToken.value) return
  actingToken.value = action.token
  try {
    const result = await aiApi.confirmAgentAction(action.token)
    message.actionResult = result
    message.pendingAction = null
    ElMessage.success(result.summary)
  } catch (error) {
    console.error('[AI助手] 确认受控操作失败', error)
    ElMessage.error(error instanceof Error ? error.message : '操作确认失败')
  } finally {
    actingToken.value = ''
    await scrollToBottom()
  }
}

async function cancelAction(message: DisplayMessage) {
  const action = message.pendingAction
  if (!action || actingToken.value) return
  actingToken.value = action.token
  try {
    const result = await aiApi.cancelAgentAction(action.token)
    message.actionResult = result
    message.pendingAction = null
    ElMessage.success('已取消，系统没有执行写操作')
  } catch (error) {
    console.error('[AI助手] 取消受控操作失败', error)
    ElMessage.error(error instanceof Error ? error.message : '取消操作失败')
  } finally {
    actingToken.value = ''
  }
}

function toDisplayMessage(message: AiConversationMessage): DisplayMessage {
  return { ...message, pendingAction: null }
}

function canNavigate(tool: AiAgentTool) {
  const navigation = sanitizeAgentNavigation(tool.navigation)
  return Boolean(navigation && auth.can(navigationPermissions[navigation.path] || ''))
}

function openToolNavigation(tool: AiAgentTool) {
  const navigation = sanitizeAgentNavigation(tool.navigation)
  if (!navigation) {
    ElMessage.warning('该工具结果没有可用的业务页面')
    return
  }
  const permission = navigationPermissions[navigation.path]
  if (!permission || !auth.can(permission)) {
    ElMessage.warning('当前账号无权访问该业务页面')
    return
  }
  void router.push(navigation)
}

async function loadCapabilities() {
  capabilityError.value = ''
  try {
    capabilities.value = await aiApi.listCapabilities()
  } catch (error) {
    capabilityError.value = error instanceof Error ? error.message : '助手能力加载失败'
  }
}

function formatExpiry(value: string) {
  return new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    .format(new Date(value))
}

function formatConversationTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
    .format(new Date(value))
}

async function applySuggestion(suggestion: AiSuggestion) {
  draft.value = suggestion.prompt
  await nextTick()
  composerInput.value?.focus()
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    void send()
  }
}

onMounted(async () => {
  const linkedConversationId = readPositiveQueryId(route.query.conversationId)
  await Promise.all([loadCapabilities(), loadConversations(!linkedConversationId)])
  if (linkedConversationId) await openConversation(linkedConversationId)
})

onBeforeUnmount(() => streamController?.abort())
</script>

<template>
  <div class="assistant-view">
    <header class="assistant-head">
      <div><p class="eyebrow">INTELLIGENCE / CHAT</p><h1>AI 助手<span class="heading-spark">.</span></h1></div>
      <div class="assistant-head__tools">
        <div class="assistant-smart-mode" title="自动选择通用问答、知识库和业务工具">
          <Sparkles :size="15" />
          <span>智能模式</span>
          <i>AUTO</i>
        </div>
        <button type="button" class="icon-button" title="归档当前会话" aria-label="归档当前会话" :disabled="!activeConversationId || sending || archiving" @click="archiveCurrentConversation"><Archive :size="17" /></button>
        <button type="button" class="icon-button" title="新建会话" aria-label="新建会话" :disabled="sending" @click="startNewConversation"><MessageSquarePlus :size="18" /></button>
      </div>
    </header>

    <div class="assistant-workspace">
      <aside class="conversation-rail" aria-label="AI 会话历史">
        <header><span>会话记录</span><small>{{ conversations.length }}</small></header>
        <button type="button" class="conversation-new" :class="{ 'conversation-new--active': !activeConversationId }" :disabled="sending" @click="startNewConversation">
          <MessageSquarePlus :size="15" /><span>新对话</span>
        </button>
        <div class="conversation-list">
          <button v-for="conversation in conversations" :key="conversation.id" type="button" class="conversation-item" :class="{ 'conversation-item--active': conversation.id === activeConversationId }" :disabled="sending" @click="openConversation(conversation.id)">
            <span>{{ conversation.title }}</span>
            <small>{{ formatConversationTime(conversation.updatedAt) }}</small>
            <i v-if="conversation.awaitingParameters">待补充</i>
          </button>
          <p v-if="conversationListLoading && !conversations.length" role="status">正在加载会话...</p>
          <div v-else-if="conversationListError" class="conversation-list__error" role="alert"><span>{{ conversationListError }}</span><button type="button" @click="loadConversations()">重试</button></div>
          <p v-else-if="!conversations.length">还没有历史会话</p>
        </div>
      </aside>

      <div class="assistant-chat">
        <section ref="timeline" class="assistant-timeline" aria-live="polite">
          <div v-if="loadingHistory" class="assistant-history-loading" role="status"><i /><span>正在读取会话</span></div>
          <div v-else-if="historyError" class="assistant-history-error" role="alert"><CircleAlert :size="25" /><strong>无法打开关联会话</strong><span>{{ historyError }}</span><button type="button" class="outline-button" @click="activeConversationId && openConversation(activeConversationId)"><RefreshCw :size="14" />重试</button></div>
          <div v-else-if="messages.length === 0" class="assistant-empty">
            <div class="assistant-core"><Bot :size="36" /><span /><span /><span /></div>
            <h2>SmartMall Intelligence</h2>
            <p>{{ activeConversation?.title || 'READY FOR INPUT' }}</p>
            <div v-if="capabilityError" class="assistant-capability-error" role="alert"><span>{{ capabilityError }}</span><button type="button" @click="loadCapabilities">重试</button></div>
            <div v-else-if="starterPrompts.length" class="assistant-starters" aria-label="可用能力示例">
              <button v-for="suggestion in starterPrompts" :key="`${suggestion.capabilityCode}-${suggestion.prompt}`" type="button" @click="applySuggestion(suggestion)">
                <span>{{ suggestion.label }}</span><strong>{{ suggestion.prompt }}</strong><ArrowUpRight :size="14" />
              </button>
            </div>
          </div>
          <article v-for="message in messages" :key="message.id" class="chat-message" :class="[`chat-message--${message.role.toLowerCase()}`, message.responseType ? `chat-message--${message.responseType.toLowerCase()}` : '']">
            <span class="chat-message__avatar"><UserRound v-if="message.role === 'USER'" :size="17" /><Sparkles v-else :size="17" /></span>
            <div>
              <header><strong>{{ message.role === 'USER' ? 'YOU' : 'SMARTMALL AI' }}</strong><small v-if="message.modelName">{{ message.modelName }} · {{ message.latencyMs }}ms</small></header>
              <div v-if="message.responseType === 'CLARIFICATION' || message.responseType === 'UNSUPPORTED'" class="chat-guidance-label">
                <CircleHelp :size="13" />{{ message.responseType === 'CLARIFICATION' ? '需要补充信息' : '当前能力说明' }}
              </div>
              <p>{{ message.content }}</p>
              <details v-if="message.sources?.length" class="chat-sources">
                <summary><BookOpenCheck :size="14" />引用来源 {{ message.sources.length }}</summary>
                <div class="chat-sources__list">
                  <article v-for="source in message.sources" :key="`${message.id}-${source.reference}`">
                    <header><strong>[资料{{ source.reference }}] {{ source.documentName }}</strong><small>片段 {{ source.chunkIndex + 1 }} · {{ Math.round(source.score * 100) }}%</small></header>
                    <p>{{ source.excerpt }}</p>
                  </article>
                </div>
              </details>
              <div v-if="message.tools?.length" class="chat-tools" aria-label="Agent 工具调用">
                <div v-for="tool in message.tools" :key="`${message.id}-${tool.toolName}`" class="chat-tool">
                  <span><Wrench :size="13" /></span><div><strong>{{ tool.displayName }}</strong><small>{{ tool.summary }} · {{ tool.latencyMs }}ms</small></div>
                  <div class="chat-tool__actions"><i>READ ONLY</i><button v-if="canNavigate(tool)" type="button" @click="openToolNavigation(tool)">查看<ArrowUpRight :size="12" /></button></div>
                </div>
              </div>
              <section v-if="message.pendingAction" class="agent-confirmation" aria-label="待确认操作">
                <header><span><ShieldCheck :size="15" /></span><div><strong>{{ message.pendingAction.displayName }}</strong><small>WRITE ACTION / USER CONFIRMATION</small></div><i><Clock3 :size="12" />{{ formatExpiry(message.pendingAction.expiresAt) }} 前有效</i></header>
                <p>{{ message.pendingAction.summary }}</p>
                <footer>
                  <button type="button" class="agent-action-button agent-action-button--cancel" :disabled="Boolean(actingToken)" @click="cancelAction(message)"><X :size="14" />取消</button>
                  <button type="button" class="agent-action-button agent-action-button--confirm" :disabled="Boolean(actingToken)" @click="confirmAction(message)"><ShieldCheck :size="14" />{{ actingToken === message.pendingAction.token ? '执行中...' : '确认执行' }}</button>
                </footer>
              </section>
              <div v-if="message.actionResult" class="agent-action-result" :class="`agent-action-result--${message.actionResult.status.toLowerCase()}`">
                <CheckCircle2 v-if="message.actionResult.status === 'SUCCEEDED'" :size="15" /><X v-else :size="15" />
                <div><strong>{{ message.actionResult.displayName }}</strong><small>{{ message.actionResult.summary }}<template v-if="message.actionResult.latencyMs"> · {{ message.actionResult.latencyMs }}ms</template></small></div><i>{{ message.actionResult.status }}</i>
              </div>
              <div v-if="message.suggestions?.length && !message.pendingAction" class="chat-suggestions" aria-label="推荐问题">
                <button v-for="suggestion in message.suggestions" :key="`${message.id}-${suggestion.capabilityCode}`" type="button" @click="applySuggestion(suggestion)">{{ suggestion.label }}<ArrowUpRight :size="12" /></button>
              </div>
            </div>
          </article>
        </section>

        <footer class="assistant-composer">
          <textarea ref="composerInput" v-model="draft" maxlength="4000" rows="3" placeholder="输入任何问题" :disabled="sending" @keydown="handleKeydown" />
          <div class="assistant-composer__meta"><span>{{ remaining }}</span><span>{{ activeConversation?.awaitingParameters ? 'MULTI-TURN / WAITING INPUT' : 'SMART AGENT / SSE' }}</span></div>
          <button v-if="sending" type="button" class="assistant-send assistant-send--stop" title="停止接收" aria-label="停止接收" @click="stopStream"><Square :size="16" /></button>
          <button v-else type="button" class="assistant-send" title="发送消息" aria-label="发送消息" :disabled="!draft.trim()" @click="send"><Send :size="19" /></button>
        </footer>
      </div>
    </div>
  </div>
</template>
