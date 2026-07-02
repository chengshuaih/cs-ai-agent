package cn.chengshuai.csaiagent.vision.service;

import java.util.List;

/**
 * 视觉实验流程模板：按任务类型预置数据/标注/划分/模型/训练/指标/风险要点。
 * 第一版固定文案，后续可外置到 resources。
 */
public enum ExperimentPlanTemplate {

    DETECTION("目标检测",
            List.of("收集多场景、多光照原始图像", "去重并筛除模糊/无目标图像", "按场景与时间归档"),
            List.of("使用 VOC/COCO 格式标注边界框", "明确类别清单，遮挡目标标记 difficult", "交叉抽检标注一致性"),
            "训练/验证/测试 = 7:2:1，保证类别与场景分布一致",
            List.of("YOLO 系列（YOLOv8/v11）", "Faster R-CNN", "RT-DETR"),
            List.of("配置数据集与类别", "选择预训练权重微调", "调参（学习率/anchor/输入尺寸）", "训练并监控 loss/mAP", "在测试集评估并可视化误检"),
            List.of("mAP@0.5", "mAP@0.5:0.95", "Precision/Recall", "FPS"),
            List.of("类别不平衡导致漏检", "小目标与遮挡场景精度下降", "标注噪声影响指标")),

    SEGMENTATION("图像分割",
            List.of("收集像素级标注可行的清晰图像", "覆盖目标边界复杂样本", "统一分辨率与命名"),
            List.of("使用像素级标注工具输出 mask（PNG/COCO）", "区分语义/实例分割类别", "处理边界与重叠区域"),
            "训练/验证/测试 = 7:2:1，保证类别像素占比均衡",
            List.of("U-Net", "DeepLabV3+", "Mask R-CNN", "SegFormer"),
            List.of("准备图像与 mask 对", "选择骨干网络与预训练权重", "训练并监控 mIoU", "后处理（CRF/形态学）", "评估并可视化分割结果"),
            List.of("mIoU", "Dice 系数", "像素准确率"),
            List.of("细小边界标注困难", "类别像素极不均衡", "高分辨率显存压力")),

    DEPTH_ESTIMATION("深度估计",
            List.of("采集带深度真值的数据（激光/双目）或选用公开数据集", "记录相机内参与基线", "对齐 RGB 与深度帧"),
            List.of("生成/校验深度真值", "标注无效深度区域掩码", "记录采集距离范围"),
            "按场景划分训练/验证/测试，避免同场景泄漏",
            List.of("MiDaS", "Monodepth2", "DPT"),
            List.of("准备 RGB-Depth 对", "选择单目/双目方案", "训练并监控 AbsRel", "尺度对齐与后处理", "评估并可视化深度图"),
            List.of("AbsRel", "RMSE", "δ<1.25 准确率"),
            List.of("深度真值获取成本高", "弱纹理/反光区域估计不准", "尺度模糊性")),

    RECONSTRUCTION("三维重建",
            List.of("多视角图像采集，保证充分重叠", "记录拍摄轨迹与相机参数", "控制光照一致性"),
            List.of("标定相机内外参", "必要时标注特征点/掩码", "剔除模糊与运动帧"),
            "按场景/物体划分实验集，验证泛化",
            List.of("COLMAP（SfM/MVS）", "NeRF 系（Instant-NGP）", "3D Gaussian Splatting"),
            List.of("特征提取与匹配", "稀疏重建（SfM）", "稠密重建（MVS）或神经渲染训练", "网格/点云生成", "评估重建精度与完整度"),
            List.of("重投影误差", "点云完整度（completeness）", "重建精度（accuracy）"),
            List.of("视角覆盖不足导致空洞", "弱纹理表面匹配失败", "计算资源消耗大"));

    private final String taskType;
    private final List<String> dataPrep;
    private final List<String> annotationDesign;
    private final String datasetSplit;
    private final List<String> recommendedModels;
    private final List<String> trainingSteps;
    private final List<String> metrics;
    private final List<String> risks;

    ExperimentPlanTemplate(String taskType, List<String> dataPrep, List<String> annotationDesign,
                           String datasetSplit, List<String> recommendedModels,
                           List<String> trainingSteps, List<String> metrics, List<String> risks) {
        this.taskType = taskType;
        this.dataPrep = dataPrep;
        this.annotationDesign = annotationDesign;
        this.datasetSplit = datasetSplit;
        this.recommendedModels = recommendedModels;
        this.trainingSteps = trainingSteps;
        this.metrics = metrics;
        this.risks = risks;
    }

    public String getTaskType() {
        return taskType;
    }

    public List<String> getDataPrep() {
        return dataPrep;
    }

    public List<String> getAnnotationDesign() {
        return annotationDesign;
    }

    public String getDatasetSplit() {
        return datasetSplit;
    }

    public List<String> getRecommendedModels() {
        return recommendedModels;
    }

    public List<String> getTrainingSteps() {
        return trainingSteps;
    }

    public List<String> getMetrics() {
        return metrics;
    }

    public List<String> getRisks() {
        return risks;
    }

    /**
     * 根据任务类型文本匹配模板，无匹配返回 DETECTION（兜底）。
     */
    public static ExperimentPlanTemplate fromTaskType(String taskType) {
        if (taskType == null || taskType.isBlank()) {
            return DETECTION;
        }
        String t = taskType.trim();
        for (ExperimentPlanTemplate tpl : values()) {
            if (t.contains(tpl.taskType) || tpl.taskType.contains(t)) {
                return tpl;
            }
        }
        if (t.contains("分割")) {
            return SEGMENTATION;
        }
        if (t.contains("深度")) {
            return DEPTH_ESTIMATION;
        }
        if (t.contains("重建") || t.contains("3d") || t.contains("三维")) {
            return RECONSTRUCTION;
        }
        return DETECTION;
    }
}
