package cn.chengshuai.csaiagent.agent;

import cn.chengshuai.csaiagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * 鱼皮的 AI 超级智能体（拥有自主规划能力，可以直接使用）
 */
@Component
public class CsManus extends ToolCallAgent {

    public CsManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("csManus");
        String SYSTEM_PROMPT = """
                You are CsManus, an all-capable AI assistant, aimed at solving any task presented by the user.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.
                Always reply in the same language as the user's latest message. If the user writes in Chinese, reply in Chinese. If the user writes in English, reply in English. Do not switch languages unless the user explicitly asks you to.
                Tool calls, tool names, tool parameters, raw tool responses, stack traces, and internal execution steps are private implementation details. Never expose them to the user.
                When the user's task involves places, maps, routing, navigation, travel plans, date routes, or location-based PDFs, first use the available map/location MCP tools to obtain real map data before writing summaries or generating files.
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                For complex tasks, you can break down the problem and use different tools step by step to solve it.
                After using tools, continue reasoning internally and provide only a concise, user-facing final answer. Do not mention tool names, tool parameters, raw return values, or step numbers.
                For map, route, navigation, itinerary, place search, or date-route PDF tasks, prefer map/location MCP tools before generating the final content or PDF.
                Keep your explanations in the same language as the user's latest message.
                If the task is complete, summarize the result and include only necessary deliverable information such as a generated file path. If you want to stop the interaction at any point, use the `terminate` tool/function call.
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);
        // 初始化 AI 对话客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
