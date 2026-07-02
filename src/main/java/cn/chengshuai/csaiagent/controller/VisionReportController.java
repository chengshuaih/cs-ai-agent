package cn.chengshuai.csaiagent.controller;

import cn.chengshuai.csaiagent.common.ApiResponse;
import cn.chengshuai.csaiagent.common.ResultCode;
import cn.chengshuai.csaiagent.vision.model.CollectionPlan;
import cn.chengshuai.csaiagent.vision.model.ExperimentPlan;
import cn.chengshuai.csaiagent.vision.model.VisionReport;
import cn.chengshuai.csaiagent.vision.service.VisionRecordStore;
import cn.chengshuai.csaiagent.vision.service.VisionReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * 视觉报告接口：生成 PDF 报告、查询报告列表、下载 PDF。对外前缀 /api/vision。
 */
@RestController
@RequestMapping("/vision")
@Slf4j
public class VisionReportController {

    private final VisionReportService reportService;
    private final VisionRecordStore recordStore;

    public VisionReportController(VisionReportService reportService, VisionRecordStore recordStore) {
        this.reportService = reportService;
        this.recordStore = recordStore;
    }

    /**
     * 生成 PDF 报告。type=collection_plan/experiment_plan 时按 sourceId 取已有计划；
     * 其它类型（research_summary/stage_summary）使用 content 文本。
     */
    @PostMapping("/report/pdf")
    public ApiResponse<VisionReport> generate(@RequestBody ReportRequest request) {
        if (request == null || request.type() == null || request.type().isBlank()) {
            return ApiResponse.error(ResultCode.PARAM_INVALID, "缺少报告类型 type");
        }
        try {
            VisionReport report = switch (request.type()) {
                case "collection_plan" -> {
                    Optional<CollectionPlan> plan = recordStore.findPlan(request.sourceId());
                    if (plan.isEmpty()) {
                        yield null;
                    }
                    yield reportService.generateCollectionReport(plan.get(), request.projectId());
                }
                case "experiment_plan" -> {
                    Optional<ExperimentPlan> plan = recordStore.findExperiment(request.sourceId());
                    if (plan.isEmpty()) {
                        yield null;
                    }
                    yield reportService.generateExperimentReport(plan.get(), request.projectId());
                }
                default -> {
                    if (request.content() == null || request.content().isBlank()) {
                        yield null;
                    }
                    String title = request.title() == null ? request.type() : request.title();
                    yield reportService.generate(request.type(), title, request.content(), request.projectId());
                }
            };
            if (report == null) {
                return ApiResponse.error(ResultCode.NOT_FOUND, "未找到来源数据或缺少 content");
            }
            return ApiResponse.success(report);
        } catch (Exception e) {
            log.error("生成报告失败", e);
            return ApiResponse.error(ResultCode.INTERNAL_ERROR, "生成报告失败：" + e.getMessage());
        }
    }

    /**
     * 报告中心：列出全部报告记录。
     */
    @GetMapping("/report/list")
    public ApiResponse<List<VisionReport>> list() {
        return ApiResponse.success(recordStore.listReports());
    }

    /**
     * 下载 PDF 文件。
     */
    @GetMapping("/report/download/{id}")
    public ResponseEntity<FileSystemResource> download(@PathVariable String id) {
        Optional<VisionReport> opt = recordStore.findReport(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        File file = new File(opt.get().pdfPath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        String encoded = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(new FileSystemResource(file));
    }

    /**
     * 报告生成请求体。
     *
     * @param type      报告类型
     * @param title     标题
     * @param sourceId  来源计划 id（采集/实验报告需要）
     * @param content   自定义文本（摘要类报告使用）
     * @param projectId 所属项目（可空）
     */
    public record ReportRequest(String type, String title, String sourceId, String content, String projectId) {
    }
}
