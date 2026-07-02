# 故障排除指南

## 问题描述
前端能够发送消息到后端，但后端的消息无法传到前端，出现 `net::ERR_CONNECTION_REFUSED` 错误。

## 问题分析

### 1. 错误类型
- `net::ERR_CONNECTION_REFUSED` - 连接被拒绝
- SSE连接建立失败
- 前端无法接收后端流式响应

### 2. 可能的原因
1. **后端服务未启动** - SpringBoot应用未运行在8123端口
2. **SSE接口实现问题** - 后端SSE响应格式不正确
3. **CORS配置问题** - 跨域请求被阻止
4. **网络配置问题** - 防火墙或代理设置

## 解决方案

### 方案1: 检查后端服务状态
```bash
# 检查8123端口是否被监听
netstat -an | grep 8123

# 或者使用curl测试
curl -v http://localhost:8123/api/health
```

### 方案2: 修复后端SSE接口
确保后端SSE接口正确实现：

```java
@GetMapping(value = "/vision/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> doChatWithVisionSSE(String message, String chatId) {
    return visionQaApp.doChatByStream(message, chatId)
        .map(data -> "data: " + data + "\n\n")
        .concatWith(Mono.just("data: [DONE]\n\n"));
}
```

### 方案3: 添加CORS配置
在SpringBoot应用中添加CORS配置：

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("http://localhost:3000")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
```

### 方案4: 使用调试工具
项目已包含调试页面 (`/debug`)，可以：
- 测试基本连接
- 测试SSE连接
- 查看网络日志
- 手动发送测试消息

## 前端优化

### 1. 多种连接方式
- 优先使用SSE
- 自动降级到轮询模式
- 连接失败时显示友好提示

### 2. 错误处理
- 自动重连机制
- 用户友好的错误提示
- 详细的调试日志

### 3. 连接状态显示
- 实时显示连接状态
- 自动切换连接模式
- 网络状态指示器

## 测试步骤

1. **启动后端服务**
   ```bash
   # 确保SpringBoot应用运行在8123端口
   ```

2. **启动前端服务**
   ```bash
   npm run dev
   ```

3. **访问调试页面**
   - 打开 http://localhost:3000/debug
   - 点击"测试基本连接"
   - 点击"测试视觉问答SSE"
   - 查看测试结果和日志

4. **检查控制台输出**
   - 查看浏览器控制台
   - 查看后端应用日志
   - 检查网络请求状态

## 常见问题

### Q: 为什么前端能发送消息但收不到回复？
A: 这通常表示HTTP请求成功，但SSE连接建立失败。检查后端SSE接口实现和CORS配置。

### Q: 如何判断是前端还是后端问题？
A: 使用调试页面的连接测试功能，可以分别测试基本HTTP连接和SSE连接。

### Q: SSE连接失败后如何恢复？
A: 前端会自动切换到轮询模式，或者可以手动刷新页面重新建立连接。

## 联系支持

如果问题仍然存在，请提供：
1. 后端应用日志
2. 浏览器控制台错误信息
3. 网络请求状态截图
4. 调试页面的测试结果 