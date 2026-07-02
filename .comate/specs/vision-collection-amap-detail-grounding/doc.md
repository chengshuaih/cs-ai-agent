# 采集规划真实定位增强 v2（vision-collection-amap-detail-grounding）

## 背景与问题（真机实测结论）

用户连续反馈：采集规划"空中楼阁"、"没变化"，要求**具体地点、具体路径、具体位置的图片、定位（距离/怎么去/时间）**。经真机联调（直接驱动高德 MCP stdio）定位到三个根因，均与运行中的旧实现有关：

1. **关键词构造错误导致零命中**
   现状 `VisionMcpToolService.buildSearchKeywords` 用 `location + sceneWord` 拼接，例如 `"上海市静安区校园道路"`。实测高德 `maps_text_search`：
   - `keywords="学校", city="上海"` → 返回 20 条真实 POI；
   - `keywords="上海市静安区校园道路", city="上海市静安区"` → 返回 0 条。
   关键词应为**纯场景词**，城市信息走 `city` 参数。

2. **`maps_text_search` 不返回坐标**
   实测单条 POI 仅含 `{id, name, address, typecode, photos.url}`，**无 `location` 字段**。当前代码 `parseLocation(textOf(poi,"location"))` 恒为 null，导致：
   - `enrichLocation` 早返回（经纬度为空），地图缩略图/导航链接为空；
   - `planRoute` 早返回，距离/耗时为空。
   必须对每个 POI 调用 `maps_search_detail`（入参 `id`）二次补全，detail 返回含 `location`（"lng,lat"）、`type`（真实分类，如"科教文化服务;学校;高等院校"）、`photos.url`（高德实拍图）。
   实测 detail 示例：`location: "121.483354,31.277067"`，`type: "科教文化服务;学校;高等院校"`，`photos.url: "https://store.is.autonavi.com/showpic/...?type=pic"`。

3. **图片来源未利用高德实拍图**
   现状仅用 Pexels 通用场景图，与"具体位置的图片"诉求不符。高德 detail 自带该 POI 的实拍照片，应优先使用，不足再用 Pexels 兜底。

> 补充：当前运行的后端进程是上一轮编译产物，包含上述旧逻辑，因此用户看到的仍是规则占位点。修复后需重新构建并重启后端。

## 目标

让采集规划返回**真实 POI 名称 + 真实坐标 + 该地点实拍图 + 真实路线距离/耗时 + 导航链接**，并保持已有的多方案（A/B/C）、多选、降级与向后兼容能力不变。

## 处理逻辑（数据流）

```
VisionTask(location, targetObjects, sceneTypes)
  └─ searchPoiCandidates
       ├─ buildSearchKeywords  → 纯场景词列表（学校/十字路口/停车场...）
       ├─ queryAmap(keywords=场景词, city=location) → 取每场景前 N 条 POI（含 id）
       └─ 对每个 POI：fetchPoiDetail(id) → 补 location(经纬度)/type/photoUrl
  └─ evaluate → CollectionSite（含坐标、高德实拍图）
  └─ enrichImages：高德实拍图优先；不足 MAX_IMAGES 时用 Pexels 补
  └─ enrichLocation：有坐标 → 高德静态地图缩略图 + 导航 URL
  └─ composeSchemes：A/B/C；buildScheme 内 planRoute（有坐标即真实距离/耗时）
```

## 影响文件

### 1. `src/main/java/cn/chengshuai/csaiagent/vision/model/PoiCandidate.java`（修改）
新增 `String poiId`、`String photoUrl` 两个字段（均可空）。保留现有 5 参 / 7 参兼容构造，新增 9 参主构造；`of(...)` 维持。photoUrl 用于把高德实拍图带到下游。

### 2. `src/main/java/cn/chengshuai/csaiagent/vision/service/VisionMcpToolService.java`（核心修改）
- 新增常量 `AMAP_SEARCH_DETAIL = "maps_search_detail"`。
- `buildSearchKeywords`：**只产出场景词**（去掉 location 前缀拼接）；空场景时给默认场景词（路口/校园/停车场）。
- `queryAmap`：
  - 入参 `keywords=场景词`、`city=task.location()`；
  - 解析 POI 的 `id`（字段名 `id`）、`name`、`address`、`typecode`/`type`、`photos.url`；
  - 命中后对每个 POI 调用 `fetchPoiDetail(id)` 补全坐标/类型/实拍图（失败则保留 text_search 的字段，坐标为空降级）。
- 新增 `fetchPoiDetail(String id)`：调用 `maps_search_detail`，解析 `location`(经纬度)、`type`、`photos.url`，返回轻量结构（如 `double[] lngLat` + type + photoUrl，或一个内部 record）。
- 新增 `extractPhotoUrl(JsonNode poi)`：从 `photos.url`（对象）或 `photos[0].url`（数组）兼容取首图。
- `enrichImages`：先收集 site 已带的高德实拍图（来自 PoiCandidate.photoUrl，经 evaluator 透传到 CollectionSite），不足 `MAX_IMAGES_PER_SITE` 时再用 Pexels `searchImage` 补足；全程尽力而为。
- `textOf` / `parseToolResult` / `locatePoiArray` 复用，无需改。

### 3. `src/main/java/cn/chengshuai/csaiagent/vision/service/CollectionSiteEvaluator.java`（修改）
`evaluate(PoiCandidate,…)` 构造 `CollectionSite` 时：
- 透传 `poi.longitude()/poi.latitude()`（已存在字段）；
- 若 `poi.photoUrl()` 非空，作为初始 imageUrls 的第一张传入（保证"具体位置图片"优先）。
保持现有四维评分逻辑不变。

### 4. `src/main/java/cn/chengshuai/csaiagent/vision/model/CollectionSite.java`（按需微调）
当前 `withImageUrls` 已可覆盖图片；evaluator 直接用主构造传入坐标与初始图。无需改结构，确认坐标字段已在主构造透传即可。

### 5. 不改：`CollectionPlanService` 主链路（composeSchemes/planRoute/enrichLocation 已就位，坐标补全后自然生效）。

## 边界与异常处理（保持"尽力而为，安全降级"）

- MCP provider/工具缺失、调用异常、解析失败：返回空/保留原值，回退规则候选点（与现状一致）。
- `maps_search_detail` 失败或无坐标：该 POI 保留 name/address，坐标为空 → 地图/路线字段为空，但 POI 名称仍真实。
- 高德实拍图缺失：用 Pexels 补；Pexels 也失败则 imageUrls 可为空（前端 `onImageError` 已处理）。
- detail 调用按 `MAX_CANDIDATES` 限量，避免过多外呼拖慢响应。

## 预期结果

- 输入"上海市静安区 / 行人 / 目标检测"：返回如"上海外国语大学(虹口校区)"等**真实 POI**，带坐标、高德实拍图、A/B/C 方案中各段真实步行距离/耗时、导航链接、地图缩略图。
- 后端单测全绿；前端构建通过；重启后端后真机返回真实数据。
