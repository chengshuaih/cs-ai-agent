# 采集辅助规划接入高德与 Pexels MCP 增强总结

## 问题背景

用户反馈“采集辅助规划”生成效果一般，期望使用已接入的高德地图和 Pexels 图片 MCP 获取具体地点和对应图片。

根因不是 MCP 配置缺失，而是结构化采集规划链路从未实际调用这两个 MCP：

- 高德 `amap-maps`、Pexels `yu-image-search-mcp-server` 已配置在 `mcp-servers.json`，但仅在智能体聊天接口 `/api/ai/vision-agent/chat` 中通过 `ToolCallbackProvider` 使用。
- 结构化接口 `/api/vision/collection/plan` 走的 `CollectionPlanService.buildCandidates()` 是纯规则生成：点位名为“区域+场景+候选点”占位文本，坐标/距离为 `-1`，没有真实地点，也没有图片。

## 修改内容

### 1. 数据模型

- `CollectionSite` 新增 `imageUrls` 字段（追加在末尾）和 `withImageUrls` 拷贝方法。
- 增加紧凑构造器，将反序列化得到的 `null` 归一为空列表，兼容缺少该字段的历史 `plans.json` 记录，避免下游空指针。

### 2. 新增 MCP 工具适配层

新增 `VisionMcpToolService`：

- 通过 `ObjectProvider<ToolCallbackProvider>` 获取已配置的 MCP 工具。
- `searchPoiCandidates(task)`：调用高德 `maps_text_search`，将任务位置、场景、目标对象转为有限次数查询，解析 POI 名称/地址/类型为 `PoiCandidate`，限制总候选 6 个、每场景 2 个并按名称去重。
- `enrichImages(sites, task)`：调用 Pexels `searchImage`，为每个点位查询参考图片 URL，每个点位最多 3 张。
- 所有外部调用尽力而为：provider 缺失、工具缺失、调用异常、解析失败、无结果统一返回空结果并记录 WARN 日志，绝不抛到上层。

### 3. 采集规划主流程接入

修改 `CollectionPlanService`：

- 构造函数注入 `VisionMcpToolService`。
- `buildCandidates` 优先使用高德 MCP 真实点位，无结果时回退到原规则逻辑（迁移为 `buildRuleBasedCandidates`，逻辑保持不变）。
- 在 `evaluator.evaluate` 后调用 `enrichImages` 补充点位参考图片。
- `savePlan`、项目归档、路线摘要等现有行为不变；评分逻辑（`CollectionSiteEvaluator`）不改。

### 4. 前端展示

修改 `CollectionPlanner.vue`：

- 点位卡片在 `site.imageUrls` 非空时展示 1-3 张缩略图，懒加载。
- 图片加载失败时通过 `onImageError` 隐藏该图，不影响点位信息展示。
- 无图片时保持原展示，创建项目/生成计划/导出 PDF 等交互不变。

## 验证结果

### 后端编译与测试

```bash
JAVA_HOME=".../jbr-17.0.14/Contents/Home" ./mvnw -o test
```

结果：

```text
BUILD SUCCESS
Tests run: 35, Failures: 0, Errors: 0, Skipped: 0
```

新增测试：

- `VisionMcpToolServiceTest`：7 个，覆盖真实 POI 解析、无 provider、无位置、工具异常、图片写入、图片错误、无工具降级。
- `CollectionPlanServiceTest`：2 个，端到端覆盖 MCP 真实点位+图片增强、MCP 无结果回退规则候选点。

### 前端构建

```bash
npm run build
```

结果：vite 构建成功，98 个模块转换通过。

## 审查结果

- 规格符合性审查：PASS（无 Critical/Important）。
- 代码质量审查：PASS。Important 提示：历史 `CollectionSite` 可能 `imageUrls` 为 null，已通过紧凑构造器归一为空列表修复；图片结果按逗号切分依赖工具返回格式，已通过 `http` 前缀过滤兜底。
- QA 评估：PASS，Failure Attribution: none。

## 降级策略说明

本次接入是增强而非强依赖：

- 高德或 Pexels MCP 未启动、未配置 API Key、调用失败或无结果时，采集规划自动回退到规则候选点，图片为空，`/api/vision/collection/plan` 接口稳定可用。
- 要看到真实地点和图片，需保证 `mcp-servers.json` 中高德 `AMAP_MAPS_API_KEY` 与 Pexels `PEXELS_API_KEY` 已正确配置且 MCP 客户端连接正常。

## 最终结果

采集辅助规划已从规则占位升级为“真实 POI 点位 + 参考图片 + 可执行采集路线”，并在 MCP 不可用时保持原有规则版能力，接口稳定性不下降。后端 35 个测试与前端构建均通过。