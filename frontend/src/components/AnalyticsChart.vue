<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { AnalyticsDimension, AnalyticsMetric, AnalyticsPoint } from '@/api/analytics'

echarts.use([BarChart, LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const props = defineProps<{
  points: AnalyticsPoint[]
  metric: AnalyticsMetric
  dimension: AnalyticsDimension
}>()

const container = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | undefined
let observer: ResizeObserver | undefined

const labelTranslations: Record<string, string> = {
  OPEN: '待处理',
  PROCESSING: '处理中',
  RESOLVED: '已解决',
  CLOSED: '已关闭',
  RETURN_EXCHANGE: '退换货',
  LOGISTICS: '物流',
  PRODUCT_QUALITY: '商品质量',
  OTHER: '其他',
}

function displayLabel(label: string) {
  return labelTranslations[label] ?? label
}

function displayValue(value: unknown) {
  const numeric = Number(value)
  if (props.metric === 'REFUND_AMOUNT') {
    return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(numeric)
  }
  return new Intl.NumberFormat('zh-CN', { maximumFractionDigits: 0 }).format(numeric)
}

function renderChart() {
  if (!chart) return
  chart.clear()

  const labels = props.points.map(point => displayLabel(point.label))
  const values = props.points.map(point => Number(point.value))
  const isTimeline = props.dimension === 'DATE'
  const commonSeries = {
    name: props.metric === 'REFUND_AMOUNT' ? '退款金额' : '数量',
    data: values,
    animationDuration: 850,
    animationEasing: 'cubicOut' as const,
  }

  chart.setOption(isTimeline ? {
    grid: { left: 54, right: 24, top: 34, bottom: 42 },
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#171f2a',
      borderColor: '#38505d',
      textStyle: { color: '#eaf0f5', fontSize: 12 },
      axisPointer: { type: 'line', lineStyle: { color: '#5c7783', type: 'dashed' } },
      valueFormatter: displayValue,
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels,
      axisLine: { lineStyle: { color: '#344350' } },
      axisTick: { show: false },
      axisLabel: { color: '#7f96a1', margin: 14, hideOverlap: true },
    },
    yAxis: {
      type: 'value',
      min: 0,
      minInterval: props.metric === 'REFUND_AMOUNT' ? undefined : 1,
      splitLine: { lineStyle: { color: '#24313c', type: 'dashed' } },
      axisLabel: { color: '#718792' },
    },
    series: [{
      ...commonSeries,
      type: 'line',
      smooth: 0.32,
      symbol: 'circle',
      symbolSize: 7,
      lineStyle: { width: 3, color: '#2dd4bf' },
      itemStyle: { color: '#2dd4bf', borderColor: '#102028', borderWidth: 2 },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(45, 212, 191, 0.28)' },
            { offset: 1, color: 'rgba(59, 130, 246, 0)' },
          ],
        },
      },
    }],
  } : {
    grid: { left: 118, right: 42, top: 24, bottom: 28 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(45, 212, 191, 0.045)' } },
      backgroundColor: '#171f2a',
      borderColor: '#38505d',
      textStyle: { color: '#eaf0f5', fontSize: 12 },
      valueFormatter: displayValue,
    },
    xAxis: {
      type: 'value',
      min: 0,
      minInterval: props.metric === 'REFUND_AMOUNT' ? undefined : 1,
      splitLine: { lineStyle: { color: '#24313c', type: 'dashed' } },
      axisLabel: { color: '#718792' },
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: labels,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#a8bac2', width: 96, overflow: 'truncate' },
    },
    series: [{
      ...commonSeries,
      type: 'bar',
      barMaxWidth: 22,
      showBackground: true,
      backgroundStyle: { color: 'rgba(107, 133, 146, 0.08)', borderRadius: 3 },
      itemStyle: {
        borderRadius: [0, 4, 4, 0],
        color: {
          type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
          colorStops: [
            { offset: 0, color: '#2dd4bf' },
            { offset: 1, color: '#3b82f6' },
          ],
        },
      },
    }],
  })

  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    chart.setOption({ animation: false })
  }
}

onMounted(() => {
  if (!container.value) return
  chart = echarts.init(container.value, undefined, { renderer: 'canvas' })
  renderChart()
  observer = new ResizeObserver(() => chart?.resize())
  observer.observe(container.value)
})

watch(() => [props.points, props.metric, props.dimension], renderChart, { deep: true })
onUnmounted(() => { observer?.disconnect(); chart?.dispose() })
</script>

<template>
  <div ref="container" class="analytics-chart" role="img" aria-label="数据分析图表" />
</template>
