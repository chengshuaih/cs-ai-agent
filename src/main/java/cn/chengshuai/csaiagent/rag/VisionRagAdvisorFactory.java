package cn.chengshuai.csaiagent.rag;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/**
 * 机器视觉 RAG 检索增强顾问工厂。
 */
public class VisionRagAdvisorFactory {

    /**
     * 创建按 domain 过滤的 RAG 检索增强顾问。
     *
     * @param vectorStore 向量存储
     * @param domain      文档领域（如 detection / segmentation），为空则不过滤
     */
    public static Advisor createVisionRagAdvisor(VectorStore vectorStore, String domain) {
        VectorStoreDocumentRetriever.Builder retrieverBuilder = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.5)
                .topK(3);
        if (domain != null && !domain.isBlank()) {
            Filter.Expression expression = new FilterExpressionBuilder()
                    .eq("domain", domain)
                    .build();
            retrieverBuilder.filterExpression(expression);
        }
        DocumentRetriever documentRetriever = retrieverBuilder.build();
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryAugmenter(VisionContextualQueryAugmenterFactory.createInstance())
                .build();
    }
}
