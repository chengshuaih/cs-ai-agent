<template>
  <div class="home">
    <header class="top-nav">
      <div class="nav-inner">
        <div class="brand">机器视觉智能问答与采集辅助系统</div>
        <div class="nav-right">
          <span v-if="username" class="user-info">{{ username }}</span>
          <button v-if="username" class="logout-btn" type="button" @click="logout">退出</button>
          <button
            class="debug-button"
            type="button"
            aria-label="打开设置与调试"
            title="设置与调试"
            @click="navigateTo('/debug')"
          >⚙</button>
        </div>
      </div>
    </header>

    <main class="main-content">
      <section class="intro">
        <p class="eyebrow">Machine Vision Workspace</p>
        <h1 class="title">面向机器视觉的智能助手</h1>
        <p class="subtitle">围绕视觉学习、数据采集、实验规划与报告生成，提供问答、采集辅助与项目管理能力。</p>
      </section>

      <section class="apps-panel" aria-label="功能入口">
        <button class="app-card main-card" type="button" @click="navigateTo('/dialog-workspace')">
          <span class="app-icon" aria-hidden="true">D</span>
          <span class="app-content">
            <span class="app-title">对话助手</span>
            <span class="app-description">在一个界面切换机器视觉知识问答与采集智能体对话</span>
            <span class="app-features">
              <span class="feature">视觉知识问答</span>
              <span class="feature">视觉采集智能体</span>
              <span class="feature">SSE 对话</span>
            </span>
          </span>
          <span class="app-arrow" aria-hidden="true">→</span>
        </button>

        <button class="app-card main-card" type="button" @click="navigateTo('/collection-workspace')">
          <span class="app-icon" aria-hidden="true">C</span>
          <span class="app-content">
            <span class="app-title">采集工作台</span>
            <span class="app-description">集中处理采集规划、项目归档与报告查看</span>
            <span class="app-features">
              <span class="feature">采集辅助规划</span>
              <span class="feature">项目工作区</span>
              <span class="feature">报告中心</span>
            </span>
          </span>
          <span class="app-arrow" aria-hidden="true">→</span>
        </button>

        <button class="app-card main-card" type="button" @click="navigateTo('/knowledge')">
          <span class="app-icon" aria-hidden="true">K</span>
          <span class="app-content">
            <span class="app-title">知识库管理</span>
            <span class="app-description">上传机器视觉领域文档，支持 RAG 知识检索问答</span>
            <span class="app-features">
              <span class="feature">文档上传</span>
              <span class="feature">知识库检索</span>
              <span class="feature">RAG 增强问答</span>
            </span>
          </span>
          <span class="app-arrow" aria-hidden="true">→</span>
        </button>
      </section>
    </main>
  </div>
</template>

<script>
export default {
  name: 'Home',
  data() {
    return { username: localStorage.getItem('username') || '' }
  },
  methods: {
    navigateTo(path) {
      this.$router.push(path)
    },
    logout() {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
.home {
  min-height: 100vh;
  overflow-x: hidden;
  background: #f7f7f5;
  color: #1f2328;
}

.top-nav {
  position: sticky;
  top: 0;
  z-index: 10;
  border-bottom: 1px solid #e8e8e3;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(16px);
}

.nav-inner {
  width: min(100%, 1080px);
  margin: 0 auto;
  padding: 14px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  box-sizing: border-box;
}

.brand {
  font-size: 1rem;
  font-weight: 650;
  letter-spacing: -0.01em;
  color: #202123;
}

.nav-right { display: flex; align-items: center; gap: 10px; }
.user-info { font-size: 13px; color: #374151; }
.logout-btn { border: 1px solid #d9d9e3; background: #fff; color: #353740; border-radius: 8px; padding: 6px 12px; font-size: 13px; cursor: pointer; }
.logout-btn:hover { background: #f1f1f3; }

.debug-button {
  width: 38px;
  height: 38px;
  border: 1px solid #deded8;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
  color: #4b5563;
  font-size: 1.05rem;
  line-height: 1;
  cursor: pointer;
  transition: background 0.18s ease, border-color 0.18s ease, color 0.18s ease, transform 0.18s ease;
}

.debug-button:hover,
.debug-button:focus-visible {
  border-color: #c9c9c2;
  background: #f1f1ee;
  color: #202123;
  transform: translateY(-1px);
  outline: none;
}

.main-content {
  width: min(100%, 1080px);
  margin: 0 auto;
  padding: 88px 24px 64px;
  box-sizing: border-box;
}

.intro {
  margin-bottom: 34px;
  text-align: center;
}

.eyebrow {
  margin: 0 0 14px;
  color: #6b7280;
  font-size: 0.85rem;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.title {
  margin: 0;
  color: #202123;
  font-size: clamp(2rem, 5vw, 3.15rem);
  font-weight: 650;
  line-height: 1.12;
  letter-spacing: -0.04em;
}

.subtitle {
  max-width: 620px;
  margin: 18px auto 0;
  color: #667085;
  font-size: 1rem;
  line-height: 1.7;
}

.apps-panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  max-width: 960px;
  margin: 0 auto;
}

.app-card {
  width: 100%;
  min-height: 210px;
  border: 1px solid #e4e4de;
  border-radius: 24px;
  padding: 24px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: flex-start;
  gap: 18px;
  background: #ffffff;
  color: inherit;
  text-align: left;
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease, background 0.18s ease;
}

.app-card:hover,
.app-card:focus-visible {
  border-color: #cfcfca;
  background: #fffefa;
  box-shadow: 0 18px 38px rgba(16, 24, 40, 0.1);
  transform: translateY(-3px);
  outline: none;
}

.app-icon {
  width: 52px;
  height: 52px;
  border-radius: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: #f1f5f3;
  color: #0f766e;
  font-size: 1.45rem;
  font-weight: 700;
}

.app-content {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.app-title {
  color: #202123;
  font-size: 1.24rem;
  font-weight: 700;
  line-height: 1.35;
}

.app-description {
  color: #6b7280;
  font-size: 0.96rem;
  line-height: 1.6;
}

.app-features {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 4px;
}

.feature {
  border: 1px solid #ecece7;
  border-radius: 999px;
  padding: 5px 10px;
  background: #fafaf8;
  color: #6b7280;
  font-size: 0.78rem;
  line-height: 1.2;
  white-space: nowrap;
}

.app-arrow {
  color: #9ca3af;
  font-size: 1.2rem;
  transition: color 0.18s ease, transform 0.18s ease;
}

.app-card:hover .app-arrow,
.app-card:focus-visible .app-arrow {
  color: #202123;
  transform: translateX(2px);
}

@media (max-width: 768px) {
  .nav-inner {
    padding: 12px 16px;
  }

  .main-content {
    padding: 56px 16px 40px;
  }

  .intro {
    text-align: left;
  }

  .subtitle {
    margin-left: 0;
    margin-right: 0;
  }

  .apps-panel {
    grid-template-columns: 1fr;
  }

  .app-card {
    min-height: 0;
    grid-template-columns: auto 1fr;
    gap: 13px;
    padding: 18px;
  }

  .app-arrow {
    display: none;
  }
}

@media (max-width: 420px) {
  .title {
    font-size: 1.85rem;
  }

  .app-card {
    align-items: flex-start;
  }

  .app-icon {
    width: 42px;
    height: 42px;
    border-radius: 14px;
    font-size: 1.2rem;
  }

  .feature {
    white-space: normal;
  }
}
</style>
