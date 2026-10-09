<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { ArrowUpRight, BookOpenCheck, Bot, CheckCircle2, CircleHelp, Clock3, Eraser, Send, ShieldCheck, Sparkles, UserRound, Wrench, X } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { aiApi, type AiAgentActionResult, type AiAgentTool, type AiCapability, type AiChatMessage, type AiChatSource, type AiPendingAction, type AiResponseType, type AiSuggestion } from '@/api/ai'

interface DisplayMessage extends AiChatMessage {
  id: number
  modelName?: string
  latencyMs?: number
  sources?: AiChatSource[]
  tools?: AiAgentTool[]
  pendingAction?: AiPendingAction | null
  actionResult?: AiAgentActionResult
  suggestions?: AiSuggestion[]
  responseType?: AiResponseType
  missingFields?: string[]
}

const messages = ref<DisplayMessage[]>([])
const draft = ref('')
const sending = ref(false)
const actingToken = ref('')
const capabilities = ref<AiCapability[]>([])
const sequence = ref(0)
const timeline = ref<HTMLElement>()
const composerInput = ref<HTMLTextAreaElement>()
const remaining = computed(() => 4000 - draft.value.length)
const starterPrompts = computed(() => capabilities.value
  .flatMap(capability => capability.examples.map(prompt => ({
    label: capability.displayName,
    prompt,
    capabilityCode: capability.code,
  })))
  .slice(0, 8))

function clearConversation() {
  messages.value = []
  draft.value = ''
}

async function scrollToBottom() {
  await nextTick()
  timeline.value?.scrollTo({ top: timeline.value.scrollHeight, behavior: 'smooth' })
}

async function send() {
  const content = draft.value.trim()
  if (!content || sending.value) return
  messages.value.push({ id: ++sequence.value, role: 'USER', content })
  draft.value = ''
  sending.value = true
  await scrollToBottom()
  try {
    const history = messages.value.slice(-30).map(({ role, content: text, actionResult }) => ({
      role,
      content: actionResult ? `${text}\n\n[受控操作结果：${actionResult.summary}]` : text,
    }))
    const reply = await aiApi.chat(history, false, true)
    messages.value.push({ id: ++sequence.value, role: 'ASSISTANT', ...reply })
  } catch (error) {
    console.error('[AI助手] 发送消息失败', error)
    messages.value.pop()
    ElMessage.error(error instanceof Error ? error.message : 'AI 请求失败')
  } finally {
    sending.value = false
    await scrollToBottom()
  }
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

function formatExpiry(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(new Date(value))
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
  try {
    capabilities.value = await aiApi.listCapabilities()
  } catch (error) {
    console.error('[AI助手] 获取可用能力失败', error)
  }
})
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
        <button type="button" class="icon-button" title="清空对话" aria-label="清空对话" :disabled="messages.length === 0" @click="clearConversation"><Eraser :size="18" /></button>
      </div>
    </header>

    <section ref="timeline" class="assistant-timeline" aria-live="polite">
      <div v-if="messages.length === 0" class="assistant-empty">
        <div class="assistant-core"><Bot :size="36" /><span /><span /><span /></div>
        <h2>SmartMall Intelligence</h2>
        <p>READY FOR INPUT</p>
        <div v-if="starterPrompts.length" class="assistant-starters" aria-label="可用能力示例">
          <button v-for="suggestion in starterPrompts" :key="`${suggestion.capabilityCode}-${suggestion.prompt}`" type="button" @click="applySuggestion(suggestion)">
            <span>{{ suggestion.label }}</span>
            <strong>{{ suggestion.prompt }}</strong>
            <ArrowUpRight :size="14" />
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
              <span><Wrench :size="13" /></span>
              <div><strong>{{ tool.displayName }}</strong><small>{{ tool.summary }} · {{ tool.latencyMs }}ms</small></div>
              <i>READ ONLY</i>
            </div>
          </div>
          <section v-if="message.pendingAction" class="agent-confirmation" aria-label="待确认操作">
            <header>
              <span><ShieldCheck :size="15" /></span>
              <div><strong>{{ message.pendingAction.displayName }}</strong><small>WRITE ACTION / USER CONFIRMATION</small></div>
              <i><Clock3 :size="12" />{{ formatExpiry(message.pendingAction.expiresAt) }} 前有效</i>
            </header>
            <p>{{ message.pendingAction.summary }}</p>
            <footer>
              <button type="button" class="agent-action-button agent-action-button--cancel" :disabled="Boolean(actingToken)" @click="cancelAction(message)"><X :size="14" />取消</button>
              <button type="button" class="agent-action-button agent-action-button--confirm" :disabled="Boolean(actingToken)" @click="confirmAction(message)"><ShieldCheck :size="14" />{{ actingToken === message.pendingAction.token ? '执行中...' : '确认执行' }}</button>
            </footer>
          </section>
          <div v-if="message.actionResult" class="agent-action-result" :class="`agent-action-result--${message.actionResult.status.toLowerCase()}`">
            <CheckCircle2 v-if="message.actionResult.status === 'SUCCEEDED'" :size="15" />
            <X v-else :size="15" />
            <div><strong>{{ message.actionResult.displayName }}</strong><small>{{ message.actionResult.summary }}<template v-if="message.actionResult.latencyMs"> · {{ message.actionResult.latencyMs }}ms</template></small></div>
            <i>{{ message.actionResult.status }}</i>
          </div>
          <div v-if="message.suggestions?.length && !message.pendingAction" class="chat-suggestions" aria-label="推荐问题">
            <button v-for="suggestion in message.suggestions" :key="`${message.id}-${suggestion.capabilityCode}`" type="button" @click="applySuggestion(suggestion)">
              {{ suggestion.label }}<ArrowUpRight :size="12" />
            </button>
          </div>
        </div>
      </article>
      <article v-if="sending" class="chat-message chat-message--assistant chat-message--typing">
        <span class="chat-message__avatar"><Sparkles :size="17" /></span>
        <div><header><strong>SMARTMALL AI</strong></header><p><i /><i /><i /></p></div>
      </article>
    </section>

    <footer class="assistant-composer">
      <textarea ref="composerInput" v-model="draft" maxlength="4000" rows="3" placeholder="输入任何问题" :disabled="sending" @keydown="handleKeydown" />
      <div class="assistant-composer__meta"><span>{{ remaining }}</span><span>SMART AGENT / AUTO</span></div>
      <button type="button" class="assistant-send" title="发送消息" aria-label="发送消息" :disabled="!draft.trim() || sending" @click="send"><Send :size="19" /></button>
    </footer>
  </div>
</template>
