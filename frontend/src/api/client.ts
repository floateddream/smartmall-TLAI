import axios from 'axios'
import type { AxiosError } from 'axios'
import type { ApiResponse } from '@/types/api'

const TOKEN_KEY = 'smartmall-tlai-access-token'
let onUnauthorized: (() => void) | undefined

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly code: string,
    public readonly status?: number,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

export const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// 所有业务请求都从唯一的 Token 存储位置读取，刷新页面后无需重新配置实例。
api.interceptors.request.use((config) => {
  const token = tokenStorage.get()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

api.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    if (body?.code && body.code !== 'SUCCESS') {
      return Promise.reject(new ApiError(body.message, body.code, response.status))
    }
    return response
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    const status = error.response?.status
    const body = error.response?.data
    // 登录接口本身的 401 应由表单显示，不触发全局退出。
    if (status === 401 && !error.config?.url?.endsWith('/login')) onUnauthorized?.()
    return Promise.reject(
      new ApiError(
        body?.message || (error.response ? '请求失败，请稍后重试' : '无法连接服务，请检查后端是否启动'),
        body?.code || 'NETWORK_ERROR',
        status,
      ),
    )
  },
)
