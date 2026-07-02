package cn.chengshuai.csaiagent.vision.service;

import cn.chengshuai.csaiagent.vision.model.VisionTask;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisionTaskParserTest {

    private final VisionTaskParser parser = new VisionTaskParser();

    @Test
    void parse_extractsTaskTypeAndLocationAndTransport() {
        VisionTask task = parser.parse("我想在北京市海淀区中关村路口骑行半天采集车辆和行人做目标检测");
        assertEquals("目标检测", task.taskType());
        assertEquals("骑行", task.transportMode());
        assertTrue(task.targetObjects().contains("车辆"));
        assertTrue(task.targetObjects().contains("行人"));
        assertTrue(task.location().contains("中关村") || task.location().contains("海淀"),
                "location 应包含地点关键字，实际：" + task.location());
    }

    @Test
    void parse_blankInput_fallsBackToDefaults() {
        VisionTask task = parser.parse("   ");
        assertEquals("目标检测", task.taskType());
        assertEquals("半天", task.timeBudget());
        assertEquals("步行", task.transportMode());
        assertEquals("未指定", task.location());
    }

    @Test
    void normalize_fillsMissingFields() {
        VisionTask raw = new VisionTask("图像分割", null, null, null, null, null);
        VisionTask task = parser.normalize(raw);
        assertEquals("图像分割", task.taskType());
        assertNotNull(task.targetObjects());
        assertEquals("半天", task.timeBudget());
        assertEquals("步行", task.transportMode());
        assertEquals("未指定", task.location());
    }
}
