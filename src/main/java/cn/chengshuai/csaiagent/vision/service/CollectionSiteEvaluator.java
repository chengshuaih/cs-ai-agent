package cn.chengshuai.csaiagent.vision.service;

import cn.chengshuai.csaiagent.vision.model.CollectionSite;
import cn.chengshuai.csaiagent.vision.model.PoiCandidate;
import cn.chengshuai.csaiagent.vision.model.VisionSceneType;
import cn.chengshuai.csaiagent.vision.model.VisionTask;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 采集点位四维规则打分（总分 0-100）：
 * <ul>
 *   <li>场景匹配 0-40：场景类型适配任务的程度</li>
 *   <li>可达性 0-20：距离越近分越高，距离不可得时按中位估算并标注</li>
 *   <li>多样性 0-20：周边 POI 类型多样性，不可得时给基础分</li>
 *   <li>安全性 0-20：园区/校园加分，临主干道/路口降分</li>
 * </ul>
 * 任何依赖地图能力但数据缺失的维度均按规则估算，并在 reason / distance 中标注"（估算）"。
 */
@Service
public class CollectionSiteEvaluator {

    private final VisionSceneClassifier sceneClassifier;

    public CollectionSiteEvaluator(VisionSceneClassifier sceneClassifier) {
        this.sceneClassifier = sceneClassifier;
    }

    /**
     * 评估单个候选点位，产出带评分与建议的 {@link CollectionSite}。
     */
    public CollectionSite evaluate(PoiCandidate poi, VisionTask task) {
        VisionSceneType scene = sceneClassifier.classify(poi.name(), poi.poiType());

        int sceneScore = scoreScene(scene, task);
        boolean distanceEstimated = poi.distanceKm() < 0;
        int reachScore = scoreReach(poi.distanceKm());
        boolean diversityEstimated = poi.nearbyPoiTypeCount() < 0;
        int diversityScore = scoreDiversity(poi.nearbyPoiTypeCount());
        int safetyScore = scoreSafety(scene);

        int total = clamp(sceneScore + reachScore + diversityScore + safetyScore, 0, 100);

        String distanceText = distanceEstimated
                ? "（估算）"
                : String.format("%.1fkm", poi.distanceKm());

        List<String> sceneTags = new ArrayList<>();
        sceneTags.add(scene.getLabel());
        if (poi.poiType() != null && !poi.poiType().isBlank()) {
            sceneTags.add(poi.poiType());
        }

        String reason = buildReason(scene, task, sceneScore, distanceEstimated, diversityEstimated);

        // 高德实拍图（若有）作为初始参考图，保证"具体位置的图片"优先
        List<String> initialImages = (poi.photoUrl() != null && !poi.photoUrl().isBlank())
                ? List.of(poi.photoUrl()) : List.of();

        return new CollectionSite(
                poi.name(),
                poi.address(),
                distanceText,
                sceneTags,
                scene.getSuitableTasks(),
                total,
                reason,
                buildCaptureSuggestions(scene, task),
                buildRiskTips(scene),
                initialImages,
                poi.longitude(),
                poi.latitude(),
                null,
                null,
                null,
                null
        );
    }

    public List<CollectionSite> evaluate(List<PoiCandidate> pois, VisionTask task) {
        return pois.stream()
                .map(p -> evaluate(p, task))
                .sorted((a, b) -> Integer.compare(b.score(), a.score()))
                .toList();
    }

    private int scoreScene(VisionSceneType scene, VisionTask task) {
        if (scene == VisionSceneType.GENERAL) {
            return 18;
        }
        // 任务/目标对象命中该场景适配任务则给高分
        List<String> suitable = scene.getSuitableTasks();
        boolean taskHit = task.taskType() != null
                && suitable.stream().anyMatch(s -> task.taskType().contains(s) || s.contains(task.taskType()));
        boolean objectHit = task.targetObjects() != null && task.targetObjects().stream()
                .anyMatch(obj -> suitable.stream().anyMatch(s -> s.contains(obj)));
        boolean sceneHit = task.sceneTypes() != null && task.sceneTypes().stream()
                .anyMatch(s -> s.contains(scene.getLabel()) || scene.getLabel().contains(s));
        int score = 25;
        if (taskHit || objectHit) {
            score += 10;
        }
        if (sceneHit) {
            score += 5;
        }
        return clamp(score, 0, 40);
    }

    private int scoreReach(double distanceKm) {
        if (distanceKm < 0) {
            return 12; // 估算中位
        }
        if (distanceKm <= 1) {
            return 20;
        }
        if (distanceKm <= 3) {
            return 16;
        }
        if (distanceKm <= 6) {
            return 10;
        }
        if (distanceKm <= 10) {
            return 6;
        }
        return 3;
    }

    private int scoreDiversity(int nearbyPoiTypeCount) {
        if (nearbyPoiTypeCount < 0) {
            return 10; // 估算基础分
        }
        return clamp(nearbyPoiTypeCount * 3, 0, 20);
    }

    private int scoreSafety(VisionSceneType scene) {
        return switch (scene) {
            case CAMPUS, INDUSTRIAL_PARK, PARKING -> 18;
            case PEDESTRIAN_PATH -> 14;
            case INTERSECTION -> 8;       // 临主干道/路口风险高，降分
            case LOW_LIGHT, REFLECTIVE_ROAD -> 10;
            default -> 12;
        };
    }

    private String buildReason(VisionSceneType scene, VisionTask task, int sceneScore,
                               boolean distanceEstimated, boolean diversityEstimated) {
        StringBuilder sb = new StringBuilder();
        sb.append(scene.getLabel()).append("场景，适合")
                .append(String.join("、", scene.getSuitableTasks()));
        if (sceneScore >= 35) {
            sb.append("，与当前任务高度匹配");
        }
        if (distanceEstimated || diversityEstimated) {
            sb.append("（可达性/多样性为规则估算）");
        }
        return sb.toString();
    }

    private List<String> buildCaptureSuggestions(VisionSceneType scene, VisionTask task) {
        List<String> tips = new ArrayList<>();
        switch (scene) {
            case INTERSECTION -> {
                tips.add("早晚高峰各采集一组，覆盖红绿灯切换全过程");
                tips.add("斜 45 度俯拍以减少目标遮挡");
            }
            case PARKING -> tips.add("覆盖不同车型与遮挡程度，注意车牌脱敏");
            case CAMPUS -> tips.add("上下课时段采集，兼顾远近不同尺度的小目标");
            case INDUSTRIAL_PARK -> tips.add("采集前向园区管理方报备，避开作业区");
            case LOW_LIGHT -> tips.add("配合补光/长曝光，记录明暗变化序列");
            case REFLECTIVE_ROAD -> tips.add("雨后采集积水反光样本，注意防滑");
            default -> tips.add("覆盖不同时段与光照条件，保证样本多样性");
        }
        if (task.timeBudget() != null) {
            tips.add("按 " + task.timeBudget() + " 预算合理分配各点位采集时长");
        }
        return tips;
    }

    private List<String> buildRiskTips(VisionSceneType scene) {
        List<String> tips = new ArrayList<>();
        switch (scene) {
            case INTERSECTION -> {
                tips.add("临主干道注意人身安全，站在人行道或安全岛内");
                tips.add("严禁进入机动车道或路口中央停留");
            }
            case INDUSTRIAL_PARK -> tips.add("园区采集需提前报备并遵守现场安全规定");
            case CAMPUS -> tips.add("校园内拍摄注意隐私，避免清晰人脸特写");
            case LOW_LIGHT -> tips.add("夜间采集穿反光衣、结伴而行");
            default -> tips.add("公共区域采集，避免针对特定个人长时间拍摄");
        }
        tips.add("含人脸/车牌的画面对外展示前需脱敏");
        return tips;
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
