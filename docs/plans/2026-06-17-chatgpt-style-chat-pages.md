# ChatGPT Style Chat Pages Plan

关联规格：`docs/specs/2026-06-17-chatgpt-style-chat-pages-design.md`

## Scope

- 触达仓库：`cs-ai-agent-fronted`
- 触达平台：Web / Vue
- 主实现文件：
  - `cs-ai-agent-fronted/src/views/LoveApp.vue`
  - `cs-ai-agent-fronted/src/views/ManusApp.vue`
- 只读参考：`cs-ai-agent-fronted/src/utils/api.js`
- 排除：后端 Java、MCP、DashScope、路由、主页、调试页。

## Existing Behavior To Preserve

- `LoveApp.vue` 保留 `/api/ai/love_app/chat/sse` 调用、`chatId` 生成、SSE/轮询 fallback、typing、返回主页、欢迎消息、消息流拼接逻辑。
- `ManusApp.vue` 保留 `/api/ai/manus/chat` 调用、`chatId` 生成、SSE/轮询 fallback、typing、返回主页、欢迎消息、消息流拼接逻辑。
- 不新增操作入口，不改变发送按钮禁用条件，不改变 Enter 发送行为。

## Task 1: 统一 ChatGPT 风格页面框架

- Status: pending
- User Story: 用户希望两个聊天页更简单大气，视觉接近 ChatGPT，同时保持功能不变。
- Acceptance Criteria:
  - 两页顶栏改为低调白底/浅色顶栏。
  - 页面主体使用居中聊天布局，不再是彩色大横幅 + 卡片容器。
  - 会话 ID 弱化展示但仍可见。
- Non-goals:
  - 不改路由和页面入口。
  - 不新增侧边栏、历史会话、设置按钮。
- Files / modules:
  - `LoveApp.vue`
  - `ManusApp.vue`
- Discovery before coding:
  - 对比两页模板差异，确认只替换 class 结构和样式，不触碰 API 方法。
- Contracts:
  - `goBack` 仍由返回按钮调用。
  - `chatId` 仍显示当前会话 ID。
- Implementation notes:
  - 保持现有 Vue Options API 结构。
  - 可调整 template 的容器层级和 class 名，但不改变方法名。
- Tests / QA:
  - `npm run build`
  - 人工打开 `/love-app`、`/manus-app` 检查布局。
- Human evidence needed:
  - 浏览器截图或人工确认两页视觉符合“简单大气 / 类 ChatGPT”。
- Reviewer focus:
  - 是否误改业务方法或 API URL。
  - 是否只做页面结构与 CSS 改造。
- QA Evaluator evidence:
  - 构建通过日志。
  - 两页截图或人工观察。

## Task 2: 重做消息区和输入区视觉

- Status: pending
- User Story: 用户希望对话阅读体验更干净，输入区更像现代 AI 聊天界面。
- Acceptance Criteria:
  - AI 消息从厚重气泡改为更轻的正文块或浅色块。
  - 用户消息保留右侧气泡，但颜色克制。
  - typing indicator 不改变逻辑，仅视觉更简洁。
  - 输入区在底部区域稳定显示，移动端不溢出。
- Non-goals:
  - 不改变消息数据结构。
  - 不改变 `v-html` 使用方式。
  - 不新增 Markdown 渲染或代码高亮。
- Files / modules:
  - `LoveApp.vue`
  - `ManusApp.vue`
- Discovery before coding:
  - 检查两页现有 `.message-*`、`.chat-*`、`.input-*` class 使用，避免删除仍被 template 使用的样式。
- Contracts:
  - `isTyping` 控制逻辑不变。
  - `addStreamingMessage` 追加消息行为不变。
  - 输入框 `v-model` 和 `@keydown.enter.prevent` 不变。
- Implementation notes:
  - 使用 CSS 控制最大宽度、滚动区域、底部输入区域。
  - 保证长文本换行，避免消息遮挡输入框。
- Tests / QA:
  - `npm run build`
  - 浏览器人工发送一条消息，确认流式显示、完成状态、滚动到底部。
- Human evidence needed:
  - `/love-app` 和 `/manus-app` 各一张消息展示截图。
- Reviewer focus:
  - 是否造成移动端横向滚动。
  - 是否造成输入区遮挡消息。
  - 是否误删流式状态样式。
- QA Evaluator evidence:
  - 构建通过。
  - 人工发送消息观察通过。

## Task 3: 清理重复视觉 CSS 并保持两页一致

- Status: pending
- User Story: 两个聊天页应有一致的视觉语言，减少维护成本。
- Acceptance Criteria:
  - 两页的布局、间距、颜色、输入区样式基本一致。
  - 保留各自标题和头像符号差异。
  - 不引入新依赖。
- Non-goals:
  - 不抽公共组件。
  - 不创建全局 CSS 重构。
- Files / modules:
  - `LoveApp.vue`
  - `ManusApp.vue`
- Discovery before coding:
  - 确认两页脚本区最新差异，避免把 LoveApp 的 SSE 收尾修复误覆盖到 Manus 或反向覆盖。
- Contracts:
  - 只允许替换视觉 CSS 和必要 template class。
  - 两页脚本逻辑维持现状。
- Implementation notes:
  - 可以让两页 scoped CSS 内容高度一致。
  - 避免使用过强渐变、过多装饰和大面积单一高饱和色。
- Tests / QA:
  - `npm run build`
  - 人工检查桌面宽度和窄屏宽度。
- Human evidence needed:
  - 若用户有偏好的 ChatGPT 风格截图，可用于二次微调；否则以规格中的简洁白底风格为准。
- Reviewer focus:
  - 是否有无关格式化或逻辑改动。
  - 是否有新增不可达/未使用样式导致混乱。
- QA Evaluator evidence:
  - 构建结果和人工视觉确认。

## Risks And Attribution

- `PRODUCT_SCOPE`: 用户只要求页面更简单大气，不能顺手新增聊天历史、设置、模型切换等功能。
- `ENVIRONMENT`: 若本地前端依赖缺失或 Vite 构建失败且不是代码问题，归因为环境问题。
- `TEST_SCRIPT`: 当前项目没有前端单元测试脚本，主要自动验证为 `npm run build`。
- `BLOCKER`: 若要求像素级复刻某截图但没有参考图，需要用户补充参考。

## Human Evidence Request

实现后需要用户或人工浏览器观察确认：

- `/love-app` 页面视觉是否符合“简单大气 / 类 ChatGPT”。
- `/manus-app` 页面视觉是否符合“简单大气 / 类 ChatGPT”。
- 两页发送消息、流式回复、返回主页是否可用。

## Execution Handoff Gate

计划确认后的下一步只能是 subagent 能力发现 gate。

主 controller 不得直接写 RED 测试、产品代码、格式化或验证命令。

若无法调用独立 implementer / reviewer / QA evaluator，报告 SUBAGENT_TOOL_UNAVAILABLE 并停止。
