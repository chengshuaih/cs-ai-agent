# ChatGPT Style Debug Page Plan

关联规格：`docs/specs/2026-06-17-chatgpt-style-debug-page-design.md`

## Scope

- 触达仓库：`cs-ai-agent-fronted`
- 触达平台：Web / Vue
- 主实现文件：
  - `cs-ai-agent-fronted/src/views/DebugPage.vue`
- 只读参考：
  - `cs-ai-agent-fronted/src/router/index.js`
- 排除：后端 Java、主页、聊天页、路由文件、全局样式。

## Existing Behavior To Preserve

- `goBack()` 仍通过 `this.$router.push('/')` 返回主页。
- `testBasicConnection()` 请求 `http://localhost:8123/api/health` 的逻辑不变。
- `testLoveAppSSE()` 请求恋爱大师 SSE 的逻辑不变。
- `testManusSSE()` 请求智能体 SSE 的逻辑不变。
- `sendTestMessage()` 手动测试逻辑不变。
- `addTestResult()`、`addLog()`、`testResults`、`networkLogs`、`testMessage`、`testResponse` 数据结构不变。
- 不新增调试能力，不删除任何现有测试入口。

## User-visible Text Boundary

当前项目没有本地化资源体系，`DebugPage.vue` 已存在硬编码中文 UI 文案。本次允许复用和轻微调整 `DebugPage.vue` 现有文案，但不引入新的多语言机制，不修改其他页面文案。

## Task 1: 重构调试页顶部和页面框架

- Status: pending
- User Story: 用户希望调试页也和其他页面一样统一为 ChatGPT 风格，顶部简洁克制。
- Acceptance Criteria:
  - 顶部使用低调浅色导航栏。
  - 保留返回主页按钮和调试页面标题。
  - 页面背景改为浅灰/白色，与主页和聊天页统一。
  - 不修改 `goBack()` 行为。
- Non-goals:
  - 不把调试页改成弹窗、抽屉或右侧面板。
  - 不修改路由。
- Files / modules:
  - `DebugPage.vue`
  - `router/index.js` 只读确认。
- Discovery before coding:
  - 确认模板顶部结构和 `goBack` 当前绑定。
- Contracts:
  - 返回按钮必须继续调用 `goBack`。
  - 标题语义仍为调试页面。
- Implementation notes:
  - 使用和其他页面相近的浅色顶栏、细边框和居中内容宽度。
  - 避免大面积渐变和厚重阴影。
- Tests / QA:
  - `npm run build`
  - 人工打开 `/debug`，点击返回主页。
- Human evidence needed:
  - `/debug` 页面顶部截图或人工确认。
- Reviewer focus:
  - 是否误改 `goBack`。
  - 是否风格与其他页面统一。
- QA Evaluator evidence:
  - 构建通过。
  - 人工返回主页观察。

## Task 2: 重做测试区、日志区、手动测试区视觉

- Status: pending
- User Story: 用户希望调试功能保持不变，但视觉更简洁、工具按钮更紧凑、日志更可读。
- Acceptance Criteria:
  - 三个测试按钮仍存在且触发原方法。
  - 测试结果列表仍渲染 `testResults`。
  - 网络请求日志仍渲染 `networkLogs`。
  - 手动测试输入框、发送测试按钮、响应内容展示仍存在。
  - 模块视觉为浅色 section，按钮更小更工具化。
  - 日志和响应区域使用浅灰代码/日志块风格。
- Non-goals:
  - 不新增测试按钮。
  - 不删除或合并已有调试模块。
  - 不修改任何请求 URL 或方法实现。
- Files / modules:
  - `DebugPage.vue`
- Discovery before coding:
  - 确认每个按钮当前绑定的方法名。
  - 确认结果、日志、响应区域 class 使用。
- Contracts:
  - `@click="testBasicConnection"`、`@click="testLoveAppSSE"`、`@click="testManusSSE"`、`@click="sendTestMessage"` 不变。
  - `v-model="testMessage"` 不变。
  - `{{ testResponse }}` 展示不变。
- Implementation notes:
  - 可以调整 template 容器层级和 class 名，但不改变脚本区方法和数据字段。
  - 日志长文本需要换行或水平可滚动，不能撑破页面。
- Tests / QA:
  - `npm run build`
  - 人工点击三个测试按钮和手动测试按钮，确认结果区域有反馈。
- Human evidence needed:
  - 调试按钮、日志、响应区截图或人工观察。
- Reviewer focus:
  - 是否误改调试请求逻辑。
  - 是否保留所有现有测试入口。
  - 日志/响应区域是否可读且不溢出。
- QA Evaluator evidence:
  - 构建通过。
  - 人工点击调试按钮观察。

## Task 3: 移动端和样式收尾

- Status: pending
- User Story: 调试页在窄屏下也应可用，日志和按钮不能横向撑破页面。
- Acceptance Criteria:
  - 窄屏下模块纵向排列。
  - 按钮可换行或纵向排列。
  - 日志和响应文本不会导致页面整体横向滚动。
  - scoped CSS 中无明显失效旧样式残留。
- Non-goals:
  - 不抽公共组件。
  - 不创建全局设计系统。
- Files / modules:
  - `DebugPage.vue`
- Discovery before coding:
  - 检查最终 class 是否都被模板使用。
- Contracts:
  - 不引入新依赖。
  - 不修改脚本区调试逻辑。
- Implementation notes:
  - 使用 `@media (max-width: 768px)` 处理窄屏。
  - 对日志和响应块使用 `overflow-wrap`、`white-space` 或 `overflow-x` 控制。
- Tests / QA:
  - `npm run build`
  - 人工窄屏检查无横向溢出。
- Human evidence needed:
  - 窄屏观察或截图。
- Reviewer focus:
  - 是否有横向滚动风险。
  - 是否有旧样式残留导致冲突。
- QA Evaluator evidence:
  - 构建通过。
  - 人工视觉确认。

## Risks And Attribution

- `PRODUCT_SCOPE`: 用户只要求调试页视觉统一，不能顺手改调试逻辑、请求 URL 或新增测试功能。
- `ENVIRONMENT`: 若前端依赖缺失或 Vite 构建失败且不是代码问题，归因为环境问题。
- `TEST_SCRIPT`: 当前项目没有前端单元测试脚本，主要自动验证为 `npm run build`。
- `BLOCKER`: 若用户要求更具体的调试信息架构或新增调试能力，需要另开需求。

## Human Evidence Request

实现后需要用户或人工浏览器观察确认：

- `/debug` 页面是否符合“类似 ChatGPT，简洁大气”。
- 返回主页按钮是否可用。
- 三个测试按钮是否仍可点击。
- 手动测试输入和发送测试是否仍可用。
- 日志、测试结果、响应内容区域是否可读。
- 窄屏下是否无横向溢出。

## Execution Handoff Gate

计划确认后的下一步只能是 subagent 能力发现 gate。

主 controller 不得直接写 RED 测试、产品代码、格式化或验证命令。

若无法调用独立 implementer / reviewer / QA evaluator，报告 SUBAGENT_TOOL_UNAVAILABLE 并停止。
