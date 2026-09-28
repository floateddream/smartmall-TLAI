import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { setUnauthorizedHandler } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'
import App from './App.vue'
import './styles/main.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)
app.use(ElementPlus)

setUnauthorizedHandler(() => {
  const auth = useAuthStore(pinia)
  auth.logout()
  // 首次导航尚未完成时由路由守卫负责跳转，避免与 401 处理重复导航。
  if (router.currentRoute.value.name && router.currentRoute.value.name !== 'login' && router.currentRoute.value.name !== 'register') {
    void router.replace({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
  }
})

app.mount('#app')
