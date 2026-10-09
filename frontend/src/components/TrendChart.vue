<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { DashboardTrendPoint } from '@/api/dashboard'

echarts.use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])
const props = defineProps<{ points: DashboardTrendPoint[] }>()
const container = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | undefined
let observer: ResizeObserver | undefined

function renderChart() {
  if (!chart) return
  chart.setOption({
    animationDuration: 950,
    animationEasing: 'cubicOut',
    grid: { left: 42, right: 16, top: 22, bottom: 32 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#191e2c', borderColor: '#343d50',
      textStyle: { color: '#e8eef5', fontSize: 12 },
      axisPointer: { type: 'line', lineStyle: { color: '#647488', type: 'dashed' } },
    },
    xAxis: {
      type: 'category', boundaryGap: false,
      data: props.points.map((point) => {
        const [, month, day] = point.date.split('-')
        return `${Number(month)}/${Number(day)}`
      }),
      axisLine: { lineStyle: { color: '#343d50' } },
      axisTick: { show: false },
      axisLabel: { color: '#8392a5', margin: 15 },
    },
    yAxis: {
      type: 'value', min: 0, minInterval: 1,
      splitLine: { lineStyle: { color: '#272e3e', type: 'dashed' } },
      axisLabel: { color: '#8392a5' },
    },
    series: [
      {
        name: '订单', type: 'line', smooth: 0.35, symbol: 'circle', symbolSize: 7,
        data: props.points.map((point) => point.orderCount),
        lineStyle: { width: 3, color: '#2dd4bf' },
        itemStyle: { color: '#2dd4bf', borderColor: '#172023', borderWidth: 2 },
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [
          { offset: 0, color: 'rgba(45, 212, 191, 0.22)' },
          { offset: 1, color: 'rgba(45, 212, 191, 0)' },
        ] } },
      },
      {
        name: '退款', type: 'line', smooth: 0.35, symbol: 'none',
        data: props.points.map((point) => point.refundCount),
        lineStyle: { width: 2, color: '#f59e0b' },
      },
    ],
  })
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) chart.setOption({ animation: false })
}

onMounted(() => {
  if (!container.value) return
  chart = echarts.init(container.value, undefined, { renderer: 'canvas' })
  renderChart()
  observer = new ResizeObserver(() => chart?.resize())
  observer.observe(container.value)
})

watch(() => props.points, renderChart, { deep: true })
onUnmounted(() => { observer?.disconnect(); chart?.dispose() })
</script>

<template><div ref="container" class="trend-chart" role="img" aria-label="近七天订单与退款趋势" /></template>
