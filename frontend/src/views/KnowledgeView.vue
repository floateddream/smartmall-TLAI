<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleAlert, CircleCheck, Database, Download, File, FileText, FileType2,
  LoaderCircle, RefreshCw, RotateCcw, Search, Trash2, UploadCloud, X,
} from 'lucide-vue-next'
import { ApiError } from '@/api/client'
import { knowledgeApi } from '@/api/knowledge'
import type { KnowledgeDocument, KnowledgeDocumentStatus } from '@/api/knowledge'
import { useAuthStore } from '@/stores/auth'
import { readQueryEnum, readQueryText } from '@/utils/routeQuery'

const MAX_FILE_SIZE = 20 * 1024 * 1024
const MAX_BATCH_FILES = 20
const UPLOAD_CONCURRENCY = 3
const ALLOWED_EXTENSIONS = new Set(['pdf', 'docx', 'txt', 'md'])

type UploadState = 'pending' | 'uploading' | 'success' | 'failed'

interface UploadQueueItem {
  id: string
  file: File
  progress: number
  state: UploadState
  error: string
}

const auth = useAuthStore()
const route = useRoute()
const rows = ref<KnowledgeDocument[]>([])
const page = ref(1)
const total = ref(0)
const keyword = ref('')
const status = ref<KnowledgeDocumentStatus | ''>('')
const loading = ref(false)
const loadError = ref('')
const uploadOpen = ref(false)
const uploadQueue = ref<UploadQueueItem[]>([])
const uploading = ref(false)
const dragActive = ref(false)
const fileInput = ref<HTMLInputElement>()
const downloadingId = ref<number | null>(null)
const deletingId = ref<number | null>(null)
const retryingId = ref<number | null>(null)
let requestId = 0
let pollTimer: number | undefined
let pollInFlight = false

const currentPageBytes = computed(() => rows.value.reduce((sum, document) => sum + document.sizeBytes, 0))
const uploadCandidates = computed(() => uploadQueue.value.filter(item => item.state === 'pending' || item.state === 'failed'))
const overallUploadProgress = computed(() => {
  if (!uploadQueue.value.length) return 0
  return Math.round(uploadQueue.value.reduce((sum, item) => sum + item.progress, 0) / uploadQueue.value.length)
})
const uploadButtonLabel = computed(() => {
  if (uploading.value) return '正在上传...'
  const failed = uploadQueue.value.filter(item => item.state === 'failed').length
  if (failed) return `重试失败文件 (${failed})`
  return `上传文件 (${uploadCandidates.value.length})`
})

const statusLabels: Record<KnowledgeDocumentStatus, string> = {
  UPLOADED: '已上传',
  PROCESSING: '处理中',
  READY: '可检索',
  FAILED: '处理失败',
}

async function loadPage(nextPage = page.value) {
  const current = ++requestId
  page.value = nextPage
  loading.value = true
  loadError.value = ''
  try {
    const result = await knowledgeApi.list(nextPage, keyword.value.trim(), status.value)
    if (current !== requestId) return
    const lastPage = Math.max(1, Math.ceil(result.total / 20))
    if (result.total > 0 && nextPage > lastPage) {
      await loadPage(lastPage)
      return
    }
    rows.value = result.items
    total.value = result.total
  } catch (error) {
    if (current !== requestId) return
    rows.value = []
    total.value = 0
    loadError.value = error instanceof ApiError ? error.message : '知识库加载失败'
  } finally {
    if (current === requestId) loading.value = false
  }
}

function resetFilters() {
  keyword.value = ''
  status.value = ''
  void loadPage(1)
}

async function pollProcessingStatus() {
  if (pollInFlight || !rows.value.some(document =>
    document.status === 'UPLOADED' || document.status === 'PROCESSING')) return
  pollInFlight = true
  const currentRequest = requestId
  const currentPage = page.value
  const currentKeyword = keyword.value.trim()
  const currentStatus = status.value
  try {
    const result = await knowledgeApi.list(currentPage, currentKeyword, currentStatus)
    // 用户在轮询期间切换筛选条件时，丢弃旧条件返回的数据。
    if (currentRequest !== requestId) return
    rows.value = result.items
    total.value = result.total
  } catch (error) {
    console.warn('[知识库] 自动刷新处理状态失败', error)
  } finally {
    pollInFlight = false
  }
}

function openUpload() {
  uploadQueue.value = []
  uploadOpen.value = true
}

function chooseFile() {
  fileInput.value?.click()
}

function onFileInput(event: Event) {
  const input = event.target as HTMLInputElement
  acceptFiles(Array.from(input.files || []))
  input.value = ''
}

function onDrop(event: DragEvent) {
  dragActive.value = false
  acceptFiles(Array.from(event.dataTransfer?.files || []))
}

function acceptFiles(files: File[]) {
  if (!files.length || uploading.value) return
  let added = 0
  let rejected = 0
  let duplicate = 0

  for (const file of files) {
    if (uploadQueue.value.length >= MAX_BATCH_FILES) break
    const error = validateUploadFile(file)
    if (error) {
      rejected++
      continue
    }
    const exists = uploadQueue.value.some(item =>
      item.file.name === file.name && item.file.size === file.size && item.file.lastModified === file.lastModified)
    if (exists) {
      duplicate++
      continue
    }
    uploadQueue.value.push({
      id: `${file.name}-${file.size}-${file.lastModified}`,
      file,
      progress: 0,
      state: 'pending',
      error: '',
    })
    added++
  }

  const overLimit = files.length > added + rejected + duplicate
  if (rejected) ElMessage.warning(`${rejected} 个文件因格式、大小或内容为空未加入队列`)
  if (duplicate) ElMessage.info(`${duplicate} 个重复文件已跳过`)
  if (overLimit) ElMessage.warning(`每批最多上传 ${MAX_BATCH_FILES} 个文件`)
}

function validateUploadFile(file: File) {
  const extension = file.name.includes('.') ? file.name.split('.').pop()!.toLowerCase() : ''
  if (!ALLOWED_EXTENSIONS.has(extension)) return '文件格式不支持'
  if (file.size === 0) return '文件内容为空'
  if (file.size > MAX_FILE_SIZE) return '文件超过 20 MB'
  return ''
}

function removeUploadItem(id: string) {
  if (uploading.value) return
  uploadQueue.value = uploadQueue.value.filter(item => item.id !== id)
}

function uploadStateLabel(item: UploadQueueItem) {
  if (item.state === 'uploading') return `${item.progress}%`
  if (item.state === 'success') return '上传成功'
  if (item.state === 'failed') return item.error || '上传失败'
  return '等待上传'
}

async function uploadAll() {
  if (!uploadCandidates.value.length || uploading.value) return
  uploading.value = true
  const candidates = [...uploadCandidates.value]
  candidates.forEach((item) => {
    item.progress = 0
    item.state = 'pending'
    item.error = ''
  })

  let nextIndex = 0
  const worker = async () => {
    while (nextIndex < candidates.length) {
      const item = candidates[nextIndex++]
      item.state = 'uploading'
      try {
        await knowledgeApi.upload(item.file, progress => { item.progress = progress })
        item.progress = 100
        item.state = 'success'
      } catch (error) {
        console.error(`[知识库] 上传文档失败: ${item.file.name}`, error)
        item.state = 'failed'
        item.error = error instanceof Error ? error.message : '上传失败'
      }
    }
  }

  try {
    const workers = Array.from({ length: Math.min(UPLOAD_CONCURRENCY, candidates.length) }, () => worker())
    await Promise.all(workers)
    const succeeded = candidates.filter(item => item.state === 'success').length
    const failed = candidates.length - succeeded
    if (succeeded) await loadPage(1)
    if (!failed) {
      ElMessage.success(`${succeeded} 个文档已上传到知识库`)
      uploadOpen.value = false
    } else {
      ElMessage.warning(`${succeeded} 个上传成功，${failed} 个上传失败，可直接重试失败文件`)
    }
  } finally {
    uploading.value = false
  }
}

async function download(document: KnowledgeDocument) {
  downloadingId.value = document.id
  try {
    const blob = await knowledgeApi.download(document.id)
    const url = URL.createObjectURL(blob)
    const link = window.document.createElement('a')
    link.href = url
    link.download = document.originalName
    link.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    console.error('[知识库] 下载文档失败', error)
    ElMessage.error(error instanceof Error ? error.message : '文档下载失败')
  } finally {
    downloadingId.value = null
  }
}

async function remove(document: KnowledgeDocument) {
  try {
    await ElMessageBox.confirm(`删除后将同时移除原文件“${document.originalName}”，且无法恢复。`, '删除文档', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  deletingId.value = document.id
  try {
    await knowledgeApi.delete(document.id)
    ElMessage.success('文档已删除')
    const targetPage = rows.value.length === 1 && page.value > 1 ? page.value - 1 : page.value
    await loadPage(targetPage)
  } catch (error) {
    console.error('[知识库] 删除文档失败', error)
    ElMessage.error(error instanceof Error ? error.message : '文档删除失败')
  } finally {
    deletingId.value = null
  }
}

async function retryProcessing(document: KnowledgeDocument) {
  retryingId.value = document.id
  try {
    await knowledgeApi.retry(document.id)
    ElMessage.success('文档已重新加入处理队列')
    await loadPage(page.value)
  } catch (error) {
    console.error('[知识库] 重新处理文档失败', error)
    ElMessage.error(error instanceof Error ? error.message : '重新处理失败')
  } finally {
    retryingId.value = null
  }
}

function formatBytes(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function fileIcon(extension: string) {
  if (extension === 'pdf') return FileType2
  if (extension === 'txt' || extension === 'md') return FileText
  return File
}

onMounted(() => {
  keyword.value = readQueryText(route.query.keyword)
  status.value = readQueryEnum(route.query.status, Object.keys(statusLabels) as KnowledgeDocumentStatus[])
  void loadPage(1)
  pollTimer = window.setInterval(() => { void pollProcessingStatus() }, 3000)
})
onUnmounted(() => {
  if (pollTimer !== undefined) window.clearInterval(pollTimer)
})
</script>

<template>
  <div class="view-shell knowledge-view">
    <div class="page-heading">
      <div>
        <p class="eyebrow">AI WORKSPACE / 知识资产</p>
        <h1>知识库<span class="heading-spark">.</span></h1>
      </div>
      <button v-if="auth.can('knowledge:manage')" type="button" class="primary-button" @click="openUpload">
        <UploadCloud :size="18" />上传文档
      </button>
    </div>

    <div class="knowledge-runtime-strip">
      <div><span>文档总数</span><strong>{{ total }}</strong></div>
      <div><span>当前页容量</span><strong>{{ formatBytes(currentPageBytes) }}</strong></div>
      <div class="knowledge-runtime-state"><Database :size="18" /><span><b>MINIO</b> OBJECT STORAGE</span></div>
      <i class="knowledge-runtime-signal" />
    </div>

    <div class="catalog-toolbar knowledge-toolbar">
      <el-input v-model="keyword" placeholder="搜索文档名称" clearable aria-label="搜索文档" :disabled="loading" @keyup.enter="loadPage(1)" @clear="loadPage(1)">
        <template #prefix><Search :size="16" /></template>
      </el-input>
      <el-select v-model="status" class="knowledge-status-filter" aria-label="文档状态" :disabled="loading" @change="loadPage(1)">
        <el-option label="全部状态" value="" />
        <el-option v-for="(label, value) in statusLabels" :key="value" :label="label" :value="value" />
      </el-select>
      <button type="button" class="outline-button" :disabled="loading" @click="loadPage(1)"><Search :size="16" />搜索</button>
      <button v-if="keyword || status" type="button" class="text-link" :disabled="loading" @click="resetFilters">重置</button>
      <button type="button" class="icon-button" title="刷新列表" aria-label="刷新列表" :disabled="loading" @click="loadPage()"><RefreshCw :size="17" :class="{ spin: loading }" /></button>
    </div>

    <div class="catalog-table-wrap knowledge-table-wrap">
      <table class="catalog-table knowledge-table">
        <thead><tr><th>文档</th><th>大小</th><th>处理状态</th><th>上传人</th><th>上传时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="document in loading || loadError ? [] : rows" :key="document.id">
            <td>
              <div class="knowledge-file">
                <span class="knowledge-file__icon"><component :is="fileIcon(document.fileExtension)" :size="18" /></span>
                <span><strong>{{ document.originalName }}</strong><small class="mono">{{ document.fileExtension.toUpperCase() }} · ID {{ document.id }}</small></span>
              </div>
            </td>
            <td class="catalog-muted mono">{{ formatBytes(document.sizeBytes) }}</td>
            <td>
              <div class="knowledge-status-cell">
                <span class="knowledge-status" :class="`knowledge-status--${document.status.toLowerCase()}`" :title="document.errorMessage || statusLabels[document.status]"><i />{{ statusLabels[document.status] }}</span>
                <small v-if="document.status === 'READY'">{{ document.chunkCount }} 个片段</small>
                <small v-else-if="document.status === 'FAILED'" :title="document.errorMessage || ''">{{ document.errorMessage || '处理失败' }}</small>
              </div>
            </td>
            <td>{{ document.uploaderName }}</td>
            <td class="catalog-muted">{{ new Date(document.createdAt).toLocaleString('zh-CN') }}</td>
            <td>
              <div class="knowledge-actions">
                <button type="button" class="icon-button" :disabled="downloadingId === document.id" :title="`下载 ${document.originalName}`" :aria-label="`下载 ${document.originalName}`" @click="download(document)"><Download :size="16" /></button>
                <button v-if="auth.can('knowledge:manage') && document.status === 'FAILED'" type="button" class="icon-button knowledge-retry" :disabled="retryingId === document.id" :title="`重新处理 ${document.originalName}`" :aria-label="`重新处理 ${document.originalName}`" @click="retryProcessing(document)"><RotateCcw :size="16" :class="{ spin: retryingId === document.id }" /></button>
                <button v-if="auth.can('knowledge:manage')" type="button" class="icon-button knowledge-delete" :disabled="deletingId === document.id" :title="`删除 ${document.originalName}`" :aria-label="`删除 ${document.originalName}`" @click="remove(document)"><Trash2 :size="16" /></button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="loading" class="catalog-feedback" role="status">正在加载知识库...</div>
      <div v-else-if="loadError" class="catalog-feedback catalog-feedback--error" role="alert">{{ loadError }} <button type="button" class="text-link" @click="loadPage()">重试</button></div>
      <div v-else-if="!rows.length" class="catalog-feedback">{{ keyword || status ? '没有匹配的文档' : '知识库中还没有文档' }}</div>
    </div>

    <el-pagination v-if="total > 20" class="catalog-pagination" background :current-page="page" :page-size="20" :total="total" layout="prev, pager, next, total" @current-change="loadPage" />

    <el-dialog v-model="uploadOpen" title="批量上传知识文档" width="min(680px, 94vw)" :close-on-click-modal="!uploading" :close-on-press-escape="!uploading" :show-close="!uploading">
      <div class="knowledge-upload" :class="{ 'knowledge-upload--active': dragActive }" @dragenter.prevent="dragActive = true" @dragover.prevent="dragActive = true" @dragleave.prevent="dragActive = false" @drop.prevent="onDrop">
        <input ref="fileInput" type="file" accept=".pdf,.docx,.txt,.md" multiple hidden @change="onFileInput" />
        <UploadCloud :size="28" />
        <div><strong>{{ uploadQueue.length ? '继续添加文档' : '拖放多个文件到这里' }}</strong><small>PDF · DOCX · TXT · MD，单个最大 20 MB，每批最多 20 个</small></div>
        <button type="button" class="outline-button" :disabled="uploading || uploadQueue.length >= MAX_BATCH_FILES" @click="chooseFile">选择文件</button>
      </div>

      <div v-if="uploadQueue.length" class="knowledge-upload-summary">
        <span>上传队列 <b>{{ uploadQueue.length }}/{{ MAX_BATCH_FILES }}</b></span>
        <span>并发数 <b>{{ UPLOAD_CONCURRENCY }}</b></span>
        <span v-if="uploading">总进度 <b>{{ overallUploadProgress }}%</b></span>
      </div>

      <div v-if="uploadQueue.length" class="knowledge-upload-list">
        <div v-for="item in uploadQueue" :key="item.id" class="knowledge-upload-item" :class="`knowledge-upload-item--${item.state}`">
          <span class="knowledge-upload__file"><component :is="fileIcon(item.file.name.split('.').pop()?.toLowerCase() || '')" :size="20" /></span>
          <div class="knowledge-upload-item__content">
            <strong :title="item.file.name">{{ item.file.name }}</strong>
            <small>{{ formatBytes(item.file.size) }}</small>
            <div class="knowledge-upload-item__track"><i :style="{ width: `${item.progress}%` }" /></div>
          </div>
          <span class="knowledge-upload-item__state" :title="uploadStateLabel(item)">
            <LoaderCircle v-if="item.state === 'uploading'" :size="16" class="spin" />
            <CircleCheck v-else-if="item.state === 'success'" :size="16" />
            <CircleAlert v-else-if="item.state === 'failed'" :size="16" />
            <i v-else />
            {{ uploadStateLabel(item) }}
          </span>
          <button v-if="!uploading" type="button" class="icon-button knowledge-upload-item__remove" :title="`移除 ${item.file.name}`" :aria-label="`移除 ${item.file.name}`" @click="removeUploadItem(item.id)"><X :size="15" /></button>
        </div>
      </div>
      <template #footer>
        <button type="button" class="outline-button" :disabled="uploading" @click="uploadOpen = false">取消</button>
        <button type="button" class="primary-button" :disabled="!uploadCandidates.length || uploading" @click="uploadAll">{{ uploadButtonLabel }}</button>
      </template>
    </el-dialog>
  </div>
</template>
