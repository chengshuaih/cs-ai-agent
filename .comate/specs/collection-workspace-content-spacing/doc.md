# 采集工作台内容区间距优化设计

## 需求分类

本次属于前端轻量 UI 视觉优化，目标是修复采集工作台中“采集项目工作区”和“报告中心”嵌入态内容离左边过近的问题。

## 背景与现状

用户在浏览采集工作台时反馈：

- “新建项目”区域离左边太近，不好看。
- “报告中心”也存在同样问题。

已定位相关实现：

- 聚合容器：`/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/CollectionWorkspace.vue`
- 项目工作区：`/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/ProjectWorkspace.vue`
- 报告中心：`/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/ReportCenter.vue`

当前问题来源：

- `ProjectWorkspace.vue` 和 `ReportCenter.vue` 在嵌入态下清除了 `.container` 的左右 padding：

```css
.workspace.embedded .container { width: 100%; padding-left: 0; padding-right: 0; }
.report-center.embedded .container { width: 100%; padding-left: 0; padding-right: 0; }
```

- 子页面 `.body` 虽有 padding，但在外层右侧内容卡片中视觉仍显贴边，尤其在大屏截图中左边留白不足。
- 采集辅助规划页刚完成表单居中优化，本次不应影响它。

## 用户故事

作为采集工作台用户，我希望项目工作区和报告中心的内容与右侧面板边缘保持舒适距离，这样页面视觉更平衡，不会显得内容贴边或拥挤。

## 方案选择

采用用户确认的方案 A：只修 `ProjectWorkspace.vue` 和 `ReportCenter.vue` 的嵌入态内容间距。

不修改外层 `CollectionWorkspace.vue` 的 `.workspace-content` padding，避免影响采集辅助规划页的表单居中效果。

## 技术方案

### 1. 项目工作区嵌入态间距

目标文件：

- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/ProjectWorkspace.vue`

修改内容：

- 为嵌入态 `.body` 设置更舒适的左右 padding。
- 桌面端建议：`padding: 32px 36px 48px`。
- 移动端仍保持较小边距，避免挤压内容。

预期样式：

```css
.workspace.embedded .body {
  padding: 32px 36px 48px;
}

@media (max-width: 820px) {
  .workspace.embedded .body {
    padding: 20px 16px 32px;
  }
}
```

### 2. 报告中心嵌入态间距

目标文件：

- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/ReportCenter.vue`

修改内容：

- 为嵌入态 `.body` 设置更舒适的左右 padding。
- 桌面端建议：`padding: 32px 36px 48px`。
- 移动端仍保持较小边距。

预期样式：

```css
.report-center.embedded .body {
  padding: 32px 36px 48px;
}

@media (max-width: 820px) {
  .report-center.embedded .body {
    padding: 20px 16px 32px;
  }
}
```

## 受影响文件

### 修改文件

1. `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/ProjectWorkspace.vue`
   - 增加嵌入态 body padding，改善“新建采集项目”区域左侧距离。

2. `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/ReportCenter.vue`
   - 增加嵌入态 body padding，改善报告列表/空态内容左侧距离。

## 非目标

本次不做以下内容：

- 不修改 `CollectionWorkspace.vue` 外层布局。
- 不修改采集辅助规划页。
- 不新增按钮、入口、跳转或交互状态。
- 不修改后端接口。
- 不调整项目工作区或报告中心的数据加载逻辑。
- 不改变旧路由独立访问时的布局。

## 边界条件

- 只在 `embedded` 模式下生效，直接访问 `/project` 和 `/report-center` 的旧页面布局保持原样。
- 移动端需要收窄 padding，避免输入框和表格被挤压。
- 报告中心表格宽度仍保持 `width: 100%`，只改变其父容器内边距。

## 验收标准

1. 在 `/collection-workspace` 中切换到“采集项目工作区”时，“新建采集项目”卡片不再贴近右侧内容面板左边缘。
2. 在 `/collection-workspace` 中切换到“报告中心”时，报告表格或空态提示不再贴近右侧内容面板左边缘。
3. “采集辅助规划”页不受本次改动影响。
4. 旧路由 `/project` 和 `/report-center` 直接访问时布局不发生明显变化。
5. 前端构建 `npm run build` 通过。

## 验证计划

- 执行前端构建：在 `cs-ai-agent-fronted` 目录运行 `npm run build`。
- 手动 UI 检查：
  - 打开 `/collection-workspace`。
  - 切换到“采集项目工作区”。
  - 切换到“报告中心”。
  - 对比内容区左侧留白是否更自然。

## 风险控制

- 本次只修改两个子页面的嵌入态样式，范围小。
- 不动外层工作台容器，避免影响已调好的采集辅助规划表单。
- 不改数据逻辑，构建通过即可证明模板和 CSS 语法有效。