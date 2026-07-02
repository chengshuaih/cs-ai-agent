package cn.chengshuai.csaiagent.vision.service;

import cn.chengshuai.csaiagent.vision.model.ExperimentPlan;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 视觉实验流程规划：任务类型 → 模板填充 → {@link ExperimentPlan}，并落库。
 * 纯本地逻辑，不依赖外部接口。
 */
@Service
public class ExperimentPlanService {

    private static final DateTimeFormatter ID_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VisionRecordStore recordStore;
    private final VisionProjectService projectService;

    public ExperimentPlanService(VisionRecordStore recordStore, VisionProjectService projectService) {
        this.recordStore = recordStore;
        this.projectService = projectService;
    }

    /**
     * 根据任务类型与目标生成实验计划。
     *
     * @param taskType  任务类型
     * @param objective 实验目标（可空，自动生成默认目标）
     */
    public ExperimentPlan plan(String taskType, String objective) {
        return plan(taskType, objective, null);
    }

    /**
     * 生成实验计划并落库；当 projectId 非空时把计划归档到对应项目。
     */
    public ExperimentPlan plan(String taskType, String objective, String projectId) {
        ExperimentPlanTemplate tpl = ExperimentPlanTemplate.fromTaskType(taskType);
        String resolvedType = tpl.getTaskType();
        String obj = (objective == null || objective.isBlank())
                ? "完成一次" + resolvedType + "实验，从数据采集到模型评估形成可复现流程"
                : objective.trim();

        List<String> dataPrep = new ArrayList<>(tpl.getDataPrep());
        List<String> annotationDesign = new ArrayList<>(tpl.getAnnotationDesign());
        List<String> models = new ArrayList<>(tpl.getRecommendedModels());
        List<String> trainingSteps = new ArrayList<>(tpl.getTrainingSteps());
        List<String> metrics = new ArrayList<>(tpl.getMetrics());
        List<String> risks = new ArrayList<>(tpl.getRisks());

        String id = "exp-" + LocalDateTime.now().format(ID_FMT);
        ExperimentPlan plan = new ExperimentPlan(
                id,
                resolvedType,
                obj,
                dataPrep,
                annotationDesign,
                tpl.getDatasetSplit(),
                models,
                trainingSteps,
                metrics,
                risks,
                null
        );
        recordStore.saveExperiment(plan);
        if (projectId != null && !projectId.isBlank()) {
            projectService.attachExperimentPlan(projectId, id);
        }
        return plan;
    }
}
