package cn.chengshuai.csaiagent.vision.report;

import cn.chengshuai.csaiagent.vision.model.CollectionPlan;
import cn.chengshuai.csaiagent.vision.model.CollectionSite;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 采集计划报告文本构建器。
 *
 * <p>因 PDFGenerationTool 仅支持单段落纯文本，构建器只产出以换行分段的纯文本，
 * 章节顺序固定，字段与 {@link CollectionPlan} 一一对应可追溯。</p>
 */
@Component
public class CollectionPlanReportBuilder {

    public String build(CollectionPlan plan) {
        StringBuilder sb = new StringBuilder();
        sb.append(plan.title()).append("\n");
        sb.append("================\n\n");

        // 1. 任务概述
        sb.append("一、任务概述\n");
        if (plan.task() != null) {
            sb.append("任务类型：").append(nv(plan.task().taskType())).append("\n");
            sb.append("目标对象：").append(joinList(plan.task().targetObjects())).append("\n");
            sb.append("采集位置：").append(nv(plan.task().location())).append("\n");
            sb.append("时间预算：").append(nv(plan.task().timeBudget())).append("\n");
            sb.append("交通方式：").append(nv(plan.task().transportMode())).append("\n");
        }
        sb.append("\n");

        // 2. 推荐采集点位
        sb.append("二、推荐采集点位\n");
        List<CollectionSite> sites = plan.sites();
        if (sites == null || sites.isEmpty()) {
            sb.append("（暂无候选点位）\n");
        } else {
            int idx = 1;
            for (CollectionSite s : sites) {
                sb.append(idx++).append(". ").append(nv(s.name()))
                        .append("（得分 ").append(s.score()).append("）\n");
                sb.append("   地址：").append(nv(s.address())).append("\n");
                sb.append("   距离：").append(nv(s.distance())).append("\n");
                sb.append("   场景标签：").append(joinList(s.sceneTags())).append("\n");
                sb.append("   适合任务：").append(joinList(s.suitableTasks())).append("\n");
                sb.append("   推荐理由：").append(nv(s.reason())).append("\n");
                sb.append("   采集建议：").append(joinList(s.captureSuggestions())).append("\n");
                sb.append("   风险提示：").append(joinList(s.riskTips())).append("\n");
            }
        }
        sb.append("\n");

        // 3. 路线与时段建议
        sb.append("三、路线与时段建议\n");
        sb.append(nv(plan.routeSummary())).append("\n\n");

        // 4. 采集清单
        sb.append("四、采集清单\n");
        appendBullets(sb, plan.checklist());
        sb.append("\n");

        // 5. 标注与隐私提示
        sb.append("五、标注与隐私提示\n");
        sb.append("标注建议：\n");
        appendBullets(sb, plan.annotationGuide());
        sb.append("隐私提示：\n");
        appendBullets(sb, plan.privacyTips());

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

    private String joinList(List<String> list) {
        return list == null || list.isEmpty() ? "（无）" : String.join("、", list);
    }

    private String nv(String s) {
        return s == null || s.isBlank() ? "（未指定）" : s;
    }
}
