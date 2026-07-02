# MCP 禁用后 ToolCallbackProvider 可选注入修复规格

## 问题分类

这是一个本地启动 bug 修复。前一轮为了避免 MCP stdio 子进程阻塞本地启动，在 `application-local.yml` 中默认设置：

```yaml
spring.ai.mcp.client.enabled=false
```

该配置生效后，Spring AI 不再创建 MCP 相关的 `ToolCallbackProvider` bean，但当前业务代码仍将它作为必需依赖注入，导致应用启动失败。

## 现象与错误链路

用户执行：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export DASHSCOPE_API_KEY=真实DashScopeKey
./mvnw resources:resources
./mvnw spring-boot:run
```

MCP 相关的 `UnsupportedClassVersionError` 与 `mcpSyncClients TimeoutException` 已消失，说明 MCP 禁用配置已生效。

新的失败为：

```text
APPLICATION FAILED TO START
Description:
A component required a bean of type 'org.springframework.ai.tool.ToolCallbackProvider' that could not be found.
Action:
Consider defining a bean of type 'org.springframework.ai.tool.ToolCallbackProvider' in your configuration.
```

调用链：

```text
application-local.yml: spring.ai.mcp.client.enabled=false
  -> Spring AI 不创建 MCP ToolCallbackProvider
  -> VisionQaApp / AiController 仍强制注入 ToolCallbackProvider
  -> Bean 创建失败
  -> Spring Boot 启动失败
```

## 根因

`ToolCallbackProvider` 是 MCP client enabled 时才存在的 bean。当前代码把 MCP 可选能力作为强依赖：

- `/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/app/VisionQaApp.java`
  - `@Resource private ToolCallbackProvider toolCallbackProvider;`
  - `doChatWithMcp(...)` 直接调用 `toolCallbackProvider`。

- `/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/controller/AiController.java`
  - `@Resource private ToolCallbackProvider toolCallbackProvider;`
  - `/ai/vision-agent/chat` 将本地工具与 MCP 工具合并。

当 local profile 默认禁用 MCP 后，这两个强依赖必须改成可选依赖，否则非 MCP 能力无法启动。

## 修复目标

- 本地默认 `spring.ai.mcp.client.enabled=false` 时，应用可以正常启动。
- `/api/ai/vision-agent/chat` 在 MCP disabled 时仍可使用本地工具运行，不因 MCP provider 缺失失败。
- 仅 MCP 专属接口/方法在 provider 缺失时返回明确提示，不影响其他问答、RAG、工具能力。
- MCP enabled 且 provider 存在时，保留原有 MCP 工具合并行为。

## 技术方案

### 1. AiController 使用可选注入

将必需字段：

```java
@Resource
private ToolCallbackProvider toolCallbackProvider;
```

改为 Spring 的 `ObjectProvider<ToolCallbackProvider>` 或构造器可选注入。

推荐最小改法：

```java
@Resource
private ObjectProvider<ToolCallbackProvider> toolCallbackProvider;
```

在 `/vision-agent/chat` 中：

```java
ToolCallbackProvider provider = toolCallbackProvider.getIfAvailable();
ToolCallback[] mcpTools = provider == null ? new ToolCallback[0] : provider.getToolCallbacks();
ToolCallback[] tools = Stream.concat(Arrays.stream(allTools), Arrays.stream(mcpTools))
        .toArray(ToolCallback[]::new);
```

这样 MCP disabled 时只使用本地工具，MCP enabled 时仍合并 MCP 工具。

### 2. VisionQaApp 使用可选注入

同样将 `ToolCallbackProvider` 改为可选注入。

`doChatWithMcp(...)` 在 provider 缺失时返回明确文本，例如：

```java
ToolCallbackProvider provider = toolCallbackProvider.getIfAvailable();
if (provider == null) {
    return "MCP 服务当前未启用，请设置 spring.ai.mcp.client.enabled=true 后重启应用。";
}
```

该方法目前不是前端主入口，但保留明确行为有利于测试与后续调试。

## 影响文件

### `/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/app/VisionQaApp.java`

修改类型：依赖注入降级处理。

影响函数：

- `doChatWithMcp(String message, String chatId)`

预期行为：

- MCP provider 存在：行为不变，调用 MCP tools。
- MCP provider 不存在：返回明确提示，不阻塞应用启动。

### `/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/controller/AiController.java`

修改类型：依赖注入降级处理。

影响函数：

- `doChatWithVisionAgent(String message)`

预期行为：

- MCP provider 存在：本地工具 + MCP 工具。
- MCP provider 不存在：仅本地工具。

### `/Users/chengshuai/Work/private/cs-ai-agent/.comate/specs/mcp-startup-java17-fallback/summary.md`

修改类型：可选补充。

补充说明：本地默认 MCP disabled 时，应用仍可用本地工具启动；MCP 专属能力需显式开启。

## 边界条件

- `DASHSCOPE_API_KEY` 仍必须真实有效，否则 RAG 初始化会 401；这不是本修复范围。
- MCP disabled 时不应创建 dummy `ToolCallbackProvider` bean，因为这会掩盖 MCP 状态并可能误导调用方。
- `/ai/vision-agent/chat` 不能因为缺 MCP 就完全不可用，它还包含本地文件/PDF/终端等工具。
- MCP enabled 后如果 MCP 子进程仍失败，应按 MCP 配置问题处理，不在本次用空 provider 静默吞掉。

## 验证方式

1. 编译：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./mvnw -q -o compile -DskipTests
```

2. local 默认 MCP disabled 启动：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export DASHSCOPE_API_KEY=真实Key
./mvnw resources:resources
./mvnw spring-boot:run
```

预期不再出现：

```text
A component required a bean of type 'org.springframework.ai.tool.ToolCallbackProvider' that could not be found
```

3. 若无法在当前环境使用真实 key 做完整启动，则至少用命令验证失败链路不再包含 `ToolCallbackProvider` 缺失；真实 key 由用户本地验证。

## 预期结果

- 本地默认关闭 MCP 后应用不再因缺 `ToolCallbackProvider` 启动失败。
- MCP 变为真正可选能力。
- 后续用户只需提供有效 DashScope key，即可启动主应用。