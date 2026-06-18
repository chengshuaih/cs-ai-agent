# ChatGPT Style Home Plan

关联规格：`docs/specs/2026-06-17-chatgpt-style-home-design.md`

## Scope

- 触达仓库：`cs-ai-agent-fronted`
- 触达平台：Web / Vue
- 主实现文件：
  - `cs-ai-agent-fronted/src/views/Home.vue`
- 只读参考：
  - `cs-ai-agent-fronted/src/router/index.js`
- 排除：后端 Java、聊天页、调试页、路由文件、全局样式。

## Existing Behavior To Preserve

- `navigateTo(path)` 仍通过 `this.$router.push(path)` 跳转。
- `/love-app`、`/manus-app`、`/debug` 路由路径不变。
- `AI 恋爱大师` 和 `AI 超级智能体` 仍可从主界面点击进入。
- 调试工具仍可从主界面进入，但入口从大卡片改为右上角齿轮。
- 不新增登录、历史会话、设置弹窗、模型切换或文件上传。

## User-visible Text Boundary

当前项目没有本地化资源体系，`Home.vue` 已存在硬编码中文 UI 文案。本次允许复用和轻微调整 `Home.vue` 现有文案，但不引入新的多语言机制，不修改其他页面文案。

## Task 1: 重构主界面顶部与调试入口

- Status: pending
- User Story: 用户希望主界面更像 ChatGPT，顶部简洁，调试工具放到右上角齿轮。
- Acceptance Criteria:
  - 顶部使用低调浅色导航栏。
  - 左侧显示 `CS AI Agent`。
  - 右上角有齿轮样式按钮，点击进入 `/debug`。
  - 调试工具不再作为第三个大卡片展示。
- Non-goals:
  - 不创建设置弹窗。
  - 不新增调试菜单层级。
  - 不修改 `/debug` 页面本身。
- Files / modules:
  - `Home.vue`
  - `router/index.js` 只读确认。
- Discovery before coding:
  - 确认 `Home.vue` 当前 `navigateTo('/debug')` 可复用。
  - 确认路由 `/debug` 已存在。
- Contracts:
  - 齿轮按钮必须调用 `navigateTo('/debug')`。
  - 不改 `navigateTo` 方法签名和实现。
- Implementation notes:
  - 使用文本齿轮符号或 CSS 按钮，不引入图标库。
  - 齿轮按钮需要有可见 hover/focus 状态。
- Tests / QA:
  - `npm run build`
  - 人工点击齿轮，确认进入 `/debug`。
- Human evidence needed:
  - 主界面截图或人工确认齿轮入口可见且可点击。
- Reviewer focus:
  - 是否误删 `/debug` 可达路径。
  - 是否把齿轮做成不可发现或不可点击。
- QA Evaluator evidence:
  - 构建通过。
  - 人工点击齿轮观察。

## Task 2: 重做主界面内容区和应用入口

- Status: pending
- User Story: 用户希望主界面应用入口更简洁大气，按钮不那么大。
- Acceptance Criteria:
  - 去掉紫色渐变大背景和大型 hero 风格。
  - 主体使用浅灰/白色背景，与聊天页风格统一。
  - 两个应用入口改为紧凑卡片或列表项。
  - 保留 `AI 恋爱大师` 与 `AI 超级智能体` 的标题、说明和点击跳转。
  - 应用入口视觉大小明显小于当前大卡片。
- Non-goals:
  - 不新增第三个主应用入口。
  - 不新增应用排序、搜索或筛选。
- Files / modules:
  - `Home.vue`
- Discovery before coding:
  - 检查当前卡片结构和 class，确定哪些样式可替换。
- Contracts:
  - 恋爱入口仍调用 `navigateTo('/love-app')`。
  - 智能体入口仍调用 `navigateTo('/manus-app')`。
- Implementation notes:
  - 保持 Vue Options API。
  - 可调整 template 结构和 scoped CSS。
  - 使用低饱和色、细边框、浅阴影或无阴影。
- Tests / QA:
  - `npm run build`
  - 人工点击两个入口，确认进入对应页面。
- Human evidence needed:
  - `/` 页面桌面截图；可选窄屏截图。
- Reviewer focus:
  - 是否保留两个主应用入口。
  - 是否避免过强渐变、过大按钮或营销式 hero。
- QA Evaluator evidence:
  - 构建通过。
  - 人工点击两个入口观察。

## Task 3: 移动端和可维护性收尾

- Status: pending
- User Story: 主界面在窄屏下也应简洁可用，入口和齿轮不溢出。
- Acceptance Criteria:
  - 窄屏下顶部内容不横向溢出。
  - 两个入口可正常换行或纵向排列。
  - 齿轮按钮仍可点击。
  - scoped CSS 中无明显失效的大段旧样式残留。
- Non-goals:
  - 不做完整响应式设计系统。
  - 不抽公共组件。
- Files / modules:
  - `Home.vue`
- Discovery before coding:
  - 检查最终 class 是否都被模板使用。
- Contracts:
  - 页面仍为单文件组件 scoped CSS。
  - 不引入新依赖。
- Implementation notes:
  - 用 `@media (max-width: 768px)` 处理窄屏。
  - 文本需要可换行，按钮不应固定过宽。
- Tests / QA:
  - `npm run build`
  - 人工窄屏检查无横向滚动。
- Human evidence needed:
  - 窄屏观察或截图。
- Reviewer focus:
  - 是否有未使用旧样式残留。
  - 是否存在移动端横向滚动风险。
- QA Evaluator evidence:
  - 构建通过。
  - 人工视觉确认。

## Risks And Attribution

- `PRODUCT_SCOPE`: 用户只要求主界面视觉调整和调试入口位置变化，不能顺手改路由、聊天页或后端。
- `ENVIRONMENT`: 若前端依赖缺失或 Vite 构建失败且不是代码问题，归因为环境问题。
- `TEST_SCRIPT`: 当前项目没有前端单元测试脚本，主要自动验证为 `npm run build`。
- `BLOCKER`: 若用户要求像素级复刻某个 ChatGPT 版本但未提供截图，需要补参考。

## Human Evidence Request

实现后需要用户或人工浏览器观察确认：

- `/` 页面是否符合“类似 ChatGPT，简洁大气”。
- 两个应用入口是否可点击并进入对应页面。
- 右上角齿轮是否可点击并进入 `/debug`。
- 窄屏下是否无横向溢出。

## Execution Handoff Gate

计划确认后的下一步只能是 subagent 能力发现 gate。

主 controller 不得直接写 RED 测试、产品代码、格式化或验证命令。

若无法调用独立 implementer / reviewer / QA evaluator，报告 SUBAGENT_TOOL_UNAVAILABLE 并停止。
