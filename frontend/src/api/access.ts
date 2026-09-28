import { api } from '@/api/client'
import type { PageResult } from '@/api/catalog'
import type { ApiResponse } from '@/types/api'

export interface ManagedUser {
  id: number
  username: string
  email: string
  enabled: boolean
  roles: string[]
  permissions: string[]
  createdAt: string
  updatedAt: string
}

export interface AccessRole {
  id: number
  code: string
  name: string
  description: string | null
  builtIn: boolean
  enabled: boolean
  permissions: string[]
  createdAt: string
  updatedAt: string
}

export interface PermissionDefinition {
  id: number
  code: string
  name: string
  module: string
  description: string | null
}

export interface PermissionAudit {
  id: number
  operatorId: number | null
  operatorName: string
  targetType: 'USER' | 'ROLE'
  targetId: number
  targetName: string
  action: string
  beforeValue: string | null
  afterValue: string | null
  createdAt: string
}

export interface CreateManagedUserInput {
  username: string
  email: string
  password: string
  roleCodes: string[]
}

export interface RoleInput {
  code: string
  name: string
  description: string
  permissionCodes: string[]
}

export const accessApi = {
  listUsers: async (page: number, keyword: string, roleCode: string, enabled: boolean | '') =>
    (await api.get<ApiResponse<PageResult<ManagedUser>>>('/admin/users', {
      params: { page, size: 20, keyword: keyword || undefined, roleCode: roleCode || undefined, enabled: enabled === '' ? undefined : enabled },
    })).data.data,
  createUser: async (input: CreateManagedUserInput) =>
    (await api.post<ApiResponse<ManagedUser>>('/admin/users', input)).data.data,
  changeUserRoles: async (id: number, roleCodes: string[]) =>
    (await api.put<ApiResponse<ManagedUser>>(`/admin/users/${id}/roles`, { roleCodes })).data.data,
  changeUserEnabled: async (id: number, enabled: boolean) =>
    (await api.patch<ApiResponse<ManagedUser>>(`/admin/users/${id}/enabled`, { enabled })).data.data,
  listRoles: async () =>
    (await api.get<ApiResponse<AccessRole[]>>('/admin/roles')).data.data,
  createRole: async (input: RoleInput) =>
    (await api.post<ApiResponse<AccessRole>>('/admin/roles', input)).data.data,
  updateRole: async (id: number, input: Pick<RoleInput, 'name' | 'description'>) =>
    (await api.put<ApiResponse<AccessRole>>(`/admin/roles/${id}`, input)).data.data,
  changeRolePermissions: async (id: number, permissionCodes: string[]) =>
    (await api.put<ApiResponse<AccessRole>>(`/admin/roles/${id}/permissions`, { permissionCodes })).data.data,
  changeRoleEnabled: async (id: number, enabled: boolean) =>
    (await api.patch<ApiResponse<AccessRole>>(`/admin/roles/${id}/enabled`, { enabled })).data.data,
  listPermissions: async () =>
    (await api.get<ApiResponse<PermissionDefinition[]>>('/admin/permissions')).data.data,
  listAudits: async (page = 1) =>
    (await api.get<ApiResponse<PageResult<PermissionAudit>>>('/admin/audits', { params: { page, size: 30 } })).data.data,
}
