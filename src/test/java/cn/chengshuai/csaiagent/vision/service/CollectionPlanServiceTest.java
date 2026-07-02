package cn.chengshuai.csaiagent.vision.service;

import cn.chengshuai.csaiagent.vision.model.CollectionPlan;
import cn.chengshuai.csaiagent.vision.model.CollectionSite;
import cn.chengshuai.csaiagent.vision.model.PoiCandidate;
import cn.chengshuai.csaiagent.vision.model.VisionTask;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link CollectionPlanService} 端到端装配测试：验证 MCP 候选点、图片/定位增强、多方案与降级回退在主流程中的接线。
 */
class CollectionPlanServiceTest {

    private final VisionTask task = new VisionTask("目标检测",
            List.of("车辆", "行人"), List.of("十字路口"), "北京", "半天", "骑行");

    private CollectionPlanService newService(VisionMcpToolService mcp) {
        VisionRecordStore recordStore = mock(VisionRecordStore.class);
        VisionProjectService projectService = mock(VisionProjectService.class);
        return new CollectionPlanService(
                new VisionTaskParser(),
                new CollectionSiteEvaluator(new VisionSceneClassifier()),
                recordStore,
                projectService,
                mcp);
    }

    /** 通用桩：定位增强原样返回、路线规划返回空，避免 NPE。 */
    private void stubPassthrough(VisionMcpToolService mcp) {
        when(mcp.enrichImages(any(), any())).thenAnswer(inv -> inv.getArgument(0));
        when(mcp.enrichLocation(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mcp.planRoute(any(), any(), any(), any())).thenReturn(new String[]{null, null});
    }

    private PoiCandidate poi(String name, String type) {
        return new PoiCandidate(name, name + "地址", type, 1.0, 5, 116.3, 39.9);
    }

    private PoiCandidate poiAt(String name, String type, double lng, double lat) {
        return new PoiCandidate(name, name + "地址", type, 1.0, 5, lng, lat);
    }

    @Test
    void plan_usesRealPoiAndImagesFromMcp() {
        VisionMcpToolService mcp = mock(VisionMcpToolService.class);
        when(mcp.searchPoiCandidates(any())).thenReturn(List.of(
                new PoiCandidate("中关村十字路口", "海淀区中关村大街", "交叉口", 1.0, 5, 116.3, 39.9)));
        when(mcp.enrichImages(any(), any())).thenAnswer(inv -> {
            List<CollectionSite> sites = inv.getArgument(0);
            return List.of(sites.get(0).withImageUrls(List.of("https://img/a.jpg")));
        });
        when(mcp.enrichLocation(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mcp.planRoute(any(), any(), any(), any())).thenReturn(new String[]{null, null});

        CollectionPlan plan = newService(mcp).plan(task, false);

        assertNotNull(plan);
        assertFalse(plan.sites().isEmpty());
        assertTrue(plan.sites().stream().anyMatch(s -> s.name().equals("中关村十字路口")),
                "应使用 MCP 返回的真实点位名称");
        assertTrue(plan.sites().get(0).imageUrls().contains("https://img/a.jpg"),
                "应包含图片增强后的 URL");
        assertFalse(plan.schemes().isEmpty(), "应至少生成一套方案");
    }

    @Test
    void plan_fallsBackToRuleCandidatesWhenMcpEmpty() {
        VisionMcpToolService mcp = mock(VisionMcpToolService.class);
        when(mcp.searchPoiCandidates(any())).thenReturn(List.of());
        stubPassthrough(mcp);

        CollectionPlan plan = newService(mcp).plan(task, false);

        assertNotNull(plan);
        assertFalse(plan.sites().isEmpty(), "MCP 无结果时应回退规则候选点并仍生成计划");
    }

    @Test
    void plan_composesMultipleSchemesWhenEnoughPois() {
        VisionMcpToolService mcp = mock(VisionMcpToolService.class);
        when(mcp.searchPoiCandidates(any())).thenReturn(List.of(
                poiAt("中关村十字路口", "交叉口", 116.30, 39.90),
                poiAt("北京大学校园道路", "学校", 116.31, 39.99),
                poiAt("朝阳大悦城停车场", "停车场", 116.50, 39.92),
                poiAt("国贸地下通道", "隧道", 116.46, 39.91),
                poiAt("望京非机动车道", "道路", 116.47, 40.00)));
        stubPassthrough(mcp);

        CollectionPlan plan = newService(mcp).plan(task, false);

        assertNotNull(plan);
        assertTrue(plan.schemes().size() >= 2, "POI 充足时应生成多套方案，实际：" + plan.schemes().size());
        assertTrue(plan.schemes().get(0).title().contains("方案A"), "首选方案标题应为方案A");
        assertNotNull(plan.schemes().get(0).recommendReason(), "方案应包含推荐理由");
    }

    @Test
    void plan_degradesToSingleSchemeWhenFewPois() {
        VisionMcpToolService mcp = mock(VisionMcpToolService.class);
        when(mcp.searchPoiCandidates(any())).thenReturn(List.of(poi("中关村十字路口", "交叉口")));
        stubPassthrough(mcp);

        CollectionPlan plan = newService(mcp).plan(task, false);

        assertNotNull(plan);
        assertTrue(plan.schemes().size() == 1, "点位不足时应降级为单套方案");
    }
}
