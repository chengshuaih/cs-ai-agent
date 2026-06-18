import axios from 'axios'

const API_BASE_URL = 'http://localhost:8123/api'

// 创建axios实例
const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  }
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    console.log('发送请求:', config.url, config.params || config.data)
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    console.log('收到响应:', response.status, response.data)
    return response
  },
  error => {
    console.error('响应错误:', error.response?.status, error.message)
    return Promise.reject(error)
  }
)

// SSE连接函数
export const createSSEConnection = (url, onMessage, onError, onComplete) => {
  console.log('尝试建立SSE连接:', url)
  
  // 首先尝试使用EventSource
  try {
    const eventSource = new EventSource(url)
    
    eventSource.onopen = () => {
      console.log('EventSource连接已建立')
    }
    
    eventSource.onmessage = (event) => {
      console.log('EventSource收到消息:', event.data)
      if (onMessage) onMessage(event.data)
    }
    
    eventSource.onerror = (error) => {
      if (eventSource.readyState === EventSource.CLOSED) {
        console.log('EventSource连接已关闭')
        if (onComplete) onComplete()
        return
      }
      console.error('EventSource连接错误:', error)
      if (onError) onError(error)
      eventSource.close()
    }
    
    return eventSource
  } catch (error) {
    console.log('EventSource失败，尝试使用fetch:', error)
    
    // 如果EventSource失败，使用fetch作为备选
    return createFetchStream(url, onMessage, onError, onComplete)
  }
}

// 使用fetch创建流式连接
const createFetchStream = (url, onMessage, onError, onComplete) => {
  console.log('使用fetch建立流式连接:', url)
  
  fetch(url, {
    method: 'GET',
    headers: {
      'Accept': 'text/event-stream',
      'Cache-Control': 'no-cache',
    }
  })
  .then(response => {
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    console.log('fetch连接成功，开始读取流')
    
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    
    const readStream = () => {
      reader.read().then(({ done, value }) => {
        if (done) {
          console.log('fetch流读取完成')
          if (onComplete) onComplete()
          return
        }
        
        const chunk = decoder.decode(value)
        console.log('fetch收到数据块:', chunk)
        
        // 解析数据块
        const lines = chunk.split('\n')
        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.slice(6)
            if (onMessage) onMessage(data)
          }
        }
        
        // 继续读取
        readStream()
      }).catch(error => {
        console.error('fetch流读取错误:', error)
        if (onError) onError(error)
      })
    }
    
    // 开始读取流
    readStream()
  })
  .catch(error => {
    console.error('fetch连接失败:', error)
    if (onError) onError(error)
  })
  
  // 返回一个模拟的EventSource对象
  return {
    close: () => {
      console.log('fetch流连接关闭')
    }
  }
}

// 轮询方式作为备选
export const pollChat = async (url, onMessage, onError, onComplete) => {
  try {
    console.log('开始轮询请求:', url)
    const response = await api.get(url)
    
    if (response.data) {
      console.log('轮询收到数据:', response.data)
      
      // 如果数据是字符串，尝试解析SSE格式
      if (typeof response.data === 'string') {
        const lines = response.data.split('\n')
        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.slice(6)
            if (data && data !== '[DONE]') {
              onMessage(data)
            }
          }
        }
      } else {
        // 如果是其他格式，直接发送
        onMessage(response.data)
      }
    }
    
    onComplete()
  } catch (error) {
    console.error('轮询请求失败:', error)
    onError(error)
  }
}

// 测试连接
export const testConnection = async () => {
  try {
    const response = await api.get('/health')
    return response.status === 200
  } catch (error) {
    console.error('连接测试失败:', error)
    return false
  }
}

export default api 