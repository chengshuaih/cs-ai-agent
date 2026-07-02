package cn.chengshuai.csaiagent.vision.service;

import cn.chengshuai.csaiagent.vision.model.CollectionSite;
import cn.chengshuai.csaiagent.vision.model.PoiCandidate;
import cn.chengshuai.csaiagent.vision.model.VisionTask;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionSiteEvaluatorTest {

    private final CollectionSiteEvaluator evaluator =
            new CollectionSiteEvaluator(new VisionSceneClassifier());

    private VisionTask detectionTask() {
        return new VisionTask("目标检测", List.of("车辆", "行人", "交通灯"),
                List.of("十字路口"), "北京", "半天", "骑行");
    }

    @Test
    void evaluate_scoreWithinRange() {
        PoiCandidate poi = new PoiCandidate("中关村十字路口", "中关村大街", "交叉口", 1.0, 5);
        CollectionSite site = evaluator.evaluate(poi, detectionTask());
        assertTrue(site.score() >= 0 && site.score() <= 100, "总分应在 0-100，实际：" + site.score());
        assertTrue(site.score() > 60, "高匹配点位应得高分，实际：" + site.score());
    }

    @Test
    void evaluate_missingDistance_marksEstimated() {
        PoiCandidate poi = new PoiCandidate("某路口", "某街", "路口", -1, -1);
        CollectionSite site = evaluator.evaluate(poi, detectionTask());
        assertTrue(site.distance().contains("估算"), "距离不可得应标注估算，实际：" + site.distance());
        assertTrue(site.score() >= 0 && site.score() <= 100);
    }

    @Test
    void evaluate_intersectionSafetyLowerThanCampus() {
        PoiCandidate intersection = new PoiCandidate("十字路口", "主干道", "路口", 1.0, 5);
        PoiCandidate campus = new PoiCandidate("某大学", "学院路", "学校", 1.0, 5);
        CollectionSite a = evaluator.evaluate(intersection, detectionTask());
        CollectionSite b = evaluator.evaluate(campus, detectionTask());
        assertNotNull(a.riskTips());
        assertTrue(b.riskTips().size() > 0);
    }

    @Test
    void evaluate_sortedDescendingByScore() {
        List<PoiCandidate> pois = List.of(
                new PoiCandidate("远处咖啡馆", "远街", "餐饮", 9.0, 1),
                new PoiCandidate("近处十字路口", "近街", "路口", 0.8, 6));
        List<CollectionSite> sites = evaluator.evaluate(pois, detectionTask());
        assertTrue(sites.get(0).score() >= sites.get(1).score(), "应按得分降序排列");
    }
}
