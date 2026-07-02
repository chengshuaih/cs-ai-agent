<template>
  <div class="workspace-shell">
    <header class="workspace-header">
      <button class="back-btn" type="button" @click="goHome">← 返回主页</button>
      <div class="header-copy">
        <p class="eyebrow">Dialog Workspace</p>
        <h1>对话助手</h1>
        <p>{{ activeDescription }}</p>
      </div>
    </header>

    <main class="workspace-layout">
      <aside class="workspace-sidebar" aria-label="对话助手功能切换">
        <button
          v-for="item in tabs"
          :key="item.key"
          type="button"
          :class="['tab-button', { active: activeTab === item.key }]"
          @click="activeTab = item.key"
        >
          <span class="tab-title">{{ item.title }}</span>
          <span class="tab-desc">{{ item.description }}</span>
        </button>
      </aside>

      <section class="workspace-content">
        <VisionQaApp v-show="activeTab === 'qa'" ref="qaApp" embedded />
        <VisionAgentApp v-show="activeTab === 'agent'" ref="agentApp" embedded />
      </section>
    </main>
  </div>
</template>

<script>
import VisionQaApp from './VisionQaApp.vue'
import VisionAgentApp from './VisionAgentApp.vue'

export default {
  name: 'DialogWorkspace',
  components: {
    VisionQaApp,
    VisionAgentApp
  },
  data() {
    return {
      activeTab: 'qa',
      tabs: [
        {
          key: 'qa',
          title: '视觉知识问答',
          description: '概念、指标、数据集与视觉知识检索'
        },
        {
          key: 'agent',
          title: '视觉采集智能体',
          description: '通过智能体对话生成采集、实验和报告建议'
        }
      ]
    }
  },
  computed: {
    activeDescription() {
      const current = this.tabs.find(item => item.key === this.activeTab)
      return current ? current.description : '通过侧边栏切换知识问答与智能体对话。'
    }
  },
  methods: {
    goHome() {
      this.$router.push('/')
    }
  }
}
</script>

<style scoped>
.workspace-shell {
  min-height: 100vh;
  background: #f7f7f5;
  color: #1f2328;
}

.workspace-header {
  width: min(100%, 1180px);
  margin: 0 auto;
  padding: 22px 24px 18px;
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: center;
  gap: 18px;
  box-sizing: border-box;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px solid #d9d9e3;
  border-radius: 999px;
  padding: 8px 13px;
  background: #ffffff;
  color: #353740;
  cursor: pointer;
  font-size: 14px;
  white-space: nowrap;
}

.back-btn:hover,
.back-btn:focus-visible {
  background: #f1f1f3;
  outline: none;
}

.header-copy {
  min-width: 0;
}

.eyebrow {
  margin: 0 0 5px;
  color: #6b7280;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.header-copy h1 {
  margin: 0;
  color: #202123;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.header-copy p:last-child {
  margin: 6px 0 0;
  color: #667085;
  font-size: 14px;
  line-height: 1.5;
}

.workspace-layout {
  width: min(100%, 1180px);
  min-height: calc(100vh - 112px);
  margin: 0 auto;
  padding: 0 24px 28px;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 18px;
  box-sizing: border-box;
}

.workspace-sidebar {
  align-self: start;
  position: sticky;
  top: 18px;
  border: 1px solid #e4e4de;
  border-radius: 18px;
  padding: 10px;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
}

.tab-button {
  width: 100%;
  border: 1px solid transparent;
  border-radius: 14px;
  padding: 13px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.tab-button:hover,
.tab-button:focus-visible {
  background: #f7f7f5;
  outline: none;
}

.tab-button.active {
  border-color: #bfe7dc;
  background: #f0fdf7;
}

.tab-title {
  color: #202123;
  font-size: 14px;
  font-weight: 700;
}

.tab-desc {
  color: #6b7280;
  font-size: 12px;
  line-height: 1.45;
}

.workspace-content {
  min-width: 0;
  min-height: 680px;
  overflow: hidden;
  border: 1px solid #e4e4de;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
}

@media (max-width: 860px) {
  .workspace-header {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 18px 16px 14px;
  }

  .back-btn {
    justify-self: start;
  }

  .workspace-layout {
    min-height: auto;
    grid-template-columns: 1fr;
    padding: 0 16px 24px;
  }

  .workspace-sidebar {
    position: static;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .workspace-content {
    min-height: 620px;
  }
}

@media (max-width: 560px) {
  .workspace-sidebar {
    grid-template-columns: 1fr;
  }

  .workspace-content {
    min-height: 580px;
  }
}
</style>
