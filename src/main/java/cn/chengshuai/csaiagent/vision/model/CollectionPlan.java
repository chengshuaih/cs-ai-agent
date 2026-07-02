package cn.chengshuai.csaiagent.vision.model;

import java.util.List;

/**
 * 采集计划（结构化输出）。
 *
 * @param id              计划 ID
 * @param title           标题
 * @param task            关联视觉任务
 * @param sites           推荐点位列表
 * @param routeSummary    路线与时段建议
 * @param checklist       采集清单
 * @param annotationGuide 标注建议
 * @param privacyTips     隐私与安全提示
 * @param pdfPath         PDF 路径（未生成则为 null）
 * @param schemes         多套可选方案（首选方案点位同时回填到 sites 以兼容）
 */
public record CollectionPlan(
        String id,
        String title,
        VisionTask task,
        List<CollectionSite> sites,
        String routeSummary,
        List<String> checklist,
        List<String> annotationGuide,
        List<String> privacyTips,
        String pdfPath,
        List<CollectionScheme> schemes
) {

    public CollectionPlan {
        if (schemes == null) {
            schemes = List.of();
        }
    }

    /**
     * 兼容旧的九参构造（无多方案）。
     */
    public CollectionPlan(String id, String title, VisionTask task, List<CollectionSite> sites,
                          String routeSummary, List<String> checklist, List<String> annotationGuide,
                          List<String> privacyTips, String pdfPath) {
        this(id, title, task, sites, routeSummary, checklist, annotationGuide, privacyTips, pdfPath, List.of());
    }

    /**
     * 返回带 pdfPath 的副本。
     */
    public CollectionPlan withPdfPath(String newPdfPath) {
        return new CollectionPlan(id, title, task, sites, routeSummary,
                checklist, annotationGuide, privacyTips, newPdfPath, schemes);
    }
}
