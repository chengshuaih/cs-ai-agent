package cn.chengshuai.csaiagent.agent;

import cn.chengshuai.csaiagent.agent.model.AgentState;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 处理工具调用的基础代理类，具体实现了 think 和 act 方法，可以用作创建实例的父类  
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    // 可用的工具
    private final ToolCallback[] availableTools;

    // 保存工具调用信息的响应结果（要调用那些工具）
    private ChatResponse toolCallChatResponse;

    // 保存无需调用工具时的模型回答
    private String lastThinkResult;

    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;

    // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
    private final ChatOptions chatOptions;

    // 流式输出目标（runStream 时注入）
    private SseEmitter streamingEmitter;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
        this.chatOptions = DashScopeChatOptions.builder()
                .withInternalToolExecutionEnabled(false)
                .build();
    }

    @Override
    protected void injectStreamingEmitter(SseEmitter emitter) {
        this.streamingEmitter = emitter;
    }

    /**
     * 处理当前状态并决定下一步行动
     *
     * @return 是否需要执行行动
     */
    @Override
    public boolean think() {
        // 1、校验提示词，拼接用户提示词
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }
        // 2、调用 AI 大模型，获取工具调用结果
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, this.chatOptions);
        try {
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(availableTools)
                    .call()
                    .chatResponse();
            // 记录响应，用于等下 Act
            this.toolCallChatResponse = chatResponse;
            // 3、解析工具调用结果，获取要调用的工具
            // 助手消息
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            // 获取要调用的工具列表
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            // 输出提示信息
            String result = assistantMessage.getText();
            log.info(getName() + "的思考：" + result);
            log.info(getName() + "选择了 " + toolCallList.size() + " 个工具来使用");
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称：%s，参数：%s", toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            // 如果不需要调用工具，返回 false
            if (toolCallList.isEmpty()) {
                // 只有不调用工具时，才需要手动记录助手消息
                getMessageList().add(assistantMessage);
                this.lastThinkResult = StrUtil.blankToDefault(result, "思考完成 - 无需行动");
                setState(AgentState.FINISHED);
                // 流式场景：把最终回复以打字机方式推送给前端
                if (streamingEmitter != null) {
                    sendAsTypewriter(this.lastThinkResult);
                }
                return false;
            } else {
                // 需要调用工具时，无需记录助手消息，因为调用工具时会自动记录
                return true;
            }
        } catch (Exception e) {
            log.error(getName() + "的思考过程遇到了问题", e);
            this.lastThinkResult = "处理时遇到了问题，请稍后再试。";
            getMessageList().add(new AssistantMessage(this.lastThinkResult));
            setState(AgentState.FINISHED);
            if (streamingEmitter != null) {
                sendAsTypewriter(this.lastThinkResult);
            }
            return false;
        }
    }

    /**
     * 将文本按小块分段 push 到 SseEmitter，模拟打字机效果。
     * 必须在 [DONE] 发送之前同步执行完毕。
     */
    private void sendAsTypewriter(String text) {
        if (streamingEmitter == null || text == null || text.isEmpty()) {
            log.warn("sendAsTypewriter 跳过：emitter={}, textLen={}", streamingEmitter != null, text == null ? 0 : text.length());
            return;
        }
        log.info("开始打字机推送，文本长度={}", text.length());
        int chunkSize = 10;
        for (int i = 0; i < text.length(); i += chunkSize) {
            String chunk = text.substring(i, Math.min(i + chunkSize, text.length()));
            try {
                streamingEmitter.send(SseEmitter.event().data(chunk));
                Thread.sleep(25);
            } catch (Exception ex) {
                log.warn("SSE typewriter send failed at {}", i, ex);
                break;
            }
        }
        log.info("打字机推送完成");
    }

    /**
     * 执行工具调用并处理结果
     *
     * @return 执行结果
     */
    @Override
    public String step() {
        boolean shouldAct = think();
        if (!shouldAct) {
            return StrUtil.blankToDefault(lastThinkResult, "思考完成 - 无需行动");
        }
        return act();
    }

    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "";
        }
        // 调用工具
        Prompt prompt = new Prompt(getMessageList(), this.chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        // 记录消息上下文，conversationHistory 已经包含了助手消息和工具调用返回的结果
        setMessageList(toolExecutionResult.conversationHistory());
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        // 判断是否调用了终止工具
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> response.name().equals("doTerminate"));
        if (terminateToolCalled && toolResponseMessage.getResponses().size() == 1) {
            // 只有单独调用终止工具时才结束；如果同一轮还有业务工具结果，需要再让模型生成最终回复。
            setState(AgentState.FINISHED);
        }
        String results = toolResponseMessage.getResponses().stream()
                .filter(response -> !response.name().equals("doTerminate"))
                .map(response -> "工具 " + response.name() + " 返回的结果：" + response.responseData())
                .collect(Collectors.joining("\n"));
        log.info(results);
        // 工具结果只进入对话上下文，不直接发给前端；下一轮 think 负责生成用户可读总结。
        return "";
    }
}