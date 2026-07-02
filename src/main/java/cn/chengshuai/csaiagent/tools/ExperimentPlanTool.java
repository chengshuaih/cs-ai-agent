package cn.chengshuai.csaiagent.tools;

import cn.chengshuai.csaiagent.vision.model.ExperimentPlan;
import cn.chengshuai.csaiagent.vision.service.ExperimentPlanService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 实验规划工具：以 ToolCallback 形式暴露 {@link ExperimentPlanService}，
 * 让 VisionAgent 通过标准工具调用输出可执行的实验流程（单一中枢）。
 */
public class ExperimentPlanTool {

    private final ExperimentPlanService experimentPlanService;

    public ExperimentPlanTool(ExperimentPlanService experimentPlanService) {
        this.experimentPlanService = experimentPlanService;
    }

    @Tool(description = "根据机器视觉任务类型生成可执行的实验流程规划，包含数据准备、标注设计、数据集划分、推荐模型、训练步骤、评价指标与风险")
    public ExperimentPlan planExperiment(
            @ToolParam(description = "任务类型，如目标检测/图像分割/深度估计/三维重建") String taskType,
            @ToolParam(description = "实验目标描述，可为空") String objective) {
        return experimentPlanService.plan(taskType, objective);
    }
}
