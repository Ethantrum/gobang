import { ref } from 'vue'

export function useToast() {
  const message = ref('')
  const visible = ref(false)
  let timer = null

  const showToast = (msg, duration = 2000) => {
    message.value = msg
    visible.value = true
    
    if (timer) {
      clearTimeout(timer)
    }
    
    timer = setTimeout(() => {
      visible.value = false
      message.value = ''
    }, duration)
  }

  const hideToast = () => {
    visible.value = false
    message.value = ''
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
  }

  return {
    message,
    visible,
    showToast,
    hideToast
  }
} 