<script setup lang="ts">
import { computed } from 'vue'
import DOMPurify from 'dompurify'
import { marked } from 'marked'

const props = defineProps<{ content: string }>()

const html = computed(() => {
  const parsed = marked.parse(props.content, {
    async: false,
    breaks: true,
    gfm: true,
  })

  // 模型输出属于不可信内容：支持常用 Markdown，但禁止可交互和可远程加载的 HTML。
  return DOMPurify.sanitize(parsed, {
    FORBID_TAGS: ['audio', 'button', 'embed', 'form', 'iframe', 'img', 'input', 'object', 'select', 'style', 'textarea', 'video'],
    FORBID_ATTR: ['class', 'id', 'style'],
  })
})
</script>

<template>
  <div class="markdown-content" v-html="html" />
</template>
