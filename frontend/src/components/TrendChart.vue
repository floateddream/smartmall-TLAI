<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])
const container = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | undefined
let observer: ResizeObserver | undefined

onMounted(() => {
  if (!container.value) return
  chart = echarts.init(container.value, undefined, { renderer: 'canvas' })
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
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      axisLine: { lineStyle: { color: '#343d50' } },
      axisTick: { show: false },
      axisLabel: { color: '#8392a5', margin: 15 },
    },
    yAxis: {
      type: 'value', min: 0, max: 600, interval: 150,
      splitLine: { lineStyle: { color: '#272e3e', type: 'dashed' } },
      axisLabel: { color: '#8392a5' },
    },
    series: [
      {
        name: '订单', type: 'line', smooth: 0.35, symbol: 'circle', symbolSize: 7,
        data: [245, 330, 298, 432, 387, 508, 474],
        lineStyle: { width: 3, color: '#2dd4bf' },
        itemStyle: { color: '#2dd4bf', borderColor: '#172023', borderWidth: 2 },
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [
          { offset: 0, color: 'rgba(45, 212, 191, 0.22)' },
          { offset: 1, color: 'rgba(45, 212, 191, 0)' },
        ] } },
      },
      {
        name: '退款', type: 'line', smooth: 0.35, symbol: 'none',
        data: [42, 57, 49, 62, 55, 78, 64],
        lineStyle: { width: 2, color: '#f59e0b' },
      },
    ],
  })
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) chart.setOption({ animation: false })
  observer = new ResizeObserver(() => chart?.resize())
  observer.observe(container.value)
})

onUnmounted(() => { observer?.disconnect(); chart?.dispose() })
</script>

<template><div ref="container" class="trend-chart" role="img" aria-label="演示数据：一周内订单与退款趋势" /></template>
