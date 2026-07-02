package cn.chengshuai.csaiagent.vision.model;

import java.util.List;

/**
 * 采集项目（工作区）聚合根。
 *
 * @param id                项目 ID
 * @param name              项目名称
 * @param description       项目描述
 * @param visionTask        关联视觉任务
 * @param collectionPlanIds 采集计划 ID 列表
 * @param experimentPlanIds 实验计划 ID 列表
 * @param reportIds         报告 ID 列表
 * @param createdAt         创建时间
 * @param updatedAt         更新时间
 */
public record VisionProject(
        String id,
        String name,
        String description,
        VisionTask visionTask,
        List<String> collectionPlanIds,
        List<String> experimentPlanIds,
        List<String> reportIds,
        String createdAt,
        String updatedAt
) {
}
