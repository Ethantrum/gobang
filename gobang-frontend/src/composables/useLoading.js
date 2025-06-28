import { ref, reactive } from 'vue'

export function useLoading() {
  // 创建单个loading状态
  const createLoading = () => ref(false)
  
  // 创建多个loading状态管理（用于房间列表等）
  const createLoadingMap = () => reactive({})
  
  // 包装异步函数，自动管理单个loading状态
  const withLoading = (loadingRef, asyncFn) => {
    return async (...args) => {
      loadingRef.value = true
      try {
        return await asyncFn(...args)
      } finally {
        loadingRef.value = false
      }
    }
  }
  
  // 包装异步函数，自动管理loadingMap状态
  const withLoadingMap = (loadingMap, asyncFn) => {
    return async (key, ...args) => {
      loadingMap[key] = true
      try {
        return await asyncFn(key, ...args)
      } finally {
        loadingMap[key] = false
      }
    }
  }
  
  return {
    createLoading,
    createLoadingMap,
    withLoading,
    withLoadingMap
  }
} 