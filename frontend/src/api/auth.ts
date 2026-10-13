import { api } from '@/api/client'
import type { ApiResponse } from '@/types/api'

export interface RegistrationStatus {
  enabled: boolean
}

export const authApi = {
  getRegistrationStatus: async () =>
    (await api.get<ApiResponse<RegistrationStatus>>('/auth/registration')).data.data,
}
