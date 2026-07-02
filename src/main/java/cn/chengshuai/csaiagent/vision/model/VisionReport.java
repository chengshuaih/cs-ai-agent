package cn.chengshuai.csaiagent.vision.model;

/**
 * 报告记录（落库元数据）。
 *
 * @param id        报告 ID
 * @param title     标题
 * @param type      collection_plan / experiment_plan / research_summary / stage_summary
 * @param projectId 所属采集项目（可空）
 * @param createdAt 创建时间
 * @param pdfPath   PDF 路径
 */
public record VisionReport(
        String id,
        String title,
        String type,
        String projectId,
        String createdAt,
        String pdfPath
) {
}
