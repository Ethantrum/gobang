<template>
  <div class="register-container">
    <h2>用户登录</h2>
    <form @submit.prevent="handleLogin">
      <div>
        <label>用户名：</label>
        <input v-model="form.username" placeholder="请输入用户名" required />
      </div>
      <div>
        <label>密码：</label>
        <input v-model="form.password" type="password" placeholder="请输入密码" required />
      </div>
      <button type="submit" :disabled="loading">
        <span v-if="loading">登录中...</span>
        <span v-else>登录</span>
      </button>
    </form>
    <router-link :to="{name: 'Register'}">注册跳转</router-link>
    <div v-if="toastMsg" class="toast-message">{{ toastMsg }}</div>
  </div>
</template>

<script>
import { userLogin } from '@/services/userService';
import { inject } from 'vue';

export default {
  name: "UserLogin",
  data() {
    return {
      form: {
        username: "",
        password: ""
      },
      loading: false,
      toastMsg: ''
    };
  },
  mounted() {
    this.globalLoading = inject('globalLoading');
    this.showGlobalToast = inject('showGlobalToast');
  },
  methods: {
    showToast(msg, duration = 2000) {
      if (this.showGlobalToast) {
        this.showGlobalToast(msg, duration);
      } else {
        this.toastMsg = msg;
        clearTimeout(this.toastTimer);
        this.toastTimer = setTimeout(() => {
          this.toastMsg = '';
        }, duration);
      }
    },
    async handleLogin() {
      // 开发环境建议开启，测试可注释以下严格校验
      /*
      const usernameReg = /^[a-zA-Z0-9_]{4,16}$/;
      const passwordReg = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,20}$/;
      if (!usernameReg.test(this.form.username)) {
        this.showToast('用户名需4-16位字母、数字或下划线');
        return;
      }
      if (!passwordReg.test(this.form.password)) {
        this.showToast('密码需6-20位且包含字母和数字');
        return;
      }
      */
      if (!this.form.username || !this.form.password) {
        this.showToast('请填写用户名和密码');
        return;
      }
      this.loading = true;
      if (this.globalLoading) this.globalLoading.visible = true;
      try {
        const response = await userLogin(this.form);
        if (response.data && response.data.token) {
          localStorage.setItem('token', response.data.token);
          localStorage.setItem('userId', response.data.userId);
          this.showToast('登录成功');
          setTimeout(() => {
            this.$router.replace({name: 'GameHall'});
          }, 800);
        } else {
          this.showToast('登录失败，未获取到 token');
        }
      } catch (error) {
        this.showToast('登录失败，请检查用户名和密码');
      } finally {
        this.loading = false;
        if (this.globalLoading) this.globalLoading.visible = false;
      }
    }
  },
  beforeUnmount() {
    clearTimeout(this.toastTimer);
  }
};
</script>

<style scoped>
.register-container {
  max-width: 400px;
  margin: 40px auto;
  padding: 24px;
  border: 1px solid #eee;
  border-radius: 8px;
  background: #fafbfc;
}
.register-container h2 {
  text-align: center;
  margin-bottom: 20px;
}
.register-container form > div {
  margin-bottom: 16px;
}
.register-container label {
  display: inline-block;
  width: 70px;
}
.register-container input {
  width: 250px;
  padding: 6px 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
}
.register-container button {
  width: 100%;
  padding: 8px 0;
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
}
.toast-message {
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