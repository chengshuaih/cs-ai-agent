package cn.chengshuai.csaiagent.vision.model;

import java.util.List;

/**
 * 视觉采集任务描述（结构化输入）。
 *
 * @param taskType      任务类型：目标检测/图像分割/小目标检测/深度估计/三维重建
 * @param targetObjects 目标对象：车辆、行人、交通灯、路沿...
 * @param sceneTypes    场景偏好（文本）
 * @param location      城市/学校/园区/地址
 * @param timeBudget    采集时长，如 2小时/半天/1天
 * @param transportMode 交通方式：步行/骑行/驾车
 */
public record VisionTask(
        String taskType,
        List<String> targetObjects,
        List<String> sceneTypes,
        String location,
        String timeBudget,
        String transportMode
) {

    /**
     * 便捷构造：逗号分隔的 targetObjects/sceneTypes 字符串。
     */
    public static VisionTask of(String taskType, String targetObjects, String location,
                                String timeBudget, String transportMode) {
        return new VisionTask(
                taskType,
                splitToList(targetObjects),
                List.of(),
                location,
                timeBudget,
                transportMode
        );
    }

    private static List<String> splitToList(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return List.of(csv.split("\\s*[,，]\\s*"));
    }
}
