<template>
  <div class="knowledge-page">
    <div class="header">
      <div class="container header-content">
        <button class="back-btn" @click="$router.push('/')"><span>←</span> 返回主页</button>
        <h1>知识库管理</h1>
        <span class="spacer"></span>
      </div>
    </div>

    <div class="container body">
      <section class="upload-card">
        <h2>上传知识文档</h2>
        <div class="field">
          <label>文档标题</label>
          <input v-model="form.title" placeholder="如：目标检测常见算法介绍" />
        </div>
        <div class="field">
          <label>文档内容</label>
          <textarea v-model="form.content" rows="8" placeholder="粘贴文档正文内容..."></textarea>
        </div>
        <p v-if="uploadMsg" :class="uploadOk ? 'msg' : 'err'">{{ uploadMsg }}</p>
        <button class="primary-btn" :disabled="uploading" @click="upload">
          {{ uploading ? '上传中...' : '上传到知识库' }}
        </button>
      </section>

      <section class="list-card">
        <div class="list-head">
          <h2>已入库文档（{{ docs.length }} 条）</h2>
          <button class="ghost-btn" :disabled="listLoading" @click="loadList">刷新</button>
        </div>
        <p v-if="listError" class="err">{{ listError }}</p>
        <p v-if="!listLoading && docs.length === 0" class="empty">暂无入库文档。</p>
        <div class="doc-item" v-for="(d, i) in docs" :key="i">
          <div class="doc-title">{{ d.title || '无标题' }}</div>
          <div class="doc-domain">领域：{{ d.domain }}</div>
          <div class="doc-preview">{{ d.preview }}</div>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import api from '../utils/api'

export default {
  name: 'KnowledgePage',
  data() {
    return {
      form: { title: '', content: '' },
      uploading: false,
      uploadMsg: '',
      uploadOk: true,
      docs: [],
      listLoading: false,
      listError: ''
    }
  },
  mounted() {
    this.loadList()
  },
  methods: {
    async upload() {
      if (!this.form.content.trim()) {
        this.uploadMsg = '内容不能为空'
        this.uploadOk = false
        return
      }
      this.uploading = true
      this.uploadMsg = ''
      try {
        const res = await api.post('/vision/knowledge/upload', {
          title: this.form.title,
          content: this.form.content
        })
        if (res.data && res.data.code === 0) {
          this.uploadOk = true
          this.uploadMsg = '上传成功！文档已入库'
          this.form = { title: '', content: '' }
          this.loadList()
        } else {
          this.uploadOk = false
          this.uploadMsg = (res.data && res.data.message) || '上传失败'
        }
      } catch (e) {
        this.uploadOk = false
        this.uploadMsg = '请求失败：' + (e.message || e)
      } finally {
        this.uploading = false
      }
    },
    async loadList() {
      this.listLoading = true
      this.listError = ''
      try {
        const res = await api.get('/vision/knowledge/list')
        if (res.data && res.data.code === 0) {
          this.docs = res.data.data || []
        } else {
          this.listError = (res.data && res.data.message) || '加载失败'
        }
      } catch (e) {
        this.listError = '请求失败：' + (e.message || e)
      } finally {
        this.listLoading = false
      }
    }
  }
}
</script>

<style scoped>
.knowledge-page { min-height: 100vh; background: #f7f7f5; color: #1f2328; }
.container { width: min(100%, 1000px); margin: 0 auto; padding: 0 24px; box-sizing: border-box; }
.header { background: rgba(255,255,255,0.96); border-bottom: 1px solid #e5e5e5; }
.header-content { min-height: 64px; display: flex; align-items: center; gap: 16px; }
.header h1 { margin: 0; font-size: 18px; font-weight: 600; }
.spacer { flex: 1; }
.back-btn { display: inline-flex; align-items: center; gap: 6px; background: transparent; border: 1px solid #d9d9e3; color: #353740; padding: 8px 12px; border-radius: 8px; cursor: pointer; font-size: 14px; }
.back-btn:hover { background: #f1f1f3; }
.body { display: grid; grid-template-columns: 400px 1fr; gap: 20px; padding: 24px 24px 48px; align-items: start; }
.upload-card, .list-card { background: #fff; border: 1px solid #e4e4de; border-radius: 14px; padding: 20px; }
.field { margin-bottom: 14px; display: flex; flex-direction: column; gap: 6px; }
label { font-size: 13px; color: #6b7280; }
input, textarea { border: 1px solid #d9d9e3; border-radius: 8px; padding: 8px 10px; font-size: 14px; outline: none; resize: vertical; font-family: inherit; }
input:focus, textarea:focus { border-color: #10a37f; }
.primary-btn { width: 100%; border: none; border-radius: 10px; background: #10a37f; color: #fff; padding: 10px; font-size: 14px; font-weight: 600; cursor: pointer; margin-top: 4px; }
.primary-btn:disabled { background: #d9d9e3; cursor: not-allowed; }
h2 { margin: 0 0 16px; font-size: 16px; }
.list-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.list-head h2 { margin: 0; }
.ghost-btn { border: 1px solid #10a37f; background: #fff; color: #10a37f; border-radius: 8px; padding: 6px 12px; font-size: 13px; cursor: pointer; }
.doc-item { border: 1px solid #eee; border-radius: 10px; padding: 12px; margin-bottom: 10px; }
.doc-title { font-weight: 600; font-size: 14px; }
.doc-domain { font-size: 12px; color: #9ca3af; margin: 2px 0; }
.doc-preview { font-size: 13px; color: #6b7280; line-height: 1.5; }
.msg { color: #10a37f; font-size: 13px; margin-bottom: 8px; }
.err { color: #c0392b; font-size: 13px; margin-bottom: 8px; }
.empty { color: #9ca3af; font-size: 13px; }
@media (max-width: 820px) {
  .body { grid-template-columns: 1fr; }
}
</style>
