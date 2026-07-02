package cn.chengshuai.csaiagent.controller;

import cn.chengshuai.csaiagent.common.ApiResponse;
import cn.chengshuai.csaiagent.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 知识库文档管理：上传文本到向量库、查询已入库文档列表。
 */
@RestController
@RequestMapping("/vision/knowledge")
@Slf4j
public class KnowledgeController {

    private final SimpleVectorStore vectorStore;

    public KnowledgeController(SimpleVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 上传文本到向量库。
     */
    @PostMapping("/upload")
    public ApiResponse<Void> upload(@RequestBody UploadRequest request) {
        if (request == null || isBlank(request.content())) {
            return ApiResponse.error(ResultCode.PARAM_INVALID, "内容不能为空");
        }
        try {
            String title = isBlank(request.title()) ? "未命名文档" : request.title();
            Document doc = new Document(request.content(),
                    Map.of("title", title, "domain", "manual"));
            vectorStore.add(List.of(doc));
            log.info("知识库文档上传成功: {}", title);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("知识库上传失败", e);
            return ApiResponse.error(ResultCode.INTERNAL_ERROR, "上传失败：" + e.getMessage());
        }
    }

    /**
     * 查询已入库文档（直接读取内存 store，不调用 embedding）。
     */
    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> list() {
        try {
            List<Map<String, Object>> result = vectorStore.get().values().stream()
                    .map(d -> Map.<String, Object>of(
                            "id", d.getId(),
                            "title", d.getMetadata().getOrDefault("title", ""),
                            "domain", d.getMetadata().getOrDefault("domain", ""),
                            "preview", d.getText().length() > 100
                                    ? d.getText().substring(0, 100) + "..." : d.getText()
                    )).toList();
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("知识库列表查询失败", e);
            return ApiResponse.error(ResultCode.INTERNAL_ERROR, "查询失败：" + e.getMessage());
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public record UploadRequest(String title, String content) {}
}
