# ChatGPT Style Home Design

## User Story
用户希望主界面也和聊天页一样简单、大气，整体学习 ChatGPT 的浅色、克制、工作台式界面；两个主要应用入口保留但不要过大，调试工具从主入口卡片移到右上角齿轮。

## Acceptance Criteria
- 主界面去掉大面积紫色渐变背景和大型 hero 卡片感，改为浅色简洁布局。
- 顶部有低调导航栏：左侧显示 `CS AI Agent`，右上角有齿轮样式调试入口。
- 调试工具不再作为第三个大卡片展示，点击右上角齿轮仍进入 `/debug`。
- `AI 恋爱大师` 和 `AI 超级智能体` 仍可点击进入原页面，但入口更紧凑、按钮/卡片更小。
- 不改变路由、不改变两个聊天页、不改变后端接口。
- 移动端不横向溢出，主入口可正常点击。

## Non-goals
- 不新增历史会话、用户登录、设置弹窗、模型切换或新页面。
- 不修改 `router/index.js` 路由定义。
- 不修改 `LoveApp.vue`、`ManusApp.vue`、`DebugPage.vue`。
- 不引入新的 UI 组件库或图标库。

## Contracts
- 前置条件：`Home.vue` 通过 `navigateTo(path)` 跳转页面。
- 后置条件：`navigateTo('/love-app')`、`navigateTo('/manus-app')`、`navigateTo('/debug')` 仍保留并可从 UI 触达。
- 不变量：路由路径 `/love-app`、`/manus-app`、`/debug` 不变。
- 失败模式：如果 CSS 导致齿轮按钮不可见、入口点击区域过小或移动端溢出，需要调整布局。

## Scope
- 主实现文件：`cs-ai-agent-fronted/src/views/Home.vue`
- 直接调用方：`cs-ai-agent-fronted/src/router/index.js` 只读确认，不修改。
- 排除路径：后端 `src/main/java/**`、聊天页、调试页、路由文件。

## Design Direction
- 页面背景：浅灰/白色，和聊天页风格一致。
- 顶栏：低调白底或透明白，细边框；左侧品牌名，右侧齿轮按钮。
- 主体：居中窄宽度布局，标题克制，不做营销式 hero。
- 应用入口：两个紧凑卡片或列表项，图标变小，文字层级清晰，hover 轻微反馈。
- 调试入口：右上角齿轮按钮，视觉弱化但可发现。

## Verification Evidence
- 运行 `npm run build` 验证 Vue 构建通过。
- 人工打开 `/` 检查：主界面简洁大气、两个应用入口可点击、齿轮进入 `/debug`、移动端无横向溢出。
