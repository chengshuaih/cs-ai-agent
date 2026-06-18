<template>
  <div class="debug-page">
    <div class="header">
      <div class="container">
        <div class="header-content">
          <button class="back-btn" @click="goBack">
            <span>←</span> 返回主页
          </button>
          <h1>调试页面</h1>
        </div>
      </div>
    </div>
    
    <div class="container">
      <div class="debug-container">
        <div class="debug-section">
          <h3>后端连接测试</h3>
          <div class="test-buttons">
            <button @click="testBasicConnection" class="test-btn">
              测试基本连接
            </button>
            <button @click="testLoveAppSSE" class="test-btn">
              测试恋爱大师SSE
            </button>
            <button @click="testManusSSE" class="test-btn">
              测试智能体SSE
            </button>
          </div>
          <div class="test-results">
            <div v-for="(result, index) in testResults" :key="index" 
                 :class="['test-result', result.type]">
              <span class="result-time">{{ result.time }}</span>
              <span class="result-status">{{ result.status }}</span>
              <span class="result-message">{{ result.message }}</span>
            </div>
          </div>
        </div>
        
        <div class="debug-section">
          <h3>网络请求日志</h3>
          <div class="log-container">
            <div v-for="(log, index) in networkLogs" :key="index" 
                 :class="['log-entry', log.type]">
              <span class="log-time">{{ log.time }}</span>
              <span class="log-type">{{ log.type.toUpperCase() }}</span>
              <span class="log-message">{{ log.message }}</span>
            </div>
          </div>
        </div>
        
        <div class="debug-section">
          <h3>手动测试</h3>
          <div class="manual-test">
            <input v-model="testMessage" placeholder="输入测试消息" class="test-input" />
            <button @click="sendTestMessage" class="send-test-btn">发送测试</button>
          </div>
          <div class="test-response">
            <h4>响应内容：</h4>
            <pre>{{ testResponse }}</pre>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'DebugPage',
  data() {
    return {
      testResults: [],
      networkLogs: [],
      testMessage: '你好，这是一条测试消息',
      testResponse: ''
    }
  },
  mounted() {
    this.addLog('info', '调试页面已加载')
  },
  methods: {
    goBack() {
      this.$router.push('/')
    },
    
    addTestResult(status, message) {
      const result = {
        time: this.getCurrentTime(),
        status: status,
        message: message,
        type: status === 'success' ? 'success' : 'error'
      }
      this.testResults.unshift(result)
      if (this.testResults.length > 10) {
        this.testResults.pop()
      }
    },
    
    addLog(type, message) {
      const log = {
        time: this.getCurrentTime(),
        type: type,
        message: message
      }
      this.networkLogs.unshift(log)
      if (this.networkLogs.length > 20) {
        this.networkLogs.pop()
      }
    },
    
    async testBasicConnection() {
      this.addLog('info', '开始测试基本连接...')
      
      try {
        const response = await axios.get('http://localhost:8123/api/health', {
          timeout: 5000
        })
        this.addTestResult('success', `连接成功: ${response.status}`)
        this.addLog('success', `HTTP ${response.status}: ${response.statusText}`)
      } catch (error) {
        const message = error.code === 'ECONNREFUSED' 
          ? '连接被拒绝 - 后端服务可能未启动' 
          : `连接失败: ${error.message}`
        this.addTestResult('error', message)
        this.addLog('error', message)
      }
    },
    
    async testLoveAppSSE() {
      this.addLog('info', '开始测试恋爱大师SSE连接...')
      
      try {
        const response = await fetch('http://localhost:8123/api/ai/love_app/chat/sse?message=测试&chatId=test_123', {
          method: 'GET',
          headers: {
            'Accept': 'text/event-stream',
            'Cache-Control': 'no-cache',
          }
        })
        
        if (response.ok) {
          this.addTestResult('success', `SSE连接成功: ${response.status}`)
          this.addLog('success', `SSE连接建立成功`)
          
          // 读取响应内容
          const reader = response.body.getReader()
          const decoder = new TextDecoder()
          
          setTimeout(async () => {
            try {
              const { done, value } = await reader.read()
              if (!done) {
                const chunk = decoder.decode(value)
                this.addLog('info', `收到SSE数据: ${chunk}`)
              }
              reader.releaseLock()
            } catch (e) {
              this.addLog('error', `读取SSE数据失败: ${e.message}`)
            }
          }, 1000)
          
        } else {
          this.addTestResult('error', `SSE连接失败: ${response.status}`)
          this.addLog('error', `HTTP ${response.status}: ${response.statusText}`)
        }
      } catch (error) {
        const message = error.code === 'ECONNREFUSED' 
          ? 'SSE连接被拒绝 - 后端服务可能未启动' 
          : `SSE连接失败: ${error.message}`
        this.addTestResult('error', message)
        this.addLog('error', message)
      }
    },
    
    async testManusSSE() {
      this.addLog('info', '开始测试智能体SSE连接...')
      
      try {
        const response = await fetch('http://localhost:8123/api/ai/manus/chat?message=测试', {
          method: 'GET',
          headers: {
            'Accept': 'text/event-stream',
            'Cache-Control': 'no-cache',
          }
        })
        
        if (response.ok) {
          this.addTestResult('success', `智能体SSE连接成功: ${response.status}`)
          this.addLog('success', `智能体SSE连接建立成功`)
        } else {
          this.addTestResult('error', `智能体SSE连接失败: ${response.status}`)
          this.addLog('error', `HTTP ${response.status}: ${response.statusText}`)
        }
      } catch (error) {
        const message = error.code === 'ECONNREFUSED' 
          ? '智能体SSE连接被拒绝 - 后端服务可能未启动' 
          : `智能体SSE连接失败: ${error.message}`
        this.addTestResult('error', message)
        this.addLog('error', message)
      }
    },
    
    async sendTestMessage() {
      if (!this.testMessage.trim()) return
      
      this.addLog('info', `发送测试消息: ${this.testMessage}`)
      
      try {
        const response = await axios.get(`http://localhost:8123/api/ai/love_app/chat/sse`, {
          params: {
            message: this.testMessage,
            chatId: 'debug_' + Date.now()
          },
          timeout: 10000
        })
        
        this.testResponse = JSON.stringify(response.data, null, 2)
        this.addLog('success', '测试消息发送成功')
      } catch (error) {
        this.testResponse = `错误: ${error.message}`
        this.addLog('error', `测试消息发送失败: ${error.message}`)
      }
    },
    
    getCurrentTime() {
      const now = new Date()
      return now.toLocaleTimeString('zh-CN', { 
        hour: '2-digit', 
        minute: '2-digit',
        second: '2-digit'
      })
    }
  }
}
</script>

<style scoped>
.debug-page {
  min-height: 100vh;
  background: #f7f7f8;
  color: #202123;
}

.debug-page * {
  box-sizing: border-box;
}

.container {
  width: min(100% - 32px, 1040px);
  margin: 0 auto;
}

.header {
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid #e5e5e5;
  backdrop-filter: blur(10px);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 64px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #ffffff;
  border: 1px solid #d9d9e3;
  color: #343541;
  padding: 7px 12px;
  border-radius: 999px;
  cursor: pointer;
  font-size: 14px;
  line-height: 1.2;
  transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.back-btn:hover {
  background: #f1f1f3;
  border-color: #c5c5d2;
}

.back-btn:focus-visible,
.test-btn:focus-visible,
.send-test-btn:focus-visible,
.test-input:focus-visible {
  outline: 2px solid #10a37f;
  outline-offset: 2px;
}

.header h1 {
  margin: 0;
  color: #202123;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.01em;
}

.debug-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 28px 0 40px;
}

.debug-section {
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 14px;
  padding: 18px;
  margin-bottom: 16px;
}

.debug-section h3 {
  margin: 0 0 14px;
  color: #202123;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: -0.01em;
}

.test-buttons {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.test-btn,
.send-test-btn {
  border: 1px solid #d9d9e3;
  padding: 7px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  line-height: 1.3;
  transition: background 0.2s ease, border-color 0.2s ease, transform 0.2s ease;
  white-space: nowrap;
}

.test-btn {
  background: #ffffff;
  color: #343541;
}

.test-btn:hover {
  background: #f7f7f8;
  border-color: #c5c5d2;
}

.send-test-btn {
  background: #10a37f;
  border-color: #10a37f;
  color: #ffffff;
}

.send-test-btn:hover {
  background: #0e8f70;
  border-color: #0e8f70;
}

.test-results {
  max-height: 220px;
  overflow-y: auto;
}

.test-result {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.test-result.success {
  background: #f0fdf7;
  color: #046c4e;
  border: 1px solid #c6f6df;
}

.test-result.error {
  background: #fff5f5;
  color: #b42318;
  border: 1px solid #fed7d7;
}

.result-time,
.result-status,
.log-time,
.log-type {
  flex: 0 0 auto;
  font-weight: 600;
}

.result-time,
.log-time {
  min-width: 72px;
  color: #6b7280;
}

.result-status {
  min-width: 54px;
}

.result-message,
.log-message {
  min-width: 0;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.log-container {
  max-height: 320px;
  overflow: auto;
  background: #f4f4f5;
  border: 1px solid #e5e5e5;
  border-radius: 10px;
  padding: 10px 12px;
}

.log-entry {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #e5e5e5;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 12px;
  line-height: 1.55;
}

.log-entry:last-child {
  border-bottom: none;
}

.log-entry.info {
  color: #2563eb;
}

.log-entry.success {
  color: #047857;
}

.log-entry.error {
  color: #dc2626;
}

.log-type {
  min-width: 52px;
}

.manual-test {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.test-input {
  flex: 1;
  min-width: 0;
  padding: 9px 11px;
  border: 1px solid #d9d9e3;
  border-radius: 8px;
  background: #ffffff;
  color: #202123;
  font-size: 14px;
  line-height: 1.4;
}

.test-input::placeholder {
  color: #8e8ea0;
}

.test-response {
  background: #f4f4f5;
  border: 1px solid #e5e5e5;
  border-radius: 10px;
  padding: 12px;
}

.test-response h4 {
  margin: 0 0 10px;
  color: #343541;
  font-size: 13px;
  font-weight: 600;
}

.test-response pre {
  max-height: 360px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  background: #ffffff;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  color: #343541;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  word-break: break-word;
}

@media (max-width: 768px) {
  .container {
    width: min(100% - 24px, 1040px);
  }

  .header-content {
    min-height: 58px;
    gap: 12px;
  }

  .header h1 {
    font-size: 16px;
  }

  .debug-container {
    padding: 18px 0 28px;
  }

  .debug-section {
    padding: 14px;
    border-radius: 12px;
  }

  .test-buttons,
  .manual-test {
    flex-direction: column;
  }

  .test-btn,
  .send-test-btn,
  .test-input {
    width: 100%;
  }

  .test-result,
  .log-entry {
    display: grid;
    grid-template-columns: auto 1fr;
    gap: 4px 8px;
  }

  .result-message,
  .log-message {
    grid-column: 1 / -1;
  }
}
</style> 