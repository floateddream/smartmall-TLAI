import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/dashboard' },
    { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { guest: true } },
    { path: '/register', name: 'register', component: () => import('@/views/RegisterView.vue'), meta: { guest: true } },
    {
      path: '/',
      component: () => import('@/layouts/ConsoleLayout.vue'),
      children: [
        { path: 'dashboard', name: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { title: '仪表盘', permission: 'dashboard:view' } },
        { path: 'products', name: 'products', component: () => import('@/views/ProductsView.vue'), meta: { title: '商品管理', section: '业务运营', permission: 'product:view' } },
        { path: 'customers', name: 'customers', component: () => import('@/views/CustomersView.vue'), meta: { title: '客户管理', section: '业务运营', permission: 'customer:view' } },
        { path: 'orders', name: 'orders', component: () => import('@/views/OrdersView.vue'), meta: { title: '订单管理', section: '业务运营', permission: 'order:view' } },
        { path: 'refunds', name: 'refunds', component: () => import('@/views/RefundsView.vue'), meta: { title: '退款管理', section: '业务运营', permission: 'refund:view' } },
        { path: 'tickets', name: 'tickets', component: () => import('@/views/TicketsView.vue'), meta: { title: '售后工单', section: '业务运营', permission: 'ticket:view' } },
        { path: 'assistant', name: 'assistant', component: () => import('@/views/AssistantView.vue'), meta: { title: 'AI 助手', section: '智能工作台', permission: 'ai:chat' } },
        { path: 'knowledge', name: 'knowledge', component: () => import('@/views/ModuleView.vue'), meta: { title: '知识库', section: '智能工作台' } },
        { path: 'access-control', name: 'access-control', component: () => import('@/views/AccessControlView.vue'), meta: { title: '用户与权限', section: '系统设置', permission: 'system:user_view' } },
        { path: 'models', name: 'models', component: () => import('@/views/ModelsView.vue'), meta: { title: '模型配置', section: '系统设置', permission: 'system:model_manage' } },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
})

// 前端守卫用于导航体验；真正的权限仍必须由后端接口校验。
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  const authenticated = await auth.restore()
  if (to.meta.guest) return authenticated ? { name: 'dashboard' } : true
  if (!authenticated) return { name: 'login', query: { redirect: to.fullPath } }
  if (typeof to.meta.permission === 'string' && !auth.can(to.meta.permission)) return { name: 'dashboard' }
  return true
})

export default router
