package cn.chengshuai.csaiagent.demo.invoke;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
public class LangChainAiInvoke {
    public static void main(String[] args) {
        QwenChatModel chatModel = QwenChatModel.builder()
                .apiKey(TestApiKey.ApiKey)
                .modelName("qwen-plus")
                .build();
        String chat = chatModel.chat("你好，我是程帅");
        System.out.println(chat);
    }
}
