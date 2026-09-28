import { ref } from 'vue'
import { defineStore } from 'pinia'
import { api, tokenStorage } from '@/api/client'
import type { ApiResponse, LoginInput, LoginResult, RegisterInput, User } from '@/types/api'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(tokenStorage.get())
  let restoring: Promise<boolean> | null = null

  async function login(input: LoginInput) {
    const response = await api.post<ApiResponse<LoginResult>>('/auth/login', input)
    tokenStorage.set(response.data.data.accessToken)
    token.value = response.data.data.accessToken
    user.value = response.data.data.user
  }

  async function register(input: RegisterInput) {
    await api.post<ApiResponse<User>>('/auth/register', input)
  }

  function logout() {
    tokenStorage.clear()
    token.value = null
    user.value = null
  }

  function can(permission: string) {
    return Boolean(user.value?.permissions.includes(permission))
  }

  async function restore(): Promise<boolean> {
    if (!token.value) return false
    if (user.value) return true
    // 多个路由同时触发时共享同一次 /me 请求，避免重复拉取用户。
    if (!restoring) {
      restoring = api.get<ApiResponse<User>>('/auth/me')
        .then((response) => {
          user.value = response.data.data
          return true
        })
        .catch(() => {
          logout()
          return false
        })
        .finally(() => { restoring = null })
    }
    return restoring
  }

  return { user, token, login, register, logout, restore, can }
})
