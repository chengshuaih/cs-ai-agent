# 前端工作台分组 UI 改造完成总结

## 完成内容

本次按已确认的方案 A 完成前端 UI/交互改造：复用现有功能页，新增两个聚合工作台容器，并保持旧路由兼容。

## 主要变更

### 1. 主页入口收敛

修改文件：

- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/Home.vue`

完成内容：

- 将原 5 个独立功能入口改为 2 个主入口：
  - 对话助手：跳转 `/dialog-workspace`
  - 采集工作台：跳转 `/collection-workspace`
- 桌面端两个按钮左右并排，移动端单列排列。
- 每个主入口通过标签说明包含的子功能。
- 主页右上角按钮说明从调试工具调整为“设置与调试”。

### 2. 新增对话助手聚合页

新增文件：

- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/DialogWorkspace.vue`

完成内容：

- 增加统一顶部栏。
- 增加侧边栏切换：
  - 视觉知识问答
  - 视觉采集智能体
- 复用已有组件：
  - `VisionQaApp`
  - `VisionAgentApp`
- 通过 `embedded` 模式避免双 header。

### 3. 新增采集工作台聚合页

新增文件：

- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/CollectionWorkspace.vue`

完成内容：

- 增加统一顶部栏。
- 增加侧边栏切换：
  - 采集辅助规划
  - 采集项目工作区
  - 报告中心
- 复用已有组件：
  - `CollectionPlanner`
  - `ProjectWorkspace`
  - `ReportCenter`
- 通过 `embedded` 模式避免双 header。

### 4. 子页面嵌入模式

修改文件：

- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/VisionQaApp.vue`
- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/VisionAgentApp.vue`
- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/CollectionPlanner.vue`
- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/ProjectWorkspace.vue`
- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/ReportCenter.vue`

完成内容：

- 增加 `embedded` prop。
- 嵌入态隐藏原页面 header。
- 嵌入态调整容器宽度、背景和边距。
- 独立路由访问时仍保留原 header 和返回主页按钮。

### 5. 采集辅助规划表单居中

修改文件：

- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/CollectionPlanner.vue`

完成内容：

- 无结果时表单居中显示。
- 有结果时切换为表单 + 结果左右分栏。
- 移动端保持单列布局。
- 嵌入采集工作台时不再贴左。

### 6. 设置与调试增强

修改文件：

- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/DebugPage.vue`

完成内容：

- 新增“环境设置概览”区域。
- 展示当前 `API_BASE_URL`。
- 展示后端默认端口和换端口提示。
- 展示 `VITE_API_BASE_URL` 配置提示。
- 展示 MCP 本地启停说明。
- 保留已有连接测试、SSE 测试和手动测试能力。

### 7. 路由更新

修改文件：

- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/router/index.js`

完成内容：

- 新增 `/dialog-workspace` 路由。
- 新增 `/collection-workspace` 路由。
- 保留旧路由：
  - `/vision-qa`
  - `/vision-agent`
  - `/collection-planner`
  - `/project`
  - `/report-center`
  - `/debug`

## 验证结果

已执行前端构建：

```bash
npm run build
```

执行目录：

```text
/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted
```

结果：

```text
✓ 96 modules transformed.
✓ built in 843ms
```

构建通过，未发现 Vue 模板或脚本编译错误。

## 未执行项

以下属于人工 UI 观察项，本次未通过浏览器截图验证：

- 主页两个主入口的实际视觉效果。
- 对话助手侧边栏切换体验。
- 采集工作台侧边栏切换体验。
- 采集辅助规划表单在真实浏览器宽度下的居中观感。
- 设置与调试页概览区的最终视觉观感。

## 任务状态

`.comate/specs/frontend-workspace-grouped-ui/tasks.md` 中 8 个顶层任务均已完成并勾选。