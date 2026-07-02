package cn.chengshuai.csaiagent.vision.service;

import cn.chengshuai.csaiagent.tools.PDFGenerationTool;
import cn.chengshuai.csaiagent.vision.model.CollectionPlan;
import cn.chengshuai.csaiagent.vision.model.ExperimentPlan;
import cn.chengshuai.csaiagent.vision.model.VisionReport;
import cn.chengshuai.csaiagent.vision.report.CollectionPlanReportBuilder;
import cn.chengshuai.csaiagent.vision.report.ExperimentPlanReportBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 报告生成统一入口（单一中枢）：构建结构化文本 → 复用 {@link PDFGenerationTool} 生成 PDF →
 * 落库 {@link VisionReport} 元数据。VisionAgent 与控制器均经此服务生成报告，
 * 不直接调用 PDFGenerationTool。
 */
@Service
public class VisionReportService {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final CollectionPlanReportBuilder collectionPlanReportBuilder;
    private final ExperimentPlanReportBuilder experimentPlanReportBuilder;
    private final VisionRecordStore recordStore;
    private final PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();

    public VisionReportService(CollectionPlanReportBuilder collectionPlanReportBuilder,
                               ExperimentPlanReportBuilder experimentPlanReportBuilder,
                               VisionRecordStore recordStore) {
        this.collectionPlanReportBuilder = collectionPlanReportBuilder;
        this.experimentPlanReportBuilder = experimentPlanReportBuilder;
        this.recordStore = recordStore;
    }

    /**
     * 生成采集计划 PDF 报告并落库。
     */
    public VisionReport generateCollectionReport(CollectionPlan plan, String projectId) {
        String content = collectionPlanReportBuilder.build(plan);
        return generate("collection_plan", plan.title(), content, projectId);
    }

    /**
     * 生成实验计划 PDF 报告并落库。
     */
    public VisionReport generateExperimentReport(ExperimentPlan plan, String projectId) {
        String content = experimentPlanReportBuilder.build(plan);
        String title = plan.taskType() + " 实验流程计划";
        return generate("experiment_plan", title, content, projectId);
    }

    /**
     * 通用报告生成。
     *
     * @param type      报告类型
     * @param title     标题
     * @param content   纯文本内容
     * @param projectId 所属项目（可空）
     */
    public VisionReport generate(String type, String title, String content, String projectId) {
        String ts = LocalDateTime.now().format(TS_FMT);
        String fileName = type + "_" + ts + ".pdf";
        String toolResult = pdfGenerationTool.generatePDF(fileName, content, recordStore.reportsDir());
        String pdfPath = extractPath(toolResult, fileName);

        VisionReport report = new VisionReport(
                "report-" + ts + "-" + UUID.randomUUID().toString().substring(0, 6),
                title,
                type,
                projectId,
                LocalDateTime.now().toString(),
                pdfPath
        );
        recordStore.saveReport(report);
        return report;
    }

    private String extractPath(String toolResult, String fileName) {
        if (toolResult != null && toolResult.contains("：")) {
            return toolResult.substring(toolResult.indexOf("：") + 1).trim();
        }
        return fileName;
    }
}
