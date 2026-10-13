<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

export type RouteMotionTone = 'aurora' | 'purple' | 'orange'

const props = defineProps<{
  sequence: number
  origin: { x: number; y: number }
  tone: RouteMotionTone
}>()

interface Particle {
  x: number
  y: number
  previousX: number
  previousY: number
  velocityX: number
  velocityY: number
  size: number
  life: number
  duration: number
  color: string
}

const canvas = ref<HTMLCanvasElement>()
const effectVisible = ref(false)
let animationFrame = 0
let effectTimer = 0
let particles: Particle[] = []

const palettes: Record<RouteMotionTone, string[]> = {
  aurora: ['45,212,191', '59,130,246', '16,185,129'],
  purple: ['139,92,246', '59,130,246', '45,212,191'],
  orange: ['249,115,22', '245,158,11', '139,92,246'],
}

function reducedMotion() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function resizeCanvas() {
  const element = canvas.value
  if (!element) return null
  const bounds = element.getBoundingClientRect()
  const ratio = Math.min(window.devicePixelRatio || 1, 1.5)
  element.width = Math.max(1, Math.round(bounds.width * ratio))
  element.height = Math.max(1, Math.round(bounds.height * ratio))
  const context = element.getContext('2d', { alpha: true })
  context?.setTransform(ratio, 0, 0, ratio, 0, 0)
  return { context, bounds }
}

function stopAnimation(releaseBuffer = false) {
  if (animationFrame) cancelAnimationFrame(animationFrame)
  animationFrame = 0
  particles = []
  const element = canvas.value
  const context = element?.getContext('2d')
  context?.clearRect(0, 0, element?.width ?? 0, element?.height ?? 0)
  if (releaseBuffer && element) {
    element.width = 1
    element.height = 1
  }
}

function startParticles() {
  stopAnimation()
  if (reducedMotion() || document.hidden) return

  const setup = resizeCanvas()
  if (!setup?.context) return
  const { context, bounds } = setup
  const constrainedOriginX = Math.max(0, Math.min(bounds.width, props.origin.x - bounds.left))
  const constrainedOriginY = Math.max(0, Math.min(bounds.height, props.origin.y - bounds.top))
  const lowPower = navigator.hardwareConcurrency > 0 && navigator.hardwareConcurrency <= 4
  const count = window.innerWidth <= 800 ? 16 : lowPower ? 22 : 34
  const colors = palettes[props.tone]

  particles = Array.from({ length: count }, (_, index) => {
    const spread = (Math.random() - .5) * 1.5
    const speed = 1.6 + Math.random() * 3.8
    const verticalBias = ((index / count) - .5) * .45
    return {
      x: constrainedOriginX,
      y: constrainedOriginY + (Math.random() - .5) * 18,
      previousX: constrainedOriginX,
      previousY: constrainedOriginY,
      velocityX: speed,
      velocityY: (spread + verticalBias) * speed,
      size: .7 + Math.random() * 1.5,
      life: 0,
      duration: 430 + Math.random() * 260,
      color: colors[index % colors.length] ?? colors[0]!,
    }
  })

  let previousTime = performance.now()
  const draw = (time: number) => {
    const delta = Math.min(32, time - previousTime)
    previousTime = time
    context.clearRect(0, 0, bounds.width, bounds.height)
    context.globalCompositeOperation = 'lighter'

    particles = particles.filter((particle) => {
      particle.life += delta
      if (particle.life >= particle.duration) return false
      particle.previousX = particle.x
      particle.previousY = particle.y
      particle.x += particle.velocityX * delta * .075
      particle.y += particle.velocityY * delta * .055
      particle.velocityX *= .988
      particle.velocityY *= .985

      const progress = particle.life / particle.duration
      const opacity = Math.sin(progress * Math.PI) * .75
      context.beginPath()
      context.moveTo(particle.previousX, particle.previousY)
      context.lineTo(particle.x, particle.y)
      context.strokeStyle = `rgba(${particle.color},${opacity * .48})`
      context.lineWidth = particle.size
      context.stroke()

      context.beginPath()
      context.arc(particle.x, particle.y, particle.size, 0, Math.PI * 2)
      context.fillStyle = `rgba(${particle.color},${opacity})`
      context.fill()
      return true
    })

    if (particles.length) animationFrame = requestAnimationFrame(draw)
    else stopAnimation(true)
  }
  animationFrame = requestAnimationFrame(draw)
}

async function play() {
  window.clearTimeout(effectTimer)
  effectVisible.value = false
  await nextTick()
  effectVisible.value = true
  startParticles()
  effectTimer = window.setTimeout(() => { effectVisible.value = false }, 720)
}

watch(() => props.sequence, () => { if (props.sequence > 0) void play() })

function handleVisibilityChange() {
  if (document.hidden) stopAnimation(true)
}

onMounted(() => {
  window.addEventListener('resize', resizeCanvas, { passive: true })
  document.addEventListener('visibilitychange', handleVisibilityChange)
})
onBeforeUnmount(() => {
  stopAnimation(true)
  window.clearTimeout(effectTimer)
  window.removeEventListener('resize', resizeCanvas)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<template>
  <div class="route-motion-layer" aria-hidden="true">
    <canvas ref="canvas" />
    <div
      v-if="effectVisible"
      :key="sequence"
      class="route-motion-effect"
      :class="`route-motion-effect--${tone}`"
      :style="{ '--motion-y': `${Math.max(0, origin.y - 66)}px` }"
    >
      <i class="route-motion-effect__beam" />
      <i class="route-motion-effect__scan" />
      <i class="route-motion-effect__pulse" />
    </div>
  </div>
</template>
