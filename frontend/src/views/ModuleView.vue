<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, ArrowUpRight, Construction } from 'lucide-vue-next'

const route = useRoute()
const title = computed(() => String(route.meta.title || '模块'))
const section = computed(() => String(route.meta.section || '工作台'))
</script>

<template>
  <div class="module-view view-shell">
    <div class="page-heading"><div><p class="eyebrow">{{ section }} / MODULE</p><h1>{{ title }}<span class="heading-spark">.</span></h1><p class="page-heading__subtitle">该模块将在后续阶段接入真实业务。</p></div><span class="demo-badge demo-badge--muted">规划中</span></div>
    <div class="module-placeholder">
      <span class="module-placeholder__index">SMARTMALL TLAI / NEXT STAGE</span>
      <Construction :size="36" :stroke-width="1.3" class="module-placeholder__icon" />
      <h2>{{ title }}正在建设</h2>
      <p>当前阶段已完成账号认证与运营工作台。业务数据和操作将在对应阶段开放。</p>
      <router-link class="outline-button" to="/dashboard"><ArrowLeft :size="16" />返回工作台</router-link>
      <ArrowUpRight :size="18" class="module-placeholder__corner" />
    </div>
  </div>
</template>
