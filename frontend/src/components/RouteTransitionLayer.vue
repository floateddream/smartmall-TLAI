<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'

export type RouteMotionTone = 'aurora' | 'purple' | 'orange'

const props = defineProps<{
  sequence: number
  origin: { x: number; y: number }
  tone: RouteMotionTone
}>()

const effectVisible = ref(false)
let effectTimer = 0
function play() {
  window.clearTimeout(effectTimer)
  effectVisible.value = true
  effectTimer = window.setTimeout(() => { effectVisible.value = false }, 620)
}

watch(() => props.sequence, () => { if (props.sequence > 0) play() })

onBeforeUnmount(() => {
  window.clearTimeout(effectTimer)
})
</script>

<template>
  <div class="route-motion-layer" aria-hidden="true">
    <div
      v-if="effectVisible"
      :key="sequence"
      class="route-motion-effect"
      :class="`route-motion-effect--${tone}`"
      :style="{ '--motion-y': `${Math.max(0, origin.y - 66)}px` }"
    >
      <i class="route-motion-effect__rail" />
      <i class="route-motion-effect__beam" />
      <i class="route-motion-effect__scan" />
      <i class="route-motion-effect__pulse" />
      <i class="route-motion-effect__veil" />
    </div>
  </div>
</template>
