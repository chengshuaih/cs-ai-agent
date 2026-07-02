<template>
  <div :class="['vision-qa-app', { embedded }]">
    <div v-if="!embedded" class="header">
      <div class="container">
        <div class="header-content">
          <button class="back-btn" @click="goBack">
            <span>←</span> 返回主页
          </button>
          <h1>视觉知识问答</h1>
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
                {{ message.type === 'user' ? '我' : 'AI' }}
              </div>
              <div class="message-bubble">
                <div class="message-text" v-if="message.type === 'ai'" v-html="renderSafe(message.content)"></div>
                <div class="message-text" v-else>{{ message.content }}</div>
                <div class="message-time">{{ message.time }}</div>
              </div>
            </div>
          </div>
          <div v-if="isTyping && !messages.some(msg => msg.type === 'ai' && msg.isStreaming)" class="message ai">
            <div class="message-content">
              <div class="message-avatar">AI</div>
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
              placeholder="请输入机器视觉相关问题..."
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
import { createSSEConnection, pollChat, testConnection, API_BASE_URL } from '../utils/api'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

export default {
  name: 'VisionQaApp',
  props: {
    embedded: {
      type: Boolean,
      default: false
    }
  },
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
    // 安全渲染：Markdown → HTML，再 DOMPurify 净化
    renderSafe(content) {
      if (content == null) return ''
      return DOMPurify.sanitize(marked.parse(String(content)))
    },

    generateChatId() {
      this.chatId = 'vision_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
    },

    addWelcomeMessage() {
      const welcomeMessage = {
        type: 'ai',
        content: '您好！我是机器视觉知识问答助手。\n\n我可以帮助您：\n• 解释目标检测、图像分割、深度估计、三维重建等概念\n• 说明 IoU、mAP、mIoU 等评价指标\n• 介绍数据采集与标注规范\n• 推荐常见数据集与开源项目\n\n请告诉我您想了解什么？',
        time: this.getCurrentTime()
      }
      this.messages.push(welcomeMessage)
    },

    async testBackendConnection() {
      try {
        const isConnected = await testConnection()
        this.isConnected = isConnected
        if (!isConnected) {
          this.connectionMethod = 'poll'
        }
      } catch (error) {
        console.error('连接测试失败:', error)
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

      const url = `${API_BASE_URL}/ai/vision/chat/sse?message=${encodeURIComponent(message)}&chatId=${this.chatId}`

      const timeoutId = setTimeout(() => {
        if (this.isTyping) {
          this.isTyping = false
          if (this.eventSource) {
            this.eventSource.close()
            this.eventSource = null
          }
          this.finishStreamingMessage()
        }
      }, 60000)

      this.eventSource = createSSEConnection(
        url,
        (data) => {
          clearTimeout(timeoutId)

          if (data === '[DONE]') {
            this.isTyping = false
            this.eventSource.close()
            this.eventSource = null
            this.finishStreamingMessage()
            return
          }

          let cleanData = data
          if (data.startsWith('data: ')) {
            cleanData = data.replace('data: ', '')
          }

          if (!cleanData || cleanData === '') {
            return
          }

          this.addStreamingMessage(cleanData)
        },
        (error) => {
          console.error('SSE错误:', error)
          clearTimeout(timeoutId)
          this.isTyping = false
          this.eventSource.close()
          this.eventSource = null
          this.addErrorMessage()
          this.connectionMethod = 'poll'
        },
        () => {
          clearTimeout(timeoutId)
          this.isTyping = false
          this.finishStreamingMessage()
        }
      )
    },

    async connectPoll(message) {
      const url = `/ai/vision/chat/sse?message=${encodeURIComponent(message)}&chatId=${this.chatId}`

      try {
        await pollChat(
          url,
          (data) => {
            if (data && data !== '[DONE]') {
              let cleanData = data
              if (data.startsWith('data: ')) {
                cleanData = data.replace('data: ', '')
              }

              cleanData = cleanData.replace(/^data:\s*/g, '').trim()

              if (!cleanData || cleanData === '') {
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

    finishStreamingMessage() {
      const currentAiMessage = this.messages.find(msg => msg.type === 'ai' && msg.isStreaming)
      if (currentAiMessage) {
        currentAiMessage.isStreaming = false
        if (currentAiMessage.content.trim() === '') {
          currentAiMessage.content = '抱歉，没有收到有效回复。'
        }
      }
    },

    addStreamingMessage(content) {
      let finalContent = content
        .replace(/^data:\s*/g, '')
        .replace(/\s*data:\s*/g, '')

      if (!finalContent) {
        return
      }

      let currentAiMessage = this.messages.find(msg => msg.type === 'ai' && msg.isStreaming)

      if (!currentAiMessage) {
        currentAiMessage = {
          type: 'ai',
          content: '',
          time: this.getCurrentTime(),
          isStreaming: true
        }
        this.messages.push(currentAiMessage)
      }

      currentAiMessage.content += finalContent

      this.$nextTick(() => {
        this.scrollToBottom()
      })
    },

    addErrorMessage() {
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
.vision-qa-app {
  min-height: 100vh;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f7f7f8;
  color: #202123;
  overflow: hidden;
}

.vision-qa-app.embedded {
  min-height: 680px;
  height: 100%;
  background: transparent;
}

.vision-qa-app.embedded > .container:last-child {
  width: 100%;
  padding: 0;
}

.vision-qa-app.embedded .chat-container {
  max-width: none;
  border-radius: 0;
}

.container {
  width: 100%;
  max-width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
}

.header {
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.96);
  color: #202123;
  border-bottom: 1px solid #e5e5e5;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.header .container {
  max-width: 960px;
  padding: 0 24px;
}

.header-content {
  min-height: 64px;
  display: grid;
  grid-template-columns: minmax(120px, 1fr) auto minmax(120px, 1fr);
  align-items: center;
  gap: 16px;
}

.back-btn {
  justify-self: start;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: 1px solid #d9d9e3;
  color: #353740;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  line-height: 1.2;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.back-btn:hover {
  background: #f1f1f3;
  border-color: #c5c5d2;
}

.header h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.01em;
  white-space: nowrap;
}

.chat-info {
  justify-self: end;
  min-width: 0;
  max-width: 100%;
  color: #8e8ea0;
  font-size: 12px;
  text-align: right;
}

.chat-id {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.vision-qa-app > .container:last-child {
  flex: 1;
  min-height: 0;
  display: flex;
  justify-content: center;
}

.chat-container {
  width: 100%;
  max-width: 920px;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #ffffff;
}

.chat-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 32px 24px 24px;
  background: #ffffff;
  scroll-behavior: smooth;
}

.message {
  width: 100%;
  margin-bottom: 22px;
}

.message-content {
  max-width: 820px;
  margin: 0 auto;
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.message.user .message-content {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 30px;
  height: 30px;
  flex: 0 0 30px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  background: #f7f7f8;
  border: 1px solid #ececf1;
}

.message-bubble {
  max-width: min(72%, 640px);
  padding: 0;
  color: #202123;
  background: transparent;
  border: none;
  box-shadow: none;
}

.message.ai .message-bubble {
  padding: 12px 16px;
  background: #f7f7f8;
  border: 1px solid #ececf1;
  border-radius: 14px;
}

.message.user .message-bubble {
  padding: 10px 14px;
  color: #ffffff;
  background: #343541;
  border-radius: 18px;
}

.message.system .message-bubble {
  padding: 10px 14px;
  color: #6b4e16;
  background: #fff7df;
  border: 1px solid #f2dfaa;
  border-radius: 12px;
  font-style: italic;
}

.message.streaming .message-bubble {
  color: #202123;
}

.message-text {
  line-height: 1.65;
  font-size: 15px;
  word-break: break-word;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.message-time {
  margin-top: 6px;
  color: #8e8ea0;
  font-size: 11px;
  line-height: 1.2;
}

.message.user .message-time {
  color: rgba(255, 255, 255, 0.72);
}

.typing-indicator {
  min-height: 24px;
  display: flex;
  align-items: center;
  gap: 5px;
  padding-top: 4px;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #8e8ea0;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0.65); opacity: 0.35; }
  40% { transform: scale(1); opacity: 1; }
}

.chat-input {
  flex-shrink: 0;
  padding: 16px 24px 24px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.84) 0%, #ffffff 28%);
  border-top: 1px solid #f0f0f0;
}

.input-container {
  max-width: 820px;
  margin: 0 auto;
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 10px;
  background: #ffffff;
  border: 1px solid #d9d9e3;
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  box-sizing: border-box;
}

.message-input {
  flex: 1;
  min-width: 0;
  max-height: 160px;
  border: none;
  border-radius: 10px;
  padding: 8px 10px;
  color: #202123;
  background: transparent;
  font-size: 15px;
  line-height: 1.5;
  resize: none;
  outline: none;
  box-sizing: border-box;
}

.message-input::placeholder {
  color: #8e8ea0;
}

.send-btn {
  flex-shrink: 0;
  min-width: 64px;
  height: 40px;
  border: none;
  border-radius: 10px;
  background: #10a37f;
  color: #ffffff;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: background 0.2s ease, transform 0.2s ease;
}

.send-btn:hover:not(:disabled) {
  background: #0e8f70;
  transform: translateY(-1px);
}

.send-btn:disabled {
  background: #d9d9e3;
  color: #ffffff;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .header .container {
    padding: 0 12px;
  }

  .header-content {
    min-height: 58px;
    grid-template-columns: auto minmax(0, 1fr);
    grid-template-areas:
      "back title"
      "info info";
    gap: 6px 10px;
    padding: 8px 0;
  }

  .back-btn {
    grid-area: back;
    padding: 7px 10px;
    font-size: 13px;
  }

  .header h1 {
    grid-area: title;
    justify-self: end;
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    font-size: 16px;
  }

  .chat-info {
    grid-area: info;
    justify-self: stretch;
    text-align: right;
    font-size: 11px;
  }

  .chat-messages {
    padding: 22px 14px 18px;
  }

  .message-content {
    gap: 10px;
  }

  .message-avatar {
    width: 28px;
    height: 28px;
    flex-basis: 28px;
    font-size: 12px;
  }

  .message-bubble {
    max-width: calc(100% - 42px);
  }

  .message.user .message-bubble {
    max-width: 78%;
  }

  .chat-input {
    padding: 12px 12px 16px;
  }

  .input-container {
    gap: 8px;
    padding: 8px;
    border-radius: 14px;
  }

  .message-input {
    font-size: 14px;
  }

  .send-btn {
    min-width: 56px;
    height: 38px;
  }
}
</style>
