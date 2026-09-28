<script setup lang="ts">
import { computed, nextTick, ref } from 'vue'
import { Bot, Eraser, Send, Sparkles, UserRound } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { aiApi, type AiChatMessage } from '@/api/ai'

interface DisplayMessage extends AiChatMessage {
  id: number
  modelName?: string
  latencyMs?: number
}

const messages = ref<DisplayMessage[]>([])
const draft = ref('')
const sending = ref(false)
const sequence = ref(0)
const timeline = ref<HTMLElement>()
const remaining = computed(() => 4000 - draft.value.length)

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
    const history = messages.value.slice(-30).map(({ role, content: text }) => ({ role, content: text }))
    const reply = await aiApi.chat(history)
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

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    void send()
  }
}
</script>

<template>
  <div class="assistant-view">
    <header class="assistant-head">
      <div><p class="eyebrow">INTELLIGENCE / CHAT</p><h1>AI 助手<span class="heading-spark">.</span></h1></div>
      <button type="button" class="icon-button" title="清空对话" aria-label="清空对话" :disabled="messages.length === 0" @click="clearConversation"><Eraser :size="18" /></button>
    </header>

    <section ref="timeline" class="assistant-timeline" aria-live="polite">
      <div v-if="messages.length === 0" class="assistant-empty">
        <div class="assistant-core"><Bot :size="36" /><span /><span /><span /></div>
        <h2>SmartMall Intelligence</h2>
        <p>READY FOR INPUT</p>
      </div>
      <article v-for="message in messages" :key="message.id" class="chat-message" :class="`chat-message--${message.role.toLowerCase()}`">
        <span class="chat-message__avatar"><UserRound v-if="message.role === 'USER'" :size="17" /><Sparkles v-else :size="17" /></span>
        <div>
          <header><strong>{{ message.role === 'USER' ? 'YOU' : 'SMARTMALL AI' }}</strong><small v-if="message.modelName">{{ message.modelName }} · {{ message.latencyMs }}ms</small></header>
          <p>{{ message.content }}</p>
        </div>
      </article>
      <article v-if="sending" class="chat-message chat-message--assistant chat-message--typing">
        <span class="chat-message__avatar"><Sparkles :size="17" /></span>
        <div><header><strong>SMARTMALL AI</strong></header><p><i /><i /><i /></p></div>
      </article>
    </section>

    <footer class="assistant-composer">
      <textarea v-model="draft" maxlength="4000" rows="3" placeholder="输入消息" :disabled="sending" @keydown="handleKeydown" />
      <div class="assistant-composer__meta"><span>{{ remaining }}</span><span>OPENAI COMPATIBLE</span></div>
      <button type="button" class="assistant-send" title="发送消息" aria-label="发送消息" :disabled="!draft.trim() || sending" @click="send"><Send :size="19" /></button>
    </footer>
  </div>
</template>
