package cn.chengshuai.csaiagent.vision.model;

/**
 * 地图候选点位（采集规划的原始输入，来自地图 MCP 或预置数据）。
 *
 * @param name       名称
 * @param address    地址
 * @param poiType    POI 类型文本（可空）
 * @param distanceKm 距离（公里，&lt;0 表示不可得，需估算）
 * @param nearbyPoiTypeCount 周边 POI 类型多样性计数（&lt;0 表示不可得）
 * @param longitude  经度（可空）
 * @param latitude   纬度（可空）
 * @param poiId      高德 POI ID（可空，用于二次详情查询）
 * @param photoUrl   高德实拍图 URL（可空，优先作为点位参考图）
 */
public record PoiCandidate(
        String name,
        String address,
        String poiType,
        double distanceKm,
        int nearbyPoiTypeCount,
        Double longitude,
        Double latitude,
        String poiId,
        String photoUrl
) {
    public static PoiCandidate of(String name, String address, String poiType) {
        return new PoiCandidate(name, address, poiType, -1, -1, null, null, null, null);
    }

    /**
     * 兼容旧的五参构造（无经纬度、无 POI 详情）。
     */
    public PoiCandidate(String name, String address, String poiType,
                        double distanceKm, int nearbyPoiTypeCount) {
        this(name, address, poiType, distanceKm, nearbyPoiTypeCount, null, null, null, null);
    }

    /**
     * 兼容七参构造（含经纬度、无 POI 详情）。
     */
    public PoiCandidate(String name, String address, String poiType,
                        double distanceKm, int nearbyPoiTypeCount,
                        Double longitude, Double latitude) {
        this(name, address, poiType, distanceKm, nearbyPoiTypeCount, longitude, latitude, null, null);
    }
}
