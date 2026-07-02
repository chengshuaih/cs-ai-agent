package cn.chengshuai.csaiagent.tools;

import cn.chengshuai.csaiagent.vision.service.CollectionPlanService;
import cn.chengshuai.csaiagent.vision.service.ExperimentPlanService;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 集中的工具注册类
 */
@Configuration
public class ToolRegistration {

    @Value("${search-api.api-key}")
    private String searchApiKey;

    @Bean
    public ToolCallback[] allTools(CollectionPlanService collectionPlanService,
                                   ExperimentPlanService experimentPlanService) {
        FileOperationTool fileOperationTool = new FileOperationTool();
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        TerminateTool terminateTool = new TerminateTool();
        CollectionPlanTool collectionPlanTool = new CollectionPlanTool(collectionPlanService);
        ExperimentPlanTool experimentPlanTool = new ExperimentPlanTool(experimentPlanService);
        return ToolCallbacks.from(
                fileOperationTool,
                webSearchTool,
                webScrapingTool,
                resourceDownloadTool,
                terminalOperationTool,
                pdfGenerationTool,
                terminateTool,
                collectionPlanTool,
                experimentPlanTool
        );
    }
}
