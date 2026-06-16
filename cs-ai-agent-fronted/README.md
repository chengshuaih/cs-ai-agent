# CS AI Agent Frontend

这是一个基于Vue3的AI智能体前端应用，包含两个主要的AI聊天应用。

## 功能特性

### 🏠 主页
- 美观的渐变背景设计
- 两个AI应用卡片展示
- 响应式布局，支持移动端

### 💕 AI 恋爱大师
- 聊天室风格的界面
- 用户消息在右侧，AI回复在左侧
- 自动生成会话ID
- 通过SSE实时显示对话内容
- 支持输入框和发送按钮

### 🤖 AI 超级智能体
- 与恋爱大师相同的聊天界面
- 不同的主题色彩（蓝色渐变）
- 通过SSE实时显示对话内容
- 多功能AI助手服务

## 技术栈

- **Vue 3** - 前端框架
- **Vue Router 4** - 路由管理
- **Axios** - HTTP请求库
- **Vite** - 构建工具
- **SSE (Server-Sent Events)** - 实时通信

## 项目结构

```
cs-ai-agent-fronted/
├── src/
│   ├── views/
│   │   ├── Home.vue          # 主页
│   │   ├── LoveApp.vue       # AI恋爱大师
│   │   └── ManusApp.vue      # AI超级智能体
│   ├── router/
│   │   └── index.js          # 路由配置
│   ├── App.vue               # 根组件
│   ├── main.js               # 入口文件
│   └── style.css             # 全局样式
├── package.json              # 项目配置
├── vite.config.js            # Vite配置
└── index.html                # HTML模板
```

## 安装和运行

### 1. 安装依赖
```bash
npm install
```

### 2. 启动开发服务器
```bash
npm run dev
```

### 3. 构建生产版本
```bash
npm run build
```

### 4. 预览生产版本
```bash
npm run preview
```

## 后端接口

项目需要配合SpringBoot后端使用，接口地址前缀：`http://localhost:8123/api`

### 接口列表
- `GET /ai/love_app/chat/sse` - AI恋爱大师聊天接口（SSE）
- `GET /ai/manus/chat` - AI超级智能体聊天接口（SSE）

## 主要特性

### 实时通信
- 使用SSE技术实现实时对话
- 支持流式响应显示
- 自动处理连接错误和重连

### 用户体验
- 打字指示器动画
- 自动滚动到最新消息
- 响应式设计，支持各种屏幕尺寸
- 美观的渐变色彩和阴影效果

### 会话管理
- 自动生成唯一会话ID
- 支持多个并发会话
- 消息时间戳显示

## 浏览器兼容性

- Chrome 60+
- Firefox 55+
- Safari 12+
- Edge 79+

## 开发说明

### 添加新的AI应用
1. 在`src/views/`目录下创建新的Vue组件
2. 在`src/router/index.js`中添加路由配置
3. 在主页`src/views/Home.vue`中添加应用卡片

### 自定义样式
- 全局样式在`src/style.css`中定义
- 组件特定样式使用`<style scoped>`
- 支持CSS变量和现代CSS特性

### 错误处理
- SSE连接错误自动处理
- 用户友好的错误提示
- 网络异常时的降级处理

## 许可证

MIT License 