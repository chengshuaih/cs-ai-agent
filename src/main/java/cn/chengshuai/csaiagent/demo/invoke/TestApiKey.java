package cn.chengshuai.csaiagent.demo.invoke;

public class TestApiKey {
    // 从环境变量读取，避免将真实密钥写入源码
    public static String ApiKey = System.getenv().getOrDefault("DASHSCOPE_API_KEY", "");
}
