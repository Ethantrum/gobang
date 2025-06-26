<template>
  <div id="app">
    <GlobalLoading :visible="globalLoading.visible" />
    <GlobalToast :message="globalToast.message" :visible="globalToast.visible" />
    <!-- 路由会根据 URL 在这里渲染对应的组件 -->
    <router-view></router-view>
  </div>
</template>

<script setup>
import { reactive, provide } from 'vue'
import GlobalLoading from '@/components/GlobalLoading.vue'
import GlobalToast from '@/components/GlobalToast.vue'

// 全局 loading 状态
const globalLoading = reactive({ visible: false })
// 提供给全局使用
provide('globalLoading', globalLoading)

// 全局 toast 状态
const globalToast = reactive({ message: '', visible: false, timer: null })
function showGlobalToast(msg, duration = 2000) {
  globalToast.message = msg
  globalToast.visible = true
  if (globalToast.timer) clearTimeout(globalToast.timer)
  globalToast.timer = setTimeout(() => {
    globalToast.visible = false
    globalToast.message = ''
  }, duration)
}
provide('showGlobalToast', showGlobalToast)
</script>