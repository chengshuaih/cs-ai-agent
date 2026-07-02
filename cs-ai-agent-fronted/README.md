# 机器视觉智能问答与采集辅助系统 - 前端

这是一个基于 Vue3 的前端应用，面向机器视觉学习、数据采集与实验规划场景，提供问答、采集辅助、报告中心与项目工作区。

## 功能特性

### 主页
- 系统功能入口卡片导航
- 响应式布局，支持移动端

### 视觉知识问答
- 聊天室风格界面，用户消息在右、AI 回复在左
- 自动生成会话 ID
- 通过 SSE 实时显示对话内容
- 支持机器视觉概念、评价指标与 RAG 知识检索

### 视觉采集智能体
- 与问答相同的聊天界面
- 理解任务后调用工具，生成采集规划、实验流程与报告
- 通过 SSE 实时显示对话内容

### 采集辅助规划
- 输入视觉任务，生成推荐点位、路线、采集清单与安全提示
- 一键导出 PDF 报告

### 报告中心 / 项目工作区
- 报告中心查看已生成报告记录
- 项目工作区按项目聚合采集计划、实验计划与报告

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
│   │   ├── Home.vue              # 主页
│   │   ├── VisionQaApp.vue       # 视觉知识问答
│   │   ├── VisionAgentApp.vue    # 视觉采集智能体
│   │   ├── CollectionPlanner.vue # 采集辅助规划
│   │   ├── ReportCenter.vue      # 报告中心
│   │   └── ProjectWorkspace.vue  # 采集项目工作区
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
- `GET /ai/vision/chat/sse` - 视觉知识问答接口（SSE）
- `GET /ai/vision/chat/sync` - 视觉知识问答接口（同步）
- `GET /ai/vision/chat/rag` - 视觉知识 RAG 问答接口
- `GET /ai/vision-agent/chat` - 视觉采集智能体接口（SSE）
- `POST /vision/collection/plan` - 生成采集规划
- `POST /vision/report/pdf` - 生成 PDF 报告
- `GET /vision/report/list` - 报告列表
- `POST /vision/project`、`GET /vision/project/list`、`GET /vision/project/{id}` - 项目工作区

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