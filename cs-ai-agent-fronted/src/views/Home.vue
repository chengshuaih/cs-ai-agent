<template>
  <div class="home">
    <div class="hero">
      <div class="container">
        <h1 class="title">CS AI Agent</h1>
        <p class="subtitle">智能AI助手，为您提供专业的服务</p>
      </div>
    </div>
    
    <div class="container">
      <div class="apps-grid">
        <div class="app-card card" @click="navigateTo('/love-app')">
          <div class="app-icon">💕</div>
          <h3>AI 恋爱大师</h3>
          <p>专业的恋爱咨询和建议，帮助您解决情感问题</p>
          <div class="app-features">
            <span class="feature">情感分析</span>
            <span class="feature">关系建议</span>
            <span class="feature">沟通技巧</span>
          </div>
        </div>
        
        <div class="app-card card" @click="navigateTo('/manus-app')">
          <div class="app-icon">🤖</div>
          <h3>AI 超级智能体</h3>
          <p>多功能AI助手，为您提供全方位的智能服务</p>
          <div class="app-features">
            <span class="feature">智能问答</span>
            <span class="feature">任务协助</span>
            <span class="feature">知识咨询</span>
          </div>
        </div>
        
        <div class="app-card card" @click="navigateTo('/debug')">
          <div class="app-icon">🔧</div>
          <h3>调试工具</h3>
          <p>测试后端连接和SSE通信，帮助诊断问题</p>
          <div class="app-features">
            <span class="feature">连接测试</span>
            <span class="feature">SSE调试</span>
            <span class="feature">网络日志</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Home',
  methods: {
    navigateTo(path) {
      this.$router.push(path)
    }
  }
}
</script>

<style scoped>
.home {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.hero {
  padding: 80px 0;
  text-align: center;
  color: white;
}

.title {
  font-size: 3.5rem;
  font-weight: 700;
  margin-bottom: 20px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
}

.subtitle {
  font-size: 1.3rem;
  opacity: 0.9;
  max-width: 600px;
  margin: 0 auto;
}

.apps-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 30px;
  margin-top: 60px;
}

.app-card {
  text-align: center;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.3s ease;
}

.app-card:hover {
  border-color: #667eea;
  transform: translateY(-8px);
}

.app-icon {
  font-size: 4rem;
  margin-bottom: 20px;
}

.app-card h3 {
  font-size: 1.5rem;
  margin-bottom: 15px;
  color: #333;
}

.app-card p {
  color: #666;
  margin-bottom: 25px;
  line-height: 1.6;
}

.app-features {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.feature {
  background: #f0f0f0;
  color: #666;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 0.9rem;
}

@media (max-width: 768px) {
  .title {
    font-size: 2.5rem;
  }
  
  .apps-grid {
    grid-template-columns: 1fr;
    gap: 20px;
    margin-top: 40px;
  }
}
</style> 