# MCP 启动 Java 17 与降级修复总结

## 完成内容

- 定位启动失败根因：不是普通编译失败，而是 Spring AI MCP client 在启动期初始化 stdio 子进程失败。
- 关键错误链路：`mcpSyncClients` 初始化等待 20s 超时；MCP 子进程 stderr 显示 `UnsupportedClassVersionError`，Java 17 编译产物被 Java 11 运行。
- 验证结论：`mcp-servers.json` 的 `command` 字段不支持 `${JAVA_HOME}` 占位解析，不能直接写 `${JAVA_HOME}/bin/java`。
- 修复 `src/main/resources/mcp-servers.json`：图片搜索 MCP 不再使用裸 `java`，改为 `/bin/sh -c` 中显式执行 `"$JAVA_HOME/bin/java"`，并在 `JAVA_HOME` 未设置时输出明确错误。
- 验证 `--spring.ai.mcp.client.enabled=false` 可作为非 MCP 本地启动路径：禁用后错误不再走 `mcpSyncClients`，说明 MCP 初始化已被绕开。
- 更新运行/验收文档，明确：主应用与 MCP 子模块都要求 Java 17；启用 MCP 前需要构建 MCP 子模块 jar；只调试非 MCP 能力可临时禁用 MCP client。

## 修改文件

- `src/main/resources/mcp-servers.json`
- `docs/specs/2026-06-18-machine-vision-agent-summary.md`
- `docs/specs/2026-06-18-machine-vision-agent-acceptance.md`
- `.comate/specs/mcp-startup-java17-fallback/doc.md`
- `.comate/specs/mcp-startup-java17-fallback/tasks.md`

- 本地默认 MCP disabled 时，`ToolCallbackProvider` 不再作为必需依赖：`VisionQaApp` 和 `AiController` 已改为可选注入。
- MCP disabled 时 `/api/ai/vision-agent/chat` 仅使用本地工具运行；MCP enabled 且 provider 存在时仍合并本地工具与 MCP 工具。
- MCP 专属对话方法在 provider 不存在时返回明确的“未启用 MCP”提示。

## 验证结果

- 主工程编译通过：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./mvnw -q -o compile -DskipTests
```

- MCP 子模块打包通过：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
cd cs-image-search-mcp-server
../mvnw -q -o package -DskipTests
```

- MCP jar 已生成：

```text
cs-image-search-mcp-server/target/cs-image-search-mcp-server-0.0.1-SNAPSHOT.jar
```

- MCP 子进程直接启动验证通过：进程在 5s 后仍运行，未出现 `UnsupportedClassVersionError`。
- MCP disabled 启动路径验证：未再出现 `mcpSyncClients` / MCP 20s 初始化超时；后续失败为 dummy DashScope key 触发的 `InvalidApiKey`，不属于本次 MCP Java 版本问题。
- 复核发现并修正 summary 中“MCP 启动命令改用 java（依赖 PATH）”的旧文档残留。

## 后续注意

- 正常启动仍需要真实 `DASHSCOPE_API_KEY`；使用 dummy key 会在 RAG 向量库初始化阶段触发 DashScope 401。
- 若只调试非 MCP 能力，可使用：

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments='--spring.ai.mcp.client.enabled=false'
```

- 启用 MCP 前必须保证：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
$JAVA_HOME/bin/java -version
(cd cs-image-search-mcp-server && ../mvnw -q package -DskipTests)
```
