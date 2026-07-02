# MCP 禁用后 ToolCallbackProvider 可选注入修复总结

## 完成内容

- 修复 `spring.ai.mcp.client.enabled=false` 后启动失败的问题。
- 将 `VisionQaApp` 中的 `ToolCallbackProvider` 从必需注入改为 `ObjectProvider<ToolCallbackProvider>` 可选注入。
- `VisionQaApp#doChatWithMcp` 在 MCP provider 不存在时返回明确提示，不再阻塞应用启动。
- 将 `AiController` 中的 `ToolCallbackProvider` 从必需注入改为可选注入。
- `/api/ai/vision-agent/chat` 在 MCP disabled 时仅使用本地工具 `allTools`；MCP enabled 且 provider 存在时仍合并本地工具与 MCP 工具。
- 更新 `.comate/specs/mcp-startup-java17-fallback/summary.md`，补充 MCP disabled 时的可选 provider 行为。

## 修改文件

- `src/main/java/cn/chengshuai/csaiagent/app/VisionQaApp.java`
- `src/main/java/cn/chengshuai/csaiagent/controller/AiController.java`
- `.comate/specs/optional-mcp-tool-provider/doc.md`
- `.comate/specs/optional-mcp-tool-provider/tasks.md`
- `.comate/specs/optional-mcp-tool-provider/summary.md`
- `.comate/specs/mcp-startup-java17-fallback/summary.md`

## 验证结果

- Java 17 编译通过：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./mvnw -q -o compile -DskipTests
```

- local 默认 MCP disabled 启动链路验证：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./mvnw -q resources:resources
./mvnw -q spring-boot:run
```

验证结果：

- 不再出现 `A component required a bean of type 'org.springframework.ai.tool.ToolCallbackProvider' that could not be found`。
- 不再出现 `UnsupportedClassVersionError`。
- 不再出现 `mcpSyncClients` 初始化超时。

## 当前剩余阻塞点

启动继续失败时，新的根因是 DashScope API 401：

```text
HTTP 401 - {"code":"InvalidApiKey","message":"Invalid API-key provided."}
```

错误发生在 `VisionVectorStoreConfig#visionVectorStore` 初始化 RAG 向量库时，调用 DashScope embedding / chat 能力失败。该问题与 MCP 和 `ToolCallbackProvider` 已无关，需要使用真实有效且具备对应模型权限的 `DASHSCOPE_API_KEY`，或另行把 RAG 初始化改为可选/延迟加载。
