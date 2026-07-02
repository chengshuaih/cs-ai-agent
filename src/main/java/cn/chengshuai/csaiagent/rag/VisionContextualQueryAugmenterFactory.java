package cn.chengshuai.csaiagent.rag;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;

/**
 * 机器视觉问答空上下文增强器：检索不到知识时给出系统边界提示。
 */
public class VisionContextualQueryAugmenterFactory {

    public static ContextualQueryAugmenter createInstance() {
        PromptTemplate emptyContextPromptTemplate = new PromptTemplate("""
                你应该输出下面的内容：
                抱歉，我目前只能回答机器视觉相关的问题（如目标检测、图像分割、深度估计、三维重建、数据采集与标注、实验流程与评价指标等）。
                """);
        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(false)
                .emptyContextPromptTemplate(emptyContextPromptTemplate)
                .build();
    }
}
