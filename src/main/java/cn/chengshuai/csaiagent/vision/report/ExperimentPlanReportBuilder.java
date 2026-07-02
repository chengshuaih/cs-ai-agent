package cn.chengshuai.csaiagent.vision.report;

import cn.chengshuai.csaiagent.vision.model.ExperimentPlan;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 实验计划报告文本构建器（纯文本，章节固定，字段与 {@link ExperimentPlan} 对应）。
 */
@Component
public class ExperimentPlanReportBuilder {

    public String build(ExperimentPlan plan) {
        StringBuilder sb = new StringBuilder();
        sb.append(nv(plan.taskType())).append(" 实验流程计划\n");
        sb.append("================\n\n");

        sb.append("一、实验目标\n").append(nv(plan.objective())).append("\n\n");
        sb.append("二、数据准备\n");
        appendBullets(sb, plan.dataPrep());
        sb.append("\n三、标注设计\n");
        appendBullets(sb, plan.annotationDesign());
        sb.append("\n四、数据集划分\n").append(nv(plan.datasetSplit())).append("\n\n");
        sb.append("五、推荐模型\n");
        appendBullets(sb, plan.recommendedModels());
        sb.append("\n六、训练步骤\n");
        appendBullets(sb, plan.trainingSteps());
        sb.append("\n七、评价指标\n");
        appendBullets(sb, plan.metrics());
        sb.append("\n八、风险与注意事项\n");
        appendBullets(sb, plan.risks());

        return sb.toString();
    }

    private void appendBullets(StringBuilder sb, List<String> items) {
        if (items == null || items.isEmpty()) {
            sb.append("- （无）\n");
            return;
        }
        for (String item : items) {
            sb.append("- ").append(item).append("\n");
        }
    }

    private String nv(String s) {
        return s == null || s.isBlank() ? "（未指定）" : s;
    }
}
