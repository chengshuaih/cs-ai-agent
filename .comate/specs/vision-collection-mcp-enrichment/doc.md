# 采集辅助规划接入高德与 Pexels MCP 增强设计

## 问题结论

当前“采集辅助规划”生成效果一般的根因不是 MCP 配置缺失，而是结构化规划链路没有实际调用高德地图和 Pexels 图片 MCP。

现状证据：

- 高德 MCP 已配置在 `/Users/chengshuai/Work/private/cs-ai-agent/src/main/resources/mcp-servers.json`，服务名为 `amap-maps`。
- Pexels 图片搜索 MCP 已配置在 `/Users/chengshuai/Work/private/cs-ai-agent/src/main/resources/mcp-servers.json`，服务名为 `yu-image-search-mcp-server`。
- 这些 MCP 工具目前只在智能体聊天接口 `/api/ai/vision-agent/chat` 中通过 `ToolCallbackProvider` 合并使用，见 `/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/controller/AiController.java`。
- 结构化采集规划接口 `/api/vision/collection/plan` 走的是 `/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/vision/service/CollectionPlanService.java`。
- `CollectionPlanService.buildCandidates()` 当前仍是规则生成：点位名为“区域 + 场景 + 候选点”，距离与多样性字段为 `-1`，没有调用高德 POI 查询，也没有调用 Pexels 图片搜索。

因此用户期望的“获取具体地点和对应图片”没有进入结构化规划数据流。

## 需求场景

用户在采集项目工作区中填写视觉任务并点击“采集辅助规划”时，系统应基于任务位置、目标对象、场景类型：

1. 调用高德地图 MCP 获取真实 POI 点位。
2. 将真实 POI 转换为采集候选点。
3. 对点位进行现有采集适配评分。
4. 调用 Pexels 图片搜索 MCP 获取与点位/场景相关的参考图片。
5. 在返回给前端的采集计划中展示更具体的地点、地址、评分理由、采集建议和参考图片。
6. MCP 不可用或无结果时保留现有规则候选点兜底，不能让接口失败。

## 技术方案

### 总体方案

在 `CollectionPlanService` 中接入 Spring AI MCP `ToolCallbackProvider`，复用已配置的 MCP 工具，不新增外部 SDK。

处理链路改为：

```text
VisionCollectionController
  -> CollectionPlanService.plan()
      -> taskParser.normalize()
      -> buildCandidates()
          -> 优先调用高德 MCP 查询真实 POI
          -> 无结果/异常时使用规则候选点兜底
      -> evaluator.evaluate()
      -> enrichSitesWithImages()
          -> 调用 Pexels MCP 为每个点位查询参考图片
          -> 无结果/异常时图片列表为空
      -> recordStore.savePlan()
```

### 高德 MCP 接入

新增一个服务层适配器，例如：

`/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/vision/service/VisionMcpToolService.java`

职责：

- 从 `ObjectProvider<ToolCallbackProvider>` 获取 MCP tool callbacks。
- 根据工具名或描述识别高德 POI 搜索工具。
- 根据任务位置和场景关键词构造查询词。
- 调用工具，解析返回 JSON/文本结果。
- 输出 `List<PoiCandidate>`。

候选查询词示例：

```text
{location} + {sceneLabel}
{location} + {targetObject}
{location} + 路口/停车场/校园/园区/商圈
```

为了避免过度调用，第一版限制：

- 每个场景最多查询一次。
- 总候选点数量限制在 6 个以内。
- 每次工具调用失败只记录并降级，不中断主流程。

### Pexels 图片 MCP 接入

继续在同一个适配器中封装图片搜索能力：

- 根据点位名、地址、场景标签构造搜索词。
- 调用 `yu-image-search-mcp-server` 暴露的图片搜索工具。
- 解析图片 URL。
- 每个点位最多保留 1-3 张参考图。
- 调用失败时返回空列表。

### 数据模型调整

当前 `CollectionSite` 没有图片字段：

`/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/vision/model/CollectionSite.java`

需要增加：

```java
List<String> imageUrls
```

建议追加到 record 末尾，减少现有字段语义变更：

```java
public record CollectionSite(
        String name,
        String address,
        String distance,
        List<String> sceneTags,
        List<String> suitableTasks,
        int score,
        String reason,
        List<String> captureSuggestions,
        List<String> riskTips,
        List<String> imageUrls
) {
}
```

同时更新所有 `new CollectionSite(...)` 构造处，默认为 `List.of()`，MCP 图片增强后写入真实 URL。

### CollectionPlanService 修改

修改文件：

`/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/vision/service/CollectionPlanService.java`

受影响函数：

- 构造函数：注入 MCP 适配服务。
- `plan()`：在 evaluator 后增加图片增强步骤。
- `buildCandidates()`：优先调用 MCP 点位候选，失败时兜底现有规则逻辑。
- 新增 `buildRuleBasedCandidates()`：保留当前规则候选点逻辑作为降级路径。

伪代码：

```java
List<PoiCandidate> candidates = mcpToolService.searchPoiCandidates(task);
if (candidates.isEmpty()) {
    candidates = buildRuleBasedCandidates(task);
}
List<CollectionSite> sites = evaluator.evaluate(candidates, task);
sites = mcpToolService.enrichImages(sites, task);
```

### CollectionSiteEvaluator 修改

修改文件：

`/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/vision/service/CollectionSiteEvaluator.java`

受影响函数：

- 所有 `new CollectionSite(...)` 构造点补充 `List.of()` 图片字段。

评分逻辑不改，避免扩大变更范围。

### 前端展示修改

需要检查并修改采集计划展示页面，预计涉及：

- `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/Home.vue`
- 或当前采集项目工作区中展示 `plan.sites` 的组件。

目标：

- 如果 `site.imageUrls` 非空，在点位卡片中展示 1-3 张缩略图。
- 图片加载失败不影响点位卡片展示。
- 如果没有图片，保持当前展示。

## 边界条件与异常处理

1. MCP provider 不存在：使用规则候选点，图片为空。
2. 高德工具未注册或工具名变化：使用规则候选点，并记录日志。
3. 高德返回无结果：使用规则候选点。
4. 高德返回结构不稳定：尽量解析名称、地址、类型；无法解析的条目跳过。
5. Pexels 返回无结果：图片列表为空。
6. Pexels 调用失败：不影响采集计划生成。
7. 用户未填写 location：仍使用“目标区域”规则兜底，不强行调用地图。
8. 任务场景过多：限制查询次数和候选数量，避免响应过慢。
9. 已存在历史 `CollectionSite` JSON 无 `imageUrls` 字段：Jackson 反序列化 record 时可能需要确认兼容性；如果不兼容，需提供兼容构造或避免旧数据读取失败影响列表。

## 数据流路径

```text
前端采集辅助规划表单
  -> POST /api/vision/collection/plan
  -> VisionCollectionController.plan()
  -> CollectionPlanService.plan()
  -> VisionTaskParser.normalize()
  -> VisionMcpToolService.searchPoiCandidates()
      -> ToolCallbackProvider
      -> amap-maps MCP
  -> CollectionSiteEvaluator.evaluate()
  -> VisionMcpToolService.enrichImages()
      -> ToolCallbackProvider
      -> yu-image-search-mcp-server / Pexels
  -> VisionRecordStore.savePlan()
  -> 返回 CollectionPlan
  -> 前端展示真实地点和图片
```

## 预期结果

修复后，采集辅助规划应从“泛化建议”提升为“真实地点 + 参考图片 + 可执行采集路线”：

- 点位名称不再主要是“候选点”占位文本。
- 地址来自高德 POI 结果。
- 路线摘要包含真实点位名称。
- 点位卡片可展示 Pexels 参考图片。
- MCP 不可用时仍能返回原规则版计划，接口稳定性不下降。

## 测试计划

### 单元测试

1. `CollectionPlanService` 在 MCP 返回 POI 时使用真实候选点。
2. MCP 无结果时回退到规则候选点。
3. MCP 异常时不抛出到 controller，仍返回计划。
4. 图片搜索返回 URL 时写入 `CollectionSite.imageUrls`。
5. 图片搜索失败时 `imageUrls` 为空列表。

### 回归测试

运行后端测试：

```bash
JAVA_HOME="/Users/chengshuai/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home" ./mvnw test
```

如前端展示有修改，运行前端构建或现有检查命令。