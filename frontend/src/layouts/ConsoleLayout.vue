<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  BarChart3, Boxes, BrainCircuit, ChevronDown, ChevronRight,
  Database, LayoutDashboard, LogOut, Menu, PanelLeftClose, PanelLeftOpen, RadioTower, ScrollText,
  RotateCcw, Settings2, ShieldCheck, ShoppingBag, Ticket, UserRound, UsersRound,
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)
const mobileOpen = ref(false)

const groups = computed(() => [
  { label: '总览', items: [{ path: '/dashboard', label: '仪表盘', icon: LayoutDashboard, permission: 'dashboard:view' }] },
  { label: '业务运营', items: [
    { path: '/products', label: '商品管理', icon: ShoppingBag, permission: 'product:view' },
    { path: '/customers', label: '客户管理', icon: UsersRound, permission: 'customer:view' },
    { path: '/orders', label: '订单管理', icon: Boxes, permission: 'order:view' },
    { path: '/refunds', label: '退款管理', icon: RotateCcw, permission: 'refund:view' },
    { path: '/tickets', label: '售后工单', icon: Ticket, permission: 'ticket:view' },
  ] },
  { label: '智能工作台', items: [
    { path: '/assistant', label: 'AI 助手', icon: BrainCircuit, permission: 'ai:chat' },
    { path: '/knowledge', label: '知识库', icon: Database, permission: 'knowledge:view' },
    { path: '/data-analysis', label: '数据分析', icon: BarChart3, permission: 'analytics:view' },
    { path: '/agent-audits', label: 'Agent 审计', icon: ScrollText, permission: 'agent:audit_view' },
  ] },
  { label: '系统设置', items: [
    { path: '/access-control', label: '用户与权限', icon: ShieldCheck, permission: 'system:user_view' },
    { path: '/models', label: '模型配置', icon: Settings2, permission: 'system:model_manage' },
    { path: '/ai-call-logs', label: '调用日志', icon: RadioTower, permission: 'ai:call_log_view' },
  ] },
].map(group => ({ ...group, items: group.items.filter(item => !item.permission || auth.can(item.permission)) }))
  .filter(group => group.items.length > 0))

const roleLabel = computed(() => {
  if (auth.user?.roles.includes('ADMIN')) return '系统管理员'
  if (auth.user?.roles.includes('OPERATOR')) return '运营人员'
  return auth.user?.roles.join(' / ') || '未分配角色'
})

watch(() => route.fullPath, () => { mobileOpen.value = false })

function logout() {
  auth.logout()
  void router.replace({ name: 'login' })
}
</script>

<template>
  <div class="console" :class="{ 'console--collapsed': collapsed, 'console--mobile-open': mobileOpen }">
    <button v-if="mobileOpen" type="button" class="mobile-scrim" aria-label="关闭导航" @click="mobileOpen = false" />
    <aside class="sidebar" aria-label="主导航">
      <div class="sidebar__brand">
        <router-link to="/dashboard" class="brand" :title="collapsed ? 'SmartMall TLAI' : undefined">
          <span class="brand__mark">S<span>.</span></span>
          <span class="brand__text">SmartMall <strong>TLAI</strong></span>
        </router-link>
        <span class="sidebar__brand-meta">OPERATIONS CONSOLE</span>
      </div>
      <div class="sidebar__divider" />
      <nav class="sidebar__nav">
        <div v-for="group in groups" :key="group.label" class="nav-group">
          <p class="nav-group__label">{{ group.label }}</p>
          <router-link v-for="item in group.items" :key="item.path" :to="item.path" class="nav-link" :title="collapsed ? item.label : undefined" :aria-label="item.label">
            <component :is="item.icon" :size="18" :stroke-width="1.8" class="nav-link__icon" />
            <span class="nav-link__text">{{ item.label }}</span>
            <ChevronRight :size="15" class="nav-link__arrow" />
          </router-link>
        </div>
      </nav>
      <div class="sidebar__bottom">
        <div class="sidebar__profile">
          <span class="avatar">{{ auth.user?.username.slice(0, 1).toUpperCase() }}</span>
          <span class="sidebar__profile-copy"><strong>{{ auth.user?.username }}</strong><small>{{ roleLabel }}</small></span>
          <span class="status-pin" />
        </div>
        <button type="button" class="sidebar__collapse icon-button" :title="collapsed ? '展开导航' : '收起导航'" :aria-label="collapsed ? '展开导航' : '收起导航'" @click="collapsed = !collapsed">
          <PanelLeftOpen v-if="collapsed" :size="18" /><PanelLeftClose v-else :size="18" />
          <span>收起导航</span>
        </button>
      </div>
    </aside>

    <div class="console__main">
      <header class="topbar">
        <div class="topbar__left">
          <button type="button" class="icon-button topbar__menu" aria-label="打开导航" title="打开导航" @click="mobileOpen = true"><Menu :size="21" /></button>
          <div class="breadcrumbs"><span>工作台</span><ChevronRight :size="15" /><strong>{{ route.meta.title || '仪表盘' }}</strong></div>
        </div>
        <div class="topbar__right">
          <span class="topbar__environment"><i class="signal-dot" /> LOCAL ENVIRONMENT</span>
          <span class="topbar__separator" />
          <el-dropdown trigger="click" popper-class="user-menu" @command="logout">
            <button type="button" class="topbar__user" aria-label="用户菜单">
              <span class="avatar avatar--small"><UserRound :size="16" /></span>
              <span>{{ auth.user?.username }}</span><ChevronDown :size="14" />
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>{{ auth.user?.email }}</el-dropdown-item>
                <el-dropdown-item command="logout"><LogOut :size="16" />退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="console__content">
        <router-view v-slot="{ Component }">
          <transition name="content" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </main>
      <footer class="console__footer"><span>SmartMall TLAI · Operations Console</span><span>系统时间 {{ new Date().toLocaleDateString('zh-CN') }}</span></footer>
    </div>
  </div>
</template>
