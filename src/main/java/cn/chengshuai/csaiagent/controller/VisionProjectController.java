package cn.chengshuai.csaiagent.controller;

import cn.chengshuai.csaiagent.common.ApiResponse;
import cn.chengshuai.csaiagent.common.ResultCode;
import cn.chengshuai.csaiagent.vision.model.VisionProject;
import cn.chengshuai.csaiagent.vision.model.VisionTask;
import cn.chengshuai.csaiagent.vision.service.VisionProjectService;
import cn.chengshuai.csaiagent.vision.service.VisionProjectService.ProjectDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * 采集项目（工作区）接口。对外前缀 /api/vision。
 */
@RestController
@RequestMapping("/vision")
@Slf4j
public class VisionProjectController {

    private final VisionProjectService projectService;

    public VisionProjectController(VisionProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping("/project")
    public ApiResponse<VisionProject> create(@RequestBody ProjectRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            return ApiResponse.error(ResultCode.PARAM_INVALID, "缺少项目名称 name");
        }
        VisionProject project = projectService.create(request.name(), request.description(), request.task());
        return ApiResponse.success(project);
    }

    @GetMapping("/project/list")
    public ApiResponse<List<VisionProject>> list() {
        return ApiResponse.success(projectService.list());
    }

    @GetMapping("/project/{id}")
    public ApiResponse<ProjectDetail> detail(@PathVariable String id) {
        Optional<ProjectDetail> detail = projectService.detail(id);
        return detail.map(ApiResponse::success)
                .orElseGet(() -> ApiResponse.error(ResultCode.NOT_FOUND, "项目不存在：" + id));
    }

    public record ProjectRequest(String name, String description, VisionTask task) {
    }
}
