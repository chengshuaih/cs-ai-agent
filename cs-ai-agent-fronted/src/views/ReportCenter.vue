<template>
  <div :class="['report-center', { embedded }]">
    <div v-if="!embedded" class="header">
      <div class="container header-content">
        <button class="back-btn" @click="goBack"><span>←</span> 返回主页</button>
        <h1>报告中心</h1>
        <button class="ghost-btn" :disabled="loading" @click="loadReports">刷新</button>
      </div>
    </div>

    <div class="container body">
      <p v-if="error" class="error">{{ error }}</p>
      <p v-if="!loading && reports.length === 0" class="empty">暂无报告记录。</p>

      <table v-if="reports.length" class="report-table">
        <thead>
          <tr>
            <th>标题</th>
            <th>类型</th>
            <th>所属项目</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(r, i) in reports" :key="i">
            <td>{{ r.title }}</td>
            <td><span class="badge">{{ typeLabel(r.type) }}</span></td>
            <td>{{ projectMap[r.projectId] || r.projectId || '未分类' }}</td>
            <td>{{ r.createdAt }}</td>
            <td><a class="download-link" :href="downloadUrl(r.id)" target="_blank" rel="noopener">下载 PDF</a></td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import api, { API_BASE_URL } from '../utils/api'

export default {
  name: 'ReportCenter',
  props: {
    embedded: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      reports: [],
      projectMap: {},
      loading: false,
      error: ''
    }
  },
  mounted() {
    this.loadReports()
  },
  methods: {
    async loadReports() {
      this.loading = true
      this.error = ''
      try {
        const [reportRes, projRes] = await Promise.all([
          api.get('/vision/report/list'),
          api.get('/vision/project/list')
        ])
        if (reportRes.data && reportRes.data.code === 0) {
          this.reports = reportRes.data.data || []
        } else {
          this.error = (reportRes.data && reportRes.data.message) || '加载失败'
        }
        if (projRes.data && projRes.data.code === 0) {
          this.projectMap = (projRes.data.data || []).reduce((m, p) => {
            m[p.id] = p.name
            return m
          }, {})
        }
      } catch (e) {
        this.error = '请求失败：' + (e.message || e)
      } finally {
        this.loading = false
      }
    },
    typeLabel(type) {
      const map = {
        collection_plan: '采集计划',
        experiment_plan: '实验计划',
        research_summary: '调研摘要',
        stage_summary: '阶段总结'
      }
      return map[type] || type
    },
    downloadUrl(id) {
      return API_BASE_URL + '/vision/report/download/' + id
    },
    goBack() {
      this.$router.push('/')
    }
  }
}
</script>

<style scoped>
.report-center { min-height: 100vh; background: #f7f7f5; color: #1f2328; }
.report-center.embedded { min-height: auto; background: transparent; }
.container { width: min(100%, 1000px); margin: 0 auto; padding: 0 24px; box-sizing: border-box; }
.report-center.embedded .container { width: 100%; padding-left: 0; padding-right: 0; }
.header { background: rgba(255,255,255,0.96); border-bottom: 1px solid #e5e5e5; }
.header-content { min-height: 64px; display: flex; align-items: center; gap: 16px; }
.header h1 { margin: 0; font-size: 18px; font-weight: 600; flex: 1; }
.back-btn { display: inline-flex; align-items: center; gap: 6px; background: transparent; border: 1px solid #d9d9e3; color: #353740; padding: 8px 12px; border-radius: 8px; cursor: pointer; font-size: 14px; }
.back-btn:hover { background: #f1f1f3; }
.ghost-btn { border: 1px solid #10a37f; background: #fff; color: #10a37f; border-radius: 8px; padding: 6px 14px; font-size: 13px; cursor: pointer; }
.body { padding: 24px 24px 48px; }
.report-center.embedded .body { padding: 32px 36px 48px; }
.error { color: #c0392b; font-size: 14px; }
.empty { color: #8e8ea0; font-size: 14px; }
.report-table { width: 100%; border-collapse: collapse; background: #fff; border: 1px solid #e4e4de; border-radius: 12px; overflow: hidden; }
th, td { text-align: left; padding: 12px 14px; font-size: 13px; border-bottom: 1px solid #f0f0ec; }
th { background: #fafaf8; color: #6b7280; font-weight: 600; }
.badge { background: #f1f5f3; border-radius: 999px; padding: 2px 10px; font-size: 12px; color: #4b5563; }
.download-link { color: #10a37f; text-decoration: none; font-size: 13px; }
.download-link:hover { text-decoration: underline; }
@media (max-width: 820px) {
  .report-center.embedded .body { padding: 20px 16px 32px; }
}
</style>
