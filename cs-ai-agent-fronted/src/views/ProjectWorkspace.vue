<template>
  <div :class="['workspace', { embedded }]">
    <div v-if="!embedded" class="header">
      <div class="container header-content">
        <button class="back-btn" @click="goBack"><span>←</span> 返回主页</button>
        <h1>采集项目工作区</h1>
        <span class="spacer"></span>
      </div>
    </div>

    <div class="container body">
      <section class="create-card">
        <h2>新建采集项目</h2>
        <div class="field">
          <label>项目名称</label>
          <input v-model="newProject.name" placeholder="如：城市道路目标检测采集" />
        </div>
        <div class="field">
          <label>项目描述</label>
          <input v-model="newProject.description" placeholder="一句话描述项目目标" />
        </div>
        <button class="primary-btn" :disabled="creating" @click="createProject">
          {{ creating ? '创建中...' : '创建项目' }}
        </button>
        <p v-if="createMsg" class="msg">{{ createMsg }}</p>
      </section>

      <section class="list-card">
        <div class="list-head">
          <h2>项目列表</h2>
          <button class="ghost-btn" @click="loadProjects">刷新</button>
        </div>
        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="!loading && projects.length === 0" class="empty">暂无项目。</p>
        <div
          class="project-item"
          v-for="(p, i) in projects"
          :key="i"
          @click="openDetail(p.id)"
        >
          <div class="project-name">{{ p.name }}</div>
          <div class="project-desc">{{ p.description || '无描述' }}</div>
          <div class="project-meta">
            采集 {{ (p.collectionPlanIds || []).length }} ·
            实验 {{ (p.experimentPlanIds || []).length }} ·
            报告 {{ (p.reportIds || []).length }}
          </div>
        </div>
      </section>

      <section class="detail-card" v-if="detail">
        <div class="detail-head">
          <h2>{{ detail.project.name }}</h2>
          <button
            class="ghost-btn"
            :disabled="reportLoading"
            @click="generateReport(detail.project.id)"
          >{{ reportLoading ? '生成中...' : '生成报告' }}</button>
        </div>
        <p v-if="reportMsg" :class="reportMsgClass">{{ reportMsg }}</p>
        <p class="detail-desc">{{ detail.project.description }}</p>

        <h3>采集计划（{{ detail.collectionPlans.length }}）</h3>
        <ul v-if="detail.collectionPlans.length">
          <li v-for="(c, ci) in detail.collectionPlans" :key="ci">{{ c.title }}</li>
        </ul>
        <p v-else class="empty">无</p>

        <h3>实验计划（{{ detail.experimentPlans.length }}）</h3>
        <ul v-if="detail.experimentPlans.length">
          <li v-for="(e, ei) in detail.experimentPlans" :key="ei">{{ e.title || e.taskType }}</li>
        </ul>
        <p v-else class="empty">无</p>

        <h3>报告（{{ detail.reports.length }}）</h3>
        <ul v-if="detail.reports.length">
          <li v-for="(r, ri) in detail.reports" :key="ri">
            {{ r.title }} — <a class="download-link" :href="downloadUrl(r.id)" target="_blank" rel="noopener">下载 PDF</a>
          </li>
        </ul>
        <p v-else class="empty">无</p>
      </section>
    </div>
  </div>
</template>

<script>
import api, { API_BASE_URL } from '../utils/api'

export default {
  name: 'ProjectWorkspace',
  props: {
    embedded: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      projects: [],
      detail: null,
      newProject: { name: '', description: '' },
      loading: false,
      creating: false,
      error: '',
      createMsg: '',
      reportLoading: false,
      reportMsg: '',
      reportMsgOk: true
    }
  },
  mounted() {
    this.loadProjects()
  },
  computed: {
    reportMsgClass() {
      return this.reportMsgOk ? 'msg' : 'error'
    }
  },
  methods: {
    async generateReport(projectId) {
      this.reportLoading = true
      this.reportMsg = ''
      try {
        const res = await api.post('/vision/report/pdf', {
          type: 'stage_summary',
          title: this.detail.project.name + ' 阶段总结',
          content: '项目：' + this.detail.project.name + '\n' + (this.detail.project.description || ''),
          projectId
        })
        if (res.data && res.data.code === 0) {
          this.reportMsgOk = true
          this.reportMsg = '已生成报告，可在报告中心查看'
        } else {
          this.reportMsgOk = false
          this.reportMsg = (res.data && res.data.message) || '生成失败'
        }
      } catch (e) {
        this.reportMsgOk = false
        this.reportMsg = '请求失败：' + (e.message || e)
      } finally {
        this.reportLoading = false
      }
    },
    async loadProjects() {
      this.loading = true
      this.error = ''
      try {
        const res = await api.get('/vision/project/list')
        if (res.data && res.data.code === 0) {
          this.projects = res.data.data || []
        } else {
          this.error = (res.data && res.data.message) || '加载失败'
        }
      } catch (e) {
        this.error = '请求失败：' + (e.message || e)
      } finally {
        this.loading = false
      }
    },
    async createProject() {
      if (!this.newProject.name.trim()) {
        this.createMsg = '请填写项目名称'
        return
      }
      this.creating = true
      this.createMsg = ''
      try {
        const res = await api.post('/vision/project', {
          name: this.newProject.name,
          description: this.newProject.description,
          task: null
        })
        if (res.data && res.data.code === 0) {
          this.createMsg = '创建成功：' + res.data.data.id
          this.newProject = { name: '', description: '' }
          this.loadProjects()
        } else {
          this.createMsg = (res.data && res.data.message) || '创建失败'
        }
      } catch (e) {
        this.createMsg = '请求失败：' + (e.message || e)
      } finally {
        this.creating = false
      }
    },
    async openDetail(id) {
      try {
        const res = await api.get('/vision/project/' + id)
        if (res.data && res.data.code === 0) {
          this.detail = res.data.data
        }
      } catch (e) {
        this.error = '加载详情失败：' + (e.message || e)
      }
    },
    goBack() {
      this.$router.push('/')
    },
    downloadUrl(id) {
      return API_BASE_URL + '/vision/report/download/' + id
    }
  }
}
</script>

<style scoped>
.workspace { min-height: 100vh; background: #f7f7f5; color: #1f2328; }
.workspace.embedded { min-height: auto; background: transparent; }
.container { width: min(100%, 1000px); margin: 0 auto; padding: 0 24px; box-sizing: border-box; }
.workspace.embedded .container { width: 100%; padding-left: 0; padding-right: 0; }
.header { background: rgba(255,255,255,0.96); border-bottom: 1px solid #e5e5e5; }
.header-content { min-height: 64px; display: flex; align-items: center; gap: 16px; }
.header h1 { margin: 0; font-size: 18px; font-weight: 600; }
.spacer { flex: 1; }
.back-btn { display: inline-flex; align-items: center; gap: 6px; background: transparent; border: 1px solid #d9d9e3; color: #353740; padding: 8px 12px; border-radius: 8px; cursor: pointer; font-size: 14px; }
.back-btn:hover { background: #f1f1f3; }
.body { display: grid; grid-template-columns: 320px 1fr; gap: 20px; padding: 24px 24px 48px; align-items: start; }
.workspace.embedded .body { padding: 32px 36px 48px; }
.create-card, .list-card, .detail-card { background: #fff; border: 1px solid #e4e4de; border-radius: 14px; padding: 20px; }
.detail-card { grid-column: 1 / -1; }
.detail-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px; }
.detail-head h2 { margin: 0; }
h2 { margin: 0 0 16px; font-size: 16px; }
h3 { margin: 18px 0 8px; font-size: 14px; color: #374151; }
.field { margin-bottom: 14px; display: flex; flex-direction: column; gap: 6px; }
label { font-size: 13px; color: #6b7280; }
input { border: 1px solid #d9d9e3; border-radius: 8px; padding: 8px 10px; font-size: 14px; outline: none; }
input:focus { border-color: #10a37f; }
.primary-btn { width: 100%; border: none; border-radius: 10px; background: #10a37f; color: #fff; padding: 10px; font-size: 14px; font-weight: 600; cursor: pointer; }
.primary-btn:disabled { background: #d9d9e3; cursor: not-allowed; }
.ghost-btn { border: 1px solid #10a37f; background: #fff; color: #10a37f; border-radius: 8px; padding: 6px 12px; font-size: 13px; cursor: pointer; }
.list-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.list-head h2 { margin: 0; }
.project-item { border: 1px solid #eee; border-radius: 10px; padding: 12px; margin-bottom: 10px; cursor: pointer; transition: border-color 0.18s ease, background 0.18s ease; }
.project-item:hover { border-color: #10a37f; background: #fafffd; }
.project-name { font-weight: 600; font-size: 14px; }
.project-desc { color: #6b7280; font-size: 13px; margin: 4px 0; }
.project-meta { color: #9ca3af; font-size: 12px; }
.detail-desc { color: #6b7280; font-size: 14px; }
.msg { color: #10a37f; font-size: 13px; margin-top: 8px; }
.error { color: #c0392b; font-size: 13px; }
.empty { color: #9ca3af; font-size: 13px; }
.download-link { color: #10a37f; text-decoration: none; font-size: 13px; }
.download-link:hover { text-decoration: underline; }
ul { margin: 6px 0; padding-left: 20px; }
li { font-size: 13px; line-height: 1.6; }
@media (max-width: 820px) {
  .body { grid-template-columns: 1fr; }
  .workspace.embedded .body { padding: 20px 16px 32px; }
}
</style>
