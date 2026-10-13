import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { decideRouteAccess } from '@/utils/routePolicy'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // 对外统一使用固定登录入口，避免根地址或历史地址长期停留在非标准 URL。
    { path: '/', redirect: '/web/toLogin' },
    { path: '/web/toLogin', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { guest: true } },
    { path: '/login', redirect: to => ({ name: 'login', query: to.query, hash: to.hash }) },
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
        { path: 'knowledge', name: 'knowledge', component: () => import('@/views/KnowledgeView.vue'), meta: { title: '知识库', section: '智能工作台', permission: 'knowledge:view' } },
        { path: 'data-analysis', name: 'data-analysis', component: () => import('@/views/AnalyticsView.vue'), meta: { title: '数据分析', section: '智能工作台', permission: 'analytics:view' } },
        { path: 'agent-audits', name: 'agent-audits', component: () => import('@/views/AgentAuditView.vue'), meta: { title: 'Agent 审计', section: '智能工作台', permission: 'agent:audit_view' } },
        { path: 'access-control', name: 'access-control', component: () => import('@/views/AccessControlView.vue'), meta: { title: '用户与权限', section: '系统设置', permission: 'system:user_view' } },
        { path: 'models', name: 'models', component: () => import('@/views/ModelsView.vue'), meta: { title: '模型配置', section: '系统设置', permission: 'system:model_manage' } },
        { path: 'ai-call-logs', name: 'ai-call-logs', component: () => import('@/views/AiCallLogView.vue'), meta: { title: '调用日志', section: '系统设置', permission: 'ai:call_log_view' } },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/web/toLogin' },
  ],
})

// 前端守卫用于导航体验；真正的权限仍必须由后端接口校验。
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  const authenticated = await auth.restore()
  return decideRouteAccess(to.meta, to.fullPath, authenticated, permission => auth.can(permission))
})

export default router
