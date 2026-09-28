<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'

const canvas = ref<HTMLCanvasElement | null>(null)
let frame = 0
let observer: ResizeObserver | undefined

onUnmounted(() => {
  cancelAnimationFrame(frame)
  observer?.disconnect()
})

onMounted(() => {
  const element = canvas.value
  if (!element) return
  const context = element.getContext('2d')
  if (!context) return

  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)')
  let width = 0
  let height = 0
  let tick = 0
  const nodes = Array.from({ length: 48 }, (_, index) => ({
    x: ((index * 89 + 13) % 101) / 101,
    y: ((index * 47 + 29) % 97) / 97,
    phase: index * 0.73,
  }))

  function resize() {
    if (!element || !context) return
    const bounds = element.getBoundingClientRect()
    width = bounds.width
    height = bounds.height
    const scale = Math.min(window.devicePixelRatio || 1, 2)
    element.width = Math.round(width * scale)
    element.height = Math.round(height * scale)
    context.setTransform(scale, 0, 0, scale, 0, 0)
    draw()
  }

  function draw() {
    if (!context) return
    context.clearRect(0, 0, width, height)
    context.lineWidth = 1
    const spacing = 52
    for (let x = -spacing; x < width + spacing; x += spacing) {
      context.strokeStyle = 'rgba(92, 130, 145, 0.09)'
      context.beginPath()
      context.moveTo(x + (tick % spacing) * 0.2, 0)
      context.lineTo(x + (tick % spacing) * 0.2, height)
      context.stroke()
    }
    for (let y = 0; y < height; y += spacing) {
      context.beginPath()
      context.moveTo(0, y)
      context.lineTo(width, y)
      context.stroke()
    }

    const positions = nodes.map((node) => ({
      x: node.x * width,
      y: node.y * height + Math.sin(tick * 0.012 + node.phase) * 7,
    }))
    positions.forEach((point, index) => {
      for (let next = index + 1; next < positions.length; next++) {
        const other = positions[next]
        if (!other) continue
        const distance = Math.hypot(point.x - other.x, point.y - other.y)
        if (distance < 140) {
          context.strokeStyle = `rgba(45, 212, 191, ${(1 - distance / 140) * 0.18})`
          context.beginPath()
          context.moveTo(point.x, point.y)
          context.lineTo(other.x, other.y)
          context.stroke()
        }
      }
      context.fillStyle = index % 8 === 0 ? '#f59e0b' : index % 6 === 0 ? '#8b5cf6' : '#2dd4bf'
      context.globalAlpha = 0.35 + Math.sin(tick * 0.03 + index) * 0.16
      context.beginPath()
      context.arc(point.x, point.y, index % 8 === 0 ? 2.7 : 1.6, 0, Math.PI * 2)
      context.fill()
      context.globalAlpha = 1
    })
  }

  function animate() {
    tick++
    draw()
    if (!reducedMotion.matches) frame = requestAnimationFrame(animate)
  }

  observer = new ResizeObserver(resize)
  observer.observe(element)
  resize()
  if (!reducedMotion.matches) animate()
})
</script>

<template>
  <div class="auth-scene" aria-hidden="true">
    <canvas ref="canvas" class="auth-scene__canvas" />
    <div class="auth-scene__horizon" />
  </div>
</template>
