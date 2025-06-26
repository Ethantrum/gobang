// 导入 Vue CLI 提供的 defineConfig 工具函数
// 用于类型检查和智能提示，帮助编写更规范的配置文件
const { defineConfig } = require('@vue/cli-service')

// 导入 webpack 模块，以便使用其 DefinePlugin
// DefinePlugin 用于在编译时创建全局常量，替换代码中的变量
const webpack = require('webpack')

// 使用 defineConfig 函数包裹配置对象，提供类型提示
module.exports = defineConfig({

  //设置启动端口为8082
  devServer:{
    port:8082
  },

  // 配置 Babel 转译选项
  // 设置为 true 会强制 Babel 转译所有依赖包
  // 适用于依赖中包含需要兼容的 ESNext 语法时
  transpileDependencies: true,

  // 扩展 webpack 配置的简便方式
  // 直接添加 webpack 插件或修改现有配置
  configureWebpack: {
    plugins: [
      // DefinePlugin 是 webpack 的内置插件
      // 用于在编译时将代码中的特定变量替换为指定值
      new webpack.DefinePlugin({
        // Vue 3 特性标志：控制生产环境中水合不匹配的详细信息
        // 设置为 false 可减少生产包体积，避免暴露敏感信息
        __VUE_PROD_HYDRATION_MISMATCH_DETAILS__: false,

        // 是否保留对 Vue 2 风格 Options API 的支持
        // 如果项目完全使用 Composition API 可设为 false
        // 设为 true 时会包含 Options API 相关代码
        __VUE_OPTIONS_API__: true,

        // 是否在生产环境中启用 Vue DevTools 支持
        // 生产环境建议禁用，避免性能开销和潜在安全风险
        __VUE_PROD_DEVTOOLS__: false,
      }),
    ],
  },
})