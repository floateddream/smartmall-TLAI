<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

type FlowTone = 'aurora' | 'purple' | 'orange'

const props = defineProps<{
  sequence: number
  tone: FlowTone
}>()

interface FlowNode {
  lane: number
  progress: number
  speed: number
  radius: number
  colorOffset: number
}

const canvas = ref<HTMLCanvasElement>()
const palettes: Record<FlowTone, string[]> = {
  aurora: ['45,212,191', '59,130,246', '16,185,129'],
  purple: ['45,212,191', '139,92,246', '59,130,246'],
  orange: ['45,212,191', '59,130,246', '249,115,22'],
}

const pointer = {
  x: 0,
  y: 0,
  targetX: 0,
  targetY: 0,
  active: false,
}

let nodes: FlowNode[] = []
let animationFrame = 0
let previousFrame = 0
let previousDraw = 0
let width = 0
let height = 0
let ratio = 1
let routeBoost = 0
let resizeObserver: ResizeObserver | null = null

function reducedMotion() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function createNodes() {
  const lowPower = navigator.hardwareConcurrency > 0 && navigator.hardwareConcurrency <= 4
  const count = window.innerWidth <= 800 ? 10 : lowPower ? 14 : 24
  nodes = Array.from({ length: count }, (_, index) => ({
    lane: index % 4,
    progress: Math.random(),
    speed: .000018 + Math.random() * .000018,
    radius: .7 + Math.random() * 1.15,
    colorOffset: index % 3,
  }))
}

function resizeCanvas() {
  const element = canvas.value
  if (!element) return null
  const bounds = element.getBoundingClientRect()
  width = Math.max(1, bounds.width)
  height = Math.max(1, bounds.height)
  ratio = Math.min(window.devicePixelRatio || 1, 1.25)
  element.width = Math.max(1, Math.round(width * ratio))
  element.height = Math.max(1, Math.round(height * ratio))
  const context = element.getContext('2d', { alpha: true })
  context?.setTransform(ratio, 0, 0, ratio, 0, 0)
  if (!nodes.length) createNodes()
  return context
}

function laneY(lane: number, x: number, time: number) {
  const base = height * (.17 + lane * .22)
  const slowWave = Math.sin(x * .0042 + time * .00018 + lane * 1.7) * 10
  const fineWave = Math.sin(x * .009 - time * .0001 + lane * .8) * 4
  let value = base + slowWave + fineWave
  if (pointer.active) {
    const distance = Math.abs(x - pointer.x)
    if (distance < 190) {
      const influence = (1 - distance / 190) ** 2
      value += (pointer.y - value) * influence * .105
    }
  }
  return value
}

function draw(time: number) {
  animationFrame = requestAnimationFrame(draw)
  if (document.hidden || time - previousDraw < 33) return

  const context = canvas.value?.getContext('2d')
  if (!context || width <= 1 || height <= 1) return
  const delta = Math.min(80, previousFrame ? time - previousFrame : 33)
  previousFrame = time
  previousDraw = time
  pointer.x += (pointer.targetX - pointer.x) * .085
  pointer.y += (pointer.targetY - pointer.y) * .085
  routeBoost *= .94

  context.clearRect(0, 0, width, height)
  context.lineCap = 'round'
  context.globalCompositeOperation = 'source-over'

  for (let lane = 0; lane < 4; lane += 1) {
    context.beginPath()
    for (let x = -40; x <= width + 40; x += 54) {
      const y = laneY(lane, x, time)
      if (x === -40) context.moveTo(x, y)
      else context.lineTo(x, y)
    }
    context.strokeStyle = `rgba(${lane % 2 ? '59,130,246' : '45,212,191'},${.025 + routeBoost * .018})`
    context.lineWidth = 1
    context.stroke()
  }

  const colors = palettes[props.tone]
  context.globalCompositeOperation = 'lighter'
  for (const node of nodes) {
    node.progress += delta * node.speed * (1 + routeBoost * 1.8)
    if (node.progress > 1.08) node.progress = -.08
    const x = node.progress * width
    const y = laneY(node.lane, x, time)
    const distance = pointer.active ? Math.hypot(x - pointer.x, y - pointer.y) : 999
    const hoverEnergy = distance < 150 ? (1 - distance / 150) : 0
    const color = colors[node.colorOffset] ?? colors[0]!
    const trail = 15 + hoverEnergy * 20 + routeBoost * 24
    const previousY = laneY(node.lane, x - trail, time)
    const opacity = .18 + hoverEnergy * .38 + routeBoost * .11

    context.beginPath()
    context.moveTo(x - trail, previousY)
    context.lineTo(x, y)
    context.strokeStyle = `rgba(${color},${opacity * .42})`
    context.lineWidth = node.radius
    context.stroke()

    context.beginPath()
    context.arc(x, y, node.radius + hoverEnergy * 1.15, 0, Math.PI * 2)
    context.fillStyle = `rgba(${color},${opacity})`
    context.fill()
  }
  context.globalCompositeOperation = 'source-over'
}

function start() {
  if (animationFrame || reducedMotion() || document.hidden) return
  resizeCanvas()
  previousFrame = 0
  previousDraw = 0
  animationFrame = requestAnimationFrame(draw)
}

function stop(releaseBuffer = false) {
  if (animationFrame) cancelAnimationFrame(animationFrame)
  animationFrame = 0
  const element = canvas.value
  element?.getContext('2d')?.clearRect(0, 0, width, height)
  if (releaseBuffer && element) {
    element.width = 1
    element.height = 1
  }
}

function handlePointerMove(event: PointerEvent) {
  const bounds = canvas.value?.getBoundingClientRect()
  if (!bounds || event.clientX < bounds.left || event.clientY < bounds.top) {
    pointer.active = false
    return
  }
  pointer.targetX = event.clientX - bounds.left
  pointer.targetY = event.clientY - bounds.top
  if (!pointer.active) {
    pointer.x = pointer.targetX
    pointer.y = pointer.targetY
  }
  pointer.active = true
}

function handlePointerOut(event: PointerEvent) {
  if (!event.relatedTarget) pointer.active = false
}

function handleVisibilityChange() {
  if (document.hidden) stop(true)
  else start()
}

watch(() => props.sequence, () => {
  if (props.sequence > 0) routeBoost = 1
})

onMounted(() => {
  resizeObserver = new ResizeObserver(() => resizeCanvas())
  if (canvas.value) resizeObserver.observe(canvas.value)
  window.addEventListener('pointermove', handlePointerMove, { passive: true })
  window.addEventListener('pointerout', handlePointerOut, { passive: true })
  document.addEventListener('visibilitychange', handleVisibilityChange)
  start()
})

onBeforeUnmount(() => {
  stop(true)
  resizeObserver?.disconnect()
  window.removeEventListener('pointermove', handlePointerMove)
  window.removeEventListener('pointerout', handlePointerOut)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<template>
  <canvas ref="canvas" class="ambient-data-flow" aria-hidden="true" />
</template>
