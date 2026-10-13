<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight, Eye, EyeOff, LockKeyhole, Mail, UserRound } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/api/client'
import AuthLayout from '@/layouts/AuthLayout.vue'

const router = useRouter()
const auth = useAuthStore()
const form = reactive({ username: '', email: '', password: '', confirmPassword: '' })
const formRef = ref<FormInstance>()
const loading = ref(false)
const showPassword = ref(false)
const errorMessage = ref('')
const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{4,32}$/, message: '4-32 位字母、数字或下划线', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
    { max: 128, message: '邮箱不能超过 128 位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/, message: '8-64 位，至少包含字母和数字', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: (_rule, value: string, callback: (error?: Error) => void) => callback(value === form.password ? undefined : new Error('两次输入的密码不一致')), trigger: 'blur' },
  ],
}

async function submit() {
  if (!formRef.value || !(await formRef.value.validate().catch(() => false))) return
  loading.value = true
  errorMessage.value = ''
  try {
    await auth.register({ username: form.username, email: form.email, password: form.password })
    ElMessage.success('注册成功，请登录')
    await router.replace({ name: 'login' })
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '注册失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout>
    <section class="auth-panel auth-panel--register" aria-labelledby="register-heading">
      <div class="auth-panel__topline"><span>NEW ACCOUNT</span><span>02 — 02</span></div>
      <div class="auth-panel__heading">
        <router-link class="back-link" :to="{ name: 'login' }"><ArrowLeft :size="15" /> 返回登录</router-link>
        <h2 id="register-heading">创建账号</h2>
        <p>加入 SmartMall TLAI 工作台</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" size="large" placeholder="4-32 位字母、数字或下划线" autocomplete="username" @input="errorMessage = ''">
            <template #prefix><UserRound :size="17" /></template>
          </el-input>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" size="large" placeholder="name@example.com" autocomplete="email" @input="errorMessage = ''">
            <template #prefix><Mail :size="17" /></template>
          </el-input>
        </el-form-item>
        <div class="auth-panel__fields">
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" size="large" :type="showPassword ? 'text' : 'password'" placeholder="至少 8 位" autocomplete="new-password" @input="errorMessage = ''">
              <template #prefix><LockKeyhole :size="17" /></template>
              <template #suffix>
                <button type="button" class="input-tool" :aria-label="showPassword ? '隐藏密码' : '显示密码'" :title="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword">
                  <EyeOff v-if="showPassword" :size="17" /><Eye v-else :size="17" />
                </button>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="form.confirmPassword" size="large" :type="showPassword ? 'text' : 'password'" placeholder="再次输入密码" autocomplete="new-password" @input="errorMessage = ''">
              <template #prefix><LockKeyhole :size="17" /></template>
            </el-input>
          </el-form-item>
        </div>
        <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>
        <button class="primary-button auth-panel__submit" type="submit" :disabled="loading">
          <span>{{ loading ? '正在创建...' : '创建账号' }}</span><ArrowRight :size="18" />
        </button>
      </el-form>
      <div class="auth-panel__switch">已有账号？ <router-link :to="{ name: 'login' }">直接登录 <ArrowRight :size="14" /></router-link></div>
    </section>
  </AuthLayout>
</template>
