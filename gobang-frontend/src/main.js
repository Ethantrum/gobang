// 从 Vue 核心库导入 createApp 函数
// 用于创建 Vue 应用实例（Vue 3 推荐的创建方式）
import { createApp } from 'vue'

// 导入项目的路由配置
// router 是使用 Vue Router 创建的路由实例
import router from './router'

// 导入根组件 App.vue
// 这是整个应用的顶层组件
import App from './App.vue'

// 创建 Vue 应用实例，传入根组件 App
// createApp 返回一个应用实例，可链式调用各种方法
createApp(App)
    // 使用 router 插件，注册路由功能
    // 使应用能够根据 URL 切换不同组件
    .use(router)
    // 将应用挂载到 DOM 中 id 为 app 的元素上
    // 通常对应 index.html 中的 <div id="app"></div>
    .mount('#app')