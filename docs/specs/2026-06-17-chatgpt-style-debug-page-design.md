# ChatGPT Style Debug Page Design

## User Story
用户希望调试页面也与主页和两个聊天页统一为 ChatGPT 风格：简洁、大气、浅色、工具感克制，同时保留现有全部调试能力。

## Acceptance Criteria
- 调试页整体改为浅色、简洁、与其他页面统一的 ChatGPT 风格。
- 顶部使用低调导航栏，保留返回主页和调试页面标题。
- 保留现有三个后端连接测试按钮：基本连接、恋爱大师 SSE、智能体 SSE。
- 保留网络请求日志列表、测试结果列表、手动测试输入和响应内容展示。
- 不修改任何请求 URL、测试方法、日志/结果数据结构、返回主页逻辑。
- 调试按钮更紧凑，模块视觉更轻，不再是厚重大卡片样式。
- 日志和响应区域使用浅灰代码/日志块样式，便于阅读。
- 移动端不横向溢出，模块纵向排列。

## Non-goals
- 不新增调试功能。
- 不删除任何现有测试入口。
- 不修改后端、路由、主页、聊天页。
- 不引入新的 UI 组件库或图标库。
- 不把调试页改成弹窗或抽屉。

## Contracts
- 前置条件：`DebugPage.vue` 当前通过 `goBack()` 返回主页，通过各测试方法发起请求并写入 `testResults` / `networkLogs`。
- 后置条件：所有现有调试方法仍可由 UI 触发。
- 不变量：`goBack`、`testBasicConnection`、`testLoveAppSSE`、`testManusSSE`、`sendTestMessage`、`addTestResult`、`addLog` 行为不变。
- 失败模式：如果 CSS 导致日志不可读、按钮不可点击、响应内容横向溢出或移动端布局溢出，需要调整样式。

## Scope
- 主实现文件：`cs-ai-agent-fronted/src/views/DebugPage.vue`
- 直接调用方：`cs-ai-agent-fronted/src/router/index.js` 只读确认，不修改。
- 排除路径：后端 `src/main/java/**`、主页、聊天页、路由文件。

## Design Direction
- 页面背景：浅灰/白色，与主页和聊天页一致。
- 顶栏：低调白底或半透明白，细边框；左侧返回按钮，中间或左侧显示“调试页面”。
- 主体：居中最大宽度布局，分区清晰。
- 模块：使用细边框、浅色背景、小标题和紧凑间距。
- 按钮：小号工具按钮，hover/focus 清晰。
- 日志/响应区：浅灰代码块风格，等宽字体，长文本换行或可滚动。

## Verification Evidence
- 运行 `npm run build` 验证 Vue 构建通过。
- 人工打开 `/debug` 检查：页面风格统一、按钮可点击、日志/结果/响应区域可读、返回主页可用、移动端无横向溢出。
