package cn.chengshuai.csaiagent.vision.model;

import java.util.List;

/**
 * 视觉场景类型，及其适配的采集任务。
 */
public enum VisionSceneType {

    INTERSECTION("十字路口", List.of("车辆检测", "行人检测", "交通灯识别")),
    PARKING("停车场", List.of("车辆检测", "遮挡场景")),
    CAMPUS("校园道路", List.of("行人检测", "小目标采集")),
    INDUSTRIAL_PARK("工业园区", List.of("园区道路", "低速场景")),
    PEDESTRIAN_PATH("非机动车道", List.of("行人", "骑行者")),
    LOW_LIGHT("低光照", List.of("夜间视觉", "明暗变化")),
    REFLECTIVE_ROAD("反光路面", List.of("积水反光", "浮空伪影")),
    GENERAL("通用场景", List.of("通用采集"));

    private final String label;
    private final List<String> suitableTasks;

    VisionSceneType(String label, List<String> suitableTasks) {
        this.label = label;
        this.suitableTasks = suitableTasks;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 用于高德 POI 文本搜索的真实可命中关键词。
     * 场景标签（如"校园道路""十字路口"）在高德 text_search 中命中率极低甚至为 0，
     * 需映射为高德可检索的地点关键词（如"学校""商场"）。
     */
    public List<String> getSearchKeywords() {
        return switch (this) {
            case INTERSECTION -> List.of("商圈", "购物中心");
            case PARKING -> List.of("停车场", "商场");
            case CAMPUS -> List.of("学校", "大学");
            case INDUSTRIAL_PARK -> List.of("产业园", "科技园");
            case PEDESTRIAN_PATH -> List.of("公园", "广场");
            case LOW_LIGHT -> List.of("地铁站", "隧道");
            case REFLECTIVE_ROAD -> List.of("公园", "广场");
            case GENERAL -> List.of("商场", "公园");
        };
    }

    public List<String> getSuitableTasks() {
        return suitableTasks;
    }

    /**
     * 根据文本（POI 名称 / 类型 / 场景描述）匹配场景类型，无匹配返回 GENERAL。
     */
    public static VisionSceneType fromText(String text) {
        if (text == null || text.isBlank()) {
            return GENERAL;
        }
        String t = text.toLowerCase();
        for (VisionSceneType type : values()) {
            if (type == GENERAL) {
                continue;
            }
            if (t.contains(type.label)) {
                return type;
            }
        }
        // 关键词补充匹配
        if (t.contains("路口") || t.contains("交叉") || t.contains("红绿灯")) {
            return INTERSECTION;
        }
        if (t.contains("停车")) {
            return PARKING;
        }
        if (t.contains("学校") || t.contains("校园") || t.contains("大学") || t.contains("学院")) {
            return CAMPUS;
        }
        if (t.contains("园区") || t.contains("工业")) {
            return INDUSTRIAL_PARK;
        }
        if (t.contains("非机动车") || t.contains("自行车") || t.contains("骑行")) {
            return PEDESTRIAN_PATH;
        }
        if (t.contains("夜") || t.contains("隧道") || t.contains("地下")) {
            return LOW_LIGHT;
        }
        if (t.contains("积水") || t.contains("反光") || t.contains("湿滑")) {
            return REFLECTIVE_ROAD;
        }
        return GENERAL;
    }
}
