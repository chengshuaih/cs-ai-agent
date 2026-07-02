# MCP 启动 Java 17 与降级修复任务计划

- [x] Task 1: 验证 MCP Java command 的占位解析能力
    - 1.1: 使用最小启动/配置验证 `mcp-servers.json` 的 `command` 是否支持 `${JAVA_HOME}/bin/java` 占位解析
    - 1.2: 记录验证结论，若不支持则选择本地可执行且不提交个人路径的替代方案
    - 1.3: 明确最终配置策略必须避免裸 `java` 解析到 Java 11

- [x] Task 2: 修复 MCP 子进程 Java 17 启动配置
    - 2.1: 修改 `src/main/resources/mcp-servers.json` 中 `yu-image-search-mcp-server.command`
    - 2.2: 保留现有 MCP stdio 参数与 jar 路径，不调整无关 server 配置
    - 2.3: 确保 MCP 子进程使用 Java 17，避免 `UnsupportedClassVersionError`

- [x] Task 3: 增加或确认本地禁用 MCP 的启动路径
    - 3.1: 验证 Spring AI MCP client 在当前版本下的禁用配置是否生效
    - 3.2: 如配置可用，补充本地 profile 配置或文档说明；如不可用，仅记录可验证结论，不写无效配置
    - 3.3: 确保非 MCP 核心接口可在 MCP disabled 路径启动

- [x] Task 4: 更新运行与验收文档
    - 4.1: 更新 `docs/specs/2026-06-18-machine-vision-agent-summary.md`，说明主应用与 MCP 子模块都要求 Java 17
    - 4.2: 更新 `docs/specs/2026-06-18-machine-vision-agent-acceptance.md`，补充 `$JAVA_HOME/bin/java -version` 与 MCP jar 构建步骤
    - 4.3: 将“裸 `java` 依赖 PATH”的风险改为明确 Java 17 前置要求

- [x] Task 5: 执行构建与启动验证
    - 5.1: 使用 Java 17 编译主工程：`./mvnw -q -o compile -DskipTests`
    - 5.2: 使用 Java 17 构建 MCP 子模块 jar
    - 5.3: 验证应用启动不再出现 Java 11/Java 17 class version mismatch
    - 5.4: 验证不再出现 `mcpSyncClients` 20s 初始化超时导致的应用上下文失败
