package cn.chengshuai.csaiagent.tools;

import cn.chengshuai.csaiagent.vision.model.CollectionPlan;
import cn.chengshuai.csaiagent.vision.model.VisionTask;
import cn.chengshuai.csaiagent.vision.service.CollectionPlanService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 采集规划工具：以 ToolCallback 形式暴露 {@link CollectionPlanService}，
 * 让 VisionAgent 通过标准工具调用触发采集规划，而不自行拼接地图逻辑（单一中枢）。
 */
public class CollectionPlanTool {

    private final CollectionPlanService planService;

    public CollectionPlanTool(CollectionPlanService planService) {
        this.planService = planService;
    }

    @Tool(description = "根据机器视觉任务与采集位置生成数据采集规划，包含推荐点位、路线、采集清单与隐私提示")
    public CollectionPlan planCollection(
            @ToolParam(description = "任务类型，如目标检测/图像分割/深度估计/三维重建") String taskType,
            @ToolParam(description = "目标对象，逗号分隔，如：车辆,行人,交通灯") String targetObjects,
            @ToolParam(description = "采集位置或城市，如：北京市海淀区中关村") String location,
            @ToolParam(description = "采集时长，如：半天/一天/2小时") String timeBudget,
            @ToolParam(description = "交通方式，如：步行/骑行/驾车") String transportMode) {
        VisionTask task = VisionTask.of(taskType, targetObjects, location, timeBudget, transportMode);
        return planService.plan(task, false);
    }
}
