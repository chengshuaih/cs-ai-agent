# 采集辅助规划「具体地点 + 定位 + 多方案」增强设计

## 背景与问题

当前采集辅助规划虽然已接入高德/Pexels MCP，但实际效果仍是“空中楼阁”：

1. 截图中点位仍是规则占位“北京市朝阳区校园道路（候选点）（估算）”，说明高德 POI 没拿到，回退了规则候选点。
2. 一个任务只推荐一个点位，没有可达性信息（距离、怎么去、耗时），缺少多方案对比。
3. 前端结果区两侧外边距为 0，贴边显示，观感差。

### 高德 POI 未生效的根因（已排查）

- 工具名 `maps_text_search`、参数 `keywords`/`city` 正确，`findTool` 的 `endsWith` 能匹配带前缀的 MCP 工具名，这些不是问题。
- 根因 A：查询词用中文场景标签（“校园道路/十字路口”），高德文本搜索返回的多为地名/道路名记录，`name`/`address` 常为空数组 `[]`，被当前 `textOf` 判空跳过，整批为空 → 回退规则候选点。
- 根因 B：高德 MCP 返回的是 MCP `content[].text` 包裹的 JSON 字符串，当前 `locatePoiArray` 只识别根级 `pois` / `data.pois` / 顶层数组，解析不到 → 回退。

## 需求目标（已与用户确认）

- 高德 MCP 已配置且联网，需要真正调通并拿到真实 POI。
- 点位图片：高德静态地图缩略图（精确标注该点经纬度）+ Pexels 场景参考图，两者都要。
- 路线导航：调用高德路线规划，给出真实距离、时长、出行方式（步行/驾车/骑行）和路线说明。
- 推荐多套方案：生成 2-3 套完整可选方案（不同点位组合 + 不同路线），每套有推荐理由，用户可挑选；并支持选择多条路线。
- 修复前端结果区贴边问题，两侧留出合理边距。

## 技术方案

### 一、打通高德 POI 解析（根因修复，最高优先级）

修改 `VisionMcpToolService`：

1. **MCP content 解包**：新增 `unwrapMcpText(raw)`，先尝试把返回解析为 `{"content":[{"type":"text","text":"..."}]}`，取出内层 `text` 再二次 `readTree`；解析不到则按原始字符串处理。兼容直接 JSON 和文本包裹两种形态。
2. **POI 数组定位增强**：`locatePoiArray` 增加对内层文本解析后的 `pois`、`results`、`data.pois`、`pois` 在任意层级的兜底查找。
3. **字段健壮解析**：`textOf` 处理高德把字段返回为空数组 `[]` 的情况（视为空）；`name` 为空的记录跳过，但解析 `location`（"lng,lat"）写入候选点经纬度。
4. **查询词优化**：`buildSearchKeywords` 用 `location + 场景词`（如“北京市朝阳区 大学”“北京市朝阳区 十字路口”“北京市朝阳区 停车场”）而非裸场景标签，提升命中真实 POI 概率。
5. **经纬度落地**：`PoiCandidate` 已有 `distanceKm`，新增承载经纬度。解析高德 `location` 字段填入 `PoiCandidate`，供后续路线规划与静态地图使用。

### 二、数据模型扩展

#### PoiCandidate（`vision/model/PoiCandidate.java`）

新增经纬度字段：

```java
public record PoiCandidate(
        String name,
        String address,
        String poiType,
        double distanceKm,
        int nearbyPoiTypeCount,
        Double longitude,   // 新增，可空
        Double latitude     // 新增，可空
) { ... }
```

#### CollectionSite（`vision/model/CollectionSite.java`）

补充定位与导航字段（追加在末尾，保持兼容；紧凑构造器统一 null 归一）：

```java
List<String> imageUrls,     // 已有：Pexels 场景图
Double longitude,           // 新增
Double latitude,            // 新增
String mapImageUrl,         // 新增：高德静态地图缩略图 URL
String travelDistance,      // 新增：到出发点/上一个点的距离（如 "2.3km"）
String travelDuration,      // 新增：预计耗时（如 "约 18 分钟"）
String navUrl               // 新增：高德导航跳转链接
```

#### 新增方案模型 CollectionScheme（`vision/model/CollectionScheme.java`）

一套方案 = 一组点位 + 一条路线 + 推荐理由：

```java
public record CollectionScheme(
        String id,
        String title,            // 如 "方案A：高密度路口优先"
        String recommendReason,  // 推荐理由
        List<CollectionSite> sites,
        String routeSummary,     // 路线与时段建议
        String totalDistance,    // 全程距离
        String totalDuration,    // 全程耗时
        String transportMode     // 出行方式
) {}
```

#### CollectionPlan（`vision/model/CollectionPlan.java`）

新增多方案字段（保留 `sites` 向后兼容，默认填充首选方案点位）：

```java
List<CollectionScheme> schemes   // 新增：2-3 套可选方案
```

`sites` 仍保留并指向首选方案，避免破坏报告生成与历史数据。

### 三、高德路线与静态地图接入

`VisionMcpToolService` 新增：

1. `enrichLocation(site)`：用高德静态地图工具（或拼接静态地图 URL）生成 `mapImageUrl`，标注点位经纬度。
   - 若高德 MCP 暴露静态地图工具则调用；否则用高德静态地图 REST URL 模板（key 复用 `AMAP_MAPS_API_KEY`）。
2. `planRoute(origin, sites, mode)`：调用高德路线规划工具（`maps_direction_walking` / `maps_direction_driving` / `maps_bicycling`），解析距离、时长，写入每段 `travelDistance`/`travelDuration` 和整体 `totalDistance`/`totalDuration`。
3. `buildNavUrl(site)`：生成高德导航跳转链接（`https://uri.amap.com/navigation?...`），作为辅助。

所有调用尽力而为，失败时该字段为空，不影响主流程。

### 四、多方案生成

`CollectionPlanService` 调整：

1. 高德返回的真实 POI 候选点（数量 > 单方案需要）作为方案池。
2. 新增 `SchemeComposer`（或在 service 内）按策略组合 2-3 套方案：
   - 方案A：评分最高的点位组合（质量优先）
   - 方案B：场景多样性最高的组合（多样性优先）
   - 方案C：路程最短/最省时的组合（效率优先，依赖路线规划结果）
3. 每套方案生成推荐理由（基于其策略与点位特征）。
4. 每套方案调用路线规划得到距离/时长。
5. POI 不足以分出多套时，降级为 1 套（保持现状不报错）。

### 五、前端改造（`CollectionPlanner.vue`）

1. **修复贴边**：结果区/容器增加左右 padding 或 margin，窄屏自适应。
2. **多方案展示**：结果区顶部增加方案切换（Tab 或卡片选择），展示每套方案标题、推荐理由、全程距离/耗时/出行方式。
3. **点位卡片增强**：每个点位展示
   - 高德静态地图缩略图（`mapImageUrl`）
   - Pexels 场景参考图（`imageUrls`）
   - 距离、预计耗时、出行方式
   - “在高德打开导航”链接（`navUrl`）
4. **多路线选择**：允许勾选多套方案进行对比（至少支持切换查看；勾选多条用于后续合并导出）。
5. 图片/地图加载失败时优雅降级，不阻断信息展示。

## 边界与异常处理

1. 高德未连通/无 key：POI、路线、静态地图全空 → 回退规则候选点单方案，前端正常展示。
2. 高德返回 content 包裹或空数组字段：解包并跳过无名记录，不报错。
3. 路线规划失败：距离/耗时为空，方案仍可用。
4. 静态地图失败：`mapImageUrl` 为空，仅展示场景图。
5. POI 数量不足：方案降级为 1 套。
6. 历史 `CollectionPlan`/`CollectionSite` 缺新字段：紧凑构造器/反序列化归一为 null/空，读取不报错。
7. 经纬度缺失：跳过静态地图与精确路线，距离按规则估算并标注。

## 数据流

```text
前端表单 -> /api/vision/collection/plan
  -> CollectionPlanService.plan()
     -> VisionMcpToolService.searchPoiCandidates()  // 高德 maps_text_search + content 解包 + 经纬度
     -> evaluator.evaluate()                        // 评分
     -> SchemeComposer.compose()                    // 组合 2-3 套方案
        每套:
          -> VisionMcpToolService.planRoute()        // 高德路线规划: 距离/时长
          -> enrichLocation()                        // 高德静态地图缩略图
          -> enrichImages()                          // Pexels 场景图
     -> recordStore.savePlan()
  -> 返回 CollectionPlan(schemes[])
  -> 前端: 多方案切换 + 点位地图/图片/距离/导航
```

## 测试计划

### 后端单元测试

1. `unwrapMcpText` 能从 `{"content":[{"text":"{...pois...}"}]}` 提取 POI。
2. 高德字段为空数组 `[]` 时跳过且不报错。
3. 解析 `location` 写入候选点经纬度。
4. `SchemeComposer` 在充足 POI 下产出多套且策略不同。
5. POI 不足时降级单套。
6. 路线/静态地图失败时字段为空、方案仍生成。
7. 历史无新字段记录反序列化不报错。

### 回归与构建

```bash
JAVA_HOME=".../jbr-17.0.14/Contents/Home" ./mvnw -o test
```

前端：

```bash
npm run build
```

### 真机联调（高德已配置）

启动后端，调用 `/api/vision/collection/plan`，确认返回真实 POI 名称、经纬度、距离/时长、静态地图 URL 和多套方案。

## 预期结果

- 点位为真实高德 POI（具体地名/地址），不再是“候选点（估算）”。
- 每个点位有高德地图缩略图 + 场景参考图 + 距离 + 耗时 + 出行方式 + 导航链接。
- 一次生成 2-3 套带推荐理由的方案，用户可切换/多选对比。
- 前端两侧留白合理，观感改善。
- 高德不可用时回退规则单方案，接口稳定。