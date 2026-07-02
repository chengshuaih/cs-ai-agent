<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="brand">机器视觉智能问答与采集辅助系统</h1>
      <div class="tabs">
        <button :class="['tab', { active: mode === 'login' }]" @click="mode = 'login'">登录</button>
        <button :class="['tab', { active: mode === 'register' }]" @click="mode = 'register'">注册</button>
      </div>
      <form @submit.prevent="submit">
        <div class="field">
          <label>用户名</label>
          <input v-model="username" type="text" placeholder="请输入用户名" autocomplete="username" />
        </div>
        <div class="field">
          <label>密码</label>
          <input v-model="password" type="password" placeholder="请输入密码" autocomplete="current-password" />
        </div>
        <p v-if="errMsg" class="err">{{ errMsg }}</p>
        <button class="submit-btn" type="submit" :disabled="loading">
          {{ loading ? '请稍候...' : (mode === 'login' ? '登录' : '注册') }}
        </button>
      </form>
    </div>
  </div>
</template>

<script>
import api from '../utils/api'

export default {
  name: 'LoginPage',
  data() {
    return { mode: 'login', username: '', password: '', errMsg: '', loading: false }
  },
  methods: {
    async submit() {
      if (!this.username.trim() || !this.password.trim()) {
        this.errMsg = '用户名和密码不能为空'
        return
      }
      this.loading = true
      this.errMsg = ''
      try {
        const url = this.mode === 'login' ? '/auth/login' : '/auth/register'
        const res = await api.post(url, { username: this.username, password: this.password })
        if (res.data && res.data.code === 0) {
          if (this.mode === 'login') {
            localStorage.setItem('token', res.data.data.token)
            localStorage.setItem('username', res.data.data.username)
            this.$router.push('/')
          } else {
            this.errMsg = '注册成功，请登录'
            this.mode = 'login'
          }
        } else {
          this.errMsg = (res.data && res.data.message) || '操作失败'
        }
      } catch (e) {
        this.errMsg = '请求失败：' + (e.message || e)
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.login-page { min-height: 100vh; background: #f7f7f5; display: flex; align-items: center; justify-content: center; }
.login-card { background: #fff; border: 1px solid #e4e4de; border-radius: 16px; padding: 36px 40px; width: 360px; }
.brand { font-size: 15px; font-weight: 600; color: #1f2328; margin: 0 0 24px; text-align: center; line-height: 1.4; }
.tabs { display: flex; gap: 0; margin-bottom: 24px; border: 1px solid #e4e4de; border-radius: 8px; overflow: hidden; }
.tab { flex: 1; padding: 9px; background: #fafaf8; border: none; cursor: pointer; font-size: 14px; color: #6b7280; }
.tab.active { background: #10a37f; color: #fff; font-weight: 600; }
.field { margin-bottom: 16px; display: flex; flex-direction: column; gap: 6px; }
label { font-size: 13px; color: #6b7280; }
input { border: 1px solid #d9d9e3; border-radius: 8px; padding: 9px 12px; font-size: 14px; outline: none; }
input:focus { border-color: #10a37f; }
.submit-btn { width: 100%; border: none; border-radius: 10px; background: #10a37f; color: #fff; padding: 10px; font-size: 14px; font-weight: 600; cursor: pointer; margin-top: 4px; }
.submit-btn:disabled { background: #d9d9e3; cursor: not-allowed; }
.err { color: #c0392b; font-size: 13px; margin: -8px 0 8px; }
</style>
