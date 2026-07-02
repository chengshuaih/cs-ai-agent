# 采集辅助规划「具体地点 + 定位 + 多方案」增强总结

## 问题背景

用户反馈采集辅助规划效果仍像“空中楼阁”：点位是“校园道路（候选点）（估算）”这类占位文本，只有一个点位，没有距离/怎么去/耗时，缺少多方案，页面两侧贴边。

排查确认高德 POI 没生效的根因不是工具名/参数错误，而是：

- 根因A：查询词用裸场景标签（“校园道路”），高德返回的多是地名/道路名记录，`name`/`address` 常为空数组 `[]`，被旧 `textOf` 判空后整批跳过 → 回退规则候选点。
- 根因B：高德 MCP 返回的是 `content[].text` 包裹的 JSON 字符串，旧 `locatePoiArray` 只认根级 `pois`，解析不到 → 回退。

## 修改内容

### 1. 打通高德 POI（根因修复）

`VisionMcpToolService`：
- 新增 `parseToolResult`：兼容直接 JSON、`content[].text` 包裹、纯文本三种返回形态。
- 增强 `locatePoiArray`：兼容 `pois`/`results`/`poiList` 及 `data.*`。
- 增强 `textOf`：高德空数组 `[]` 字段视为空。
- 新增 `parseLocation`：解析 `"lng,lat"` 写入候选点经纬度。
- `buildSearchKeywords`：改用「位置 + 场景词」组合查询，提升真实 POI 命中率。

### 2. 数据模型扩展（向后兼容）

- `PoiCandidate` 新增 `longitude`/`latitude`，保留五参兼容构造。
- `CollectionSite` 新增 `longitude`/`latitude`/`mapImageUrl`/`travelDistance`/`travelDuration`/`navUrl`，紧凑构造器归一 `imageUrls`，保留十参兼容构造，新增 `withLocation`/`withTravel`。
- 新增 `CollectionScheme`（点位组合 + 路线 + 推荐理由 + 全程距离/耗时/出行方式）。
- `CollectionPlan` 新增 `schemes`，紧凑构造器归一为空列表，保留九参兼容构造；`sites` 回填首选方案点位以兼容报告与历史数据。

### 3. 高德路线规划与静态地图

`VisionMcpToolService`：
- `planRoute`：按出行方式调用步行/驾车/骑行路线规划，解析距离与耗时并格式化。
- `enrichLocation`：生成标注点位经纬度的高德静态地图缩略图 URL。
- `buildNavUrl`：生成高德标点跳转链接。
- 全部尽力而为，缺工具/缺经纬度/失败时字段为空。

### 4. 多方案生成

`CollectionPlanService.composeSchemes`：
- 方案A 质量优先（评分最高）、方案B 多样性优先（不同场景标签）、方案C 效率优先（基于经纬度 haversine 选最近点位，更集中）。
- 每套方案带推荐理由，去重避免重复方案。
- 每套方案做路线规划得到全程距离/耗时。
- POI 不足时降级为单套。

### 5. 主流程接入

`plan()` 串联：POI → 评分 → 图片增强 → 定位增强 → 多方案组合（含路线）→ 落库；首选方案点位回填 `sites`。

### 6. 前端

`CollectionPlanner.vue`：
- 修复结果区/容器两侧贴边（container 左右 padding，embedded 16px，窄屏自适应）。
- 顶部方案切换 Tab，支持多选对比，展示方案标题/推荐理由/全程距离/耗时/出行方式。
- 点位卡片展示高德地图缩略图 + Pexels 场景图 + 距离/耗时/出行方式 + 高德导航链接。
- 图片/地图加载失败优雅隐藏；无方案时回退原 sites 展示。

## 验证结果

### 后端

```bash
JAVA_HOME=".../jbr-17.0.14/Contents/Home" ./mvnw -o test
```

```text
BUILD SUCCESS
Tests run: 43, Failures: 0, Errors: 0, Skipped: 0
```

新增/扩展测试：
- `VisionMcpToolServiceTest`（12）：content 包裹解析、空数组跳过、经纬度解析、路线规划距离/耗时、缺经纬度降级、静态地图降级等。
- `CollectionPlanServiceTest`（4）：真实 POI + 图片、规则回退、多方案组合、点位不足降级单套。
- `VisionRecordStoreTest`（5）：新增历史无新字段计划反序列化回归。

### 前端

```bash
npm run build
```

vite 构建成功，98 个模块转换通过。

## 审查结果

- 规格符合性审查：PASS（无 Critical/Important）。
- 代码质量审查：PASS（无 Critical）。已落实修复：`routeToolName` 补括号消歧义；`pickEfficient` 改用经纬度 haversine 实现真正的“效率优先就近选点”；新增历史计划反序列化回归测试。
- QA 评估：PASS。Failure Attribution: ENVIRONMENT —— 真实 POI/地图/路线渲染依赖高德 MCP 在运行时联网且配置 `AMAP_MAPS_API_KEY`，单测无法覆盖，属合理环境依赖，代码已全路径安全降级。

## 运行前提

要看到真实地点、地图缩略图和路线距离/耗时：

- 高德 MCP（`amap-maps`）需正常连接，`AMAP_MAPS_API_KEY` 已配置（路线/POI 经 MCP，静态地图缩略图也读取该环境变量）。
- Pexels MCP（`searchImage`）需配置 `PEXELS_API_KEY`。
- 任一不可用时自动回退：POI 用规则候选点、单方案、无图无距离，接口仍稳定。

## 最终结果

采集辅助规划从“泛化占位”升级为“真实 POI 地点 + 地图缩略图 + 场景图 + 距离/耗时/导航 + 2-3 套可选方案（带推荐理由、可多选对比）”，并修复了页面两侧贴边问题；高德/Pexels 不可用时全链路安全降级。后端 43 个测试与前端构建均通过。