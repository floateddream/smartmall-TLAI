<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Check, CirclePower, Edit3, KeyRound, Plus, Radio, RefreshCw, ServerCog, TestTube2 } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import {
  aiApi,
  type AiModelConfig,
  type AiModelConfigInput,
  type AiModelType,
  type AiProviderCode,
  type AiProviderOption,
} from '@/api/ai'

const models = ref<AiModelConfig[]>([])
const providers = ref<AiProviderOption[]>([])
const loading = ref(false)
const saving = ref(false)
const testingId = ref<number | null>(null)
const dialogOpen = ref(false)
const editing = ref<AiModelConfig | null>(null)
const form = reactive<AiModelConfigInput>({
  name: '',
  modelType: 'CHAT',
  provider: 'DEEPSEEK',
  baseUrl: 'https://api.deepseek.com/v1',
  modelName: 'deepseek-chat',
  apiKey: '',
  temperature: 0.7,
  maxTokens: 2048,
  embeddingDimensions: null,
})

const activeChatModel = computed(() => models.value.find(model => model.active && model.modelType === 'CHAT'))
const activeEmbeddingModel = computed(() => models.value.find(model => model.active && model.modelType === 'EMBEDDING'))
const credentialCount = computed(() => models.value.filter(model => model.credentialConfigured).length)
const selectedProvider = computed(() => providers.value.find(provider => provider.code === form.provider))
const availableProviders = computed(() => providers.value.filter(provider => provider.supportedTypes.includes(form.modelType)))
const modelOptions = computed(() => form.modelType === 'EMBEDDING'
  ? selectedProvider.value?.embeddingModels ?? []
  : selectedProvider.value?.chatModels ?? [])
const credentialRequired = computed(() => selectedProvider.value?.credentialRequired ?? true)

function reportError(action: string, error: unknown) {
  console.error(`[AI模型配置] ${action}失败`, error)
  ElMessage.error(error instanceof Error ? error.message : `${action}失败，请查看浏览器控制台`)
}

async function loadModels() {
  loading.value = true
  try {
    models.value = await aiApi.listModels()
  } catch (error) {
    reportError('加载模型列表', error)
  } finally {
    loading.value = false
  }
}

async function loadProviders() {
  try {
    providers.value = await aiApi.listProviders()
  } catch (error) {
    reportError('加载供应商列表', error)
  }
}

function applyProvider(providerCode: AiProviderCode) {
  const provider = providers.value.find(item => item.code === providerCode)
  if (!provider) return
  form.baseUrl = provider.defaultBaseUrl
  form.modelName = (form.modelType === 'EMBEDDING' ? provider.embeddingModels : provider.chatModels)[0] ?? ''
  form.apiKey = ''
}

function applyModelType(modelType: AiModelType) {
  const preferredProvider = modelType === 'EMBEDDING' ? 'OLLAMA' : 'DEEPSEEK'
  const provider = providers.value.find(item => item.code === preferredProvider && item.supportedTypes.includes(modelType))
    ?? providers.value.find(item => item.supportedTypes.includes(modelType))
  if (!provider) return
  form.provider = provider.code
  form.embeddingDimensions = modelType === 'EMBEDDING' ? 1024 : null
  form.temperature = 0.7
  form.maxTokens = 2048
  applyProvider(provider.code)
}

function providerName(code: AiProviderCode) {
  return providers.value.find(provider => provider.code === code)?.name ?? code
}

function openCreate() {
  editing.value = null
  Object.assign(form, {
    name: '', modelType: 'CHAT', provider: 'DEEPSEEK', baseUrl: 'https://api.deepseek.com/v1',
    modelName: 'deepseek-chat', apiKey: '', temperature: 0.7, maxTokens: 2048, embeddingDimensions: null,
  })
  dialogOpen.value = true
}

function openEdit(model: AiModelConfig) {
  editing.value = model
  Object.assign(form, {
    name: model.name,
    modelType: model.modelType,
    provider: model.provider,
    baseUrl: model.baseUrl,
    modelName: model.modelName,
    apiKey: '',
    temperature: model.temperature,
    maxTokens: model.maxTokens,
    embeddingDimensions: model.embeddingDimensions,
  })
  dialogOpen.value = true
}

async function save() {
  if (!form.name.trim() || !form.baseUrl.trim() || !form.modelName.trim()) {
    ElMessage.warning('请填写完整的模型配置')
    return
  }
  if (form.modelType === 'EMBEDDING' && (!form.embeddingDimensions || form.embeddingDimensions < 1)) {
    ElMessage.warning('请填写正确的向量维度')
    return
  }
  if (credentialRequired.value && !editing.value && !form.apiKey.trim()) {
    ElMessage.warning('新增模型时必须填写 API Key')
    return
  }
  if (credentialRequired.value && editing.value && editing.value.provider !== form.provider && !form.apiKey.trim()) {
    ElMessage.warning('切换供应商时必须填写新的 API Key')
    return
  }
  saving.value = true
  console.info('[AI模型配置] 开始保存', {
    id: editing.value?.id,
    type: form.modelType,
    provider: form.provider,
    model: form.modelName,
    credentialChanged: Boolean(form.apiKey.trim()),
  })
  try {
    if (editing.value) await aiApi.updateModel(editing.value.id, { ...form })
    else await aiApi.createModel({ ...form })
    ElMessage.success(editing.value ? '模型配置已更新' : '模型配置已创建')
    dialogOpen.value = false
    await loadModels()
  } catch (error) {
    reportError(editing.value ? '更新模型配置' : '新增模型配置', error)
  } finally {
    saving.value = false
  }
}

async function activate(model: AiModelConfig) {
  console.info('[AI模型配置] 开始激活模型', { id: model.id, provider: model.provider, model: model.modelName })
  try {
    await aiApi.activateModel(model.id)
    ElMessage.success(`已切换到 ${model.name}`)
    await loadModels()
  } catch (error) {
    reportError('激活模型', error)
  }
}

async function toggleEnabled(model: AiModelConfig) {
  console.info('[AI模型配置] 开始切换启停状态', { id: model.id, enabled: !model.enabled })
  try {
    await aiApi.changeModelEnabled(model.id, !model.enabled)
    ElMessage.success(model.enabled ? '模型配置已停用' : '模型配置已启用')
    await loadModels()
  } catch (error) {
    reportError(model.enabled ? '停用模型' : '启用模型', error)
  }
}

async function test(model: AiModelConfig) {
  testingId.value = model.id
  console.info('[AI模型配置] 开始测试连接', { id: model.id, provider: model.provider, model: model.modelName })
  try {
    const result = await aiApi.testModel(model.id)
    ElMessage.success(`连接成功：${result.reply}`)
  } catch (error) {
    reportError('测试模型连接', error)
  } finally {
    testingId.value = null
  }
}

onMounted(async () => {
  await Promise.all([loadProviders(), loadModels()])
})
</script>

<template>
  <div class="models-view view-shell">
    <div class="page-heading">
      <div>
        <p class="eyebrow">SYSTEM / MODEL RUNTIME</p>
        <h1>模型配置<span class="heading-spark">.</span></h1>
        <p class="page-heading__subtitle">Chat & Embedding Runtime</p>
      </div>
      <div class="page-heading__actions">
        <span class="demo-badge" :class="credentialCount > 0 ? 'demo-badge--success' : 'demo-badge--warning'">
          <KeyRound :size="14" />已配置 {{ credentialCount }} 个密钥
        </span>
        <button type="button" class="primary-button" @click="openCreate"><Plus :size="16" />新增模型</button>
      </div>
    </div>

    <section class="model-runtime-strip">
      <div><span>CHAT RUNTIME</span><strong>{{ activeChatModel?.name || '未选择' }}</strong></div>
      <div><span>EMBEDDING RUNTIME</span><strong>{{ activeEmbeddingModel?.name || '未选择' }}</strong></div>
      <div><span>VECTOR MODEL</span><strong class="mono">{{ activeEmbeddingModel?.modelName || '—' }}</strong></div>
      <Radio :size="22" :class="{ 'runtime-pulse': activeChatModel || activeEmbeddingModel }" />
    </section>

    <div v-loading="loading" class="model-grid">
      <article v-for="model in models" :key="model.id" class="model-card" :class="{ 'model-card--active': model.active, 'model-card--disabled': !model.enabled }">
        <header>
          <span class="model-card__icon"><ServerCog :size="20" /></span>
          <div><h3>{{ model.name }}</h3><code>{{ model.modelType }} · {{ providerName(model.provider) }} / {{ model.modelName }}</code></div>
          <span v-if="model.active" class="model-live"><i />CURRENT</span>
        </header>
        <dl>
          <div><dt>API</dt><dd>{{ model.baseUrl }}</dd></div>
          <div><dt>认证</dt><dd :class="!model.credentialRequired || model.credentialConfigured ? 'text-green' : 'text-orange'">{{ !model.credentialRequired ? '本地服务免密' : model.credentialConfigured ? '已加密保存' : '未配置' }}</dd></div>
          <template v-if="model.modelType === 'CHAT'">
            <div><dt>温度</dt><dd>{{ model.temperature }}</dd></div>
            <div><dt>最大输出</dt><dd>{{ model.maxTokens }} tokens</dd></div>
          </template>
          <div v-else><dt>向量维度</dt><dd>{{ model.embeddingDimensions }}</dd></div>
        </dl>
        <footer>
          <button type="button" class="icon-button" title="编辑配置" aria-label="编辑配置" @click="openEdit(model)"><Edit3 :size="16" /></button>
          <span :title="!model.credentialRequired || model.credentialConfigured ? '测试连接' : '请先编辑并配置 API Key'"><button type="button" class="icon-button" aria-label="测试连接" :disabled="testingId === model.id || (model.credentialRequired && !model.credentialConfigured)" @click="test(model)"><RefreshCw v-if="testingId === model.id" :size="16" class="spin" /><TestTube2 v-else :size="16" /></button></span>
          <button v-if="!model.active" type="button" class="outline-button model-activate" :title="model.credentialRequired && !model.credentialConfigured ? '请先编辑并配置 API Key' : !model.enabled ? '请先启用模型' : '设为当前模型'" :disabled="!model.enabled || (model.credentialRequired && !model.credentialConfigured)" @click="activate(model)"><Check :size="15" />设为当前</button>
          <span v-else class="model-current"><Check :size="14" />使用中</span>
          <button type="button" class="icon-button" :class="{ 'text-green': model.enabled }" :title="model.enabled ? '停用配置' : '启用配置'" :aria-label="model.enabled ? '停用配置' : '启用配置'" @click="toggleEnabled(model)"><CirclePower :size="16" /></button>
        </footer>
      </article>
      <div v-if="!loading && models.length === 0" class="model-empty">
        <ServerCog :size="34" /><strong>暂无模型配置</strong>
      </div>
    </div>

    <el-dialog v-model="dialogOpen" :title="editing ? '编辑模型配置' : '新增模型配置'" width="min(520px, 94vw)" class="model-dialog">
      <el-form label-position="top">
        <el-form-item label="配置名称"><el-input v-model="form.name" maxlength="80" /></el-form-item>
        <el-form-item label="模型类型">
          <el-radio-group v-model="form.modelType" :disabled="Boolean(editing?.active)" @change="applyModelType">
            <el-radio-button value="CHAT">对话模型</el-radio-button>
            <el-radio-button value="EMBEDDING">向量模型</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="模型供应商">
          <el-select v-model="form.provider" style="width: 100%" @change="applyProvider">
            <el-option v-for="provider in availableProviders" :key="provider.code" :label="provider.name" :value="provider.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="接口地址"><el-input v-model="form.baseUrl" maxlength="500" /></el-form-item>
        <el-form-item label="模型名称">
          <el-select v-model="form.modelName" filterable allow-create default-first-option style="width: 100%" placeholder="选择或输入模型 ID">
            <el-option v-for="modelName in modelOptions" :key="modelName" :label="modelName" :value="modelName" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="credentialRequired" :label="editing ? 'API Key（留空表示不修改）' : 'API Key'">
          <el-input v-model="form.apiKey" type="password" show-password maxlength="512" autocomplete="new-password" :placeholder="editing && editing.credentialConfigured ? '已配置，留空保留原密钥' : '请输入供应商 API Key'" />
        </el-form-item>
        <div v-if="form.modelType === 'CHAT'" class="model-form-grid">
          <el-form-item label="温度"><el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" :precision="1" /></el-form-item>
          <el-form-item label="最大输出"><el-input-number v-model="form.maxTokens" :min="128" :max="32768" :step="128" /></el-form-item>
        </div>
        <el-form-item v-else label="向量维度">
          <el-input-number v-model="form.embeddingDimensions" :min="1" :max="4096" :step="128" />
        </el-form-item>
      </el-form>
      <template #footer><button type="button" class="primary-button" :disabled="saving" @click="save">{{ saving ? '保存中' : '保存配置' }}</button></template>
    </el-dialog>
  </div>
</template>
