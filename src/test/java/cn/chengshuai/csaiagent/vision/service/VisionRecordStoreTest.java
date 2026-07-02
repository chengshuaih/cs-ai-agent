package cn.chengshuai.csaiagent.vision.service;

import cn.chengshuai.csaiagent.constant.FileConstant;
import cn.chengshuai.csaiagent.vision.model.CollectionPlan;
import cn.chengshuai.csaiagent.vision.model.CollectionSite;
import cn.chengshuai.csaiagent.vision.model.ExperimentPlan;
import cn.chengshuai.csaiagent.vision.model.VisionProject;
import cn.chengshuai.csaiagent.vision.model.VisionReport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisionRecordStoreTest {

    private static final Path PROJECTS_FILE = Path.of(FileConstant.FILE_SAVE_DIR, "vision", "projects.json");
    private static final Path PLANS_FILE = Path.of(FileConstant.FILE_SAVE_DIR, "vision", "plans.json");
    private static final Path EXPERIMENTS_FILE = Path.of(FileConstant.FILE_SAVE_DIR, "vision", "experiments.json");
    private static final Path RECORDS_FILE = Path.of(FileConstant.FILE_SAVE_DIR, "vision", "records.json");
    private static final List<Path> DATA_FILES = List.of(PROJECTS_FILE, PLANS_FILE, EXPERIMENTS_FILE, RECORDS_FILE);

    private final Map<Path, byte[]> originalFileContents = new HashMap<>();
    private final Map<Path, Boolean> originalFileExisted = new HashMap<>();

    @BeforeEach
    void backupDataFiles() throws IOException {
        for (Path file : DATA_FILES) {
            boolean existed = Files.exists(file);
            originalFileExisted.put(file, existed);
            originalFileContents.put(file, existed ? Files.readAllBytes(file) : null);
        }
    }

    @AfterEach
    void restoreDataFiles() throws IOException {
        for (Path file : DATA_FILES) {
            if (Boolean.TRUE.equals(originalFileExisted.get(file))) {
                Files.createDirectories(file.getParent());
                Files.write(file, originalFileContents.get(file));
            } else {
                Files.deleteIfExists(file);
            }
        }
    }

    @Test
    void dirtyProjectsJsonWithEmptyObject_isIgnoredAndDoesNotBreakProjectOperations() throws IOException {
        writeDirtyArray(PROJECTS_FILE);

        VisionRecordStore store = newStore();
        VisionProject validProject = new VisionProject(
                "project-1",
                "脏数据回归项目",
                "验证 projects.json 中存在空对象时的项目持久化行为",
                null,
                List.of(),
                List.of(),
                List.of(),
                "2026-06-24T00:00:00",
                "2026-06-24T00:00:00"
        );

        assertDoesNotThrow(() -> store.saveProject(validProject));

        List<VisionProject> projects = store.listProjects();
        assertEquals(1, projects.size());
        assertEquals("project-1", projects.get(0).id());
        assertFalse(projects.stream().anyMatch(project -> project.id() == null || project.id().isBlank()));

        assertTrue(store.findProject(null).isEmpty());
        assertTrue(store.findProject("   ").isEmpty());
        assertTrue(store.findProject("missing-project").isEmpty());
    }

    @Test
    void dirtyPlansJsonWithEmptyObject_isIgnoredAndDoesNotBreakPlanOperations() throws IOException {
        writeDirtyArray(PLANS_FILE);

        VisionRecordStore store = newStore();
        CollectionPlan validPlan = new CollectionPlan(
                "plan-1",
                "脏数据回归采集计划",
                null,
                List.of(),
                "路线建议",
                List.of("采集清单"),
                List.of("标注建议"),
                List.of("隐私提示"),
                null
        );

        assertDoesNotThrow(() -> store.savePlan(validPlan));

        List<CollectionPlan> plans = store.listPlans();
        assertEquals(1, plans.size());
        assertEquals("plan-1", plans.get(0).id());
        assertFalse(plans.stream().anyMatch(plan -> plan.id() == null || plan.id().isBlank()));

        assertTrue(store.findPlan(null).isEmpty());
        assertTrue(store.findPlan("   ").isEmpty());
        assertTrue(store.findPlan("missing-plan").isEmpty());
    }

    @Test
    void dirtyExperimentsJsonWithEmptyObject_isIgnoredAndDoesNotBreakExperimentOperations() throws IOException {
        writeDirtyArray(EXPERIMENTS_FILE);

        VisionRecordStore store = newStore();
        ExperimentPlan validExperiment = new ExperimentPlan(
                "experiment-1",
                "classification",
                "实验目标",
                List.of("数据准备"),
                List.of("标注设计"),
                "训练/验证/测试",
                List.of("ResNet"),
                List.of("训练步骤"),
                List.of("Accuracy"),
                List.of("风险"),
                null
        );

        assertDoesNotThrow(() -> store.saveExperiment(validExperiment));

        List<ExperimentPlan> experiments = store.listExperiments();
        assertEquals(1, experiments.size());
        assertEquals("experiment-1", experiments.get(0).id());
        assertFalse(experiments.stream().anyMatch(plan -> plan.id() == null || plan.id().isBlank()));

        assertTrue(store.findExperiment(null).isEmpty());
        assertTrue(store.findExperiment("   ").isEmpty());
        assertTrue(store.findExperiment("missing-experiment").isEmpty());
    }

    @Test
    void dirtyRecordsJsonWithEmptyObject_isIgnoredAndDoesNotBreakReportQueries() throws IOException {
        writeDirtyArray(RECORDS_FILE);

        VisionRecordStore store = newStore();
        VisionReport validReport = new VisionReport(
                "report-1",
                "脏数据回归报告",
                "collection_plan",
                "project-1",
                "2026-06-24T00:00:00",
                "/tmp/report.pdf"
        );

        assertDoesNotThrow(() -> store.saveReport(validReport));

        List<VisionReport> reports = store.listReports();
        assertEquals(1, reports.size());
        assertEquals("report-1", reports.get(0).id());
        assertFalse(reports.stream().anyMatch(report -> report.id() == null || report.id().isBlank()));

        assertTrue(store.findReport(null).isEmpty());
        assertTrue(store.findReport("   ").isEmpty());
        assertTrue(store.findReport("missing-report").isEmpty());
    }

    @Test
    void legacyPlanJsonWithoutNewFields_deserializesSafely() throws IOException {
        // 模拟历史 plans.json：CollectionSite 无 imageUrls/经纬度/导航字段，CollectionPlan 无 schemes
        String legacy = "[{\"id\":\"plan-legacy\",\"title\":\"历史采集计划\","
                + "\"task\":{\"taskType\":\"目标检测\",\"targetObjects\":[\"车辆\"],\"sceneTypes\":[],"
                + "\"location\":\"北京\",\"timeBudget\":\"半天\",\"transportMode\":\"步行\"},"
                + "\"sites\":[{\"name\":\"老点位\",\"address\":\"老地址\",\"distance\":\"1.0km\","
                + "\"sceneTags\":[\"十字路口\"],\"suitableTasks\":[\"车辆检测\"],\"score\":80,"
                + "\"reason\":\"历史理由\",\"captureSuggestions\":[\"建议\"],\"riskTips\":[\"风险\"]}],"
                + "\"routeSummary\":\"历史路线\",\"checklist\":[\"清单\"],\"annotationGuide\":[\"标注\"],"
                + "\"privacyTips\":[\"隐私\"],\"pdfPath\":null}]";
        Files.createDirectories(PLANS_FILE.getParent());
        Files.writeString(PLANS_FILE, legacy, StandardCharsets.UTF_8);

        VisionRecordStore store = newStore();

        List<CollectionPlan> plans = assertDoesNotThrow(store::listPlans);
        assertEquals(1, plans.size());
        CollectionPlan plan = plans.get(0);
        assertEquals("plan-legacy", plan.id());
        assertNotNull(plan.schemes(), "历史无 schemes 字段应归一为空列表");
        assertTrue(plan.schemes().isEmpty());
        CollectionSite site = plan.sites().get(0);
        assertNotNull(site.imageUrls(), "历史无 imageUrls 字段应归一为空列表");
        assertTrue(site.imageUrls().isEmpty());
        assertEquals(null, site.longitude(), "历史无经纬度字段应为 null");
        assertEquals(null, site.mapImageUrl(), "历史无地图字段应为 null");
    }

    private VisionRecordStore newStore() {
        VisionRecordStore store = new VisionRecordStore();
        store.init();
        return store;
    }

    private void writeDirtyArray(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, "[{}]", StandardCharsets.UTF_8);
    }
}
