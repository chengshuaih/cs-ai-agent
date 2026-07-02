package cn.chengshuai.csaiagent.vision.service;

import cn.chengshuai.csaiagent.vision.model.CollectionPlan;
import cn.chengshuai.csaiagent.vision.model.ExperimentPlan;
import cn.chengshuai.csaiagent.vision.model.VisionProject;
import cn.chengshuai.csaiagent.vision.model.VisionReport;
import cn.chengshuai.csaiagent.vision.model.VisionTask;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 采集项目（工作区）聚合服务，基于 projects.json。
 * 聚合返回其采集计划、实验计划、报告。
 */
@Service
public class VisionProjectService {

    private static final DateTimeFormatter ID_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VisionRecordStore recordStore;

    public VisionProjectService(VisionRecordStore recordStore) {
        this.recordStore = recordStore;
    }

    public VisionProject create(String name, String description, VisionTask task) {
        String now = LocalDateTime.now().toString();
        VisionProject project = new VisionProject(
                "proj-" + LocalDateTime.now().format(ID_FMT),
                name,
                description,
                task,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                now,
                now
        );
        recordStore.saveProject(project);
        return project;
    }

    public List<VisionProject> list() {
        return recordStore.listProjects();
    }

    public Optional<VisionProject> find(String id) {
        return recordStore.findProject(id);
    }

    /**
     * 将采集计划归档到项目：把 planId 写入 collectionPlanIds 并更新 updatedAt。
     *
     * @return 是否归档成功（项目存在且已写入）
     */
    public boolean attachCollectionPlan(String projectId, String planId) {
        return attach(projectId, planId, true);
    }

    /**
     * 将实验计划归档到项目：把 experimentId 写入 experimentPlanIds 并更新 updatedAt。
     *
     * @return 是否归档成功（项目存在且已写入）
     */
    public boolean attachExperimentPlan(String projectId, String experimentId) {
        return attach(projectId, experimentId, false);
    }

    private boolean attach(String projectId, String planId, boolean collection) {
        if (projectId == null || projectId.isBlank() || planId == null || planId.isBlank()) {
            return false;
        }
        Optional<VisionProject> projectOpt = recordStore.findProject(projectId);
        if (projectOpt.isEmpty()) {
            return false;
        }
        VisionProject project = projectOpt.get();
        List<String> collectionIds = new ArrayList<>(
                project.collectionPlanIds() == null ? List.of() : project.collectionPlanIds());
        List<String> experimentIds = new ArrayList<>(
                project.experimentPlanIds() == null ? List.of() : project.experimentPlanIds());
        if (collection) {
            if (!collectionIds.contains(planId)) {
                collectionIds.add(planId);
            }
        } else {
            if (!experimentIds.contains(planId)) {
                experimentIds.add(planId);
            }
        }
        VisionProject updated = new VisionProject(
                project.id(),
                project.name(),
                project.description(),
                project.visionTask(),
                collectionIds,
                experimentIds,
                project.reportIds(),
                project.createdAt(),
                LocalDateTime.now().toString()
        );
        recordStore.saveProject(updated);
        return true;
    }

    /**
     * 项目详情聚合：项目元数据 + 关联采集计划/实验计划/报告。
     */
    public Optional<ProjectDetail> detail(String id) {
        Optional<VisionProject> projectOpt = recordStore.findProject(id);
        if (projectOpt.isEmpty()) {
            return Optional.empty();
        }
        VisionProject project = projectOpt.get();

        List<CollectionPlan> plans = recordStore.listPlans().stream()
                .filter(p -> project.collectionPlanIds() != null && project.collectionPlanIds().contains(p.id()))
                .toList();
        List<ExperimentPlan> experiments = recordStore.listExperiments().stream()
                .filter(e -> project.experimentPlanIds() != null && project.experimentPlanIds().contains(e.id()))
                .toList();
        List<VisionReport> reports = recordStore.listReportsByProject(id);

        return Optional.of(new ProjectDetail(project, plans, experiments, reports));
    }

    /**
     * 项目详情聚合返回体。
     */
    public record ProjectDetail(
            VisionProject project,
            List<CollectionPlan> collectionPlans,
            List<ExperimentPlan> experimentPlans,
            List<VisionReport> reports
    ) {
    }
}
