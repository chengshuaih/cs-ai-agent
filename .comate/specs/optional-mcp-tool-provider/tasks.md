# MCP 禁用后 ToolCallbackProvider 可选注入修复任务计划

- [x] Task 1: 调整 VisionQaApp 的 MCP provider 注入
    - 1.1: 将 `ToolCallbackProvider` 必需注入改为可选注入，避免 `spring.ai.mcp.client.enabled=false` 时 Bean 创建失败
    - 1.2: 在 `doChatWithMcp` 中处理 provider 不存在的场景，返回明确的 MCP 未启用提示
    - 1.3: 保持 provider 存在时的原有 MCP 工具调用逻辑不变
    - 1.4: 不创建 dummy `ToolCallbackProvider`，避免掩盖 MCP 实际启用状态

- [x] Task 2: 调整 AiController 的 MCP provider 注入
    - 2.1: 将 `ToolCallbackProvider` 必需注入改为可选注入，解除 Controller 对 MCP client 的强依赖
    - 2.2: 在 `/ai/vision-agent/chat` 中缺少 provider 时仅使用 `allTools` 本地工具
    - 2.3: 保持 provider 存在时“本地工具 + MCP 工具”的合并逻辑不变
    - 2.4: 确保本地默认 MCP disabled 时 `/ai/vision-agent/chat` 不因 provider 缺失而阻塞应用启动

- [x] Task 3: 更新修复总结文档
    - 3.1: 补充本地默认 MCP disabled 后 provider 为可选依赖
    - 3.2: 说明 MCP disabled 时 `/ai/vision-agent/chat` 使用本地工具运行
    - 3.3: 说明 MCP 专属能力需显式开启 MCP client
    - 3.4: 记录当前对话中的边界：MCP Java 11 / `mcpSyncClients` 已不是最新阻塞点，新的阻塞点是 `ToolCallbackProvider` 必需注入
    - 3.5: 记录 DashScope key 仍是独立启动前置；若 key 无效会在 RAG/Embedding 初始化阶段失败，不属于本任务修复范围

- [x] Task 4: 执行编译与启动链路验证
    - 4.1: 使用 Java 17 编译主工程：`export JAVA_HOME=$(/usr/libexec/java_home -v 17) && ./mvnw -q -o compile -DskipTests`
    - 4.2: 运行 `./mvnw resources:resources`，确保修改后的 local 配置进入 `target/classes`
    - 4.3: 验证 local 默认 MCP disabled 时不再出现 `A component required a bean of type 'org.springframework.ai.tool.ToolCallbackProvider' that could not be found`
    - 4.4: 如启动仍失败，确认失败原因不是 MCP provider 缺失，并按实际错误归因，例如 DashScope key、Embedding 初始化或其他 Bean 创建问题
