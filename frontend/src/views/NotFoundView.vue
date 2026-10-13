<script setup lang="ts">
import { CircleSlash2 } from 'lucide-vue-next'
</script>

<template>
  <main class="entry-denied">
    <CircleSlash2 :size="34" :stroke-width="1.5" />
    <p class="eyebrow">404 / ENTRANCE NOT FOUND</p>
    <h1>入口不存在</h1>
    <span>请检查访问地址是否完整。</span>
  </main>
</template>
