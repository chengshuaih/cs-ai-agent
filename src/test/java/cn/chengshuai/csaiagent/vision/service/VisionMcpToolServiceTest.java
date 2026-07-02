package cn.chengshuai.csaiagent.vision.service;

import cn.chengshuai.csaiagent.vision.model.CollectionSite;
import cn.chengshuai.csaiagent.vision.model.PoiCandidate;
import cn.chengshuai.csaiagent.vision.model.VisionTask;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link VisionMcpToolService} 单元测试：覆盖高德 POI 增强、图片增强及各类降级路径。
 */
class VisionMcpToolServiceTest {

    private final VisionTask task = new VisionTask("目标检测",
            List.of("车辆", "行人"), List.of("十字路口"), "北京", "半天", "骑行");

    private ToolCallback toolNamed(String name) {
        ToolCallback callback = mock(ToolCallback.class);
        ToolDefinition def = mock(ToolDefinition.class);
        when(def.name()).thenReturn(name);
        when(callback.getToolDefinition()).thenReturn(def);
        return callback;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<ToolCallbackProvider> providerOf(ToolCallback... callbacks) {
        ToolCallbackProvider provider = mock(ToolCallbackProvider.class);
        when(provider.getToolCallbacks()).thenReturn(callbacks);
        ObjectProvider<ToolCallbackProvider> objectProvider = mock(ObjectProvider.class);
        when(objectProvider.getIfAvailable()).thenReturn(provider);
        return objectProvider;
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<ToolCallbackProvider> emptyProvider() {
        ObjectProvider<ToolCallbackProvider> objectProvider = mock(ObjectProvider.class);
        when(objectProvider.getIfAvailable()).thenReturn(null);
        return objectProvider;
    }

    @Test
    void searchPoiCandidates_returnsRealPoisFromAmap() throws Exception {
        ToolCallback amap = toolNamed("maps_text_search");
        when(amap.call(anyString())).thenReturn(
                "{\"pois\":[{\"name\":\"中关村十字路口\",\"address\":\"海淀区中关村大街\",\"type\":\"交叉口\"}]}");
        VisionMcpToolService service = new VisionMcpToolService(providerOf(amap), "test-key");

        List<PoiCandidate> candidates = service.searchPoiCandidates(task);

        assertFalse(candidates.isEmpty(), "应解析出真实 POI 候选点");
        assertTrue(candidates.stream().anyMatch(p -> p.name().equals("中关村十字路口")));
        assertTrue(candidates.stream().anyMatch(p -> p.address().contains("中关村大街")));
    }

    @Test
    void searchPoiCandidates_parsesContentWrappedJsonAndLocation() throws Exception {
        ToolCallback amap = toolNamed("maps_text_search");
        // 高德 MCP 常以 content[].text 包裹 JSON 字符串返回
        when(amap.call(anyString())).thenReturn(
                "{\"content\":[{\"type\":\"text\",\"text\":\"{\\\"pois\\\":[{\\\"name\\\":\\\"望京十字路口\\\","
                        + "\\\"address\\\":\\\"朝阳区望京街\\\",\\\"type\\\":\\\"交叉口\\\","
                        + "\\\"location\\\":\\\"116.471,39.996\\\"}]}\"}]}");
        VisionMcpToolService service = new VisionMcpToolService(providerOf(amap), "test-key");

        List<PoiCandidate> candidates = service.searchPoiCandidates(task);

        assertFalse(candidates.isEmpty(), "应能从 content 包裹中解析 POI");
        PoiCandidate p = candidates.get(0);
        assertEquals("望京十字路口", p.name());
        assertEquals(116.471, p.longitude(), 0.0001, "应解析经度");
        assertEquals(39.996, p.latitude(), 0.0001, "应解析纬度");
    }

    @Test
    void searchPoiCandidates_skipsEmptyArrayNameRecords() throws Exception {
        ToolCallback amap = toolNamed("maps_text_search");
        // 高德对缺失字段常返回空数组 []，name 为 [] 的记录应被跳过
        when(amap.call(anyString())).thenReturn(
                "{\"pois\":[{\"name\":[],\"address\":[],\"type\":\"道路\"},"
                        + "{\"name\":\"有效路口\",\"address\":\"某街\",\"type\":\"交叉口\"}]}");
        VisionMcpToolService service = new VisionMcpToolService(providerOf(amap), "test-key");

        List<PoiCandidate> candidates = service.searchPoiCandidates(task);

        assertTrue(candidates.stream().allMatch(p -> !p.name().isBlank()), "空数组 name 记录应被跳过");
        assertTrue(candidates.stream().anyMatch(p -> p.name().equals("有效路口")));
    }

    @Test
    void searchPoiCandidates_noTool_returnsEmpty() {
        VisionMcpToolService service = new VisionMcpToolService(emptyProvider(), "test-key");
        assertTrue(service.searchPoiCandidates(task).isEmpty(), "无 MCP provider 时应返回空列表");
    }

    @Test
    void buildSearchKeywords_usesSearchableKeywordsAndNormalizedCity() throws Exception {
        // 关键词应为高德可命中的地点词（非场景标签），city 去掉区后缀
        ToolCallback amap = toolNamed("maps_text_search");
        java.util.List<String> sentKeywords = new java.util.ArrayList<>();
        java.util.List<String> sentCities = new java.util.ArrayList<>();
        when(amap.call(anyString())).thenAnswer(inv -> {
            String json = inv.getArgument(0);
            com.fasterxml.jackson.databind.JsonNode node =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            sentKeywords.add(node.get("keywords").asText());
            sentCities.add(node.get("city").asText());
            return "{\"pois\":[]}";
        });
        VisionMcpToolService service = new VisionMcpToolService(providerOf(amap), "test-key");
        VisionTask shanghai = new VisionTask("目标检测", List.of("行人"),
                List.of("校园道路"), "上海市静安区", "半天", "步行");

        service.searchPoiCandidates(shanghai);

        assertFalse(sentKeywords.isEmpty(), "应发起关键词查询");
        assertTrue(sentKeywords.stream().noneMatch(k -> k.contains("校园道路")),
                "不应直接用场景标签作为关键词，实际：" + sentKeywords);
        assertTrue(sentKeywords.stream().anyMatch(k -> k.contains("学校")),
                "校园场景应映射为可命中的地点词（学校），实际：" + sentKeywords);
        assertTrue(sentKeywords.stream().anyMatch(k -> k.contains("静安区")),
                "关键词应带上区级地名以偏置到具体区域，实际：" + sentKeywords);
        assertTrue(sentCities.stream().allMatch(c -> c.equals("上海市")),
                "city 应归一化为去区后缀，实际：" + sentCities);
    }

    @Test
    void searchPoiCandidates_backfillsCoordinatesViaDetail() throws Exception {
        // text_search 不返回坐标，仅给 id；应调用 maps_search_detail 补全坐标/类型/实拍图
        ToolCallback amap = toolNamed("maps_text_search");
        when(amap.call(anyString())).thenReturn(
                "{\"pois\":[{\"id\":\"B001\",\"name\":\"上海外国语大学\",\"address\":\"大连西路550号\","
                        + "\"typecode\":\"141201\"}]}");
        ToolCallback detail = toolNamed("maps_search_detail");
        when(detail.call(anyString())).thenReturn(
                "{\"id\":\"B001\",\"name\":\"上海外国语大学\",\"location\":\"121.4833,31.2770\","
                        + "\"type\":\"科教文化服务;学校;高等院校\","
                        + "\"photos\":{\"url\":\"https://store.is.autonavi.com/showpic/abc?type=pic\"}}");
        VisionMcpToolService service = new VisionMcpToolService(providerOf(amap, detail), "test-key");

        List<PoiCandidate> candidates = service.searchPoiCandidates(task);

        assertFalse(candidates.isEmpty());
        PoiCandidate p = candidates.get(0);
        assertEquals(121.4833, p.longitude(), 0.0001, "应由详情补全经度");
        assertEquals(31.2770, p.latitude(), 0.0001, "应由详情补全纬度");
        assertTrue(p.poiType().contains("学校"), "应由详情补全类型");
        assertTrue(p.photoUrl() != null && p.photoUrl().startsWith("http"), "应由详情补全实拍图");
    }

    @Test
    void searchPoiCandidates_blankLocation_returnsEmpty() {
        ToolCallback amap = toolNamed("maps_text_search");
        VisionMcpToolService service = new VisionMcpToolService(providerOf(amap), "test-key");
        VisionTask noLocation = new VisionTask("目标检测", List.of("车辆"), List.of(), "", "半天", "步行");
        assertTrue(service.searchPoiCandidates(noLocation).isEmpty(), "无位置时不调用地图");
    }

    @Test
    void searchPoiCandidates_retriesOnQpsLimitThenSucceeds() throws Exception {
        // 首次返回限流文本，重试后返回正常 POI
        ToolCallback amap = toolNamed("maps_text_search");
        when(amap.call(anyString()))
                .thenReturn("{\"content\":[{\"type\":\"text\",\"text\":\"Text Search failed: CUQPS_HAS_EXCEEDED_THE_LIMIT\"}]}")
                .thenReturn("{\"pois\":[{\"id\":\"B1\",\"name\":\"上海大学\",\"address\":\"延长路149号\",\"location\":\"121.41,31.30\"}]}");
        VisionMcpToolService service = new VisionMcpToolService(providerOf(amap), "test-key");

        List<PoiCandidate> candidates = service.searchPoiCandidates(task);

        assertFalse(candidates.isEmpty(), "限流重试后应拿到 POI");
        assertTrue(candidates.stream().anyMatch(p -> p.name().equals("上海大学")));
    }

    @Test
    void searchPoiCandidates_toolThrows_returnsEmpty() throws Exception {
        ToolCallback amap = toolNamed("maps_text_search");
        when(amap.call(anyString())).thenThrow(new RuntimeException("mcp down"));
        VisionMcpToolService service = new VisionMcpToolService(providerOf(amap), "test-key");
        assertTrue(service.searchPoiCandidates(task).isEmpty(), "工具异常时应降级为空列表");
    }

    @Test
    void enrichImages_writesImageUrls() throws Exception {
        ToolCallback imageTool = toolNamed("searchImage");
        when(imageTool.call(anyString())).thenReturn(
                "https://images.pexels.com/a/medium.jpg,https://images.pexels.com/b/medium.jpg");
        VisionMcpToolService service = new VisionMcpToolService(providerOf(imageTool), "test-key");

        CollectionSite site = new CollectionSite("中关村路口", "中关村大街", "1.2km",
                List.of("十字路口"), List.of("车辆检测"), 86, "典型路口",
                List.of("早晚高峰采集"), List.of("注意人身安全"), List.of());

        List<CollectionSite> enriched = service.enrichImages(List.of(site), task);

        assertEquals(2, enriched.get(0).imageUrls().size(), "应写入参考图片 URL");
        assertTrue(enriched.get(0).imageUrls().get(0).startsWith("http"));
    }

    @Test
    void enrichImages_toolError_keepsEmptyImages() throws Exception {
        ToolCallback imageTool = toolNamed("searchImage");
        when(imageTool.call(anyString())).thenReturn("Error search image: 未配置 PEXELS_API_KEY");
        VisionMcpToolService service = new VisionMcpToolService(providerOf(imageTool), "test-key");

        CollectionSite site = new CollectionSite("中关村路口", "中关村大街", "1.2km",
                List.of("十字路口"), List.of("车辆检测"), 86, "典型路口",
                List.of("早晚高峰采集"), List.of("注意人身安全"), List.of());

        List<CollectionSite> enriched = service.enrichImages(List.of(site), task);

        assertTrue(enriched.get(0).imageUrls().isEmpty(), "图片搜索错误时图片应为空且不影响点位");
        assertEquals("中关村路口", enriched.get(0).name());
    }

    @Test
    void enrichImages_noTool_returnsSitesUnchanged() {
        VisionMcpToolService service = new VisionMcpToolService(emptyProvider(), "test-key");
        CollectionSite site = new CollectionSite("中关村路口", "中关村大街", "1.2km",
                List.of("十字路口"), List.of("车辆检测"), 86, "典型路口",
                List.of("早晚高峰采集"), List.of("注意人身安全"), List.of());
        List<CollectionSite> result = service.enrichImages(List.of(site), task);
        assertTrue(result.get(0).imageUrls().isEmpty());
    }

    @Test
    void enrichImages_amapPhotoTakesPriorityThenPexelsFills() throws Exception {
        ToolCallback imageTool = toolNamed("searchImage");
        when(imageTool.call(anyString())).thenReturn(
                "https://images.pexels.com/a/medium.jpg,https://images.pexels.com/b/medium.jpg");
        VisionMcpToolService service = new VisionMcpToolService(providerOf(imageTool), "test-key");

        // 已带高德实拍图时优先保留，不再用 Pexels（避免慢调用拖累响应）
        CollectionSite site = new CollectionSite("上海外国语大学", "大连西路550号", "（估算）",
                List.of("校园道路"), List.of("行人检测"), 80, "理由",
                List.of(), List.of(),
                List.of("https://store.is.autonavi.com/showpic/abc?type=pic"));

        List<CollectionSite> enriched = service.enrichImages(List.of(site), task);

        List<String> urls = enriched.get(0).imageUrls();
        assertTrue(urls.get(0).contains("autonavi.com"), "高德实拍图应排首位");
        assertTrue(urls.stream().noneMatch(u -> u.contains("pexels")),
                "已有高德实拍图时不应再用 Pexels");
    }

    @Test
    void enrichImages_noAmapPhoto_fallsBackToPexels() throws Exception {
        ToolCallback imageTool = toolNamed("searchImage");
        when(imageTool.call(anyString())).thenReturn(
                "https://images.pexels.com/a/medium.jpg,https://images.pexels.com/b/medium.jpg");
        VisionMcpToolService service = new VisionMcpToolService(providerOf(imageTool), "test-key");

        CollectionSite site = new CollectionSite("中关村路口", "中关村大街", "1.2km",
                List.of("十字路口"), List.of("车辆检测"), 86, "典型路口",
                List.of("早晚高峰采集"), List.of("注意人身安全"), List.of());

        List<CollectionSite> enriched = service.enrichImages(List.of(site), task);

        assertEquals(2, enriched.get(0).imageUrls().size(), "无高德实拍图时应用 Pexels 补足");
    }

    @Test
    void planRoute_parsesDistanceAndDuration() throws Exception {
        ToolCallback walk = toolNamed("maps_direction_walking");
        when(walk.call(anyString())).thenReturn(
                "{\"route\":{\"paths\":[{\"distance\":\"2300\",\"duration\":\"1080\"}]}}");
        VisionMcpToolService service = new VisionMcpToolService(providerOf(walk), "test-key");

        CollectionSite site = new CollectionSite("目标点", "某街", "（估算）",
                List.of("十字路口"), List.of("车辆检测"), 80, "理由",
                List.of(), List.of(), List.of(),
                116.5, 39.99, null, null, null, null);

        String[] route = service.planRoute(116.4, 39.9, site, "步行");

        assertEquals("2.3km", route[0], "应解析并格式化距离");
        assertTrue(route[1] != null && route[1].contains("18"), "应解析耗时为约 18 分钟，实际：" + route[1]);
    }

    @Test
    void planRoute_missingCoordinates_returnsEmpty() {
        ToolCallback walk = toolNamed("maps_direction_walking");
        VisionMcpToolService service = new VisionMcpToolService(providerOf(walk), "test-key");
        CollectionSite site = new CollectionSite("目标点", "某街", "（估算）",
                List.of("十字路口"), List.of("车辆检测"), 80, "理由",
                List.of(), List.of(), List.of());
        String[] route = service.planRoute(null, null, site, "步行");
        assertEquals(null, route[0]);
        assertEquals(null, route[1]);
    }

    @Test
    void enrichLocation_withoutCoordinates_returnsUnchanged() {
        VisionMcpToolService service = new VisionMcpToolService(emptyProvider(), "test-key");
        CollectionSite site = new CollectionSite("目标点", "某街", "（估算）",
                List.of("十字路口"), List.of("车辆检测"), 80, "理由",
                List.of(), List.of(), List.of());
        CollectionSite result = service.enrichLocation(site);
        assertEquals(null, result.mapImageUrl(), "无经纬度时不生成静态地图");
    }

    @Test
    void enrichLocation_withCoordinatesAndKey_buildsStaticMapAndNav() {
        VisionMcpToolService service = new VisionMcpToolService(emptyProvider(), "test-key");
        CollectionSite site = new CollectionSite("上海外国语大学", "大连西路550号", "（估算）",
                List.of("校园道路"), List.of("行人检测"), 80, "理由",
                List.of(), List.of(), List.of(),
                121.4833, 31.2770, null, null, null, null);
        CollectionSite result = service.enrichLocation(site);
        assertTrue(result.mapImageUrl() != null && result.mapImageUrl().contains("key=test-key"),
                "有坐标且配置 key 时应生成静态地图 URL");
        assertTrue(result.navUrl() != null && result.navUrl().contains("121.4833"),
                "应生成高德导航链接");
    }
}
