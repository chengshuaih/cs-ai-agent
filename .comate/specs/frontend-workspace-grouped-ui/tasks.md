# 前端工作台分组 UI 改造任务计划

- [✓] Task 1: 改造主页为两个并排主入口
    - 1.1: 修改 `Home.vue` 的功能入口区域，将 5 个独立功能卡片收敛为“对话助手”和“采集工作台”两个主卡片
    - 1.2: 将“对话助手”跳转目标设置为 `/dialog-workspace`
    - 1.3: 将“采集工作台”跳转目标设置为 `/collection-workspace`
    - 1.4: 调整主页入口样式，桌面端左右并排，窄屏端单列排列
    - 1.5: 将主页右上角调试按钮说明调整为“设置与调试”

- [✓] Task 2: 新增对话助手聚合容器页
    - 2.1: 新建 `DialogWorkspace.vue`
    - 2.2: 增加统一顶部栏，包含返回主页、标题和说明
    - 2.3: 增加侧边栏 tab，包含“视觉知识问答”和“视觉采集智能体”
    - 2.4: 在内容区复用 `VisionQaApp` 和 `VisionAgentApp`，并传入 `embedded`
    - 2.5: 增加桌面端侧边栏布局和窄屏响应式布局

- [✓] Task 3: 新增采集工作台聚合容器页
    - 3.1: 新建 `CollectionWorkspace.vue`
    - 3.2: 增加统一顶部栏，包含返回主页、标题和说明
    - 3.3: 增加侧边栏 tab，包含“采集辅助规划”“采集项目工作区”“报告中心”
    - 3.4: 在内容区复用 `CollectionPlanner`、`ProjectWorkspace` 和 `ReportCenter`，并传入 `embedded`
    - 3.5: 增加桌面端侧边栏布局和窄屏响应式布局

- [✓] Task 4: 增加现有子页面嵌入模式
    - 4.1: 为 `VisionQaApp.vue` 增加 `embedded` prop，嵌入态隐藏原 header 并调整容器样式
    - 4.2: 为 `VisionAgentApp.vue` 增加 `embedded` prop，嵌入态隐藏原 header 并调整容器样式
    - 4.3: 为 `CollectionPlanner.vue` 增加 `embedded` prop，嵌入态隐藏原 header 并调整容器样式
    - 4.4: 为 `ProjectWorkspace.vue` 增加 `embedded` prop，嵌入态隐藏原 header 并调整容器样式
    - 4.5: 为 `ReportCenter.vue` 增加 `embedded` prop，嵌入态隐藏原 header 并调整容器样式
    - 4.6: 保持旧路由直接访问时仍显示原页面 header 和返回主页按钮

- [✓] Task 5: 调整采集辅助规划表单布局
    - 5.1: 修改 `CollectionPlanner.vue` 的 body class，使其可根据是否已有 `plan` 切换布局
    - 5.2: 无结果时将采集任务表单居中显示
    - 5.3: 有结果时恢复表单与结果左右分栏
    - 5.4: 确保嵌入采集工作台时表单不贴左且结果区宽度正常
    - 5.5: 保持移动端单列布局

- [✓] Task 6: 增强设置与调试页面
    - 6.1: 在 `DebugPage.vue` 顶部新增环境设置概览区域
    - 6.2: 展示当前 `API_BASE_URL`
    - 6.3: 展示本地后端端口与前端环境变量配置提示
    - 6.4: 展示 MCP 本地启停说明
    - 6.5: 保留并整理已有连接测试、SSE 测试和手动测试能力

- [✓] Task 7: 更新路由配置
    - 7.1: 在 `router/index.js` 中导入 `DialogWorkspace.vue`
    - 7.2: 在 `router/index.js` 中导入 `CollectionWorkspace.vue`
    - 7.3: 新增 `/dialog-workspace` 路由
    - 7.4: 新增 `/collection-workspace` 路由
    - 7.5: 保留 `/vision-qa`、`/vision-agent`、`/collection-planner`、`/project`、`/report-center` 旧路由

- [✓] Task 8: 构建并检查前端结果
    - 8.1: 在 `cs-ai-agent-fronted` 目录执行 `npm run build`
    - 8.2: 如出现 Vue 模板或脚本错误，定位并修复
    - 8.3: 检查主页两个主入口、两个聚合容器、旧路由兼容和设置页展示是否满足设计
    - 8.4: 记录验证结果和未执行的人工 UI 检查项
