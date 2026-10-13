<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  BarChart3, Boxes, BrainCircuit, ChevronDown, ChevronRight,
  Database, LayoutDashboard, LogOut, Menu, PanelLeftClose, PanelLeftOpen, RadioTower, ScrollText,
  RotateCcw, Settings2, ShieldCheck, ShoppingBag, Ticket, UserRound, UsersRound,
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import AmbientDataFlow from '@/components/AmbientDataFlow.vue'
import RouteTransitionLayer, { type RouteMotionTone } from '@/components/RouteTransitionLayer.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)
const mobileOpen = ref(false)
const routeMotionSequence = ref(0)
const routeMotionActive = ref(false)
const routeMotionDirection = ref<'forward' | 'backward'>('forward')
const routeMotionOrigin = ref({ x: 246, y: 150 })
const routeMotionTone = ref<RouteMotionTone>('aurora')
const sidebar = ref<HTMLElement>()
const sidebarNav = ref<HTMLElement>()
const activePlateReady = ref(false)
const activePlateTop = ref(0)
const activePlateHeight = ref(41)
let routeMotionTimer = 0
let preparedMotionPath = ''
let sidebarTiltFrame = 0
let sidebarRotateX = 0
let sidebarRotateY = 0
let sidebarTargetX = 0
let sidebarTargetY = 0

const routeOrder = [
  '/dashboard', '/products', '/customers', '/orders', '/refunds', '/tickets',
  '/assistant', '/knowledge', '/data-analysis', '/agent-audits',
  '/models', '/access-control', '/system-models', '/ai-call-logs',
]

const activeMenuPath = computed(() => routeOrder.find(path => route.path === path || route.path.startsWith(`${path}/`)) ?? route.path)

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
    { path: '/models', label: '我的模型', icon: Settings2, permission: 'ai:chat' },
    { path: '/knowledge', label: '知识库', icon: Database, permission: 'knowledge:view' },
    { path: '/data-analysis', label: '数据分析', icon: BarChart3, permission: 'analytics:view' },
    { path: '/agent-audits', label: 'Agent 审计', icon: ScrollText, permission: 'agent:audit_view' },
  ] },
  { label: '系统设置', items: [
    { path: '/access-control', label: '用户与权限', icon: ShieldCheck, permission: 'system:user_view' },
    { path: '/system-models', label: '系统模型', icon: Settings2, permission: 'system:model_manage' },
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

watch(() => route.path, (path, previousPath) => {
  const currentPath = routeOrder.find(item => path === item || path.startsWith(`${item}/`)) ?? path
  const oldPath = routeOrder.find(item => previousPath === item || previousPath.startsWith(`${item}/`)) ?? previousPath
  const currentIndex = routeOrder.indexOf(currentPath)
  const previousIndex = routeOrder.indexOf(oldPath)
  routeMotionDirection.value = currentIndex >= previousIndex ? 'forward' : 'backward'
  if (preparedMotionPath !== path) {
    routeMotionOrigin.value = { x: collapsed.value ? 72 : 246, y: 108 }
  }
  preparedMotionPath = ''
  routeMotionTone.value = toneForPath(path)
  routeMotionSequence.value += 1
  routeMotionActive.value = true
  window.clearTimeout(routeMotionTimer)
  routeMotionTimer = window.setTimeout(() => { routeMotionActive.value = false }, 620)
  void updateActivePlate()
})

function toneForPath(path: string): RouteMotionTone {
  if (path.startsWith('/assistant') || path.startsWith('/models') || path.startsWith('/knowledge') || path.startsWith('/data-analysis') || path.startsWith('/agent-audits')) return 'purple'
  if (path.startsWith('/refunds') || path.startsWith('/tickets')) return 'orange'
  return 'aurora'
}

function prepareRouteMotion(event: MouseEvent, path: string) {
  if (path === route.path) return
  const bounds = (event.currentTarget as HTMLElement).getBoundingClientRect()
  routeMotionOrigin.value = { x: bounds.right, y: bounds.top + bounds.height / 2 }
  routeMotionTone.value = toneForPath(path)
  const currentIndex = routeOrder.indexOf(activeMenuPath.value)
  const targetIndex = routeOrder.indexOf(path)
  routeMotionDirection.value = targetIndex >= currentIndex ? 'forward' : 'backward'
  routeMotionActive.value = false
  requestAnimationFrame(() => { routeMotionActive.value = true })
  preparedMotionPath = path
}

async function updateActivePlate() {
  await nextTick()
  const nav = sidebarNav.value
  const active = nav?.querySelector<HTMLElement>(`[data-route="${activeMenuPath.value}"]`)
  if (!active || !nav) return
  let top = 0
  let current: HTMLElement | null = active
  while (current && current !== nav) {
    top += current.offsetTop
    current = current.offsetParent as HTMLElement | null
  }
  if (current !== nav) {
    top = active.getBoundingClientRect().top - nav.getBoundingClientRect().top + nav.scrollTop
  }
  activePlateTop.value = top
  activePlateHeight.value = active.offsetHeight
  if (!activePlateReady.value) requestAnimationFrame(() => { activePlateReady.value = true })
}

function animateSidebarTilt() {
  sidebarTiltFrame = 0
  sidebarRotateX += (sidebarTargetX - sidebarRotateX) * .14
  sidebarRotateY += (sidebarTargetY - sidebarRotateY) * .14
  sidebar.value?.style.setProperty('--sidebar-rx', `${sidebarRotateX.toFixed(3)}deg`)
  sidebar.value?.style.setProperty('--sidebar-ry', `${sidebarRotateY.toFixed(3)}deg`)
  if (Math.abs(sidebarTargetX - sidebarRotateX) > .015 || Math.abs(sidebarTargetY - sidebarRotateY) > .015) {
    sidebarTiltFrame = requestAnimationFrame(animateSidebarTilt)
  }
}

function requestSidebarTilt() {
  if (!sidebarTiltFrame) sidebarTiltFrame = requestAnimationFrame(animateSidebarTilt)
}

function handleSidebarPointerMove(event: PointerEvent) {
  if (window.innerWidth <= 800) return
  const element = sidebar.value
  if (!element) return
  const bounds = element.getBoundingClientRect()
  const horizontal = (event.clientX - bounds.left) / bounds.width - .5
  const vertical = (event.clientY - bounds.top) / bounds.height - .5
  sidebarTargetX = vertical * -.75
  sidebarTargetY = horizontal * 1.5
  requestSidebarTilt()

  const link = (event.target as HTMLElement).closest<HTMLElement>('.nav-link')
  if (link) {
    const linkBounds = link.getBoundingClientRect()
    link.style.setProperty('--nav-light-x', `${event.clientX - linkBounds.left}px`)
  }
}

function resetSidebarTilt() {
  sidebarTargetX = 0
  sidebarTargetY = 0
  requestSidebarTilt()
}

function logout() {
  auth.logout()
  void router.replace({ name: 'login' })
}

onMounted(() => {
  void updateActivePlate()
  window.addEventListener('resize', updateActivePlate, { passive: true })
})

onBeforeUnmount(() => {
  window.clearTimeout(routeMotionTimer)
  if (sidebarTiltFrame) cancelAnimationFrame(sidebarTiltFrame)
  window.removeEventListener('resize', updateActivePlate)
})
</script>

<template>
  <div class="console" :class="{ 'console--collapsed': collapsed, 'console--mobile-open': mobileOpen, 'console--route-motion': routeMotionActive, 'console--route-backward': routeMotionDirection === 'backward' }">
    <button v-if="mobileOpen" type="button" class="mobile-scrim" aria-label="关闭导航" @click="mobileOpen = false" />
    <aside ref="sidebar" class="sidebar" aria-label="主导航" @pointermove="handleSidebarPointerMove" @pointerleave="resetSidebarTilt">
      <div class="sidebar__brand">
        <router-link to="/dashboard" class="brand" :title="collapsed ? 'SmartMall TLAI' : undefined">
          <span class="brand__mark">S<span>.</span></span>
          <span class="brand__text">SmartMall <strong>TLAI</strong></span>
        </router-link>
        <span class="sidebar__brand-meta">OPERATIONS CONSOLE</span>
      </div>
      <div class="sidebar__divider" />
      <nav ref="sidebarNav" class="sidebar__nav">
        <span
          class="nav-active-plate"
          :class="[`nav-active-plate--${routeMotionTone}`, { 'nav-active-plate--ready': activePlateReady }]"
          :style="{ height: `${activePlateHeight}px`, transform: `translate3d(0, ${activePlateTop}px, 12px)` }"
          aria-hidden="true"
        />
        <div v-for="group in groups" :key="group.label" class="nav-group">
          <p class="nav-group__label">{{ group.label }}</p>
          <router-link v-for="item in group.items" :key="item.path" :to="item.path" class="nav-link" :data-route="item.path" :title="collapsed ? item.label : undefined" :aria-label="item.label" @click="prepareRouteMotion($event, item.path)">
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
      <AmbientDataFlow :sequence="routeMotionSequence" :tone="routeMotionTone" />
      <RouteTransitionLayer :sequence="routeMotionSequence" :origin="routeMotionOrigin" :tone="routeMotionTone" />
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
          <transition name="route-data">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </main>
      <footer class="console__footer"><span>SmartMall TLAI · Operations Console</span><span>系统时间 {{ new Date().toLocaleDateString('zh-CN') }}</span></footer>
    </div>
  </div>
</template>
