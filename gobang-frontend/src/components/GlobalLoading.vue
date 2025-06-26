<template>
  <div v-if="visible" class="global-loading-mask">
    <div class="spinner"></div>
  </div>
</template>

<script>
export default {
  name: 'GlobalLoading',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  }
}
</script>

<style scoped>
.global-loading-mask {
  position: fixed;
  z-index: 9999;
  left: 0; top: 0; right: 0; bottom: 0;
  background: rgba(100,100,100,0.3);
  display: flex;
  align-items: center;
  justify-content: center;
}
.spinner {
  width: 48px; height: 48px;
  border: 6px solid #eee;
  border-top: 6px solid #888;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}
@keyframes spin { 100% { transform: rotate(360deg); } }
</style> 