package cn.chengshuai.csaiagent.demo.invoke;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.ContentType;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONArray;

/*
 * Http AI SDK 调用
 * */

public class HttpAiInvoke {
    public static void main(String[] args) {
        String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
        String apiKey = TestApiKey.ApiKey; // 或直接写成字符串

        // 构建请求体
        JSONObject body = new JSONObject();
        body.set("model", "qwen-plus");

        // input.messages
        JSONObject input = new JSONObject();
        JSONArray messages = new JSONArray();
        JSONObject systemMsg = new JSONObject();
        systemMsg.set("role", "system");
        systemMsg.set("content", "You are a helpful assistant.");
        JSONObject userMsg = new JSONObject();
        userMsg.set("role", "user");
        userMsg.set("content", "你是谁？");
        messages.add(systemMsg);
        messages.add(userMsg);
        input.set("messages", messages);
        body.set("input", input);

        // parameters
        JSONObject parameters = new JSONObject();
        parameters.set("result_format", "message");
        body.set("parameters", parameters);

        // 发送POST请求
        HttpResponse response = HttpRequest.post(url)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", ContentType.JSON.getValue())
            .body(body.toString())
            .execute();

        // 输出响应
        System.out.println(response.body());
    }
}