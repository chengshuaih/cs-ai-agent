package cn.chengshuai.csaiagent.vision.service;

import cn.chengshuai.csaiagent.vision.model.CollectionPlan;
import cn.chengshuai.csaiagent.vision.model.CollectionScheme;
import cn.chengshuai.csaiagent.vision.model.CollectionSite;
import cn.chengshuai.csaiagent.vision.model.PoiCandidate;
import cn.chengshuai.csaiagent.vision.model.VisionSceneType;
import cn.chengshuai.csaiagent.vision.model.VisionTask;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 采集规划中枢（单一入口）：任务 → 候选点 → 分类评估 → 路线/清单 → {@link CollectionPlan}，并落库。
 *
 * <p>对话入口（VisionAgent 经 CollectionPlanTool）与结构化入口（VisionCollectionController）
 * 共用本服务，避免两套逻辑。第一版候选点由规则生成（基于任务场景偏好 + 位置），
 * 地图 MCP 接入后可替换 {@link #buildCandidates} 的来源。</p>
 */
@Service
public class CollectionPlanService {

    private static final DateTimeFormatter ID_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VisionTaskParser taskParser;
    private final CollectionSiteEvaluator evaluator;
    private final VisionRecordStore recordStore;
    private final VisionProjectService projectService;
    private final VisionMcpToolService mcpToolService;

    public CollectionPlanService(VisionTaskParser taskParser,
                                 CollectionSiteEvaluator evaluator,
                                 VisionRecordStore recordStore,
                                 VisionProjectService projectService,
                                 VisionMcpToolService mcpToolService) {
        this.taskParser = taskParser;
        this.evaluator = evaluator;
        this.recordStore = recordStore;
        this.projectService = projectService;
        this.mcpToolService = mcpToolService;
    }

    /**
     * 生成采集计划并落库（不生成 PDF；PDF 由 VisionReportService 单独触发）。
     */
    public CollectionPlan plan(VisionTask rawTask, boolean ignoredNeedPdf) {
        return plan(rawTask, ignoredNeedPdf, null);
    }

    /**
     * 生成采集计划并落库；当 projectId 非空时把计划归档到对应项目。
     */
    public CollectionPlan plan(VisionTask rawTask, boolean ignoredNeedPdf, String projectId) {
        VisionTask task = taskParser.normalize(rawTask);

        List<PoiCandidate> candidates = buildCandidates(task);
        List<CollectionSite> sites = evaluator.evaluate(candidates, task);
        // 为所有点位补充场景参考图与高德静态地图/导航
        sites = mcpToolService.enrichImages(sites, task);
        sites = enrichLocations(sites);

        // 组合多套方案（每套含路线规划）
        List<CollectionScheme> schemes = composeSchemes(task, sites);
        // 首选方案点位回填到 sites，保持报告与历史兼容
        List<CollectionSite> primarySites = schemes.isEmpty() ? sites : schemes.get(0).sites();

        String id = "plan-" + LocalDateTime.now().format(ID_FMT);
        String title = buildTitle(task);

        CollectionPlan plan = new CollectionPlan(
                id,
                title,
                task,
                primarySites,
                buildRouteSummary(task, primarySites),
                buildChecklist(),
                buildAnnotationGuide(task),
                buildPrivacyTips(),
                null,
                schemes
        );
        recordStore.savePlan(plan);
        if (projectId != null && !projectId.isBlank()) {
            projectService.attachCollectionPlan(projectId, id);
        }
        return plan;
    }

    private List<CollectionSite> enrichLocations(List<CollectionSite> sites) {
        List<CollectionSite> result = new ArrayList<>(sites.size());
        for (CollectionSite site : sites) {
            result.add(mcpToolService.enrichLocation(site));
        }
        return result;
    }

    /**
     * 组合 2-3 套方案：质量优先 / 多样性优先 / 效率优先。点位不足时降级为单套。
     */
    private List<CollectionScheme> composeSchemes(VisionTask task, List<CollectionSite> sites) {
        if (sites == null || sites.isEmpty()) {
            return List.of();
        }
        int perScheme = Math.min(3, sites.size());
        List<CollectionScheme> schemes = new ArrayList<>();

        // 方案A：质量优先（评分最高）
        List<CollectionSite> quality = sites.stream()
                .sorted(Comparator.comparingInt(CollectionSite::score).reversed())
                .limit(perScheme)
                .toList();
        schemes.add(buildScheme(task, "A", "质量优先", "综合评分最高的点位组合，单点采集质量与任务匹配度最佳", quality));

        if (sites.size() <= perScheme) {
            // 点位不足以分出多套，仅保留一套
            return schemes;
        }

        // 方案B：多样性优先（不同场景标签）
        List<CollectionSite> diversity = pickDiverse(sites, perScheme);
        if (isDistinct(diversity, schemes)) {
            schemes.add(buildScheme(task, "B", "多样性优先",
                    "覆盖更多不同场景类型，样本多样性更好，利于模型泛化", diversity));
        }

        // 方案C：效率优先（已有真实距离则按距离最近，否则取后段点位避免与 A 重复）
        List<CollectionSite> efficiency = pickEfficient(sites, perScheme);
        if (isDistinct(efficiency, schemes)) {
            schemes.add(buildScheme(task, "C", "效率优先",
                    "点位相对集中、路程更短，适合时间预算有限时高效采集", efficiency));
        }

        return schemes;
    }

    private List<CollectionSite> pickDiverse(List<CollectionSite> sites, int n) {
        List<CollectionSite> picked = new ArrayList<>();
        Set<String> seenTags = new LinkedHashSet<>();
        for (CollectionSite s : sites) {
            String tag = (s.sceneTags() == null || s.sceneTags().isEmpty()) ? s.name() : s.sceneTags().get(0);
            if (seenTags.add(tag)) {
                picked.add(s);
            }
            if (picked.size() >= n) {
                break;
            }
        }
        // 不足补齐
        for (CollectionSite s : sites) {
            if (picked.size() >= n) {
                break;
            }
            if (!picked.contains(s)) {
                picked.add(s);
            }
        }
        return picked;
    }

    private List<CollectionSite> pickEfficient(List<CollectionSite> sites, int n) {
        // 以评分最高点为锚，按经纬度直线距离选最近的 n 个点，使点位更集中、路程更短。
        CollectionSite anchor = sites.stream()
                .max(Comparator.comparingInt(CollectionSite::score))
                .orElse(sites.get(0));
        boolean hasCoords = sites.stream()
                .anyMatch(s -> s.longitude() != null && s.latitude() != null);
        if (hasCoords && anchor.longitude() != null && anchor.latitude() != null) {
            return sites.stream()
                    .sorted(Comparator.comparingDouble(s -> haversine(anchor, s)))
                    .limit(n)
                    .toList();
        }
        // 无经纬度时取后段点位，避免与质量优先方案完全重合
        int from = Math.max(0, sites.size() - n);
        return new ArrayList<>(sites.subList(from, sites.size()));
    }

    /**
     * 两点经纬度的直线距离（米）。任一点缺经纬度返回极大值，排序时排到最后。
     */
    private double haversine(CollectionSite a, CollectionSite b) {
        if (a.longitude() == null || a.latitude() == null
                || b.longitude() == null || b.latitude() == null) {
            return Double.MAX_VALUE;
        }
        double r = 6371000;
        double dLat = Math.toRadians(b.latitude() - a.latitude());
        double dLng = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(a.latitude())) * Math.cos(Math.toRadians(b.latitude()))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.asin(Math.min(1, Math.sqrt(h)));
    }

    private boolean isDistinct(List<CollectionSite> candidate, List<CollectionScheme> existing) {
        Set<String> candidateNames = new LinkedHashSet<>();
        for (CollectionSite s : candidate) {
            candidateNames.add(s.name());
        }
        for (CollectionScheme scheme : existing) {
            Set<String> names = new LinkedHashSet<>();
            for (CollectionSite s : scheme.sites()) {
                names.add(s.name());
            }
            if (names.equals(candidateNames)) {
                return false;
            }
        }
        return true;
    }

    private CollectionScheme buildScheme(VisionTask task, String code, String strategy,
                                         String reason, List<CollectionSite> baseSites) {
        // 按方案点位顺序做路线规划：以首个点位为出发点，依次到后续点位
        List<CollectionSite> routed = new ArrayList<>(baseSites.size());
        Double originLng = baseSites.isEmpty() ? null : baseSites.get(0).longitude();
        Double originLat = baseSites.isEmpty() ? null : baseSites.get(0).latitude();
        double totalMeters = 0;
        long totalSeconds = 0;
        boolean anyRoute = false;
        for (int i = 0; i < baseSites.size(); i++) {
            CollectionSite site = baseSites.get(i);
            if (i == 0) {
                routed.add(site);
                continue;
            }
            String[] route = mcpToolService.planRoute(
                    baseSites.get(i - 1).longitude(), baseSites.get(i - 1).latitude(), site, task.transportMode());
            CollectionSite withTravel = (route[0] != null || route[1] != null)
                    ? site.withTravel(route[0], route[1]) : site;
            routed.add(withTravel);
            if (route[0] != null) {
                anyRoute = true;
                totalMeters += metersOf(route[0]);
            }
            if (route[1] != null) {
                totalSeconds += minutesOf(route[1]) * 60L;
            }
        }
        String totalDistance = anyRoute ? formatMeters(totalMeters) : null;
        String totalDuration = totalSeconds > 0 ? "约 " + (totalSeconds / 60) + " 分钟" : null;

        String title = "方案" + code + "：" + strategy;
        String routeSummary = buildRouteSummary(task, routed);
        return new CollectionScheme("scheme-" + code, title, reason, routed,
                routeSummary, totalDistance, totalDuration, task.transportMode());
    }

    private double metersOf(String distanceText) {
        try {
            if (distanceText.endsWith("km")) {
                return Double.parseDouble(distanceText.replace("km", "").trim()) * 1000;
            }
            if (distanceText.endsWith("m")) {
                return Double.parseDouble(distanceText.replace("m", "").trim());
            }
        } catch (Exception ignored) {
            // ignore
        }
        return 0;
    }

    private long minutesOf(String durationText) {
        try {
            String digits = durationText.replaceAll("[^0-9]", "");
            return digits.isBlank() ? 0 : Long.parseLong(digits);
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatMeters(double m) {
        return m >= 1000 ? String.format("%.1fkm", m / 1000) : Math.round(m) + "m";
    }

    /**
     * 候选点位生成：优先调用高德地图 MCP 获取真实 POI，无结果时回退规则候选点。
     */
    private List<PoiCandidate> buildCandidates(VisionTask task) {
        List<PoiCandidate> mcpCandidates = mcpToolService.searchPoiCandidates(task);
        if (mcpCandidates != null && !mcpCandidates.isEmpty()) {
            return mcpCandidates;
        }
        return buildRuleBasedCandidates(task);
    }

    /**
     * 候选点位生成（规则版兜底）。依据任务场景偏好与位置，组合典型场景点位。
     */
    private List<PoiCandidate> buildRuleBasedCandidates(VisionTask task) {
        String loc = task.location() == null || task.location().isBlank() ? "目标区域" : task.location();
        List<PoiCandidate> list = new ArrayList<>();

        // 依据任务目标对象/场景偏好推导需要的场景类型
        List<VisionSceneType> scenes = inferScenes(task);
        for (VisionSceneType scene : scenes) {
            list.add(new PoiCandidate(
                    loc + scene.getLabel() + "（候选点）",
                    loc + "周边" + scene.getLabel(),
                    scene.getLabel(),
                    -1,
                    -1,
                    null,
                    null
            ));
        }
        return list;
    }

    private List<VisionSceneType> inferScenes(VisionTask task) {
        List<VisionSceneType> scenes = new ArrayList<>();
        if (task.sceneTypes() != null) {
            for (String s : task.sceneTypes()) {
                VisionSceneType t = VisionSceneType.fromText(s);
                if (!scenes.contains(t)) {
                    scenes.add(t);
                }
            }
        }
        // 由目标对象补充
        if (task.targetObjects() != null) {
            for (String obj : task.targetObjects()) {
                VisionSceneType t = VisionSceneType.fromText(obj);
                if (t != VisionSceneType.GENERAL && !scenes.contains(t)) {
                    scenes.add(t);
                }
                if (obj.contains("车") && !scenes.contains(VisionSceneType.INTERSECTION)) {
                    scenes.add(VisionSceneType.INTERSECTION);
                }
                if (obj.contains("行人") && !scenes.contains(VisionSceneType.CAMPUS)) {
                    scenes.add(VisionSceneType.CAMPUS);
                }
            }
        }
        if (scenes.isEmpty()) {
            scenes.add(VisionSceneType.INTERSECTION);
            scenes.add(VisionSceneType.CAMPUS);
            scenes.add(VisionSceneType.PARKING);
        }
        return scenes;
    }

    private String buildTitle(VisionTask task) {
        String loc = task.location() == null || task.location().isBlank() ? "" : task.location();
        return loc + task.taskType() + "数据采集计划";
    }

    private String buildRouteSummary(VisionTask task, List<CollectionSite> sites) {
        if (sites.isEmpty()) {
            return "暂无候选点位，建议补充更明确的采集位置后重试。";
        }
        String names = sites.stream().map(CollectionSite::name).reduce((a, b) -> a + " → " + b).orElse("");
        return "建议采集顺序：" + names + "。交通方式：" + task.transportMode()
                + "，时间预算：" + task.timeBudget() + "，按点位优先级与时段合理安排。";
    }

    private List<String> buildChecklist() {
        return List.of(
                "相机/手机已充电，存储空间充足（≥32GB）",
                "备用电源与存储卡",
                "记录每段采集的时间、地点、天气与光照",
                "三脚架/稳定器（按需）",
                "采集授权/报备材料（园区、校园等场景）"
        );
    }

    private List<String> buildAnnotationGuide(VisionTask task) {
        List<String> guide = new ArrayList<>();
        guide.add("按场景与时间分目录归档原始数据");
        if (task.taskType() != null && task.taskType().contains("分割")) {
            guide.add("使用像素级标注工具，输出 mask（PNG/COCO 格式）");
        } else if (task.taskType() != null && task.taskType().contains("深度")) {
            guide.add("记录相机内参与采集距离，便于深度真值对齐");
        } else {
            guide.add("使用 VOC/COCO 格式标注边界框，遮挡目标标记 difficult");
        }
        if (task.targetObjects() != null && !task.targetObjects().isEmpty()) {
            guide.add("类别：" + String.join(" / ", task.targetObjects()));
        }
        return guide;
    }

    private List<String> buildPrivacyTips() {
        return List.of(
                "不采集可识别的人脸特写与车牌清晰特写",
                "公共区域采集，避开私人住宅与敏感区域",
                "含个人信息的数据对外展示前进行脱敏",
                "采集数据仅用于约定的研究与系统验证用途"
        );
    }
}
