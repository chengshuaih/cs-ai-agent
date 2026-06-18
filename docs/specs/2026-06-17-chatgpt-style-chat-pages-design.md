# ChatGPT Style Chat Pages Design

## User Story
用户希望 AI 恋爱大师和 AI 超级智能体两个聊天页面更简单、大气，接近 ChatGPT 的对话体验，同时保持现有功能、接口和流式输出行为不变。

## Acceptance Criteria
- `LoveApp.vue` 和 `ManusApp.vue` 都采用统一的简洁聊天布局。
- 顶部从大面积渐变横幅改为低调顶栏，保留返回主页、页面标题、会话 ID。
- 聊天区居中，减少卡片感和强装饰，AI 消息更像正文文本流，用户消息保留右侧气泡。
- 输入区固定在聊天页面底部区域，输入、发送、禁用、typing 状态保持原逻辑。
- 不修改后端接口、不修改 SSE/轮询逻辑、不修改 chatId 生成逻辑、不修改欢迎消息内容语义。
- 前端构建通过。

## Non-goals
- 不新增功能入口、快捷按钮、历史会话列表、模型切换、文件上传或设置页。
- 不调整后端 Java 代码、提示词、MCP、DashScope 配置。
- 不改路由、不改 Home 页、不改 Debug 页。
- 不引入新的 UI 组件库或图标库。

## Contracts
- 前置条件：`LoveApp.vue` 和 `ManusApp.vue` 当前已有可用的发送、流式接收、typing、错误处理、返回主页逻辑。
- 后置条件：两个页面视觉风格统一，功能事件和 API URL 与当前保持一致。
- 不变量：`sendMessage`、`connectSSE`、`connectPoll`、`addStreamingMessage`、`goBack` 的行为契约不变。
- 失败模式：若 CSS 导致输入区遮挡消息或移动端溢出，应调整布局约束；若 Vue 构建失败，必须修复后再完成。

## Scope
- 主实现文件：`cs-ai-agent-fronted/src/views/LoveApp.vue`
- 直接同类页面：`cs-ai-agent-fronted/src/views/ManusApp.vue`
- 共享调用链：`cs-ai-agent-fronted/src/utils/api.js` 只读参考，不修改。
- 排除路径：后端 `src/main/java/**`、路由、主页、调试页。

## Design Direction
- 页面背景：浅灰或白色，减少强色渐变。
- 顶栏：固定高度、白底、细边框或轻阴影，标题居中/左侧清晰显示。
- 消息区：最大宽度约 800 到 900px，居中；AI 消息无厚重卡片，用户消息使用克制深色/中性色气泡。
- 输入区：居中最大宽度，白底圆角输入框，发送按钮简洁明确。
- 移动端：消息宽度、输入框、顶栏内容不溢出。

## Verification Evidence
- 运行 `npm run build` 验证 Vue 构建通过。
- 人工打开 `/love-app` 和 `/manus-app` 检查：发送消息、流式回复、typing、返回主页、移动端宽度表现。
