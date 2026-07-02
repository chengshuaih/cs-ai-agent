package cn.chengshuai.csaiagent.vision.model;

import java.util.List;

/**
 * 一套采集方案：一组点位 + 一条路线 + 推荐理由。
 *
 * @param id              方案 ID
 * @param title           方案标题，如"方案A：高密度路口优先"
 * @param recommendReason 推荐理由
 * @param sites           点位列表
 * @param routeSummary    路线与时段建议
 * @param totalDistance   全程距离（可空）
 * @param totalDuration   全程耗时（可空）
 * @param transportMode   出行方式
 */
public record CollectionScheme(
        String id,
        String title,
        String recommendReason,
        List<CollectionSite> sites,
        String routeSummary,
        String totalDistance,
        String totalDuration,
        String transportMode
) {
}
