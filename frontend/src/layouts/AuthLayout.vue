<script setup lang="ts">
import AuthScene from '@/components/AuthScene.vue'
</script>

<template>
  <main class="auth-layout">
    <AuthScene />
    <div class="auth-layout__content">
      <div class="auth-intro">
        <router-link :to="{ name: 'login' }" class="brand brand--auth" aria-label="SmartMall TLAI 登录">
          <span class="brand__mark">S<span>.</span></span>
          <span>SmartMall <strong>TLAI</strong></span>
        </router-link>
        <div class="auth-intro__copy">
          <p class="eyebrow"><span class="eyebrow__line" /> COMMERCE INTELLIGENCE</p>
          <h1>让每一步运营<br /><em>更有依据。</em></h1>
          <p>连接业务、数据与智能决策。</p>
          <div class="auth-intro__signals">
            <span><i class="signal-dot" /> BUSINESS ONLINE</span>
            <span>01 / OPERATIONS CONSOLE</span>
          </div>
        </div>
      </div>
      <slot />
    </div>
    <span class="auth-layout__footer">© 2026 SmartMall TLAI</span>
  </main>
</template>
