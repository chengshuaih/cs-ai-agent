package cn.chengshuai.csaiagent.controller;

import cn.chengshuai.csaiagent.agent.VisionAgent;
import cn.chengshuai.csaiagent.app.VisionQaApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.stream.Stream;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private VisionQaApp visionQaApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private ObjectProvider<ToolCallbackProvider> toolCallbackProvider;

    /**
     * 同步问答。
     */
    @GetMapping("/vision/chat/sync")
    public String doChatWithVisionSync(String message, String chatId) {
        return visionQaApp.doChat(message, chatId);
    }

    /**
     * SSE 流式问答。
     */
    @GetMapping(value = "/vision/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithVisionSSE(String message, String chatId) {
        return visionQaApp.doChatByStream(message, chatId);
    }

    /**
     * RAG 知识库问答（同步）。
     */
    @GetMapping("/vision/chat/rag")
    public String doChatWithVisionRag(String message, String chatId) {
        return visionQaApp.doChatWithRag(message, chatId);
    }

    /**
     * 视觉智能体（工具/地图/PDF）。
     */
    @GetMapping("/vision-agent/chat")
    public SseEmitter doChatWithVisionAgent(String message) {
        ToolCallbackProvider provider = toolCallbackProvider.getIfAvailable();
        ToolCallback[] mcpTools = provider == null ? new ToolCallback[0] : provider.getToolCallbacks();
        ToolCallback[] tools = Stream.concat(Arrays.stream(allTools), Arrays.stream(mcpTools))
                .toArray(ToolCallback[]::new);
        VisionAgent visionAgent = new VisionAgent(tools, dashscopeChatModel);
        return visionAgent.runStream(message);
    }
}
