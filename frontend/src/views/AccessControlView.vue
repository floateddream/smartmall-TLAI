<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { BookOpen, Edit3, History, Plus, RefreshCw, Search, ShieldCheck, UserPlus } from 'lucide-vue-next'
import { accessApi } from '@/api/access'
import type { AccessRole, ManagedUser, PermissionAudit, PermissionDefinition } from '@/api/access'
import { ApiError } from '@/api/client'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const activeTab = ref('users')
const users = ref<ManagedUser[]>([])
const roles = ref<AccessRole[]>([])
const permissions = ref<PermissionDefinition[]>([])
const audits = ref<PermissionAudit[]>([])
const userPage = ref(1)
const userTotal = ref(0)
const auditPage = ref(1)
const auditTotal = ref(0)
const keyword = ref('')
const roleFilter = ref('')
const enabledFilter = ref<boolean | ''>('')
const loadingUsers = ref(false)
const loadingRoles = ref(false)
const userLoadError = ref('')
const roleLoadError = ref('')
const auditLoading = ref(false)
const auditError = ref('')
const saving = ref(false)
const actingUserId = ref<number | null>(null)
const actingRoleId = ref<number | null>(null)
const userDialog = ref(false)
const roleDialog = ref(false)
const auditDrawer = ref(false)
const editingUser = ref<ManagedUser | null>(null)
const editingRole = ref<AccessRole | null>(null)

const userForm = reactive({ username: '', email: '', password: '', roleCodes: [] as string[] })
const roleForm = reactive({ code: '', name: '', description: '', permissionCodes: [] as string[] })
let userRequestId = 0
let roleRequestId = 0
let auditRequestId = 0

const canManageUsers = computed(() => auth.can('system:user_manage'))
const canManageRoles = computed(() => auth.can('system:rbac_manage'))
const permissionGroups = computed(() => {
  const grouped = new Map<string, PermissionDefinition[]>()
  for (const permission of permissions.value) {
    const items = grouped.get(permission.module) || []
    items.push(permission)
    grouped.set(permission.module, items)
  }
  return [...grouped.entries()].map(([module, items]) => ({ module, items }))
})

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message : fallback
}

function formatTime(value: string) {
  return new Date(value).toLocaleString('zh-CN')
}

function roleName(code: string) {
  return roles.value.find(role => role.code === code)?.name || code
}

async function loadUsers(nextPage = userPage.value) {
  const currentRequest = ++userRequestId
  loadingUsers.value = true
  userLoadError.value = ''
  userPage.value = nextPage
  try {
    const result = await accessApi.listUsers(nextPage, keyword.value.trim(), roleFilter.value, enabledFilter.value)
    if (currentRequest !== userRequestId) return
    const lastPage = Math.max(1, Math.ceil(result.total / 20))
    if (result.total > 0 && nextPage > lastPage) {
      await loadUsers(lastPage)
      return
    }
    users.value = result.items
    userTotal.value = result.total
  } catch (error) {
    if (currentRequest !== userRequestId) return
    users.value = []
    userTotal.value = 0
    userLoadError.value = errorMessage(error, '用户列表加载失败')
  } finally {
    if (currentRequest === userRequestId) loadingUsers.value = false
  }
}

async function loadRoles() {
  const currentRequest = ++roleRequestId
  loadingRoles.value = true
  roleLoadError.value = ''
  try {
    const [roleItems, permissionItems] = await Promise.all([
      accessApi.listRoles(),
      canManageRoles.value ? accessApi.listPermissions() : Promise.resolve([]),
    ])
    if (currentRequest !== roleRequestId) return
    roles.value = roleItems
    permissions.value = permissionItems
  } catch (error) {
    if (currentRequest !== roleRequestId) return
    roles.value = []
    permissions.value = []
    roleLoadError.value = errorMessage(error, '角色信息加载失败')
  } finally {
    if (currentRequest === roleRequestId) loadingRoles.value = false
  }
}

function resetUserFilters() {
  keyword.value = ''
  roleFilter.value = ''
  enabledFilter.value = ''
  void loadUsers(1)
}

function openCreateUser() {
  editingUser.value = null
  Object.assign(userForm, { username: '', email: '', password: '', roleCodes: [] })
  userDialog.value = true
}

function openUserRoles(user: ManagedUser) {
  editingUser.value = user
  Object.assign(userForm, { username: user.username, email: user.email, password: '', roleCodes: [...user.roles] })
  userDialog.value = true
}

async function saveUser() {
  if (!userForm.roleCodes.length) return ElMessage.warning('请至少选择一个角色')
  saving.value = true
  try {
    if (editingUser.value) {
      await accessApi.changeUserRoles(editingUser.value.id, userForm.roleCodes)
      ElMessage.success('用户角色已更新')
    } else {
      if (!userForm.username || !userForm.email || !userForm.password) return ElMessage.warning('请填写完整用户信息')
      await accessApi.createUser(userForm)
      ElMessage.success('用户已创建')
    }
    userDialog.value = false
    await loadUsers(editingUser.value ? userPage.value : 1)
  } catch (error) {
    ElMessage.error(errorMessage(error, '用户保存失败'))
  } finally {
    saving.value = false
  }
}

async function toggleUser(user: ManagedUser) {
  if (actingUserId.value !== null) return
  const action = user.enabled ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确认${action}用户“${user.username}”？`, `${action}用户`, {
      type: user.enabled ? 'warning' : 'info', confirmButtonText: `确认${action}`, cancelButtonText: '取消',
    })
    actingUserId.value = user.id
    await accessApi.changeUserEnabled(user.id, !user.enabled)
    ElMessage.success(`用户已${action}`)
    await loadUsers()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(errorMessage(error, `${action}失败`))
  } finally {
    actingUserId.value = null
  }
}

function openCreateRole() {
  editingRole.value = null
  Object.assign(roleForm, { code: '', name: '', description: '', permissionCodes: [] })
  roleDialog.value = true
}

function openEditRole(role: AccessRole) {
  editingRole.value = role
  Object.assign(roleForm, { code: role.code, name: role.name, description: role.description || '', permissionCodes: [...role.permissions] })
  roleDialog.value = true
}

async function saveRole() {
  if (!roleForm.code || !roleForm.name) return ElMessage.warning('请填写角色编码和名称')
  saving.value = true
  try {
    if (editingRole.value) {
      await accessApi.updateRole(editingRole.value.id, { name: roleForm.name, description: roleForm.description })
      if (editingRole.value.code !== 'ADMIN') {
        await accessApi.changeRolePermissions(editingRole.value.id, roleForm.permissionCodes)
      }
      ElMessage.success('角色配置已更新')
    } else {
      await accessApi.createRole(roleForm)
      ElMessage.success('角色已创建')
    }
    roleDialog.value = false
    await loadRoles()
    await loadUsers()
  } catch (error) {
    ElMessage.error(errorMessage(error, '角色保存失败'))
  } finally {
    saving.value = false
  }
}

async function toggleRole(role: AccessRole) {
  if (actingRoleId.value !== null) return
  const action = role.enabled ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确认${action}角色“${role.name}”？`, `${action}角色`, {
      type: role.enabled ? 'warning' : 'info', confirmButtonText: `确认${action}`, cancelButtonText: '取消',
    })
    actingRoleId.value = role.id
    await accessApi.changeRoleEnabled(role.id, !role.enabled)
    ElMessage.success(`角色已${action}`)
    await loadRoles()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(errorMessage(error, `${action}失败`))
  } finally {
    actingRoleId.value = null
  }
}

async function openAudits() {
  auditDrawer.value = true
  await loadAuditPage(1)
}

async function loadAuditPage(page: number) {
  const currentRequest = ++auditRequestId
  auditLoading.value = true
  auditError.value = ''
  auditPage.value = page
  try {
    const result = await accessApi.listAudits(page)
    if (currentRequest !== auditRequestId) return
    const lastPage = Math.max(1, Math.ceil(result.total / 30))
    if (result.total > 0 && page > lastPage) {
      await loadAuditPage(lastPage)
      return
    }
    audits.value = result.items
    auditTotal.value = result.total
  } catch (error) {
    if (currentRequest !== auditRequestId) return
    audits.value = []
    auditTotal.value = 0
    auditError.value = errorMessage(error, '审计记录加载失败')
  } finally {
    if (currentRequest === auditRequestId) auditLoading.value = false
  }
}

const actionLabels: Record<string, string> = {
  USER_CREATED: '创建用户', USER_ROLES_CHANGED: '调整用户角色', USER_ENABLED_CHANGED: '调整用户状态',
  ROLE_CREATED: '创建角色', ROLE_UPDATED: '编辑角色', ROLE_PERMISSIONS_CHANGED: '调整角色权限', ROLE_ENABLED_CHANGED: '调整角色状态',
}

onMounted(async () => {
  await loadRoles()
  await loadUsers(1)
})
</script>

<template>
  <div class="view-shell access-view">
    <div class="page-heading">
      <div><p class="eyebrow">SYSTEM / RBAC</p><h1>用户与权限<span class="heading-spark">.</span></h1><p class="page-heading__subtitle">通过角色为后台用户分配最小必要权限</p></div>
      <div class="page-heading__actions">
        <button v-if="canManageRoles" type="button" class="outline-button" @click="openAudits"><History :size="16" />操作审计</button>
        <span class="demo-badge demo-badge--muted"><ShieldCheck :size="14" /> RBAC</span>
      </div>
    </div>

    <div v-if="roleLoadError" class="catalog-feedback catalog-feedback--error" role="alert">{{ roleLoadError }} <button type="button" class="text-link" :disabled="loadingRoles" @click="loadRoles">重试</button></div>

    <el-tabs v-model="activeTab" class="access-tabs">
      <el-tab-pane label="用户管理" name="users">
        <div class="catalog-toolbar access-toolbar">
          <el-input v-model="keyword" placeholder="搜索用户名或邮箱" clearable aria-label="搜索后台用户" :disabled="loadingUsers" @keyup.enter="loadUsers(1)" @clear="loadUsers(1)"><template #prefix><Search :size="16" /></template></el-input>
          <el-select v-model="roleFilter" placeholder="全部角色" clearable aria-label="筛选用户角色" :disabled="loadingUsers" @change="loadUsers(1)"><el-option label="全部角色" value="" /><el-option v-for="role in roles" :key="role.id" :label="role.name" :value="role.code" /></el-select>
          <el-select v-model="enabledFilter" placeholder="全部状态" aria-label="筛选用户状态" :disabled="loadingUsers" @change="loadUsers(1)"><el-option label="全部状态" value="" /><el-option label="已启用" :value="true" /><el-option label="已停用" :value="false" /></el-select>
          <button type="button" class="outline-button" :disabled="loadingUsers" @click="loadUsers(1)"><Search :size="16" />筛选</button>
          <button v-if="keyword || roleFilter || enabledFilter !== ''" type="button" class="text-link" :disabled="loadingUsers" @click="resetUserFilters">重置</button>
          <button type="button" class="icon-button catalog-refresh" title="刷新" aria-label="刷新用户列表" :disabled="loadingUsers" @click="loadUsers()"><RefreshCw :size="17" :class="{ spinning: loadingUsers }" /></button>
          <button v-if="canManageUsers" type="button" class="primary-button access-create" @click="openCreateUser"><UserPlus :size="16" />新建用户</button>
        </div>

        <div class="catalog-table-wrap">
          <table class="catalog-table access-table">
            <thead><tr><th>用户</th><th>角色</th><th>权限数</th><th>状态</th><th>创建时间</th><th>操作</th></tr></thead>
            <tbody>
              <tr v-for="user in users" :key="user.id">
                <td><strong>{{ user.username }}</strong><small>{{ user.email }}</small></td>
                <td><div class="access-tags"><span v-for="role in user.roles" :key="role" class="access-tag">{{ roleName(role) }}</span></div></td>
                <td><span class="mono">{{ user.permissions.length }}</span></td>
                <td><span class="catalog-status" :class="user.enabled ? 'catalog-status--active' : 'catalog-status--inactive'">{{ user.enabled ? '已启用' : '已停用' }}</span></td>
                <td class="catalog-muted">{{ formatTime(user.createdAt) }}</td>
                <td><div class="order-actions">
                  <button v-if="canManageUsers" type="button" class="icon-button catalog-edit" title="分配角色" aria-label="分配用户角色" :disabled="user.id === auth.user?.id || actingUserId !== null" @click="openUserRoles(user)"><Edit3 :size="16" /></button>
                  <el-switch v-if="canManageUsers" :model-value="user.enabled" :aria-label="`${user.enabled ? '停用' : '启用'}用户 ${user.username}`" :disabled="user.id === auth.user?.id || actingUserId !== null" :loading="actingUserId === user.id" :before-change="() => { void toggleUser(user); return false }" />
                </div></td>
              </tr>
            </tbody>
          </table>
          <div v-if="loadingUsers" class="catalog-feedback" role="status">正在加载用户...</div>
          <div v-else-if="userLoadError" class="catalog-feedback catalog-feedback--error" role="alert">{{ userLoadError }} <button type="button" class="text-link" @click="loadUsers()">重试</button></div>
          <div v-else-if="!users.length" class="catalog-feedback">{{ keyword || roleFilter || enabledFilter !== '' ? '没有匹配的用户' : '暂无后台用户' }}</div>
        </div>
        <el-pagination v-if="userTotal > 20" class="catalog-pagination" background :current-page="userPage" :page-size="20" :total="userTotal" layout="prev, pager, next, total" @current-change="loadUsers" />
      </el-tab-pane>

      <el-tab-pane v-if="canManageRoles" label="角色管理" name="roles">
        <div class="access-role-head"><span>共 {{ roles.length }} 个角色，权限变更立即影响下一次接口请求</span><button type="button" class="primary-button access-create" :disabled="loadingRoles || Boolean(roleLoadError)" @click="openCreateRole"><Plus :size="16" />新建角色</button></div>
        <div class="role-grid">
          <article v-for="role in roles" :key="role.id" class="role-card" :class="{ 'role-card--disabled': !role.enabled }">
            <header><span class="role-card__icon"><ShieldCheck :size="19" /></span><div><h3>{{ role.name }}</h3><code>{{ role.code }}</code></div><span v-if="role.builtIn" class="demo-badge demo-badge--muted">内置</span></header>
            <p>{{ role.description || '暂无角色说明' }}</p>
            <div class="role-card__stats"><span><strong>{{ role.permissions.length }}</strong> 项权限</span><span :class="role.enabled ? 'text-green' : 'text-muted'">{{ role.enabled ? '启用中' : '已停用' }}</span></div>
            <footer><button type="button" class="outline-button" :disabled="actingRoleId !== null" @click="openEditRole(role)"><Edit3 :size="15" />编辑配置</button><el-switch :model-value="role.enabled" :aria-label="`${role.enabled ? '停用' : '启用'}角色 ${role.name}`" :disabled="role.code === 'ADMIN' || actingRoleId !== null" :loading="actingRoleId === role.id" :before-change="() => { void toggleRole(role); return false }" /></footer>
          </article>
          <div v-if="loadingRoles" class="catalog-feedback" role="status">正在加载角色...</div>
          <div v-else-if="!roles.length && !roleLoadError" class="catalog-feedback">暂无角色</div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="userDialog" :title="editingUser ? '分配用户角色' : '新建后台用户'" width="min(520px, 94vw)" :close-on-click-modal="!saving" :show-close="!saving">
      <el-form class="catalog-form access-form" label-position="top">
        <div v-if="!editingUser" class="catalog-form__row"><el-form-item label="用户名"><el-input v-model="userForm.username" maxlength="32" /></el-form-item><el-form-item label="邮箱"><el-input v-model="userForm.email" maxlength="128" /></el-form-item></div>
        <el-form-item v-if="!editingUser" label="初始密码"><el-input v-model="userForm.password" type="password" show-password maxlength="64" placeholder="至少 8 位，包含字母和数字" /></el-form-item>
        <el-form-item label="角色"><el-checkbox-group v-model="userForm.roleCodes" class="access-role-options"><el-checkbox v-for="role in roles.filter(item => item.enabled)" :key="role.id" :value="role.code"><strong>{{ role.name }}</strong><small>{{ role.code }}</small></el-checkbox></el-checkbox-group></el-form-item>
        <div class="catalog-form__actions"><button type="button" class="outline-button" :disabled="saving" @click="userDialog = false">取消</button><button type="button" class="primary-button" :disabled="saving" @click="saveUser">{{ saving ? '保存中...' : '确认保存' }}</button></div>
      </el-form>
    </el-dialog>

    <el-dialog v-model="roleDialog" :title="editingRole ? '编辑角色' : '新建角色'" width="min(760px, 96vw)" :close-on-click-modal="!saving" :show-close="!saving">
      <el-form class="catalog-form access-form" label-position="top">
        <div class="catalog-form__row"><el-form-item label="角色编码"><el-input v-model="roleForm.code" :disabled="Boolean(editingRole)" maxlength="64" placeholder="例如 CONTENT_REVIEWER" /></el-form-item><el-form-item label="角色名称"><el-input v-model="roleForm.name" maxlength="64" /></el-form-item></div>
        <el-form-item label="角色说明"><el-input v-model="roleForm.description" maxlength="300" /></el-form-item>
        <el-form-item label="权限矩阵"><div class="permission-matrix" :class="{ 'permission-matrix--locked': editingRole?.code === 'ADMIN' }"><section v-for="group in permissionGroups" :key="group.module"><h4>{{ group.module }}</h4><el-checkbox-group v-model="roleForm.permissionCodes" :disabled="editingRole?.code === 'ADMIN'"><el-checkbox v-for="permission in group.items" :key="permission.id" :value="permission.code"><span>{{ permission.name }}</span><code>{{ permission.code }}</code></el-checkbox></el-checkbox-group></section></div></el-form-item>
        <div class="catalog-form__actions"><button type="button" class="outline-button" :disabled="saving" @click="roleDialog = false">取消</button><button type="button" class="primary-button" :disabled="saving" @click="saveRole">{{ saving ? '保存中...' : '保存角色' }}</button></div>
      </el-form>
    </el-dialog>

    <el-drawer v-model="auditDrawer" title="权限操作审计" size="min(620px, 96vw)" class="order-detail-drawer">
      <div v-if="auditLoading" class="catalog-feedback" role="status">正在加载审计记录...</div>
      <div v-else-if="auditError" class="catalog-feedback catalog-feedback--error" role="alert">{{ auditError }} <button type="button" class="text-link" @click="loadAuditPage(auditPage)">重试</button></div>
      <div v-else class="audit-list"><article v-for="audit in audits" :key="audit.id" class="audit-item"><span class="audit-item__icon"><BookOpen :size="16" /></span><div><header><strong>{{ actionLabels[audit.action] || audit.action }}</strong><time>{{ formatTime(audit.createdAt) }}</time></header><p><b>{{ audit.operatorName }}</b> 操作 {{ audit.targetType === 'USER' ? '用户' : '角色' }}“{{ audit.targetName }}”</p><small v-if="audit.beforeValue || audit.afterValue">{{ audit.beforeValue || '空' }} → {{ audit.afterValue || '空' }}</small></div></article><p v-if="!audits.length" class="catalog-feedback">暂无审计记录</p></div>
      <el-pagination v-if="auditTotal > 30" class="catalog-pagination" background :current-page="auditPage" :page-size="30" :total="auditTotal" layout="prev, pager, next" @current-change="loadAuditPage" />
    </el-drawer>
  </div>
</template>
