# 采集规划真实定位增强 v2 任务计划

- [x] Task 1: PoiCandidate 增加 poiId/photoUrl 字段
    - 1.1: 新增 `String poiId`、`String photoUrl`（可空）到 record
    - 1.2: 调整主构造为 9 参，更新 javadoc
    - 1.3: 保留 5 参 / 7 参兼容构造（委托主构造，新字段补 null）
    - 1.4: 保留/调整 `of(...)` 静态工厂

- [x] Task 2: VisionMcpToolService 修复关键词与坐标补全
    - 2.1: 新增常量 `AMAP_SEARCH_DETAIL = "maps_search_detail"`
    - 2.2: `buildSearchKeywords` 改为只产出纯场景词（去掉 location 前缀拼接）
    - 2.3: `queryAmap` 解析 POI 的 id/name/address/typecode/photos.url，city 用 task.location()
    - 2.4: 新增 `fetchPoiDetail(id)` 调 maps_search_detail，解析 location/type/photos.url
    - 2.5: 命中 POI 后用 detail 补全坐标/类型/实拍图，失败则降级保留 text_search 字段
    - 2.6: 新增 `extractPhotoUrl` 兼容 photos.url 对象与 photos[].url 数组

- [x] Task 3: 图片来源改为高德实拍优先、Pexels 兜底
    - 3.1: CollectionSiteEvaluator 透传 poi.longitude/latitude 到 CollectionSite
    - 3.2: poi.photoUrl 非空时作为初始 imageUrls 第一张传入
    - 3.3: enrichImages 先用 site 已有高德实拍图，不足再用 Pexels 补足至上限

- [x] Task 4: 更新与新增测试并跑全量
    - 4.1: VisionMcpToolServiceTest 增补：纯场景词关键词、detail 补坐标、photos 解析、实拍图优先
    - 4.2: 修正受 PoiCandidate 构造变化影响的现有测试
    - 4.3: CollectionPlanServiceTest 验证坐标/实拍图透传不破坏多方案
    - 4.4: 用 Java 17 跑 `./mvnw test` 全绿

- [x] Task 5: 重新构建并重启后端做真机联调
    - 5.1: Java 17 构建后端
    - 5.2: 重启后端进程
    - 5.3: curl 采集规划接口，确认返回真实 POI 名称/坐标/实拍图/路线距离耗时

- [x] Task 6: 生成 summary.md
