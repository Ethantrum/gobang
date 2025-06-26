<template>
  <div v-if="visible" class="global-toast">{{ message }}</div>
</template>

<script>
export default {
  name: 'GlobalToast',
  props: {
    message: String,
    visible: Boolean
  }
}
</script>

<style scoped>
.global-toast {
  position: fixed;
  top: 40px;
  left: 50%;
  transform: translateX(-50%);
  background: #323232;
  color: #fff;
  padding: 12px 32px;
  border-radius: 6px;
  font-size: 16px;
  z-index: 9999;
  opacity: 0.95;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  transition: opacity 0.3s;
}
</style> 