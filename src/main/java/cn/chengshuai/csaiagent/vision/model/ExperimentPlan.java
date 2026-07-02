package cn.chengshuai.csaiagent.vision.model;

import java.util.List;

/**
 * 视觉实验流程计划。
 *
 * @param id               计划 ID
 * @param taskType         任务类型
 * @param objective        实验目标
 * @param dataPrep         数据准备步骤
 * @param annotationDesign 标注设计
 * @param datasetSplit     数据集划分
 * @param recommendedModels 推荐模型
 * @param trainingSteps    训练步骤
 * @param metrics          评价指标
 * @param risks            风险
 * @param pdfPath          PDF 路径（未生成则为 null）
 */
public record ExperimentPlan(
        String id,
        String taskType,
        String objective,
        List<String> dataPrep,
        List<String> annotationDesign,
        String datasetSplit,
        List<String> recommendedModels,
        List<String> trainingSteps,
        List<String> metrics,
        List<String> risks,
        String pdfPath
) {

    public ExperimentPlan withPdfPath(String newPdfPath) {
        return new ExperimentPlan(id, taskType, objective, dataPrep, annotationDesign,
                datasetSplit, recommendedModels, trainingSteps, metrics, risks, newPdfPath);
    }
}
