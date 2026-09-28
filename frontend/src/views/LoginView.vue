<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, Eye, EyeOff, LockKeyhole, UserRound } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/api/client'
import AuthLayout from '@/layouts/AuthLayout.vue'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const form = reactive({ account: '', password: '' })
const formRef = ref<FormInstance>()
const loading = ref(false)
const showPassword = ref(false)
const errorMessage = ref('')
const rules: FormRules = {
  account: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submit() {
  if (!formRef.value || !(await formRef.value.validate().catch(() => false))) return
  loading.value = true
  errorMessage.value = ''
  try {
    await auth.login({ account: form.account.trim(), password: form.password })
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
      && !route.query.redirect.startsWith('//') ? route.query.redirect : '/dashboard'
    await router.replace(redirect)
  } catch (error) {
    errorMessage.value = error instanceof ApiError ? error.message : '登录失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthLayout>
    <section class="auth-panel" aria-labelledby="login-heading">
      <div class="auth-panel__topline"><span>ACCESS PORTAL</span><span>01 — 02</span></div>
      <div class="auth-panel__heading">
        <h2 id="login-heading">欢迎回来</h2>
        <p>登录你的运营工作台</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名或邮箱" prop="account">
          <el-input v-model="form.account" size="large" placeholder="请输入用户名或邮箱" autocomplete="username" @input="errorMessage = ''">
            <template #prefix><UserRound :size="17" /></template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" size="large" :type="showPassword ? 'text' : 'password'" placeholder="请输入密码" autocomplete="current-password" @input="errorMessage = ''">
            <template #prefix><LockKeyhole :size="17" /></template>
            <template #suffix>
              <button type="button" class="input-tool" :aria-label="showPassword ? '隐藏密码' : '显示密码'" :title="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword">
                <EyeOff v-if="showPassword" :size="17" /><Eye v-else :size="17" />
              </button>
            </template>
          </el-input>
        </el-form-item>
        <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>
        <button class="primary-button auth-panel__submit" type="submit" :disabled="loading">
          <span>{{ loading ? '正在登录...' : '进入工作台' }}</span><ArrowRight :size="18" />
        </button>
      </el-form>
      <div class="auth-panel__switch">还没有账号？ <router-link to="/register">创建账号 <ArrowRight :size="14" /></router-link></div>
    </section>
  </AuthLayout>
</template>
