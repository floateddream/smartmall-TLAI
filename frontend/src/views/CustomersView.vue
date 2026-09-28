<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Pencil, RefreshCw, Search, UserRoundPlus } from 'lucide-vue-next'
import { ApiError } from '@/api/client'
import { customerApi } from '@/api/catalog'
import type { Customer, CustomerInput } from '@/api/catalog'
import { useAuthStore } from '@/stores/auth'

type CustomerForm = { name: string; phone: string; email: string }

const auth = useAuthStore()
const rows = ref<Customer[]>([])
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
const form = reactive<CustomerForm>(emptyForm())
let requestId = 0

const rules: FormRules = {
  name: [{ required: true, whitespace: true, message: '请输入客户名称', trigger: 'blur' }, { max: 100, message: '最多 100 个字符', trigger: 'blur' }],
  phone: [{ max: 32, message: '最多 32 个字符', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }, { max: 128, message: '最多 128 个字符', trigger: 'blur' }],
}

function emptyForm(): CustomerForm {
  return { name: '', phone: '', email: '' }
}

async function loadPage(nextPage = page.value) {
  const current = ++requestId
  page.value = nextPage
  loading.value = true
  loadError.value = ''
  try {
    const result = await customerApi.list(nextPage, keyword.value.trim())
    if (current !== requestId) return
    rows.value = result.items
    total.value = result.total
  } catch (error) {
    if (current !== requestId) return
    rows.value = []
    total.value = 0
    loadError.value = error instanceof ApiError ? error.message : '客户加载失败'
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

function openEdit(customer: Customer) {
  editingId.value = customer.id
  Object.assign(form, { name: customer.name, phone: customer.phone || '', email: customer.email || '' })
  formError.value = ''
  dialogOpen.value = true
  formRef.value?.clearValidate()
}

async function save() {
  if (!formRef.value || !(await formRef.value.validate().catch(() => false))) return
  saving.value = true
  formError.value = ''
  try {
    const input: CustomerInput = { name: form.name.trim(), phone: form.phone.trim() || null, email: form.email.trim() || null }
    if (editingId.value === null) await customerApi.create(input)
    else await customerApi.update(editingId.value, input)
    dialogOpen.value = false
    ElMessage.success('客户已保存')
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
      <div><p class="eyebrow">OPERATIONS / 客户</p><h1>客户管理<span class="heading-spark">.</span></h1></div>
      <button v-if="auth.can('customer:manage')" type="button" class="primary-button catalog-add" @click="openCreate"><UserRoundPlus :size="18" />新增客户</button>
    </div>

    <div class="catalog-toolbar">
      <el-input v-model="keyword" placeholder="搜索姓名、电话或邮箱" clearable aria-label="搜索客户" @keyup.enter="loadPage(1)" @clear="loadPage(1)">
        <template #prefix><Search :size="16" /></template>
      </el-input>
      <button type="button" class="outline-button" @click="loadPage(1)"><Search :size="16" />搜索</button>
      <button type="button" class="icon-button catalog-refresh" title="刷新列表" aria-label="刷新列表" @click="loadPage()"><RefreshCw :size="17" /></button>
      <span class="catalog-count">共 {{ total }} 位客户</span>
    </div>

    <div class="catalog-table-wrap">
      <table class="catalog-table">
        <thead><tr><th>客户名称</th><th>联系电话</th><th>邮箱</th><th>创建时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="customer in loading || loadError ? [] : rows" :key="customer.id">
            <td><strong>{{ customer.name }}</strong><small class="mono">#{{ customer.id }}</small></td>
            <td>{{ customer.phone || '—' }}</td>
            <td class="catalog-muted">{{ customer.email || '—' }}</td>
            <td class="catalog-muted">{{ new Date(customer.createdAt).toLocaleString('zh-CN') }}</td>
            <td><button v-if="auth.can('customer:manage')" type="button" class="icon-button catalog-edit" :aria-label="`编辑 ${customer.name}`" :title="`编辑 ${customer.name}`" @click="openEdit(customer)"><Pencil :size="17" /></button></td>
          </tr>
        </tbody>
      </table>
      <div v-if="loading" class="catalog-feedback" role="status">正在加载客户...</div>
      <div v-else-if="loadError" class="catalog-feedback catalog-feedback--error" role="alert">{{ loadError }} <button type="button" class="text-link" @click="loadPage()">重试</button></div>
      <div v-else-if="!rows.length" class="catalog-feedback">{{ keyword ? '没有匹配的客户' : '暂无客户' }}</div>
    </div>
    <el-pagination v-if="total > 20" class="catalog-pagination" background :current-page="page" :page-size="20" :total="total" layout="prev, pager, next, total" @current-change="loadPage" />

    <el-dialog v-model="dialogOpen" :title="editingId === null ? '新增客户' : '编辑客户'" width="min(520px, 92vw)" :close-on-click-modal="!saving" :show-close="!saving">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="catalog-form" @submit.prevent="save">
        <el-form-item label="客户名称" prop="name"><el-input v-model="form.name" maxlength="100" /></el-form-item>
        <el-form-item label="联系电话" prop="phone"><el-input v-model="form.phone" maxlength="32" placeholder="可选" /></el-form-item>
        <el-form-item label="邮箱" prop="email"><el-input v-model="form.email" maxlength="128" placeholder="可选" /></el-form-item>
        <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>
        <div class="catalog-form__actions">
          <button type="button" class="outline-button" :disabled="saving" @click="dialogOpen = false">取消</button>
          <button type="submit" class="primary-button" :disabled="saving">{{ saving ? '保存中...' : '保存客户' }}</button>
        </div>
      </el-form>
    </el-dialog>
  </div>
</template>
