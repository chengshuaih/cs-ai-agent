# 采集辅助规划接入高德与 Pexels MCP 增强任务计划

- [✓] Task 1: 确认 MCP 工具回调能力与调用契约
    - 1.1: 检查 Spring AI `ToolCallback` 可用方法、工具名、输入格式和返回格式
    - 1.2: 确认高德 MCP 暴露的 POI/地点搜索工具名称与参数结构
    - 1.3: 确认 Pexels 图片 MCP 暴露的图片搜索工具名称与参数结构
    - 1.4: 记录不可用或格式不稳定时的降级策略，不修改业务逻辑

- [✓] Task 2: 增加采集点图片字段并保持现有评分逻辑
    - 2.1: 在 `CollectionSite` record 末尾新增 `imageUrls` 字段
    - 2.2: 更新 `CollectionSiteEvaluator` 中所有 `CollectionSite` 构造点，默认传入空图片列表
    - 2.3: 更新受影响测试断言，确保现有评分、排序、理由和风险提示不变
    - 2.4: 检查历史计划读取兼容性，必要时补充兼容处理或回归测试

- [✓] Task 3: 新增 MCP 工具服务适配层
    - 3.1: 新增 `VisionMcpToolService`，通过 `ObjectProvider<ToolCallbackProvider>` 获取 MCP 工具
    - 3.2: 实现高德 POI 搜索方法，将任务位置、场景和目标对象转换为有限次数查询
    - 3.3: 将高德返回结果解析为 `PoiCandidate`，限制总候选数量并过滤无效点位
    - 3.4: 实现 Pexels 图片搜索方法，根据点位名、地址和场景标签查询图片 URL
    - 3.5: 对 MCP provider 缺失、工具缺失、调用异常、解析失败统一返回空结果并记录日志

- [✓] Task 4: 将 MCP 点位与图片增强接入采集规划主流程
    - 4.1: 修改 `CollectionPlanService` 构造函数注入 `VisionMcpToolService`
    - 4.2: 将当前 `buildCandidates` 规则逻辑迁移为 `buildRuleBasedCandidates` 兜底方法
    - 4.3: 在 `buildCandidates` 中优先使用高德 MCP 候选点，无结果时回退规则候选点
    - 4.4: 在 `evaluator.evaluate` 后对 `CollectionSite` 执行图片 URL 增强
    - 4.5: 保持 `recordStore.savePlan`、项目归档和路线摘要现有行为不变

- [✓] Task 5: 补充后端单元测试与降级回归测试
    - 5.1: 覆盖 MCP 返回真实 POI 时采集计划使用真实点位名称和地址
    - 5.2: 覆盖 MCP 无结果时回退规则候选点
    - 5.3: 覆盖 MCP 调用异常时接口仍能生成计划
    - 5.4: 覆盖图片搜索返回 URL 时写入 `CollectionSite.imageUrls`
    - 5.5: 覆盖图片搜索失败时 `imageUrls` 为空且不影响保存计划

- [✓] Task 6: 更新前端采集计划点位图片展示
    - 6.1: 定位当前展示 `plan.sites` 的前端组件或页面
    - 6.2: 在点位卡片中展示 `site.imageUrls` 的 1-3 张缩略图
    - 6.3: 图片为空时保持当前展示不变
    - 6.4: 图片加载失败时不阻断点位信息展示
    - 6.5: 保持现有创建项目、生成计划和计划列表交互不变

- [✓] Task 7: 运行验证并生成总结
    - 7.1: 运行后端定向测试，修复失败项
    - 7.2: 运行后端全量测试，确认没有回归
    - 7.3: 如前端有可用脚本，运行前端构建或检查命令
    - 7.4: 执行规格符合性审查、代码质量审查和最终 QA 评估
    - 7.5: 生成 `summary.md`，记录 MCP 接入效果、降级策略和验证结果
