<template>
  <div class="register-container">
    <h2>用户注册</h2>
    <form @submit.prevent="handleRegister">
      <div>
        <label>用户名：</label>
        <input v-model="form.username" placeholder="请输入用户名" required />
      </div>
      <div>
        <label>昵称：</label>
        <input v-model="form.nickname" placeholder="请输入昵称" required />
      </div>
      <div>
        <label>密码：</label>
        <input v-model="form.password" type="password" placeholder="请输入密码" required />
      </div>
      <div>
        <label>确认密码：</label>
        <input v-model="confirmPassword" type="password" placeholder="请再次输入密码" required />
      </div>
      <div>
        <label>邮箱：</label>
        <input v-model="form.email" type="email" placeholder="请输入邮箱" required />
      </div>
      <button type="submit" :disabled="loading">
        <span v-if="loading">注册中...</span>
        <span v-else>注册</span>
      </button>
    </form>
    <router-link :to="{name: 'Login'}">登录跳转</router-link>
    <div v-if="toastMsg" class="toast-message">{{ toastMsg }}</div>
  </div>
</template>

<script>
import { userRegister } from '@/services/userService';
import { inject } from 'vue';

export default {
  name: "UserRegister",
  data() {
    return {
      form: {
        username: "",
        nickname: "",
        password: "",
        email: ""
      },
      confirmPassword: "",
      loading: false,
      toastMsg: ''
    };
  },
  mounted() {
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
    async handleRegister() {
      const usernameReg = /^[a-zA-Z0-9_]{4,16}$/;
      const nicknameReg = /^[\u4e00-\u9fa5a-zA-Z0-9_]{2,16}$/;
      const passwordReg = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,20}$/;
      const emailReg = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
      if (!this.form.username || !this.form.nickname || !this.form.password || !this.form.email) {
        this.showToast('请填写所有信息');
        return;
      }
      if (!usernameReg.test(this.form.username)) {
        this.showToast('用户名需4-16位字母、数字或下划线');
        return;
      }
      if (!nicknameReg.test(this.form.nickname)) {
        this.showToast('昵称需2-16位中英文、数字或下划线');
        return;
      }
      if (!passwordReg.test(this.form.password)) {
        this.showToast('密码需6-20位且包含字母和数字');
        return;
      }
      if (this.form.password !== this.confirmPassword) {
        this.showToast('两次输入的密码不一致');
        return;
      }
      if (!emailReg.test(this.form.email)) {
        this.showToast('邮箱格式不正确');
        return;
      }
      this.loading = true;
      try {
        const data = await userRegister(this.form);
        if (data && data.success !== false) {
          this.showToast('注册成功，请登录！');
          setTimeout(() => {
            this.$router.replace({name: 'Login'});
          }, 1000);
        } else {
          this.showToast(data.message || '注册失败');
        }
      } catch (error) {
        this.showToast('注册失败，请检查信息');
      } finally {
        this.loading = false;
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