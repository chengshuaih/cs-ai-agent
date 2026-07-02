package cn.chengshuai.csaiagent.vision.service;

import cn.chengshuai.csaiagent.vision.model.VisionTask;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 自然语言 → 结构化 {@link VisionTask}。
 *
 * <p>第一版以规则抽取为主（关键词匹配 taskType / targetObjects / location / timeBudget /
 * transportMode），保证在没有大模型结构化输出时链路仍可跑通；当上游已传入结构化 task 时，
 * 仅做规范化与缺省值兜底。</p>
 */
@Service
public class VisionTaskParser {

    private static final List<String> TASK_TYPES = List.of(
            "目标检测", "小目标检测", "图像分割", "语义分割", "实例分割",
            "深度估计", "三维重建", "图像分类", "目标跟踪");

    private static final List<String> OBJECT_KEYWORDS = List.of(
            "车辆", "行人", "交通灯", "红绿灯", "车牌", "路沿", "骑行者",
            "自行车", "电动车", "卡车", "公交", "标志牌", "斑马线");

    /**
     * 规范化已结构化的任务，填充缺省值。
     */
    public VisionTask normalize(VisionTask task) {
        if (task == null) {
            return new VisionTask("目标检测", List.of(), List.of(), "未指定", "半天", "步行");
        }
        String taskType = isBlank(task.taskType()) ? "目标检测" : task.taskType().trim();
        List<String> targets = task.targetObjects() == null ? List.of() : task.targetObjects();
        List<String> scenes = task.sceneTypes() == null ? List.of() : task.sceneTypes();
        String location = isBlank(task.location()) ? "未指定" : task.location().trim();
        String timeBudget = isBlank(task.timeBudget()) ? "半天" : task.timeBudget().trim();
        String transport = isBlank(task.transportMode()) ? "步行" : task.transportMode().trim();
        return new VisionTask(taskType, targets, scenes, location, timeBudget, transport);
    }

    /**
     * 从自然语言文本中规则抽取任务。
     */
    public VisionTask parse(String text) {
        if (isBlank(text)) {
            return normalize(null);
        }
        String t = text.trim();

        String taskType = TASK_TYPES.stream().filter(t::contains).findFirst().orElse("目标检测");

        Set<String> targets = new LinkedHashSet<>();
        for (String kw : OBJECT_KEYWORDS) {
            if (t.contains(kw)) {
                targets.add(kw);
            }
        }

        String location = extractLocation(t);
        String timeBudget = extractTimeBudget(t);
        String transportMode = extractTransport(t);

        return new VisionTask(taskType, new ArrayList<>(targets), List.of(),
                location, timeBudget, transportMode);
    }

    private String extractLocation(String t) {
        // 规则：匹配常见地点后缀，取“最具体”（在文本中出现最靠后）的后缀作为锚点，
        // 这样“北京市海淀区中关村路口”能保留到 路口 而非止步于 市。
        List<String> suffixes = List.of("市", "区", "县", "路口", "路", "街", "园区", "校区", "大学", "学院", "广场", "口");
        int bestEnd = -1;
        String bestSuffix = null;
        for (String suffix : suffixes) {
            int idx = t.lastIndexOf(suffix);
            if (idx > 0 && idx + suffix.length() > bestEnd) {
                bestEnd = idx + suffix.length();
                bestSuffix = suffix;
            }
        }
        if (bestSuffix == null) {
            return "未指定";
        }
        int start = Math.max(0, bestEnd - bestSuffix.length() - 8);
        String seg = t.substring(start, bestEnd);
        // 去掉动词前缀
        for (String verb : List.of("在", "去", "到", "于", "想", "要")) {
            int v = seg.lastIndexOf(verb);
            if (v >= 0 && v < seg.length() - 1) {
                seg = seg.substring(v + 1);
            }
        }
        return seg;
    }

    private String extractTimeBudget(String t) {
        for (String unit : List.of("整天", "一天", "全天", "半天", "小时", "分钟")) {
            int idx = t.indexOf(unit);
            if (idx >= 0) {
                int start = Math.max(0, idx - 3);
                return t.substring(start, idx + unit.length());
            }
        }
        return "半天";
    }

    private String extractTransport(String t) {
        if (t.contains("驾车") || t.contains("开车") || t.contains("自驾")) {
            return "驾车";
        }
        if (t.contains("骑行") || t.contains("骑车") || t.contains("自行车") || t.contains("电动车")) {
            return "骑行";
        }
        return "步行";
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
