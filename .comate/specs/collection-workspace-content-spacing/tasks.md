# 采集工作台内容区间距优化任务计划

- [✓] Task 1: 调整项目工作区嵌入态间距
    - 1.1: 修改 `ProjectWorkspace.vue` 的嵌入态 `.body` 样式
    - 1.2: 桌面端为嵌入态内容区增加更舒适的左右 padding
    - 1.3: 移动端为嵌入态内容区设置较小 padding
    - 1.4: 保持 `/project` 旧路由独立访问布局不变

- [✓] Task 2: 调整报告中心嵌入态间距
    - 2.1: 修改 `ReportCenter.vue` 的嵌入态 `.body` 样式
    - 2.2: 桌面端为嵌入态内容区增加更舒适的左右 padding
    - 2.3: 移动端为嵌入态内容区设置较小 padding
    - 2.4: 保持 `/report-center` 旧路由独立访问布局不变

- [✓] Task 3: 构建并记录验证结果
    - 3.1: 在 `cs-ai-agent-fronted` 目录执行 `npm run build`
    - 3.2: 如出现 CSS 或 Vue 编译错误，定位并修复
    - 3.3: 记录构建结果和需要人工检查的 UI 页面
