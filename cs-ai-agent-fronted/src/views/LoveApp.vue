<template>
  <div class="love-app">
    <div class="header">
      <div class="container">
        <div class="header-content">
          <button class="back-btn" @click="goBack">
            <span>←</span> 返回主页
          </button>
          <h1>💕 AI 恋爱大师</h1>
          <div class="chat-info">
            <span class="chat-id">会话ID: {{ chatId }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <div class="container">
      <div class="chat-container">
        <div class="chat-messages" ref="chatMessages">
          <div 
            v-for="(message, index) in messages" 
            :key="index" 
            :class="['message', message.type]"
          >
            <div class="message-content">
              <div class="message-avatar">
                {{ message.type === 'user' ? '👤' : '💕' }}
              </div>
              <div class="message-bubble">
                <div class="message-text" v-html="message.content"></div>
                <div class="message-time">{{ message.time }}</div>
              </div>
            </div>
          </div>
          <div v-if="isTyping && !messages.some(msg => msg.type === 'ai' && msg.isStreaming)" class="message ai">
            <div class="message-content">
              <div class="message-avatar">💕</div>
              <div class="message-bubble">
                <div class="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="chat-input">
          <div class="input-container">
            <textarea
              v-model="inputMessage"
              @keydown.enter.prevent="sendMessage"
              placeholder="请输入您的问题..."
              rows="3"
              class="message-input"
            ></textarea>
            <button 
              @click="sendMessage" 
              :disabled="!inputMessage.trim() || isTyping"
              class="send-btn"
            >
              发送
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { createSSEConnection, pollChat, testConnection } from '../utils/api'

export default {
  name: 'LoveApp',
  data() {
    return {
      chatId: '',
      messages: [],
      inputMessage: '',
      isTyping: false,
      eventSource: null,
      connectionMethod: 'sse', // 'sse' 或 'poll'
      isConnected: false
    }
  },
  mounted() {
    this.generateChatId()
    this.addWelcomeMessage()
    this.testBackendConnection()
  },
  beforeUnmount() {
    if (this.eventSource) {
      this.eventSource.close()
    }
  },
  methods: {
    generateChatId() {
      this.chatId = 'love_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
    },
    
    addWelcomeMessage() {
      const welcomeMessage = {
        type: 'ai',
        content: '您好！我是您的AI恋爱大师 💕<br><br>我可以帮助您：<br>• 分析情感问题<br>• 提供恋爱建议<br>• 改善沟通技巧<br>• 处理关系冲突<br><br>请告诉我您需要什么帮助？',
        time: this.getCurrentTime()
      }
      this.messages.push(welcomeMessage)
    },
    
    async testBackendConnection() {
      try {
        const isConnected = await testConnection()
        this.isConnected = isConnected
        if (!isConnected) {
          // 静默切换到轮询模式，不显示提示
          this.connectionMethod = 'poll'
        }
      } catch (error) {
        console.error('连接测试失败:', error)
        // 静默切换到轮询模式，不显示提示
        this.connectionMethod = 'poll'
      }
    },
    
    addSystemMessage(content) {
      const systemMessage = {
        type: 'system',
        content: content,
        time: this.getCurrentTime()
      }
      this.messages.push(systemMessage)
    },
    
    async sendMessage() {
      if (!this.inputMessage.trim() || this.isTyping) return
      
      // 清除之前所有流式消息的标记
      this.messages.forEach(msg => {
        if (msg.isStreaming) {
          msg.isStreaming = false
        }
      })
      
      const userMessage = {
        type: 'user',
        content: this.inputMessage,
        time: this.getCurrentTime()
      }
      
      this.messages.push(userMessage)
      const message = this.inputMessage
      this.inputMessage = ''
      this.isTyping = true
      
      // 滚动到底部
      this.$nextTick(() => {
        this.scrollToBottom()
      })
      
      try {
        if (this.connectionMethod === 'sse') {
          await this.connectSSE(message)
        } else {
          await this.connectPoll(message)
        }
      } catch (error) {
        console.error('连接失败:', error)
        this.addErrorMessage()
      }
    },
    
    async connectSSE(message) {
      if (this.eventSource) {
        this.eventSource.close()
      }
      
      const url = `http://localhost:8123/api/ai/love_app/chat/sse?message=${encodeURIComponent(message)}&chatId=${this.chatId}`
      
      console.log('开始SSE连接:', url) // 调试日志
      
      // 设置超时，如果5秒内没有收到数据，显示备用消息
      const timeoutId = setTimeout(() => {
        console.log('SSE连接超时，显示备用消息') // 调试日志
        if (this.isTyping) {
          this.addFallbackMessage()
          this.isTyping = false
        }
      }, 5000)
      
      this.eventSource = createSSEConnection(
        url,
        (data) => {
          console.log('SSE收到数据:', data) // 调试日志
          
          // 清除超时
          clearTimeout(timeoutId)
          
          if (data === '[DONE]') {
            console.log('SSE连接完成') // 调试日志
            this.isTyping = false
            this.eventSource.close()
            this.eventSource = null
            
            // 标记当前流式消息完成
            const currentAiMessage = this.messages.find(msg => msg.type === 'ai' && msg.isStreaming)
            if (currentAiMessage) {
              currentAiMessage.isStreaming = false
              console.log('流式消息完成，最终内容:', currentAiMessage.content) // 调试日志
              // 确保内容不为空
              if (currentAiMessage.content.trim() === '') {
                currentAiMessage.content = '抱歉，没有收到有效回复。'
              }
            }
            return
          }
          
          // 过滤掉SSE格式，只保留实际内容
          let cleanData = data
          if (data.startsWith('data: ')) {
            cleanData = data.replace('data: ', '')
          }
          
          console.log('清理后的SSE数据:', cleanData) // 调试日志
          
          // 如果内容为空或只包含空白字符，跳过
          if (!cleanData || cleanData === '') {
            console.log('SSE数据为空，跳过') // 调试日志
            return
          }
          
          // 实时显示AI回复
          this.addStreamingMessage(cleanData)
        },
        (error) => {
          console.error('SSE错误:', error)
          clearTimeout(timeoutId) // 清除超时
          this.isTyping = false
          this.eventSource.close()
          this.eventSource = null
          this.addErrorMessage()
          // 静默切换到轮询模式，不显示提示
          this.connectionMethod = 'poll'
        },
        () => {
          console.log('SSE连接完成回调') // 调试日志
          clearTimeout(timeoutId) // 清除超时
          this.isTyping = false
        }
      )
    },
    
    async connectPoll(message) {
      const url = `/ai/love_app/chat/sse?message=${encodeURIComponent(message)}&chatId=${this.chatId}`
      
      try {
        await pollChat(
          url,
                    (data) => {
            console.log('轮询收到数据:', data) // 调试日志
            if (data && data !== '[DONE]') {
              // 过滤掉SSE格式，只保留实际内容
              let cleanData = data
              if (data.startsWith('data: ')) {
                cleanData = data.replace('data: ', '')
              }
              
              // 进一步清理可能的多余格式
              cleanData = cleanData.replace(/^data:\s*/g, '').trim()
              
              console.log('轮询清理后数据:', cleanData) // 调试日志
              
              // 如果内容为空或只包含空白字符，跳过
              if (!cleanData || cleanData === '') {
                console.log('轮询数据为空，跳过') // 调试日志
                return
              }
              
              this.addStreamingMessage(cleanData)
            }
          },
          (error) => {
            console.error('轮询错误:', error)
            this.addErrorMessage()
          },
          () => {
            this.isTyping = false
          }
        )
      } catch (error) {
        console.error('轮询连接失败:', error)
        this.addErrorMessage()
      }
    },
    
    addStreamingMessage(content) {
      console.log('收到流式内容:', content) // 调试日志
      
      // 最终清理数据，确保没有SSE格式残留
      let finalContent = content
        .replace(/^data:\s*/g, '')  // 移除开头的data:
        .replace(/\s*data:\s*/g, '') // 移除中间的data:
        .replace(/\s+/g, ' ')        // 合并多个空格
        .trim()
      
      console.log('清理后内容:', finalContent) // 调试日志
      
      // 如果清理后内容为空，跳过
      if (!finalContent) {
        console.log('内容为空，跳过') // 调试日志
        return
      }
      
      // 检查是否已有正在进行的AI回复
      let currentAiMessage = this.messages.find(msg => msg.type === 'ai' && msg.isStreaming)
      
      if (!currentAiMessage) {
        console.log('创建新的AI消息') // 调试日志
        // 创建新的AI消息，初始内容为空
        currentAiMessage = {
          type: 'ai',
          content: '',
          time: this.getCurrentTime(),
          isStreaming: true
        }
        this.messages.push(currentAiMessage)
      }
      
      // 直接累积内容到当前正在进行的消息
      currentAiMessage.content += finalContent
      
      console.log('累积后消息内容:', currentAiMessage.content) // 调试日志
      
      this.$nextTick(() => {
        this.scrollToBottom()
      })
    },
    
    // 添加一个备用方法，确保即使没有流式数据也能显示消息
    addFallbackMessage() {
      console.log('添加备用消息') // 调试日志
      const fallbackMessage = {
        type: 'ai',
        content: '正在思考中...',
        time: this.getCurrentTime(),
        isStreaming: false
      }
      this.messages.push(fallbackMessage)
      this.$nextTick(() => {
        this.scrollToBottom()
      })
    },
    

    

    
    addErrorMessage() {
      // 不显示错误消息，静默处理
      this.isTyping = false
    },
    
    getCurrentTime() {
      const now = new Date()
      return now.toLocaleTimeString('zh-CN', { 
        hour: '2-digit', 
        minute: '2-digit' 
      })
    },
    
    scrollToBottom() {
      const chatMessages = this.$refs.chatMessages
      if (chatMessages) {
        chatMessages.scrollTop = chatMessages.scrollHeight
      }
    },
    
    goBack() {
      this.$router.push('/')
    }
  }
}
</script>

<style scoped>
.love-app {
  min-height: 100vh;
  background: #f8f9fa;
}

.header {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
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

.chat-info {
  font-size: 14px;
  opacity: 0.9;
}

.chat-container {
  max-width: 800px;
  margin: 30px auto;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.chat-messages {
  height: 500px;
  overflow-y: auto;
  padding: 20px;
  background: #f8f9fa;
}

.message {
  margin-bottom: 20px;
}

.message.user {
  text-align: right;
}

.message.ai {
  text-align: left;
}

.message-content {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.message.user .message-content {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
}

.message-bubble {
  max-width: 70%;
  background: white;
  padding: 12px 16px;
  border-radius: 18px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.message.user .message-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.message.system .message-bubble {
  background: #fff3cd;
  border: 1px solid #ffeaa7;
  color: #856404;
  font-style: italic;
}

.message.streaming .message-bubble {
  background: #e8f5e8;
  border: 1px solid #c3e6c3;
  color: #155724;
}

.message-text {
  line-height: 1.5;
  word-wrap: break-word;
}

.message-time {
  font-size: 12px;
  opacity: 0.7;
  margin-top: 8px;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  align-items: center;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ccc;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

.chat-input {
  padding: 20px;
  background: white;
  border-top: 1px solid #eee;
}

.input-container {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.message-input {
  flex: 1;
  border: 2px solid #e9ecef;
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 14px;
  resize: none;
  outline: none;
  transition: border-color 0.3s ease;
}

.message-input:focus {
  border-color: #667eea;
}

.send-btn {
  background: linear-gradient(135deg, #ff6b6b 0%, #ee5a24 100%);
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 12px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
}

.send-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    gap: 15px;
    text-align: center;
  }
  
  .chat-container {
    margin: 20px;
  }
  
  .chat-messages {
    height: 400px;
  }
  
  .message-bubble {
    max-width: 85%;
  }
}
</style> 