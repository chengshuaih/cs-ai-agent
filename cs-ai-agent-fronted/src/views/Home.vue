<template>
  <div class="home">
    <header class="top-nav">
      <div class="nav-inner">
        <div class="brand">CS AI Agent</div>
        <button
          class="debug-button"
          type="button"
          aria-label="打开调试工具"
          title="调试工具"
          @click="navigateTo('/debug')"
        >
          ⚙
        </button>
      </div>
    </header>

    <main class="main-content">
      <section class="intro">
        <p class="eyebrow">AI Workspace</p>
        <h1 class="title">今天想让 AI 帮你做什么？</h1>
        <p class="subtitle">选择一个智能体开始对话，获得简洁、专注的 AI 助手体验。</p>
      </section>

      <section class="apps-panel" aria-label="应用入口">
        <button class="app-card" type="button" @click="navigateTo('/love-app')">
          <span class="app-icon" aria-hidden="true">💕</span>
          <span class="app-content">
            <span class="app-title">AI 恋爱大师</span>
            <span class="app-description">专业的恋爱咨询和建议，帮助您解决情感问题</span>
            <span class="app-features">
              <span class="feature">情感分析</span>
              <span class="feature">关系建议</span>
              <span class="feature">沟通技巧</span>
            </span>
          </span>
          <span class="app-arrow" aria-hidden="true">→</span>
        </button>

        <button class="app-card" type="button" @click="navigateTo('/manus-app')">
          <span class="app-icon" aria-hidden="true">🤖</span>
          <span class="app-content">
            <span class="app-title">AI 超级智能体</span>
            <span class="app-description">多功能AI助手，为您提供全方位的智能服务</span>
            <span class="app-features">
              <span class="feature">智能问答</span>
              <span class="feature">任务协助</span>
              <span class="feature">知识咨询</span>
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
  width: min(100%, 860px);
  margin: 0 auto;
  padding: 88px 24px 64px;
  box-sizing: border-box;
}

.intro {
  margin-bottom: 28px;
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
  max-width: 560px;
  margin: 18px auto 0;
  color: #667085;
  font-size: 1rem;
  line-height: 1.7;
}

.apps-panel {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
  max-width: 680px;
  margin: 0 auto;
}

.app-card {
  width: 100%;
  border: 1px solid #e4e4de;
  border-radius: 18px;
  padding: 18px 20px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 16px;
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
  box-shadow: 0 12px 28px rgba(16, 24, 40, 0.08);
  transform: translateY(-2px);
  outline: none;
}

.app-icon {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: #f6f6f3;
  font-size: 1.35rem;
}

.app-content {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.app-title {
  color: #202123;
  font-size: 1.05rem;
  font-weight: 650;
  line-height: 1.35;
}

.app-description {
  color: #6b7280;
  font-size: 0.92rem;
  line-height: 1.55;
}

.app-features {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 2px;
}

.feature {
  border: 1px solid #ecece7;
  border-radius: 999px;
  padding: 4px 9px;
  background: #fafaf8;
  color: #6b7280;
  font-size: 0.78rem;
  line-height: 1.2;
  white-space: nowrap;
}

.app-arrow {
  color: #9ca3af;
  font-size: 1.1rem;
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

  .app-card {
    grid-template-columns: auto 1fr;
    gap: 13px;
    padding: 16px;
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
    width: 38px;
    height: 38px;
    border-radius: 12px;
    font-size: 1.2rem;
  }

  .feature {
    white-space: normal;
  }
}
</style>
