package cn.chengshuai.csaiagent.vision.service;

import cn.chengshuai.csaiagent.vision.model.VisionSceneType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VisionSceneClassifierTest {

    private final VisionSceneClassifier classifier = new VisionSceneClassifier();

    @Test
    void classify_intersectionKeyword() {
        assertEquals(VisionSceneType.INTERSECTION, classifier.classify("中关村大街十字路口", "交叉口"));
        assertEquals(VisionSceneType.INTERSECTION, classifier.classify("红绿灯路口", null));
    }

    @Test
    void classify_campusAndParking() {
        assertEquals(VisionSceneType.CAMPUS, classifier.classify("某某大学南门", "学校"));
        assertEquals(VisionSceneType.PARKING, classifier.classify("地下停车场", "停车"));
    }

    @Test
    void classify_unknown_returnsGeneral() {
        assertEquals(VisionSceneType.GENERAL, classifier.classify("某咖啡馆", "餐饮"));
    }
}
