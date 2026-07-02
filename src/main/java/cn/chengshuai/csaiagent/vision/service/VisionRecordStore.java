package cn.chengshuai.csaiagent.vision.service;

import cn.chengshuai.csaiagent.constant.FileConstant;
import cn.chengshuai.csaiagent.vision.model.CollectionPlan;
import cn.chengshuai.csaiagent.vision.model.ExperimentPlan;
import cn.chengshuai.csaiagent.vision.model.VisionProject;
import cn.chengshuai.csaiagent.vision.model.VisionReport;
import cn.hutool.core.io.FileUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 轻量本地 JSON 持久化。统一存储根目录 tmp/vision/：
 * <ul>
 *   <li>records.json   报告记录索引（List&lt;VisionReport&gt;）</li>
 *   <li>plans.json     采集计划（List&lt;CollectionPlan&gt;）</li>
 *   <li>experiments.json 实验计划（List&lt;ExperimentPlan&gt;）</li>
 *   <li>projects.json  采集项目（List&lt;VisionProject&gt;）</li>
 *   <li>reports/       生成的 PDF</li>
 * </ul>
 * 第一版用文件级 synchronized 防并发写，规模小、足够稳定。
 */
@Component
public class VisionRecordStore {

    private static final String BASE_DIR = FileConstant.FILE_SAVE_DIR + "/vision";
    private static final String REPORTS_DIR = BASE_DIR + "/reports";
    private static final String RECORDS_FILE = BASE_DIR + "/records.json";
    private static final String PLANS_FILE = BASE_DIR + "/plans.json";
    private static final String EXPERIMENTS_FILE = BASE_DIR + "/experiments.json";
    private static final String PROJECTS_FILE = BASE_DIR + "/projects.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Object lock = new Object();

    @PostConstruct
    public void init() {
        FileUtil.mkdir(REPORTS_DIR);
    }

    public String reportsDir() {
        return REPORTS_DIR;
    }

    // ---------- 报告记录 ----------

    public void saveReport(VisionReport report) {
        if (!isValidReport(report)) {
            throw new IllegalArgumentException("报告 id 不能为空");
        }
        synchronized (lock) {
            List<VisionReport> list = listReports();
            list.removeIf(r -> report.id().equals(r.id()));
            list.add(report);
            writeList(RECORDS_FILE, list);
        }
    }

    public List<VisionReport> listReports() {
        List<VisionReport> reports = readList(RECORDS_FILE, VisionReport.class);
        reports.removeIf(report -> !isValidReport(report));
        return reports;
    }

    public Optional<VisionReport> findReport(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return listReports().stream().filter(r -> id.equals(r.id())).findFirst();
    }

    public List<VisionReport> listReportsByProject(String projectId) {
        return listReports().stream()
                .filter(r -> projectId != null && projectId.equals(r.projectId()))
                .toList();
    }

    // ---------- 采集计划 ----------

    public void savePlan(CollectionPlan plan) {
        if (!isValidPlan(plan)) {
            throw new IllegalArgumentException("采集计划 id 不能为空");
        }
        synchronized (lock) {
            List<CollectionPlan> list = listPlans();
            list.removeIf(p -> plan.id().equals(p.id()));
            list.add(plan);
            writeList(PLANS_FILE, list);
        }
    }

    public List<CollectionPlan> listPlans() {
        List<CollectionPlan> plans = readList(PLANS_FILE, CollectionPlan.class);
        plans.removeIf(plan -> !isValidPlan(plan));
        return plans;
    }

    public Optional<CollectionPlan> findPlan(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return listPlans().stream().filter(p -> id.equals(p.id())).findFirst();
    }

    // ---------- 实验计划 ----------

    public void saveExperiment(ExperimentPlan plan) {
        if (!isValidExperiment(plan)) {
            throw new IllegalArgumentException("实验计划 id 不能为空");
        }
        synchronized (lock) {
            List<ExperimentPlan> list = listExperiments();
            list.removeIf(p -> plan.id().equals(p.id()));
            list.add(plan);
            writeList(EXPERIMENTS_FILE, list);
        }
    }

    public List<ExperimentPlan> listExperiments() {
        List<ExperimentPlan> experiments = readList(EXPERIMENTS_FILE, ExperimentPlan.class);
        experiments.removeIf(plan -> !isValidExperiment(plan));
        return experiments;
    }

    public Optional<ExperimentPlan> findExperiment(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return listExperiments().stream().filter(p -> id.equals(p.id())).findFirst();
    }

    // ---------- 采集项目 ----------

    public void saveProject(VisionProject project) {
        if (!isValidProject(project)) {
            throw new IllegalArgumentException("项目 id 不能为空");
        }
        synchronized (lock) {
            List<VisionProject> list = listProjects();
            list.removeIf(p -> project.id().equals(p.id()));
            list.add(project);
            writeList(PROJECTS_FILE, list);
        }
    }

    public List<VisionProject> listProjects() {
        List<VisionProject> projects = readList(PROJECTS_FILE, VisionProject.class);
        projects.removeIf(project -> !isValidProject(project));
        return projects;
    }

    public Optional<VisionProject> findProject(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return listProjects().stream().filter(p -> id.equals(p.id())).findFirst();
    }

    private boolean isValidReport(VisionReport report) {
        return report != null && report.id() != null && !report.id().isBlank();
    }

    private boolean isValidPlan(CollectionPlan plan) {
        return plan != null && plan.id() != null && !plan.id().isBlank();
    }

    private boolean isValidExperiment(ExperimentPlan plan) {
        return plan != null && plan.id() != null && !plan.id().isBlank();
    }

    private boolean isValidProject(VisionProject project) {
        return project != null && project.id() != null && !project.id().isBlank();
    }

    // ---------- 通用读写 ----------

    private <T> List<T> readList(String path, Class<T> clazz) {
        File file = new File(path);
        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }
        String json = FileUtil.readUtf8String(file);
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            if (!root.isArray()) {
                FileUtil.writeUtf8String("[]", file);
                return new ArrayList<>();
            }
            List<T> result = new ArrayList<>();
            for (JsonNode item : root) {
                try {
                    result.add(OBJECT_MAPPER.treeToValue(item, clazz));
                } catch (Exception e) {
                    // 跳过无法反序列化的脏数据条目
                }
            }
            return result;
        } catch (Exception e) {
            // JSON 整体解析失败，重置文件
            FileUtil.writeUtf8String("[]", file);
            return new ArrayList<>();
        }
    }

    private void writeList(String path, List<?> list) {
        FileUtil.mkParentDirs(path);
        try {
            FileUtil.writeUtf8String(OBJECT_MAPPER.writeValueAsString(list), path);
        } catch (Exception e) {
            throw new IllegalStateException("写入 JSON 列表失败: " + path, e);
        }
    }
}
