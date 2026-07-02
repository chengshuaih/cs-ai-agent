package cn.chengshuai.csaiagent.vision.model;

import java.util.List;

/**
 * 推荐采集点位。
 *
 * @param name                点位名称
 * @param address             地址
 * @param distance            距离（不可得时标注"（估算）"）
 * @param sceneTags           场景标签
 * @param suitableTasks       适合任务
 * @param score               评分 0-100
 * @param reason              推荐理由
 * @param captureSuggestions  采集建议
 * @param riskTips            风险提示
 * @param imageUrls           参考图片 URL（来自图片检索 MCP，可空）
 * @param longitude           经度（可空）
 * @param latitude            纬度（可空）
 * @param mapImageUrl         高德静态地图缩略图 URL（可空）
 * @param travelDistance      到出发点/上一点的距离文本（可空）
 * @param travelDuration      预计耗时文本（可空）
 * @param navUrl              高德导航跳转链接（可空）
 */
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
        List<String> imageUrls,
        Double longitude,
        Double latitude,
        String mapImageUrl,
        String travelDistance,
        String travelDuration,
        String navUrl
) {
    public CollectionSite {
        // 历史记录可能缺少 imageUrls 字段，反序列化为 null；统一归一为空列表，避免下游空指针。
        if (imageUrls == null) {
            imageUrls = List.of();
        }
    }

    /**
     * 兼容旧的十参构造（无定位/导航字段）。
     */
    public CollectionSite(String name, String address, String distance,
                          List<String> sceneTags, List<String> suitableTasks, int score,
                          String reason, List<String> captureSuggestions, List<String> riskTips,
                          List<String> imageUrls) {
        this(name, address, distance, sceneTags, suitableTasks, score, reason,
                captureSuggestions, riskTips, imageUrls, null, null, null, null, null, null);
    }

    public CollectionSite withImageUrls(List<String> newImageUrls) {
        return new CollectionSite(name, address, distance, sceneTags, suitableTasks,
                score, reason, captureSuggestions, riskTips, newImageUrls,
                longitude, latitude, mapImageUrl, travelDistance, travelDuration, navUrl);
    }

    /**
     * 返回带定位与导航增强字段的副本。
     */
    public CollectionSite withLocation(Double newLongitude, Double newLatitude,
                                       String newMapImageUrl, String newNavUrl) {
        return new CollectionSite(name, address, distance, sceneTags, suitableTasks,
                score, reason, captureSuggestions, riskTips, imageUrls,
                newLongitude, newLatitude, newMapImageUrl, travelDistance, travelDuration, newNavUrl);
    }

    /**
     * 返回带行程（距离/耗时）的副本。
     */
    public CollectionSite withTravel(String newTravelDistance, String newTravelDuration) {
        return new CollectionSite(name, address, distance, sceneTags, suitableTasks,
                score, reason, captureSuggestions, riskTips, imageUrls,
                longitude, latitude, mapImageUrl, newTravelDistance, newTravelDuration, navUrl);
    }
}
