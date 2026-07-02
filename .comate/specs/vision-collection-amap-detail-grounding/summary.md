# 采集规划真实定位增强 v2 — 完成总结

## 用户诉求
"加一个具体定位的功能，生成的规划要带具体路径、具体地点、那里的图片"，且之前几轮"感觉没变化"。

## 为什么之前没生效（根因，真机联调定位）
通过直接驱动高德 MCP（stdio）逐项验证，确认是三个真实根因，而非代码"看起来对了"：

1. **关键词命中率为 0**：旧逻辑用场景标签（"校园道路""十字路口"）作为高德 `maps_text_search` 的 keywords，实测返回 0 条 POI；高德能命中的是"学校""商场""公园"等可检索地点词。
2. **`maps_text_search` 不返回坐标**：单条 POI 只有 `{id,name,address,typecode,photos}`，没有 `location`。导致 `enrichLocation`/`planRoute` 因经纬度为空全部早退，地图缩略图、路线距离/耗时、导航全为空。
3. **`city` 带区后缀清零命中**：`city="上海市静安区"` 命中数骤降；需归一化为"上海市"，把"静安区"拼进 keyword 做区域偏置。

此外还暴露两个运行期问题：高德个人 Key 触发 `CUQPS_HAS_EXCEEDED_THE_LIMIT` 限流；MCP 返回是**数组形态** content 包裹（`[{"type":"text","text":"..."}]`），旧 `parseToolResult` 只兼容对象形态 `{"content":[...]}`。

## 已实现
- **关键词映射**：`VisionSceneType.getSearchKeywords()` 把场景类型映射为高德可命中地点词；`buildSearchKeywords` 把"区/县"级地名拼到关键词前做区域偏置；`normalizeCity` 把 city 归一到市级。
- **坐标/类型/实拍图二次补全**：对每个 POI 调 `maps_search_detail`（`fetchPoiDetail`）补全 `location`、可读 `type`、`photos.url`（高德实拍图）。
- **图片来源**：高德实拍图优先，无实拍图才用 Pexels 兜底。
- **定位增强**：有坐标即生成高德静态地图缩略图（`amapKey` 注入，环境变量优先、否则取 `local-api-keys.amap.maps-api-key`）+ 导航链接 + 各段步行距离/耗时（`planRoute`）。
- **多方案**：A 质量优先 / B 多样性优先 / C 效率优先（基于经纬度 haversine），各带推荐理由、总距离、总耗时，前端可多选。
- **稳健性**：`parseToolResult` 兼容数组/对象两种 content 包裹；`callAmapWithRetry` 对 QPS 限流退避重试 + 串行间隔；按剩余额度 `remaining` 取 POI，避免对将被丢弃的 POI 浪费 detail 调用。

## 影响文件
- `vision/model/PoiCandidate.java`：新增 `poiId`/`photoUrl`，9 参主构造 + 5/7 参兼容构造。
- `vision/model/VisionSceneType.java`：新增 `getSearchKeywords()`。
- `vision/service/VisionMcpToolService.java`：关键词映射、`extractDistrict`/`normalizeCity`、`fetchPoiDetail`、`extractPhotoUrl`、`callAmapWithRetry`、`parseToolResult` 双形态、`amapKey` 注入、enrichImages 优先级、额度控制。
- `vision/service/CollectionSiteEvaluator.java`：透传坐标与高德实拍图到 `CollectionSite`。
- 测试：`VisionMcpToolServiceTest`（含限流重试、detail 补坐标、关键词映射、静态图、Pexels 兜底）、`CollectionPlanServiceTest`。

## 验证
- 全量后端测试 **49 通过，0 失败（BUILD SUCCESS）**，Java 17。
- 真机联调（重启后端，高德/Pexels MCP 在线）："上海市静安区/行人/目标检测"返回 **3 套方案**，含真实 POI（上海大学延长校区、上海市市西中学、上海市静安区业余大学等）、真实坐标、地图缩略图、各段步行距离/耗时、导航链接。
- 代码质量审查：Important（normalizeCity 丢弃区级、detail 时延、越界 detail 浪费、重复日志）与限流重试边界均已修复。

## 运行前提与已知限制
- 需高德/Pexels MCP 在线且 Key 配置正确（`mcp-servers-local.json` / `application-local.yml`）。
- 高德个人 Key QPS 较低：已用串行间隔 + 退避重试缓解，极端情况下个别路线段距离/耗时可能仍为空（安全降级，不影响 POI 与坐标）。
- Pexels 图片 MCP 偶发 20s 超时：仅作为无实拍图时的兜底，不阻断主流程。
