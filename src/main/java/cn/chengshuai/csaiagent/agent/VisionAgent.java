package cn.chengshuai.csaiagent.agent;

import cn.chengshuai.csaiagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

/**
 * 机器视觉实验智能体：面向机器视觉数据采集、实验规划与报告生成的自主规划代理。
 *
 * <p>请求级实例化（参考 {@link CsManus}）。采集规划统一经 CollectionPlanTool 触发，
 * 报告导出经 PDF 工具，资料检索经搜索/图片检索工具，自身不拼接地图逻辑（单一中枢）。</p>
 */
public class VisionAgent extends ToolCallAgent {

    public VisionAgent(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("visionAgent");
        String systemPrompt = """
                你是机器视觉实验智能体，专注于协助用户完成机器视觉相关的数据采集规划、实验流程规划与报告生成。
                你具备多种工具，可按需调用以高效完成复杂任务。
                始终使用与用户最新消息相同的语言回复。
                工具调用、工具名称、工具参数、原始工具返回、堆栈与内部执行步骤均为内部实现细节，绝不向用户暴露。
                当任务涉及数据采集点位、路线、采集计划时，优先调用采集规划工具（planCollection）获取结构化采集方案，再据此撰写总结；
                当任务涉及生成 PDF 报告时，使用 PDF 生成工具；当需要查阅资料或样例图时，使用搜索/图片检索工具。
                与机器视觉无关的请求，先说明系统边界再给出简要说明。
                """;
        this.setSystemPrompt(systemPrompt);
        String nextStepPrompt = """
                根据用户需求，主动选择最合适的工具或工具组合。
                复杂任务可拆解后分步使用不同工具解决。
                采集点位/路线/采集计划相关任务优先使用 planCollection 工具；报告导出使用 PDF 工具；资料/样例图使用搜索类工具。
                使用工具后继续内部推理，仅向用户输出简洁的最终结果，不要提及工具名称、参数、原始返回或步骤编号。
                保持与用户最新消息相同的语言。
                任务完成时总结结果并仅包含必要的交付信息（如生成的文件路径）。如需结束交互，调用 terminate 工具。
                """;
        this.setNextStepPrompt(nextStepPrompt);
        this.setMaxSteps(20);
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
