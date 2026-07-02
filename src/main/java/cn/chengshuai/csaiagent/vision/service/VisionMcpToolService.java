package cn.chengshuai.csaiagent.vision.service;

import cn.chengshuai.csaiagent.vision.model.CollectionSite;
import cn.chengshuai.csaiagent.vision.model.PoiCandidate;
import cn.chengshuai.csaiagent.vision.model.VisionSceneType;
import cn.chengshuai.csaiagent.vision.model.VisionTask;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 采集规划的 MCP 增强适配层：复用已配置的高德地图 MCP 与 Pexels 图片搜索 MCP，
 * 为采集规划提供真实 POI 候选点与点位参考图片。
 *
 * <p>所有外部调用均为尽力而为：MCP provider 缺失、工具缺失、调用异常或解析失败时，
 * 统一返回空结果，由上层（{@link CollectionPlanService}）回退到规则候选点，
 * 保证采集规划接口稳定可用。</p>
 */
@Service
public class VisionMcpToolService {

    private static final Logger log = LoggerFactory.getLogger(VisionMcpToolService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 高德 POI 文本搜索工具名。 */
    private static final String AMAP_TEXT_SEARCH = "maps_text_search";
    /** 高德 POI 详情查询工具名（按 POI id 补全坐标/类型/实拍图）。 */
    private static final String AMAP_SEARCH_DETAIL = "maps_search_detail";
    /** 高德步行/驾车/骑行路线规划工具名。 */
    private static final String AMAP_WALK = "maps_direction_walking";
    private static final String AMAP_DRIVE = "maps_direction_driving";
    private static final String AMAP_BICYCLE = "maps_bicycling";
    /** 图片搜索工具名（cs-image-search-mcp-server 暴露的 @Tool 方法名）。 */
    private static final String IMAGE_SEARCH = "searchImage";

    /**
     * 高德 Web 服务 Key（用于静态地图 URL，可空，缺失则不生成静态图）。
     * 优先取环境变量 AMAP_MAPS_API_KEY，其次取本地配置 local-api-keys.amap.maps-api-key。
     */
    private final String amapKey;

    /** 候选点总量上限，避免响应过慢与过度调用。 */
    private static final int MAX_CANDIDATES = 6;
    /** 每个场景最多取的 POI 数量。 */
    private static final int MAX_POI_PER_SCENE = 2;
    /** 每个点位最多保留的参考图数量。 */
    private static final int MAX_IMAGES_PER_SITE = 3;
    /** 高德个人 Key 并发 QPS 较低，串行调用间隔（毫秒），规避 CUQPS_HAS_EXCEEDED_THE_LIMIT。 */
    private static final long AMAP_CALL_INTERVAL_MS = 400;
    /** 命中 QPS 限流时的重试次数。 */
    private static final int AMAP_QPS_RETRY = 3;

    private final ObjectProvider<ToolCallbackProvider> toolCallbackProvider;

    public VisionMcpToolService(
            ObjectProvider<ToolCallbackProvider> toolCallbackProvider,
            @org.springframework.beans.factory.annotation.Value(
                    "${local-api-keys.amap.maps-api-key:}") String configuredAmapKey) {
        this.toolCallbackProvider = toolCallbackProvider;
        String envKey = System.getenv("AMAP_MAPS_API_KEY");
        this.amapKey = (envKey != null && !envKey.isBlank()) ? envKey : configuredAmapKey;
    }

    /**
     * 通过高德地图 MCP 查询真实 POI 候选点。无可用工具或无结果时返回空列表。
     */
    public List<PoiCandidate> searchPoiCandidates(VisionTask task) {
        if (task == null) {
            return List.of();
        }
        String location = task.location();
        if (location == null || location.isBlank()) {
            // 无明确位置时不强行调用地图，交由规则兜底
            return List.of();
        }
        ToolCallback amap = findTool(AMAP_TEXT_SEARCH);
        if (amap == null) {
            log.warn("未找到高德 POI 搜索工具 {}，降级规则候选点", AMAP_TEXT_SEARCH);
            return List.of();
        }

        List<PoiCandidate> candidates = new ArrayList<>();
        Set<String> seenNames = new LinkedHashSet<>();
        String city = normalizeCity(location);
        for (String keyword : buildSearchKeywords(task)) {
            int remaining = MAX_CANDIDATES - candidates.size();
            if (remaining <= 0) {
                break;
            }
            candidates.addAll(queryAmap(amap, keyword, city, seenNames, remaining));
        }
        log.info("高德 POI 搜索完成，location={}，候选点数={}", location, candidates.size());
        if (candidates.size() > MAX_CANDIDATES) {
            return new ArrayList<>(candidates.subList(0, MAX_CANDIDATES));
        }
        return candidates;
    }

    /**
     * 为采集点位补充参考图片：高德实拍图（已在 site.imageUrls 中）优先，
     * 不足 {@link #MAX_IMAGES_PER_SITE} 时用 Pexels 场景图补足。
     */
    public List<CollectionSite> enrichImages(List<CollectionSite> sites, VisionTask task) {
        if (sites == null || sites.isEmpty()) {
            return sites == null ? List.of() : sites;
        }
        ToolCallback imageTool = findTool(IMAGE_SEARCH);
        List<CollectionSite> enriched = new ArrayList<>(sites.size());
        for (CollectionSite site : sites) {
            List<String> merged = new ArrayList<>();
            // 高德实拍图优先（更贴近具体地点）
            if (site.imageUrls() != null) {
                for (String url : site.imageUrls()) {
                    if (url != null && url.startsWith("http") && !merged.contains(url)) {
                        merged.add(url);
                    }
                }
            }
            // 仅当没有任何高德实拍图时才用 Pexels 兜底，避免图片 MCP 慢调用拖累整体响应
            if (merged.isEmpty() && imageTool != null) {
                for (String url : searchImages(imageTool, buildImageQuery(site, task))) {
                    if (!merged.contains(url)) {
                        merged.add(url);
                    }
                    if (merged.size() >= MAX_IMAGES_PER_SITE) {
                        break;
                    }
                }
            }
            enriched.add(merged.isEmpty() ? site : site.withImageUrls(merged));
        }
        return enriched;
    }

    /**
     * 为点位补充高德静态地图缩略图与导航链接（依赖经纬度）。失败时保持原点位。
     */
    public CollectionSite enrichLocation(CollectionSite site) {
        if (site == null || site.longitude() == null || site.latitude() == null) {
            return site;
        }
        String mapImageUrl = buildStaticMapUrl(site.longitude(), site.latitude());
        String navUrl = buildNavUrl(site);
        return site.withLocation(site.longitude(), site.latitude(), mapImageUrl, navUrl);
    }

    /**
     * 调用高德路线规划，返回 [距离文本, 耗时文本]；失败或缺工具返回 [null, null]。
     *
     * @param originLng 出发点经度（可空）
     * @param originLat 出发点纬度（可空）
     * @param site      目标点位
     * @param mode      出行方式（步行/驾车/骑行）
     */
    public String[] planRoute(Double originLng, Double originLat, CollectionSite site, String mode) {
        String[] empty = new String[]{null, null};
        if (site == null || site.longitude() == null || site.latitude() == null
                || originLng == null || originLat == null) {
            return empty;
        }
        ToolCallback routeTool = findTool(routeToolName(mode));
        if (routeTool == null) {
            return empty;
        }
        try {
            String input = OBJECT_MAPPER.writeValueAsString(java.util.Map.of(
                    "origin", originLng + "," + originLat,
                    "destination", site.longitude() + "," + site.latitude()
            ));
            JsonNode root = parseToolResult(callAmapWithRetry(routeTool, input));
            if (root == null) {
                return empty;
            }
            JsonNode path = locateRoutePath(root);
            if (path == null) {
                return empty;
            }
            String distance = formatDistance(textOf(path, "distance"));
            String duration = formatDuration(textOf(path, "duration"));
            return new String[]{distance, duration};
        } catch (Exception e) {
            log.warn("高德路线规划失败，mode={}：{}", mode, e.getMessage());
            return empty;
        }
    }

    private String routeToolName(String mode) {
        if (mode == null) {
            return AMAP_WALK;
        }
        if (mode.contains("驾") || (mode.contains("车") && !mode.contains("骑"))) {
            return AMAP_DRIVE;
        }
        if (mode.contains("骑")) {
            return AMAP_BICYCLE;
        }
        return AMAP_WALK;
    }

    private JsonNode locateRoutePath(JsonNode root) {
        // 兼容 {route:{paths:[{distance,duration}]}} 或 {paths:[...]} 或 {results:[...]}
        JsonNode route = root.has("route") ? root.get("route") : root;
        for (String key : new String[]{"paths", "results", "transits"}) {
            if (route.has(key) && route.get(key).isArray() && !route.get(key).isEmpty()) {
                return route.get(key).get(0);
            }
        }
        if (route.has("distance") || route.has("duration")) {
            return route;
        }
        return null;
    }

    private String formatDistance(String meters) {
        if (meters == null || meters.isBlank()) {
            return null;
        }
        try {
            double m = Double.parseDouble(meters);
            return m >= 1000 ? String.format("%.1fkm", m / 1000) : Math.round(m) + "m";
        } catch (Exception e) {
            return null;
        }
    }

    private String formatDuration(String seconds) {
        if (seconds == null || seconds.isBlank()) {
            return null;
        }
        try {
            long s = (long) Double.parseDouble(seconds);
            long min = Math.round(s / 60.0);
            return "约 " + Math.max(1, min) + " 分钟";
        } catch (Exception e) {
            return null;
        }
    }

    private String buildStaticMapUrl(double lng, double lat) {
        if (amapKey == null || amapKey.isBlank()) {
            return null;
        }
        String loc = lng + "," + lat;
        return "https://restapi.amap.com/v3/staticmap?location=" + loc
                + "&zoom=16&size=300*180&markers=mid,,A:" + loc + "&key=" + amapKey;
    }

    private String buildNavUrl(CollectionSite site) {
        String name = site.name() == null ? "目标点位" : site.name();
        return "https://uri.amap.com/marker?position=" + site.longitude() + "," + site.latitude()
                + "&name=" + name + "&src=cs-ai-agent&coordinate=gaode";
    }

    private List<String> buildSearchKeywords(VisionTask task) {
        // 收集场景类型，再映射为高德可命中的地点关键词（场景标签本身命中率极低）
        Set<VisionSceneType> scenes = new LinkedHashSet<>();
        if (task.sceneTypes() != null) {
            for (String s : task.sceneTypes()) {
                VisionSceneType t = VisionSceneType.fromText(s);
                if (t != VisionSceneType.GENERAL) {
                    scenes.add(t);
                }
            }
        }
        if (task.targetObjects() != null) {
            for (String obj : task.targetObjects()) {
                VisionSceneType type = VisionSceneType.fromText(obj);
                if (type != VisionSceneType.GENERAL) {
                    scenes.add(type);
                }
            }
        }
        if (scenes.isEmpty()) {
            scenes.add(VisionSceneType.CAMPUS);
            scenes.add(VisionSceneType.PARKING);
            scenes.add(VisionSceneType.PEDESTRIAN_PATH);
        }
        // 高德 maps_text_search：keywords 用可检索的地点词，城市走 city 参数；
        // 同时把「区/县」级地名拼到关键词前，把结果偏置到用户填写的具体区域。
        String district = extractDistrict(task.location());
        Set<String> keywords = new LinkedHashSet<>();
        for (VisionSceneType scene : scenes) {
            for (String word : scene.getSearchKeywords()) {
                keywords.add(district.isBlank() ? word : district + word);
            }
        }
        return new ArrayList<>(keywords);
    }

    /**
     * 提取地名中「市」之后的区/县级片段（如"上海市静安区"→"静安区"）。
     * 无「市」字或无区级片段返回空串。
     */
    private String extractDistrict(String location) {
        if (location == null || location.isBlank()) {
            return "";
        }
        String c = location.trim();
        int idxCity = c.indexOf("市");
        if (idxCity >= 0 && idxCity < c.length() - 1) {
            return c.substring(idxCity + 1);
        }
        return "";
    }

    /**
     * 归一化高德 city 参数：去掉过细的「区/县」后缀（如"上海市静安区"→"上海市"），
     * 实测带区后缀会显著降低甚至清零 text_search 命中。
     */
    private String normalizeCity(String city) {
        if (city == null || city.isBlank()) {
            return city;
        }
        String c = city.trim();
        int idxCity = c.indexOf("市");
        if (idxCity > 0 && idxCity < c.length() - 1) {
            // 形如「上海市静安区」→ 取到「市」为止
            return c.substring(0, idxCity + 1);
        }
        return c;
    }

    private List<PoiCandidate> queryAmap(ToolCallback amap, String keyword, String city,
                                         Set<String> seenNames, int remaining) {
        List<PoiCandidate> result = new ArrayList<>();
        if (remaining <= 0) {
            return result;
        }
        int cap = Math.min(MAX_POI_PER_SCENE, remaining);
        try {
            String input = OBJECT_MAPPER.writeValueAsString(java.util.Map.of(
                    "keywords", keyword,
                    "city", city
            ));
            String raw = callAmapWithRetry(amap, input);
            JsonNode root = parseToolResult(raw);
            JsonNode pois = locatePoiArray(root);
            if (pois == null || !pois.isArray()) {
                return result;
            }
            ToolCallback detailTool = findTool(AMAP_SEARCH_DETAIL);
            int taken = 0;
            for (JsonNode poi : pois) {
                if (taken >= cap) {
                    break;
                }
                String name = textOf(poi, "name");
                if (name.isBlank() || !seenNames.add(name)) {
                    continue;
                }
                String address = textOf(poi, "address");
                String type = textOf(poi, "type");
                String poiId = textOf(poi, "id");
                String photoUrl = extractPhotoUrl(poi);
                double[] lngLat = parseLocation(textOf(poi, "location"));

                // maps_text_search 不返回坐标，按 POI id 二次查询详情补全坐标/类型/实拍图
                PoiDetail detail = (lngLat == null && !poiId.isBlank())
                        ? fetchPoiDetail(detailTool, poiId) : null;
                if (detail != null) {
                    if (lngLat == null) {
                        lngLat = detail.lngLat();
                    }
                    // 详情的 type 是可读分类（如"科教文化服务;学校"），优先于 text_search 的 typecode
                    if (detail.type() != null) {
                        type = detail.type();
                    }
                    if ((photoUrl == null || photoUrl.isBlank()) && detail.photoUrl() != null) {
                        photoUrl = detail.photoUrl();
                    }
                }
                if (type.isBlank()) {
                    type = textOf(poi, "typecode");
                }

                result.add(new PoiCandidate(
                        name,
                        address.isBlank() ? city + keyword : address,
                        type.isBlank() ? keyword : type,
                        -1,
                        -1,
                        lngLat == null ? null : lngLat[0],
                        lngLat == null ? null : lngLat[1],
                        poiId.isBlank() ? null : poiId,
                        (photoUrl == null || photoUrl.isBlank()) ? null : photoUrl
                ));
                taken++;
            }
        } catch (Exception e) {
            log.warn("高德 POI 搜索失败，关键词={}，降级规则候选点：{}", keyword, e.getMessage());
        }
        return result;
    }

    /**
     * 按 POI id 查询高德详情，补全坐标/类型/实拍图。失败返回 null。
     */
    private PoiDetail fetchPoiDetail(ToolCallback detailTool, String poiId) {
        if (detailTool == null || poiId == null || poiId.isBlank()) {
            return null;
        }
        try {
            String input = OBJECT_MAPPER.writeValueAsString(java.util.Map.of("id", poiId));
            JsonNode root = parseToolResult(callAmapWithRetry(detailTool, input));
            if (root == null) {
                return null;
            }
            // 详情可能直接在根，也可能在 data/result 下
            JsonNode node = root;
            for (String key : new String[]{"data", "result", "poi"}) {
                if (root.has(key) && root.get(key).isObject()) {
                    node = root.get(key);
                    break;
                }
            }
            double[] lngLat = parseLocation(textOf(node, "location"));
            String type = textOf(node, "type");
            String photoUrl = extractPhotoUrl(node);
            if (lngLat == null && (type == null || type.isBlank())
                    && (photoUrl == null || photoUrl.isBlank())) {
                return null;
            }
            return new PoiDetail(lngLat, type.isBlank() ? null : type,
                    (photoUrl == null || photoUrl.isBlank()) ? null : photoUrl);
        } catch (Exception e) {
            log.warn("高德 POI 详情查询失败，id={}：{}", poiId, e.getMessage());
            return null;
        }
    }

    /**
     * 调用高德工具并对 QPS 限流（CUQPS_HAS_EXCEEDED_THE_LIMIT）做退避重试。
     * 个人 Key 并发受限，串行+小间隔可显著降低限流概率。
     */
    private String callAmapWithRetry(ToolCallback tool, String input) {
        String raw = null;
        for (int attempt = 0; attempt <= AMAP_QPS_RETRY; attempt++) {
            try {
                raw = tool.call(input);
                if (raw == null || !raw.contains("CUQPS_HAS_EXCEEDED_THE_LIMIT")) {
                    break;
                }
            } catch (Exception e) {
                // 高德 MCP 会把限流以异常文本抛出，命中则退避重试，否则直接抛出
                if (e.getMessage() == null || !e.getMessage().contains("CUQPS_HAS_EXCEEDED_THE_LIMIT")
                        || attempt == AMAP_QPS_RETRY) {
                    throw e;
                }
            }
            // 命中限流：退避后重试（最后一次不再额外等待）
            if (attempt < AMAP_QPS_RETRY) {
                sleepQuietly(AMAP_CALL_INTERVAL_MS * (attempt + 2));
            }
        }
        if (raw != null && raw.contains("CUQPS_HAS_EXCEEDED_THE_LIMIT")) {
            log.warn("高德调用仍被 QPS 限流，已重试 {} 次仍失败，本次降级", AMAP_QPS_RETRY);
        }
        // 调用后小憩，降低后续串行调用触发限流的概率
        sleepQuietly(AMAP_CALL_INTERVAL_MS);
        return raw;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 兼容 photos.url（对象）与 photos[0].url（数组）两种结构提取首张实拍图。
     */
    private String extractPhotoUrl(JsonNode poi) {
        if (poi == null) {
            return null;
        }
        JsonNode photos = poi.get("photos");
        if (photos == null || photos.isNull()) {
            return null;
        }
        if (photos.isArray()) {
            for (JsonNode p : photos) {
                String url = textOf(p, "url");
                if (!url.isBlank()) {
                    return url;
                }
            }
            return null;
        }
        if (photos.isObject()) {
            String url = textOf(photos, "url");
            return url.isBlank() ? null : url;
        }
        return null;
    }

    /** 高德 POI 详情轻量载体。 */
    private record PoiDetail(double[] lngLat, String type, String photoUrl) {
    }


    /**
     * 解析高德 "lng,lat" 字符串为 [经度, 纬度]，无法解析返回 null。
     */
    private double[] parseLocation(String location) {
        if (location == null || location.isBlank() || !location.contains(",")) {
            return null;
        }
        try {
            String[] parts = location.split(",");
            return new double[]{Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim())};
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> searchImages(ToolCallback imageTool, String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        try {
            String input = OBJECT_MAPPER.writeValueAsString(java.util.Map.of("query", query));
            String raw = imageTool.call(input);
            if (raw == null || raw.isBlank() || raw.startsWith("Error")) {
                return List.of();
            }
            List<String> urls = new ArrayList<>();
            for (String part : raw.split(",")) {
                String url = part.trim();
                if (url.startsWith("http")) {
                    urls.add(url);
                }
                if (urls.size() >= MAX_IMAGES_PER_SITE) {
                    break;
                }
            }
            return urls;
        } catch (Exception e) {
            log.warn("图片搜索失败，query={}：{}", query, e.getMessage());
            return List.of();
        }
    }

    private String buildImageQuery(CollectionSite site, VisionTask task) {
        StringBuilder sb = new StringBuilder();
        if (site.sceneTags() != null && !site.sceneTags().isEmpty()) {
            sb.append(site.sceneTags().get(0));
        } else if (site.name() != null) {
            sb.append(site.name());
        }
        if (task != null && task.targetObjects() != null && !task.targetObjects().isEmpty()) {
            sb.append(' ').append(task.targetObjects().get(0));
        }
        return sb.toString().trim();
    }

    /**
     * 解析 MCP 工具返回结果，兼容三种形态：
     * 1) 直接 JSON 对象/数组；
     * 2) MCP content 包裹 {"content":[{"type":"text","text":"<json>"}]}；
     * 3) 纯文本（无法解析时返回 null）。
     */
    JsonNode parseToolResult(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(raw);
        } catch (Exception e) {
            return null;
        }
        // content 包裹有两种形态：
        //  a) 对象：{"content":[{"type":"text","text":"<json>"}]}
        //  b) 数组：[{"type":"text","text":"<json>"}]（高德 MCP 实际返回此形态）
        JsonNode content = root.isArray() ? root : root.get("content");
        if (content != null && content.isArray() && !content.isEmpty()) {
            StringBuilder text = new StringBuilder();
            for (JsonNode item : content) {
                JsonNode t = item.get("text");
                if (t != null && !t.isNull()) {
                    text.append(t.asText(""));
                }
            }
            if (text.length() > 0) {
                try {
                    return OBJECT_MAPPER.readTree(text.toString());
                } catch (Exception e) {
                    // 内层不是 JSON，保留外层
                    return root;
                }
            }
        }
        return root;
    }

    /**
     * 高德返回结构兼容定位：根级/内层的 pois、results、data.pois 或直接数组。
     */
    private JsonNode locatePoiArray(JsonNode root) {
        if (root == null) {
            return null;
        }
        if (root.isArray()) {
            return root;
        }
        for (String key : new String[]{"pois", "results", "poiList"}) {
            if (root.has(key) && root.get(key).isArray()) {
                return root.get(key);
            }
        }
        if (root.has("data")) {
            JsonNode data = root.get("data");
            if (data.isArray()) {
                return data;
            }
            for (String key : new String[]{"pois", "results", "poiList"}) {
                if (data.has(key) && data.get(key).isArray()) {
                    return data.get(key);
                }
            }
        }
        return null;
    }

    private String textOf(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return "";
        }
        // 高德常把缺失字段返回为空数组 []，视为空
        if (value.isArray()) {
            return "";
        }
        return value.asText("").trim();
    }

    private ToolCallback findTool(String toolName) {
        ToolCallbackProvider provider = toolCallbackProvider.getIfAvailable();
        if (provider == null) {
            return null;
        }
        ToolCallback[] callbacks;
        try {
            callbacks = provider.getToolCallbacks();
        } catch (Exception e) {
            log.warn("获取 MCP 工具回调失败：{}", e.getMessage());
            return null;
        }
        if (callbacks == null) {
            return null;
        }
        for (ToolCallback callback : callbacks) {
            String name = callback.getToolDefinition().name();
            if (name != null && (name.equals(toolName) || name.endsWith(toolName))) {
                return callback;
            }
        }
        return null;
    }
}
