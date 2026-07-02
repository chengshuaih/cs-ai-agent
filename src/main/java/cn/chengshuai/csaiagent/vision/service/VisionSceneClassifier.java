package cn.chengshuai.csaiagent.vision.service;

import cn.chengshuai.csaiagent.vision.model.VisionSceneType;
import org.springframework.stereotype.Service;

/**
 * POI / 文本特征 → {@link VisionSceneType} 映射。
 *
 * <p>规则匹配封装在 {@link VisionSceneType#fromText(String)} 中，此服务作为业务层入口，
 * 便于后续接入更复杂的分类策略（如 POI 类型编码映射）。</p>
 */
@Service
public class VisionSceneClassifier {

    /**
     * 根据 POI 名称 + 类型文本判定场景类型。
     *
     * @param poiName 名称
     * @param poiType POI 类型（可空）
     * @return 视觉场景类型，无匹配返回 GENERAL
     */
    public VisionSceneType classify(String poiName, String poiType) {
        String combined = (poiName == null ? "" : poiName) + " " + (poiType == null ? "" : poiType);
        return VisionSceneType.fromText(combined);
    }

    public VisionSceneType classify(String text) {
        return VisionSceneType.fromText(text);
    }
}
