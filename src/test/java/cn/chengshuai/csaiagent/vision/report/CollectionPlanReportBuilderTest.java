package cn.chengshuai.csaiagent.vision.report;

import cn.chengshuai.csaiagent.vision.model.CollectionPlan;
import cn.chengshuai.csaiagent.vision.model.CollectionSite;
import cn.chengshuai.csaiagent.vision.model.VisionTask;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionPlanReportBuilderTest {

    private final CollectionPlanReportBuilder builder = new CollectionPlanReportBuilder();

    @Test
    void build_containsAllRequiredSections() {
        VisionTask task = new VisionTask("目标检测", List.of("车辆", "行人"),
                List.of("十字路口"), "北京", "半天", "骑行");
        CollectionSite site = new CollectionSite("中关村路口", "中关村大街", "1.2km",
                List.of("十字路口"), List.of("车辆检测"), 86, "典型路口",
                List.of("早晚高峰采集"), List.of("注意人身安全"), List.of());
        CollectionPlan plan = new CollectionPlan("plan-1", "北京目标检测数据采集计划", task,
                List.of(site), "建议骑行路线……",
                List.of("相机充电"), List.of("VOC 格式标注"), List.of("不采集人脸特写"), null);

        String text = builder.build(plan);

        assertTrue(text.contains("一、任务概述"), "缺少任务概述章节");
        assertTrue(text.contains("二、推荐采集点位"), "缺少推荐采集点位章节");
        assertTrue(text.contains("三、路线与时段建议"), "缺少路线章节");
        assertTrue(text.contains("四、采集清单"), "缺少采集清单章节");
        assertTrue(text.contains("五、标注与隐私提示"), "缺少标注与隐私提示章节");
        assertTrue(text.contains("中关村路口"), "应包含点位名称");
        assertTrue(text.contains("86"), "应包含点位得分");
    }
}
