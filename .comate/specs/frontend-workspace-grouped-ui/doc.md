# 前端工作台分组 UI 改造设计

## 需求分类

本次属于前端 UI/交互改造任务，涉及 Vue3 前端多个已有页面的入口组织、页面容器和局部样式调整。按 Spec Driven Development 流程执行，先产出设计文档，确认后再生成任务清单，最后实施。

## 背景与现状

当前前端主页在 `cs-ai-agent-fronted/src/views/Home.vue` 中直接展示 5 个独立功能入口：

- 视觉知识问答：`/vision-qa`
- 视觉采集智能体：`/vision-agent`
- 采集辅助规划：`/collection-planner`
- 采集项目工作区：`/project`
- 报告中心：`/report-center`

路由统一在 `cs-ai-agent-fronted/src/router/index.js` 中配置。现有页面已经具备核心功能，但入口较分散，测试 UI 前需要先把信息架构收敛成两个主工作区：

- 前两个对话类功能合并为一个主入口，通过侧边栏切换。
- 后三个采集/项目/报告类功能合并为一个主入口，通过侧边栏切换。

同时，采集辅助规划页面 `cs-ai-agent-fronted/src/views/CollectionPlanner.vue` 当前 `.body` 使用 `grid-template-columns: 360px 1fr`，当无结果时表单位于左侧，视觉上偏左，需要在聚合工作台场景下更居中。设置入口当前只体现为主页右上角调试按钮，功能感较弱，需要补充更实用的信息与操作入口。

## 用户故事

作为机器视觉智能问答与采集辅助系统的使用者，我希望主页只保留两个清晰的大入口，并在进入后通过侧边栏切换同类功能，这样可以减少入口数量、降低理解成本，并在一个工作区内完成相关任务。

作为采集规划使用者，我希望采集辅助规划表单在页面中更居中、更稳定，而不是在没有结果时贴近左侧，以便填写任务时视觉焦点更明确。

作为本地开发/测试使用者，我希望设置页能展示当前后端地址、运行提示和调试入口，而不是只有一个调试按钮，方便理解当前环境。

## 总体方案

采用用户确认的方案 A：复用现有页面组件，新增两个聚合容器页，尽量不改动后端接口和现有核心业务逻辑。

### 新的信息架构

主页改为两个并排主按钮：

1. 对话助手
   - 包含：视觉知识问答、视觉采集智能体
   - 目标路由：`/dialog-workspace`
   - 在内部侧边栏切换两个子功能

2. 采集工作台
   - 包含：采集辅助规划、采集项目工作区、报告中心
   - 目标路由：`/collection-workspace`
   - 在内部侧边栏切换三个子功能

设置入口仍保留在主页右上角，但由单纯调试感增强为“设置与调试”。可复用/扩展现有 `DebugPage.vue`，避免新增复杂设置系统。

### 页面容器策略

新增两个容器页：

- `cs-ai-agent-fronted/src/views/DialogWorkspace.vue`
- `cs-ai-agent-fronted/src/views/CollectionWorkspace.vue`

容器页通过侧边栏控制当前 tab，并使用已有页面组件作为内容区域。

考虑到现有页面 `VisionQaApp.vue`、`VisionAgentApp.vue`、`CollectionPlanner.vue`、`ProjectWorkspace.vue`、`ReportCenter.vue` 都自带 header 和“返回主页”按钮，为避免嵌套后出现双顶部栏，需要给这些页面增加可选的嵌入模式参数：

```vue
<VisionQaApp embedded />
<VisionAgentApp embedded />
<CollectionPlanner embedded />
<ProjectWorkspace embedded />
<ReportCenter embedded />
```

各子页面新增 `props: { embedded: Boolean }` 后：

- `embedded === false`：保留原有 header 和返回主页按钮，兼容旧路由直接访问。
- `embedded === true`：隐藏原页面 header，让容器页统一提供顶部栏与侧边栏。

这样可以减少重复代码，同时保持旧路由可用，降低改造风险。

## 详细设计

### 1. 主页入口改造

目标文件：

- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/Home.vue`

修改内容：

- 将当前 `apps-panel` 中 5 个独立 `button.app-card` 合并为 2 个主卡片。
- 卡片左右并排，窄屏时上下排列。
- 左侧卡片跳转 `/dialog-workspace`，标题建议为“对话助手”。
- 右侧卡片跳转 `/collection-workspace`，标题建议为“采集工作台”。
- 卡片内部用 feature 标签展示包含的子功能，避免用户不知道功能去哪了。

预期结构示例：

```vue
<section class="apps-panel grouped" aria-label="功能入口">
  <button class="app-card main-card" type="button" @click="navigateTo('/dialog-workspace')">
    <span class="app-icon" aria-hidden="true">D</span>
    <span class="app-content">
      <span class="app-title">对话助手</span>
      <span class="app-description">在一个界面切换知识问答与智能体对话</span>
      <span class="app-features">
        <span class="feature">视觉知识问答</span>
        <span class="feature">视觉采集智能体</span>
      </span>
    </span>
    <span class="app-arrow" aria-hidden="true">→</span>
  </button>

  <button class="app-card main-card" type="button" @click="navigateTo('/collection-workspace')">
    <span class="app-icon" aria-hidden="true">C</span>
    <span class="app-content">
      <span class="app-title">采集工作台</span>
      <span class="app-description">集中处理采集规划、项目归档与报告查看</span>
      <span class="app-features">
        <span class="feature">采集辅助规划</span>
        <span class="feature">项目工作区</span>
        <span class="feature">报告中心</span>
      </span>
    </span>
    <span class="app-arrow" aria-hidden="true">→</span>
  </button>
</section>
```

样式调整：

- `.main-content` 适当加宽到 `1080px` 左右。
- `.apps-panel` 在桌面端使用 `grid-template-columns: repeat(2, minmax(0, 1fr))`。
- 主卡片高度更大，强化两个主入口并排。
- 移动端 `@media (max-width: 760px)` 回退为单列。

### 2. 新增对话助手容器页

目标文件：

- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/DialogWorkspace.vue`

职责：

- 提供统一顶部栏：返回主页、标题“对话助手”、当前子功能说明。
- 提供左侧 sidebar tab：
  - 视觉知识问答
  - 视觉采集智能体
- 内容区域渲染已有组件：
  - `<VisionQaApp embedded />`
  - `<VisionAgentApp embedded />`

核心结构示例：

```vue
<template>
  <div class="workspace-shell">
    <header class="workspace-header">
      <button class="back-btn" @click="$router.push('/')">← 返回主页</button>
      <div>
        <h1>对话助手</h1>
        <p>通过侧边栏切换知识问答与智能体对话。</p>
      </div>
    </header>

    <main class="workspace-layout">
      <aside class="workspace-sidebar">
        <button
          v-for="item in tabs"
          :key="item.key"
          :class="['tab-button', { active: activeTab === item.key }]"
          @click="activeTab = item.key"
        >
          <span class="tab-title">{{ item.title }}</span>
          <span class="tab-desc">{{ item.description }}</span>
        </button>
      </aside>

      <section class="workspace-content">
        <VisionQaApp v-if="activeTab === 'qa'" embedded />
        <VisionAgentApp v-else embedded />
      </section>
    </main>
  </div>
</template>
```

边界条件：

- 切换 tab 时组件会重新挂载，当前会话状态会随组件卸载重置；这是本次可接受行为，因为需求只要求侧边栏切换，没有要求跨 tab 保留会话。
- 如果后续需要保留对话，可在容器中使用 `<KeepAlive>` 包裹动态组件，但本次不作为必须项，避免额外复杂度。

### 3. 新增采集工作台容器页

目标文件：

- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/CollectionWorkspace.vue`

职责：

- 提供统一顶部栏：返回主页、标题“采集工作台”、说明。
- 提供左侧 sidebar tab：
  - 采集辅助规划
  - 采集项目工作区
  - 报告中心
- 内容区域复用已有组件：
  - `<CollectionPlanner embedded />`
  - `<ProjectWorkspace embedded />`
  - `<ReportCenter embedded />`

核心结构示例：

```vue
<section class="workspace-content">
  <CollectionPlanner v-if="activeTab === 'planner'" embedded />
  <ProjectWorkspace v-else-if="activeTab === 'project'" embedded />
  <ReportCenter v-else embedded />
</section>
```

边界条件：

- 报告中心、项目工作区在 mounted 时会加载数据。切换 tab 后重新挂载会重新加载，符合用户预期。
- 不改变后端接口路径和数据结构。
- 不改变已有独立路由页面能力。

### 4. 子页面嵌入模式改造

目标文件：

- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/VisionQaApp.vue`
- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/VisionAgentApp.vue`
- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/CollectionPlanner.vue`
- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/ProjectWorkspace.vue`
- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/ReportCenter.vue`

处理逻辑：

- 为每个组件增加 `embedded` 布尔 prop。
- 模板中原 header 增加 `v-if="!embedded"`。
- 根容器 class 追加嵌入态 class，例如：

```vue
<div :class="['vision-qa-app', { embedded }]">
```

- CSS 增加嵌入态样式，避免嵌入容器内重复全屏背景和多余外边距：

```css
.vision-qa-app.embedded {
  min-height: auto;
  background: transparent;
}

.vision-qa-app.embedded .container {
  width: 100%;
  max-width: none;
  padding: 0;
}
```

对各页面影响：

- 独立访问 `/vision-qa`、`/vision-agent`、`/collection-planner`、`/project`、`/report-center` 时仍保持原来效果。
- 从新容器进入时隐藏子页面 header，避免双返回按钮。

### 5. 路由改造

目标文件：

- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/router/index.js`

新增 import：

```js
import DialogWorkspace from '../views/DialogWorkspace.vue'
import CollectionWorkspace from '../views/CollectionWorkspace.vue'
```

新增路由：

```js
{
  path: '/dialog-workspace',
  name: 'DialogWorkspace',
  component: DialogWorkspace
},
{
  path: '/collection-workspace',
  name: 'CollectionWorkspace',
  component: CollectionWorkspace
}
```

保留旧路由不删除，原因：

- 降低回归风险。
- 允许用户直接访问某个老页面。
- 避免其他已有链接立即失效。

### 6. 采集辅助规划表单居中

目标文件：

- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/CollectionPlanner.vue`

当前问题：

- `.body { display: grid; grid-template-columns: 360px 1fr; }` 导致无结果时表单在左侧。

修改策略：

- 无结果时使用单列居中布局。
- 有结果时再使用左右分栏。
- 可通过根节点 class 或 body class 根据 `plan` 状态控制：

```vue
<div :class="['container body', { 'has-result': plan }]">
```

样式建议：

```css
.body {
  display: grid;
  grid-template-columns: minmax(320px, 520px);
  justify-content: center;
  gap: 20px;
}

.body.has-result {
  grid-template-columns: minmax(320px, 380px) minmax(0, 1fr);
  justify-content: stretch;
}
```

嵌入采集工作台时保持同样逻辑，避免表单贴左。

### 7. 设置功能增强

目标文件：

- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/DebugPage.vue`
- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/Home.vue`

调整方向：

- 首页右上角按钮从单纯齿轮图标保留，但 title/aria-label 改为“设置与调试”。
- `DebugPage.vue` 顶部新增“环境设置概览”区域，展示：
  - 当前后端地址：`API_BASE_URL`
  - 本地开发提示：后端默认端口 `8123`，如被占用可改端口并同步前端环境变量。
  - MCP 提示：本地可通过 `spring.ai.mcp.client.enabled=false/true` 控制是否启用 MCP。
  - 调试入口说明：连接测试、SSE 测试、手动测试。

不新增持久化设置、不新增后端接口、不保存用户偏好，避免扩大范围。

## 数据流路径

### 对话助手路径

1. 用户从主页点击“对话助手”。
2. 路由进入 `/dialog-workspace`。
3. 容器页显示侧边栏。
4. 用户选择“视觉知识问答”或“视觉采集智能体”。
5. 容器页挂载对应已有组件。
6. 已有组件继续通过 `API_BASE_URL` 调用：
   - `/ai/vision/chat/sse`
   - `/ai/vision-agent/chat`

### 采集工作台路径

1. 用户从主页点击“采集工作台”。
2. 路由进入 `/collection-workspace`。
3. 容器页显示三项侧边栏。
4. 用户选择子功能。
5. 容器页挂载对应已有组件。
6. 已有组件继续调用原后端接口：
   - `/vision/collection/plan`
   - `/vision/project/list`
   - `/vision/project/{id}`
   - `/vision/report/list`
   - `/vision/report/pdf`

### 设置与调试路径

1. 用户点击主页右上角设置按钮。
2. 路由进入 `/debug`。
3. 页面展示环境概览和调试工具。
4. 用户可执行已有连接测试和 SSE 测试。

## 受影响文件

### 修改文件

1. `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/Home.vue`
   - 修改主页入口卡片，从 5 个独立入口改为 2 个并排主入口。
   - 调整 `.apps-panel` 和 `.app-card` 样式。
   - 设置按钮说明增强。

2. `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/router/index.js`
   - 新增两个聚合容器页路由。
   - 保留旧路由。

3. `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/VisionQaApp.vue`
   - 增加 `embedded` prop。
   - 嵌入态隐藏 header。
   - 增加嵌入态样式。

4. `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/VisionAgentApp.vue`
   - 增加 `embedded` prop。
   - 嵌入态隐藏 header。
   - 增加嵌入态样式。

5. `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/CollectionPlanner.vue`
   - 增加 `embedded` prop。
   - 嵌入态隐藏 header。
   - 表单无结果时居中，有结果时分栏。
   - 增加嵌入态样式。

6. `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/ProjectWorkspace.vue`
   - 增加 `embedded` prop。
   - 嵌入态隐藏 header。
   - 增加嵌入态样式。

7. `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/ReportCenter.vue`
   - 增加 `embedded` prop。
   - 嵌入态隐藏 header。
   - 增加嵌入态样式。

8. `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/DebugPage.vue`
   - 新增环境设置概览区域。
   - 展示当前 `API_BASE_URL`。
   - 补充本地启动/MCP/调试说明。

### 新增文件

1. `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/DialogWorkspace.vue`
   - 对话类功能聚合容器。

2. `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/CollectionWorkspace.vue`
   - 采集类功能聚合容器。

## 非目标

本次不做以下内容：

- 不修改后端接口。
- 不改变 AI 对话、采集规划、项目、报告的数据结构。
- 不新增用户登录、权限、持久化偏好设置。
- 不删除旧路由。
- 不重构为全局布局系统或引入 UI 组件库。
- 不改变 MCP 的实际启停逻辑，只在设置页展示说明。

## 边界条件与异常处理

- 旧路由仍可访问，避免外部链接失效。
- 新容器页侧边栏切换时，如果组件重新挂载导致内部临时状态丢失，属于本次接受范围。
- 后端未启动时，已有页面的错误处理逻辑继续生效。
- 设置页展示的运行说明仅作为信息展示，不修改本地配置文件。
- 响应式布局需保证窄屏下侧边栏变为顶部横向/卡片式导航，避免内容被挤压。

## 验收标准

1. 主页只展示两个主功能入口，且桌面端左右并排排列。
2. 点击左侧入口进入对话助手页，可通过侧边栏切换“视觉知识问答”和“视觉采集智能体”。
3. 点击右侧入口进入采集工作台页，可通过侧边栏切换“采集辅助规划”“采集项目工作区”“报告中心”。
4. 直接访问旧路由 `/vision-qa`、`/vision-agent`、`/collection-planner`、`/project`、`/report-center` 仍能显示对应页面并有返回主页按钮。
5. 聚合容器内不出现重复 header 或重复返回主页按钮。
6. 采集辅助规划在无结果时表单居中；生成结果后再使用左右分栏。
7. 设置与调试页展示当前 `API_BASE_URL`、本地启动提示、MCP 启停说明和现有调试能力。
8. 前端构建通过：在 `cs-ai-agent-fronted` 目录执行 `npm run build` 成功。

## 验证计划

- 静态检查：确认路由 import 和组件 props 无语法错误。
- 构建验证：执行前端 `npm run build`。
- 手动验证：
  - 打开主页确认两个主入口布局。
  - 进入对话助手切换两个 tab。
  - 进入采集工作台切换三个 tab。
  - 打开旧路由确认兼容。
  - 打开设置与调试页确认信息展示。

## 风险与控制

- 风险：把现有页面嵌入容器后，原页面 scoped 样式的 `.container`、`.header` 等可能与容器视觉不协调。
  - 控制：只通过 `embedded` class 做局部覆盖，不改全局样式。

- 风险：新增两个容器页导致重复布局样式。
  - 控制：本次接受少量重复，避免提前抽象；后续稳定后再考虑公共 Shell 组件。

- 风险：侧边栏切换导致组件状态重置。
  - 控制：本次不承诺跨 tab 保留状态；如测试后需要，可追加 `KeepAlive`。

- 风险：设置功能需求可能继续扩大。
  - 控制：本次仅做环境概览和调试说明，不做持久化设置。