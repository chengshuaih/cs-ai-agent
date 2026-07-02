package cn.chengshuai.csaiagent.controller;

import cn.chengshuai.csaiagent.common.ApiResponse;
import cn.chengshuai.csaiagent.common.ResultCode;
import cn.chengshuai.csaiagent.vision.model.ExperimentPlan;
import cn.chengshuai.csaiagent.vision.service.ExperimentPlanService;
import cn.chengshuai.csaiagent.vision.service.VisionRecordStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 视觉实验流程规划接口。对外前缀 /api/vision。
 */
@RestController
@RequestMapping("/vision")
@Slf4j
public class VisionExperimentController {

    private final ExperimentPlanService experimentPlanService;
    private final VisionRecordStore recordStore;

    public VisionExperimentController(ExperimentPlanService experimentPlanService, VisionRecordStore recordStore) {
        this.experimentPlanService = experimentPlanService;
        this.recordStore = recordStore;
    }

    @PostMapping("/experiment/plan")
    public ApiResponse<ExperimentPlan> plan(@RequestBody ExperimentRequest request) {
        if (request == null || request.taskType() == null || request.taskType().isBlank()) {
            return ApiResponse.error(ResultCode.PARAM_INVALID, "缺少任务类型 taskType");
        }
        try {
            ExperimentPlan plan = experimentPlanService.plan(
                    request.taskType(), request.objective(), request.projectId());
            return ApiResponse.success(plan);
        } catch (Exception e) {
            log.error("生成实验计划失败", e);
            return ApiResponse.error(ResultCode.INTERNAL_ERROR, "生成实验计划失败，请稍后重试");
        }
    }

    public record ExperimentRequest(String taskType, String objective, String projectId) {
    }

    @GetMapping("/experiment/plan/list")
    public ApiResponse<List<ExperimentPlan>> listExperiments() {
        return ApiResponse.success(recordStore.listExperiments());
    }
}
