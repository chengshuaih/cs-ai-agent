<template>
  <div :class="['planner', { embedded }]">
    <div v-if="!embedded" class="header">
      <div class="container header-content">
        <button class="back-btn" @click="goBack"><span>←</span> 返回主页</button>
        <h1>采集辅助规划</h1>
        <span class="spacer"></span>
      </div>
    </div>

    <div :class="['container body', { 'has-result': plan }]">
      <section class="form-card">
        <h2>采集任务</h2>
        <div class="field">
          <label>任务类型</label>
          <select v-model="form.taskType">
            <option>目标检测</option>
            <option>图像分割</option>
            <option>深度估计</option>
            <option>三维重建</option>
            <option>图像分类</option>
            <option>目标跟踪</option>
          </select>
        </div>
        <div class="field">
          <label>采集目标（逗号分隔）</label>
          <input v-model="targetText" placeholder="如：车辆,行人,交通灯" />
        </div>
        <div class="field">
          <label>采集地点</label>
          <div class="location-row">
            <input v-model="form.location" placeholder="如：上海市静安区" class="location-input" />
            <button
              type="button"
              class="locate-btn"
              :disabled="locating"
              :title="locating ? '定位中...' : '使用当前位置'"
              @click="locateMe"
            >{{ locating ? '…' : '📍' }}</button>
          </div>
          <span v-if="locateError" class="locate-error">{{ locateError }}</span>
        </div>
        <div class="field-row">
          <div class="field">
            <label>时间预算</label>
            <select v-model="form.timeBudget">
              <option>半天</option>
              <option>整天</option>
              <option>2小时</option>
            </select>
          </div>
          <div class="field">
            <label>出行方式</label>
            <select v-model="form.transportMode">
              <option>步行</option>
              <option>骑行</option>
              <option>驾车</option>
            </select>
          </div>
        </div>
        <div class="field">
          <label>归属项目（可选）</label>
          <select v-model="projectId">
            <option value="">不归档（未分类）</option>
            <option v-for="p in projectList" :key="p.id" :value="p.id">{{ p.name }}</option>
          </select>
        </div>
        <button class="primary-btn" :disabled="loading" @click="generatePlan">
          {{ loading ? '生成中...' : '生成采集规划' }}
        </button>
        <p v-if="error" class="error">{{ error }}</p>
      </section>

      <section class="result-card" v-if="plan">
        <div class="result-head">
          <h2>{{ plan.title }}</h2>
          <button class="ghost-btn" :disabled="pdfLoading" @click="exportPdf">
            {{ pdfLoading ? '导出中...' : '导出 PDF' }}
          </button>
        </div>
        <p v-if="pdfMsg" class="pdf-msg">{{ pdfMsg }}</p>

        <!-- 多方案切换 -->
        <div class="scheme-tabs" v-if="schemes.length">
          <button
            v-for="(sc, si) in schemes"
            :key="si"
            :class="['scheme-tab', { active: selectedIndexes.includes(si) }]"
            @click="toggleScheme(si)"
          >
            {{ sc.title }}
          </button>
          <span class="scheme-hint" v-if="schemes.length > 1">可多选对比</span>
        </div>

        <div class="scheme-block" v-for="si in selectedIndexes" :key="'scheme-' + si">
          <div class="scheme-head" v-if="schemes.length">
            <h3>{{ schemes[si].title }}</h3>
            <div class="scheme-meta">
              <span v-if="schemes[si].totalDistance">全程 {{ schemes[si].totalDistance }}</span>
              <span v-if="schemes[si].totalDuration">· {{ schemes[si].totalDuration }}</span>
              <span v-if="schemes[si].transportMode">· {{ schemes[si].transportMode }}</span>
            </div>
          </div>
          <p class="scheme-reason" v-if="schemes[si].recommendReason">
            <b>推荐理由：</b>{{ schemes[si].recommendReason }}
          </p>

          <div class="site" v-for="(s, i) in schemes[si].sites" :key="i">
            <div class="site-head">
              <span class="site-name">{{ i + 1 }}. {{ s.name }}</span>
              <span class="site-score">得分 {{ s.score }}</span>
            </div>
            <div class="site-meta">{{ s.address }} · {{ s.distance }}</div>
            <div class="site-travel" v-if="s.travelDistance || s.travelDuration">
              <span v-if="s.travelDistance">距上一点 {{ s.travelDistance }}</span>
              <span v-if="s.travelDuration">· {{ s.travelDuration }}</span>
              <span class="travel-mode">· {{ schemes[si].transportMode }}前往</span>
              <a v-if="s.navUrl" class="nav-link" :href="s.navUrl" target="_blank" rel="noopener">在高德打开 ›</a>
            </div>
            <div class="site-tags">
              <span class="tag" v-for="(t, ti) in s.sceneTags" :key="ti">{{ t }}</span>
            </div>
            <div class="site-line" v-if="s.reason"><b>推荐理由：</b>{{ s.reason }}</div>
            <ul v-if="s.captureSuggestions && s.captureSuggestions.length">
              <li v-for="(c, ci) in s.captureSuggestions" :key="ci">{{ c }}</li>
            </ul>
            <div class="site-line risk" v-if="s.riskTips && s.riskTips.length">
              <b>风险提示：</b>{{ s.riskTips.join('；') }}
            </div>
            <div class="site-images">
              <img
                v-if="s.mapImageUrl"
                :src="s.mapImageUrl"
                class="site-image map-image"
                alt="点位地图"
                loading="lazy"
                @error="onImageError"
              />
              <img
                v-for="(img, ii) in (s.imageUrls || [])"
                :key="ii"
                :src="img"
                class="site-image"
                alt="采集点位参考图"
                loading="lazy"
                @error="onImageError"
              />
            </div>
          </div>

          <div class="scheme-route" v-if="schemes[si].routeSummary">
            <b>路线与时段：</b>{{ schemes[si].routeSummary }}
          </div>
        </div>

        <!-- 无方案时回退展示原 sites -->
        <template v-if="!schemes.length">
          <h3>推荐采集点位</h3>
          <div class="site" v-for="(s, i) in plan.sites" :key="i">
            <div class="site-head">
              <span class="site-name">{{ i + 1 }}. {{ s.name }}</span>
              <span class="site-score">得分 {{ s.score }}</span>
            </div>
            <div class="site-meta">{{ s.address }} · {{ s.distance }}</div>
            <div class="site-tags">
              <span class="tag" v-for="(t, ti) in s.sceneTags" :key="ti">{{ t }}</span>
            </div>
            <div class="site-line" v-if="s.reason"><b>推荐理由：</b>{{ s.reason }}</div>
            <ul v-if="s.captureSuggestions && s.captureSuggestions.length">
              <li v-for="(c, ci) in s.captureSuggestions" :key="ci">{{ c }}</li>
            </ul>
            <div class="site-line risk" v-if="s.riskTips && s.riskTips.length">
              <b>风险提示：</b>{{ s.riskTips.join('；') }}
            </div>
            <div class="site-images" v-if="s.imageUrls && s.imageUrls.length">
              <img
                v-for="(img, ii) in s.imageUrls"
                :key="ii"
                :src="img"
                class="site-image"
                alt="采集点位参考图"
                loading="lazy"
                @error="onImageError"
              />
            </div>
          </div>
          <h3 v-if="plan.routeSummary">路线与时段建议</h3>
          <p v-if="plan.routeSummary">{{ plan.routeSummary }}</p>
        </template>

        <h3 v-if="plan.checklist && plan.checklist.length">采集清单</h3>
        <ul v-if="plan.checklist">
          <li v-for="(c, ci) in plan.checklist" :key="ci">{{ c }}</li>
        </ul>

        <h3 v-if="plan.annotationGuide && plan.annotationGuide.length">标注指引</h3>
        <ul v-if="plan.annotationGuide">
          <li v-for="(a, ai) in plan.annotationGuide" :key="ai">{{ a }}</li>
        </ul>

        <h3 v-if="plan.privacyTips && plan.privacyTips.length">隐私与安全提示</h3>
        <ul v-if="plan.privacyTips">
          <li v-for="(p, pi) in plan.privacyTips" :key="pi">{{ p }}</li>
        </ul>
      </section>
    </div>
  </div>
</template>

<script>
import api from '../utils/api'

export default {
  name: 'CollectionPlanner',
  props: {
    embedded: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      form: {
        taskType: '目标检测',
        location: '',
        timeBudget: '半天',
        transportMode: '步行'
      },
      targetText: '',
      projectId: '',
      plan: null,
      selectedIndexes: [],
      loading: false,
      pdfLoading: false,
      error: '',
      pdfMsg: '',
      locating: false,
      locateError: '',
      projectList: []
    }
  },
  mounted() {
    this.loadProjects()
  },
  computed: {
    schemes() {
      return (this.plan && this.plan.schemes) ? this.plan.schemes : []
    }
  },
  methods: {
    async loadProjects() {
      try {
        const res = await api.get('/vision/project/list')
        if (res.data && res.data.code === 0) {
          this.projectList = res.data.data || []
        }
      } catch (_) {}
    },
    async locateMe() {
      if (!navigator.geolocation) {
        this.locateError = '浏览器不支持定位'
        return
      }
      this.locating = true
      this.locateError = ''
      navigator.geolocation.getCurrentPosition(
        async (pos) => {
          const { latitude, longitude } = pos.coords
          try {
            // 高德逆地理编码（Web API，不需要 MCP key，用 restapi key 直接调）
            const res = await api.get('/vision/geocode/reverse', {
              params: { lat: latitude, lng: longitude }
            })
            if (res.data && res.data.code === 0 && res.data.data) {
              this.form.location = res.data.data
            } else {
              this.locateError = '无法解析位置，请手动输入'
            }
          } catch (_) {
            this.locateError = '定位解析失败，请手动输入'
          } finally {
            this.locating = false
          }
        },
        (err) => {
          this.locating = false
          this.locateError = err.code === 1 ? '已拒绝定位权限' : '定位失败，请手动输入'
        },
        { timeout: 8000 }
      )
    },
    async generatePlan() {
      this.error = ''
      this.pdfMsg = ''
      this.loading = true
      try {
        const targets = this.targetText
          ? this.targetText.split(/[,，]/).map(s => s.trim()).filter(Boolean)
          : []
        const payload = {
          task: {
            taskType: this.form.taskType,
            targetObjects: targets,
            sceneTypes: [],
            location: this.form.location,
            timeBudget: this.form.timeBudget,
            transportMode: this.form.transportMode
          },
          needPdf: false,
          projectId: this.projectId || null
        }
        const res = await api.post('/vision/collection/plan', payload)
        if (res.data && res.data.code === 0) {
          this.plan = res.data.data
          // 默认选中首选方案
          this.selectedIndexes = (this.plan.schemes && this.plan.schemes.length) ? [0] : []
        } else {
          this.error = (res.data && res.data.message) || '生成失败'
        }
      } catch (e) {
        this.error = '请求失败：' + (e.message || e)
      } finally {
        this.loading = false
      }
    },
    async exportPdf() {
      if (!this.plan) return
      this.pdfLoading = true
      this.pdfMsg = ''
      try {
        const payload = {
          type: 'collection_plan',
          title: this.plan.title,
          sourceId: this.plan.id,
          projectId: this.projectId || null
        }
        const res = await api.post('/vision/report/pdf', payload)
        if (res.data && res.data.code === 0) {
          this.pdfMsg = '已生成报告：' + (res.data.data.pdfPath || res.data.data.id)
        } else {
          this.pdfMsg = (res.data && res.data.message) || '导出失败'
        }
      } catch (e) {
        this.pdfMsg = '导出失败：' + (e.message || e)
      } finally {
        this.pdfLoading = false
      }
    },
    goBack() {
      this.$router.push('/')
    },
    toggleScheme(index) {
      const pos = this.selectedIndexes.indexOf(index)
      if (pos >= 0) {
        // 至少保留一套选中
        if (this.selectedIndexes.length > 1) {
          this.selectedIndexes.splice(pos, 1)
        }
      } else {
        this.selectedIndexes.push(index)
        this.selectedIndexes.sort((a, b) => a - b)
      }
    },
    onImageError(e) {
      // 参考图/地图加载失败时隐藏该图，不影响点位信息展示
      e.target.style.display = 'none'
    }
  }
}
</script>

<style scoped>
.planner { min-height: 100vh; background: #f7f7f5; color: #1f2328; }
.planner.embedded { min-height: auto; background: transparent; }
.container { width: min(100%, 1100px); margin: 0 auto; padding: 0 24px; box-sizing: border-box; }
.planner.embedded .container { width: 100%; padding-left: 16px; padding-right: 16px; }
.header { background: rgba(255,255,255,0.96); border-bottom: 1px solid #e5e5e5; }
.header-content { min-height: 64px; display: flex; align-items: center; gap: 16px; }
.header h1 { margin: 0; font-size: 18px; font-weight: 600; }
.spacer { flex: 1; }
.back-btn { display: inline-flex; align-items: center; gap: 6px; background: transparent; border: 1px solid #d9d9e3; color: #353740; padding: 8px 12px; border-radius: 8px; cursor: pointer; font-size: 14px; }
.back-btn:hover { background: #f1f1f3; }
.body { display: grid; grid-template-columns: minmax(320px, 520px); justify-content: center; gap: 20px; padding-top: 24px; padding-bottom: 48px; align-items: start; }
.body.has-result { grid-template-columns: minmax(320px, 380px) minmax(0, 1fr); justify-content: stretch; }
.form-card, .result-card { background: #fff; border: 1px solid #e4e4de; border-radius: 14px; padding: 20px; }
h2 { margin: 0 0 16px; font-size: 16px; }
h3 { margin: 20px 0 8px; font-size: 14px; color: #374151; }
.field { margin-bottom: 14px; display: flex; flex-direction: column; gap: 6px; }
.field-row { display: flex; gap: 12px; }
.field-row .field { flex: 1; }
label { font-size: 13px; color: #6b7280; }
input, select { border: 1px solid #d9d9e3; border-radius: 8px; padding: 8px 10px; font-size: 14px; outline: none; }
input:focus, select:focus { border-color: #10a37f; }
.location-row { display: flex; gap: 8px; align-items: center; }
.location-input { flex: 1; }
.locate-btn { border: 1px solid #d9d9e3; background: #fff; border-radius: 8px; padding: 8px 10px; font-size: 14px; cursor: pointer; flex-shrink: 0; line-height: 1; }
.locate-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.locate-btn:not(:disabled):hover { border-color: #10a37f; background: #f0fbf7; }
.locate-error { font-size: 12px; color: #c0392b; }
.primary-btn { width: 100%; margin-top: 6px; border: none; border-radius: 10px; background: #10a37f; color: #fff; padding: 10px; font-size: 14px; font-weight: 600; cursor: pointer; }
.primary-btn:disabled { background: #d9d9e3; cursor: not-allowed; }
.ghost-btn { border: 1px solid #10a37f; background: #fff; color: #10a37f; border-radius: 8px; padding: 6px 12px; font-size: 13px; cursor: pointer; }
.ghost-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.error { color: #c0392b; font-size: 13px; margin-top: 10px; }
.pdf-msg { color: #10a37f; font-size: 13px; }
.result-head { display: flex; align-items: center; justify-content: space-between; }
.site { border: 1px solid #eee; border-radius: 10px; padding: 12px; margin-bottom: 12px; }
.site-head { display: flex; justify-content: space-between; align-items: center; }
.site-name { font-weight: 600; }
.site-score { color: #10a37f; font-weight: 600; font-size: 13px; }
.site-meta { color: #6b7280; font-size: 13px; margin: 4px 0; }
.site-tags { display: flex; flex-wrap: wrap; gap: 6px; margin: 6px 0; }
.tag { background: #f1f5f3; border-radius: 999px; padding: 2px 10px; font-size: 12px; color: #4b5563; }
.site-line { font-size: 13px; margin: 6px 0; line-height: 1.5; }
.site-line.risk { color: #b7791f; }
.site-images { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
.site-image { width: 96px; height: 72px; object-fit: cover; border-radius: 8px; border: 1px solid #eee; }
.map-image { width: 132px; height: 80px; border-color: #cfe6dd; }
.scheme-tabs { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin: 12px 0 4px; }
.scheme-tab { border: 1px solid #d9d9e3; background: #fff; color: #374151; border-radius: 999px; padding: 6px 14px; font-size: 13px; cursor: pointer; }
.scheme-tab.active { border-color: #10a37f; background: #e8f6f1; color: #0c7c5e; font-weight: 600; }
.scheme-hint { font-size: 12px; color: #9ca3af; }
.scheme-block { border-top: 1px dashed #e5e5e5; padding-top: 12px; margin-top: 8px; }
.scheme-head { display: flex; align-items: baseline; gap: 12px; flex-wrap: wrap; }
.scheme-meta { font-size: 13px; color: #6b7280; }
.scheme-reason { font-size: 13px; color: #374151; margin: 4px 0 10px; line-height: 1.5; }
.scheme-route { font-size: 13px; color: #374151; margin: 8px 0 4px; line-height: 1.5; }
.site-travel { font-size: 12px; color: #6b7280; margin: 2px 0 4px; display: flex; flex-wrap: wrap; gap: 6px; align-items: center; }
.travel-mode { color: #6b7280; }
.nav-link { color: #10a37f; text-decoration: none; font-size: 12px; }
.nav-link:hover { text-decoration: underline; }
ul { margin: 6px 0; padding-left: 20px; }
li { font-size: 13px; line-height: 1.6; }
@media (max-width: 820px) {
  .body,
  .body.has-result { grid-template-columns: 1fr; }
}
</style>
