<template>
  <div class="debug-page">
    <div class="header">
      <div class="container">
        <div class="header-content">
          <button class="back-btn" @click="goBack">
            <span>←</span> 返回主页
          </button>
          <h1>🔧 调试页面</h1>
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
  background: #f8f9fa;
}

.header {
  background: linear-gradient(135deg, #6c5ce7 0%, #a29bfe 100%);
  color: white;
  padding: 20px 0;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.back-btn {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
  padding: 8px 16px;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.header h1 {
  font-size: 1.8rem;
  margin: 0;
}

.debug-container {
  max-width: 1000px;
  margin: 30px auto;
}

.debug-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.debug-section h3 {
  margin-bottom: 20px;
  color: #333;
  border-bottom: 2px solid #6c5ce7;
  padding-bottom: 10px;
}

.test-buttons {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.test-btn {
  background: linear-gradient(135deg, #6c5ce7 0%, #a29bfe 100%);
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
}

.test-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
}

.test-results {
  max-height: 200px;
  overflow-y: auto;
}

.test-result {
  display: flex;
  gap: 15px;
  padding: 10px;
  border-radius: 6px;
  margin-bottom: 8px;
  font-size: 14px;
}

.test-result.success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6c3;
}

.test-result.error {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.result-time {
  font-weight: bold;
  min-width: 80px;
}

.result-status {
  font-weight: bold;
  min-width: 60px;
}

.log-container {
  max-height: 300px;
  overflow-y: auto;
  background: #f8f9fa;
  border-radius: 6px;
  padding: 15px;
}

.log-entry {
  display: flex;
  gap: 15px;
  padding: 8px 0;
  border-bottom: 1px solid #e9ecef;
  font-family: monospace;
  font-size: 13px;
}

.log-entry:last-child {
  border-bottom: none;
}

.log-entry.info {
  color: #0066cc;
}

.log-entry.success {
  color: #28a745;
}

.log-entry.error {
  color: #dc3545;
}

.log-time {
  font-weight: bold;
  min-width: 80px;
}

.log-type {
  font-weight: bold;
  min-width: 60px;
}

.manual-test {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.test-input {
  flex: 1;
  padding: 10px;
  border: 2px solid #e9ecef;
  border-radius: 8px;
  font-size: 14px;
}

.send-test-btn {
  background: linear-gradient(135deg, #00b894 0%, #00cec9 100%);
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.test-response {
  background: #f8f9fa;
  border-radius: 6px;
  padding: 15px;
}

.test-response h4 {
  margin-bottom: 10px;
  color: #333;
}

.test-response pre {
  background: #2d3748;
  color: #e2e8f0;
  padding: 15px;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 12px;
  line-height: 1.4;
}
</style> 