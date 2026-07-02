package cn.chengshuai.csaiagent.controller;

import cn.chengshuai.csaiagent.common.ApiResponse;
import cn.chengshuai.csaiagent.common.ResultCode;
import cn.chengshuai.csaiagent.vision.model.CollectionPlan;
import cn.chengshuai.csaiagent.vision.model.VisionTask;
import cn.chengshuai.csaiagent.vision.service.CollectionPlanService;
import cn.chengshuai.csaiagent.vision.service.VisionRecordStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 场景化数据采集规划接口。对外路径前缀 /api/vision（context-path=/api）。
 */
@RestController
@RequestMapping("/vision")
@Slf4j
public class VisionCollectionController {

    private final CollectionPlanService collectionPlanService;
    private final VisionRecordStore recordStore;

    public VisionCollectionController(CollectionPlanService collectionPlanService, VisionRecordStore recordStore) {
        this.collectionPlanService = collectionPlanService;
        this.recordStore = recordStore;
    }

    /**
     * 生成采集计划。
     *
     * @param request 采集规划请求体
     * @return 统一包装的 CollectionPlan
     */
    @PostMapping("/collection/plan")
    public ApiResponse<CollectionPlan> plan(@RequestBody CollectionPlanRequest request) {
        if (request == null || request.task() == null) {
            return ApiResponse.error(ResultCode.PARAM_INVALID, "缺少视觉任务 task");
        }
        try {
            CollectionPlan plan = collectionPlanService.plan(
                    request.task(), Boolean.TRUE.equals(request.needPdf()), request.projectId());
            return ApiResponse.success(plan);
        } catch (Exception e) {
            log.error("生成采集计划失败", e);
            return ApiResponse.error(ResultCode.INTERNAL_ERROR, "生成采集计划失败，请稍后重试");
        }
    }

    /**
     * 采集规划请求体。
     *
     * @param task      视觉任务
     * @param needPdf   是否同时生成 PDF（PDF 生成在报告模块接入后生效）
     * @param projectId 所属采集项目（可空）
     */
    public record CollectionPlanRequest(VisionTask task, Boolean needPdf, String projectId) {
    }

    @GetMapping("/collection/plan/list")
    public ApiResponse<List<CollectionPlan>> listPlans() {
        return ApiResponse.success(recordStore.listPlans());
    }
}
