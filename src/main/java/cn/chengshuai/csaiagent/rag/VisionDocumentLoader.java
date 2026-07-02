package cn.chengshuai.csaiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 机器视觉知识文档加载器。
 * 文件名约定：{domain}_{描述}.md，如 detection_目标检测常见问题.md，下划线前缀作为 domain 元数据。
 * 无下划线时 domain 记为 general，避免对中文文件名做位置截取导致越界或无意义。
 */
@Component
@Slf4j
public class VisionDocumentLoader {

    private final ResourcePatternResolver resourcePatternResolver;

    public VisionDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 加载所有 markdown 文档。
     */
    public List<Document> loadMarkdowns() {
        List<Document> documents = new ArrayList<>();
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                String domain = resolveDomain(filename);
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeBlockquote(false)
                        .withIncludeCodeBlock(false)
                        .withAdditionalMetadata("filename", filename)
                        .withAdditionalMetadata("domain", domain)
                        .build();
                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
                documents.addAll(reader.get());
            }
        } catch (IOException e) {
            log.error("markdown 文档加载失败", e);
        }
        return documents;
    }

    private String resolveDomain(String filename) {
        if (filename == null || !filename.contains("_")) {
            return "general";
        }
        return filename.substring(0, filename.indexOf('_'));
    }
}
