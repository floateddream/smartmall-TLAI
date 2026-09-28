<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { PackagePlus, Pencil, RefreshCw, Search } from 'lucide-vue-next'
import { ApiError } from '@/api/client'
import { productApi } from '@/api/catalog'
import type { Product, ProductInput } from '@/api/catalog'
import { useAuthStore } from '@/stores/auth'

type ProductForm = Omit<ProductInput, 'description'> & { description: string }

const auth = useAuthStore()
const rows = ref<Product[]>([])
const page = ref(1)
const total = ref(0)
const keyword = ref('')
const loading = ref(false)
const loadError = ref('')
const dialogOpen = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formError = ref('')
const formRef = ref<FormInstance>()
const form = reactive<ProductForm>(emptyForm())
let requestId = 0

const rules: FormRules = {
  sku: [{ required: true, whitespace: true, message: '请输入 SKU', trigger: 'blur' }, { max: 64, message: '最多 64 个字符', trigger: 'blur' }],
  name: [{ required: true, whitespace: true, message: '请输入商品名称', trigger: 'blur' }, { max: 120, message: '最多 120 个字符', trigger: 'blur' }],
  description: [{ max: 1000, message: '最多 1000 个字符', trigger: 'blur' }],
  price: [{ required: true, type: 'number', min: 0.01, message: '价格必须大于 0', trigger: 'change' }],
  stock: [{ required: true, type: 'integer', min: 0, message: '库存不能小于 0', trigger: 'change' }],
}

function emptyForm(): ProductForm {
  return { sku: '', name: '', description: '', price: 0.01, stock: 0, active: true }
}

async function loadPage(nextPage = page.value) {
  const current = ++requestId
  page.value = nextPage
  loading.value = true
  loadError.value = ''
  try {
    const result = await productApi.list(nextPage, keyword.value.trim())
    if (current !== requestId) return
    rows.value = result.items
    total.value = result.total
  } catch (error) {
    if (current !== requestId) return
    rows.value = []
    total.value = 0
    loadError.value = error instanceof ApiError ? error.message : '商品加载失败'
  } finally {
    if (current === requestId) loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  formError.value = ''
  dialogOpen.value = true
  formRef.value?.clearValidate()
}

function openEdit(product: Product) {
  editingId.value = product.id
  Object.assign(form, { sku: product.sku, name: product.name, description: product.description || '',
    price: product.price, stock: product.stock, active: product.active })
  formError.value = ''
  dialogOpen.value = true
  formRef.value?.clearValidate()
}

async function save() {
  if (!formRef.value || !(await formRef.value.validate().catch(() => false))) return
  saving.value = true
  formError.value = ''
  try {
    const input: ProductInput = { ...form, sku: form.sku.trim(), name: form.name.trim(), description: form.description.trim() || null }
    if (editingId.value === null) await productApi.create(input)
    else await productApi.update(editingId.value, input)
    dialogOpen.value = false
    ElMessage.success('商品已保存')
    await loadPage(editingId.value === null ? 1 : page.value)
  } catch (error) {
    formError.value = error instanceof ApiError ? error.message : '保存失败，请重试'
  } finally {
    saving.value = false
  }
}

onMounted(() => { void loadPage(1) })
</script>

<template>
  <div class="view-shell catalog-view">
    <div class="page-heading">
      <div><p class="eyebrow">OPERATIONS / 商品</p><h1>商品管理<span class="heading-spark">.</span></h1></div>
      <button v-if="auth.can('product:manage')" type="button" class="primary-button catalog-add" @click="openCreate"><PackagePlus :size="18" />新增商品</button>
    </div>

    <div class="catalog-toolbar">
      <el-input v-model="keyword" placeholder="搜索 SKU 或商品名称" clearable aria-label="搜索商品" @keyup.enter="loadPage(1)" @clear="loadPage(1)">
        <template #prefix><Search :size="16" /></template>
      </el-input>
      <button type="button" class="outline-button" @click="loadPage(1)"><Search :size="16" />搜索</button>
      <button type="button" class="icon-button catalog-refresh" title="刷新列表" aria-label="刷新列表" @click="loadPage()"><RefreshCw :size="17" /></button>
      <span class="catalog-count">共 {{ total }} 件商品</span>
    </div>

    <div class="catalog-table-wrap">
      <table class="catalog-table">
        <thead><tr><th>SKU / 商品</th><th>价格</th><th>库存</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="product in loading || loadError ? [] : rows" :key="product.id">
            <td><strong>{{ product.name }}</strong><small class="mono">{{ product.sku }}</small></td>
            <td class="catalog-money">¥ {{ Number(product.price).toFixed(2) }}</td>
            <td>{{ product.stock }}</td>
            <td><span class="catalog-status" :class="product.active ? 'catalog-status--active' : 'catalog-status--inactive'">{{ product.active ? '上架' : '停用' }}</span></td>
            <td class="catalog-muted">{{ new Date(product.createdAt).toLocaleString('zh-CN') }}</td>
            <td><button v-if="auth.can('product:manage')" type="button" class="icon-button catalog-edit" :aria-label="`编辑 ${product.name}`" :title="`编辑 ${product.name}`" @click="openEdit(product)"><Pencil :size="17" /></button></td>
          </tr>
        </tbody>
      </table>
      <div v-if="loading" class="catalog-feedback" role="status">正在加载商品...</div>
      <div v-else-if="loadError" class="catalog-feedback catalog-feedback--error" role="alert">{{ loadError }} <button type="button" class="text-link" @click="loadPage()">重试</button></div>
      <div v-else-if="!rows.length" class="catalog-feedback">{{ keyword ? '没有匹配的商品' : '暂无商品' }}</div>
    </div>
    <el-pagination v-if="total > 20" class="catalog-pagination" background :current-page="page" :page-size="20" :total="total" layout="prev, pager, next, total" @current-change="loadPage" />

    <el-dialog v-model="dialogOpen" :title="editingId === null ? '新增商品' : '编辑商品'" width="min(520px, 92vw)" :close-on-click-modal="!saving" :show-close="!saving">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="catalog-form" @submit.prevent="save">
        <el-form-item label="SKU" prop="sku"><el-input v-model="form.sku" maxlength="64" placeholder="例如 SM-001" /></el-form-item>
        <el-form-item label="商品名称" prop="name"><el-input v-model="form.name" maxlength="120" /></el-form-item>
        <div class="catalog-form__row">
          <el-form-item label="售价（元）" prop="price"><el-input-number v-model="form.price" :min="0.01" :max="9999999999.99" :precision="2" :step="1" controls-position="right" /></el-form-item>
          <el-form-item label="库存" prop="stock"><el-input-number v-model="form.stock" :min="0" :max="2147483647" :precision="0" controls-position="right" /></el-form-item>
        </div>
        <el-form-item label="说明" prop="description"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="1000" show-word-limit /></el-form-item>
        <el-form-item label="上架状态"><el-switch v-model="form.active" inline-prompt active-text="上架" inactive-text="停用" /></el-form-item>
        <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>
        <div class="catalog-form__actions">
          <button type="button" class="outline-button" :disabled="saving" @click="dialogOpen = false">取消</button>
          <button type="submit" class="primary-button" :disabled="saving">{{ saving ? '保存中...' : '保存商品' }}</button>
        </div>
      </el-form>
    </el-dialog>
  </div>
</template>
