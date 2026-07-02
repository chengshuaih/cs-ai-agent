# MCP 启动 Java 17 与降级修复规格

## 问题分类

这是一个启动期 bug 修复，影响后端应用启动、MCP 工具初始化与本地开发运行方式。需要修改配置与文档，属于多文件修复，按 SDD 流程执行。

## 现象与错误链路

用户终端日志显示应用不是 Maven 编译阶段失败，而是在 Spring Boot 启动时创建 MCP 工具回调失败：

```text
Error creating bean with name 'mcpSyncClients'
Factory method 'mcpSyncClients' threw exception
java.util.concurrent.TimeoutException: Did not observe any item or terminal signal within 20000ms
```

同时 MCP stdio 子进程 stderr 有关键错误：

```text
java.lang.UnsupportedClassVersionError: org/springframework/boot/loader/launch/JarLauncher
has been compiled by a more recent version of the Java Runtime (class file version 61.0),
this version of the Java Runtime only recognizes class file versions up to 55.0
```

含义：

- class file version 61 = Java 17。
- class file version 55 = Java 11。
- `cs-image-search-mcp-server` 是 Spring Boot 3.x / Java 17 编译产物。
- 主应用虽然用 Java 17 启动，但 `mcp-servers.json` 中 MCP 子进程命令是裸 `java`，它依赖当前进程 PATH；在 IDE/终端环境里该 `java` 实际解析为 Java 11。
- MCP 子进程立刻启动失败，Spring AI MCP client 等不到初始化响应，20s 后抛 TimeoutException，最终导致 `dashscopeChatModel`、`toolCallingManager`、`csManus` 依赖链创建失败，应用启动失败。

日志中的 `Broken pipe` 是后续 SSE 客户端断开/应用失败后的伴随错误，不是根因。

## 根因假设

根因是 MCP 子进程 Java 运行时与项目编译目标不一致：

1. 主项目和 MCP 子模块都要求 Java 17。
2. `src/main/resources/mcp-servers.json` 使用 `"command": "java"`。
3. Spring AI MCP stdio client 启动子进程时没有自动继承用户 shell 中的 `JAVA_HOME` 语义，也不保证 PATH 中第一个 `java` 是 Java 17。
4. 子进程用 Java 11 执行 Java 17 jar，报 `UnsupportedClassVersionError`。
5. MCP client 初始化超时，Spring 应用上下文启动失败。

## 修复目标

- 后端应用在本地 IDE/终端启动时，不因 MCP 子进程使用 Java 11 而失败。
- MCP 子进程应明确使用 Java 17，不再依赖不确定的 PATH `java`。
- MCP 相关配置仍避免硬编码个人机器私有路径作为唯一方案。
- 本地开发可在 MCP 不可用时明确关闭 MCP client，避免非核心能力阻塞应用启动。
- 文档中明确：主应用和 MCP 子模块均需 Java 17；若 IDE 启动，需配置 `JAVA_HOME` 或禁用 MCP。

## 技术方案

### 方案 A：mcp-servers.json 使用 JAVA_HOME 变量拼接（首选）

将 MCP server command 从裸 `java` 改为环境变量占位：

```json
"command": "${JAVA_HOME}/bin/java"
```

优点：

- 不绑定个人机器路径。
- 明确要求 Java 17。
- 与文档中 `export JAVA_HOME=$(/usr/libexec/java_home -v 17)` 保持一致。

风险与边界：

- 需要验证 Spring AI MCP 的 servers-configuration 是否会对 `command` 字段做 `${JAVA_HOME}` 占位解析。
- 如果不解析，子进程会找不到 `${JAVA_HOME}/bin/java`，需要退回方案 B。

### 方案 B：配置本地可覆盖的绝对 Java 命令（备选）

如果 Spring AI 不解析 command 中的环境变量，则使用可配置配置文件策略：

- 仓库提交版保留安全默认或禁用 MCP。
- 本地 `application-local.yml` / 本地 mcp servers 文件使用本机 Java 17 绝对路径，例如：

```json
"command": "/Users/chengshuai/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home/bin/java"
```

但这会重新引入个人路径，不适合作为通用提交默认值；因此仅作为本地覆盖方案，不作为首选提交修复。

### 方案 C：增加本地禁用 MCP 的开关说明（配套）

对无需 MCP 的普通启动/调试，允许通过配置禁用 MCP client，避免外部 MCP 失败阻塞主应用：

```yaml
spring:
  ai:
    mcp:
      client:
        enabled: false
```

需要先验证 Spring AI 1.0.0 MCP client 的实际开关名称是否生效。若 `enabled: false` 不生效，则不写入代码，只在文档中给出验证后的可用方式。

## 影响文件

### `/Users/chengshuai/Work/private/cs-ai-agent/src/main/resources/mcp-servers.json`

修改类型：配置修复。

受影响字段：

- `mcpServers.yu-image-search-mcp-server.command`

预期变更：

- 从 `java` 改为明确 Java 17 的启动方式。
- 保留 `-Dspring.ai.mcp.server.stdio=true`、`-Dspring.main.web-application-type=none`、jar 路径不变。

### `/Users/chengshuai/Work/private/cs-ai-agent/src/main/resources/application.yml`

修改类型：可选配置增强。

可能新增/调整：

- MCP client 开关或本地禁用说明对应配置（需验证后再写）。

### `/Users/chengshuai/Work/private/cs-ai-agent/src/main/resources/application-local.yml`

修改类型：本地开发配置增强。

可能新增：

- 对本地开发 MCP 开关或 Java 17 要求的配置。

注意：不能写入真实密钥或个人敏感路径。

### `/Users/chengshuai/Work/private/cs-ai-agent/docs/specs/2026-06-18-machine-vision-agent-summary.md`

修改类型：运行说明补充。

需要补充：

- 主应用与 MCP 子模块都需要 Java 17。
- IDE 运行时也要确保 MCP 子进程使用 Java 17。
- 若仅调试非 MCP 能力，可禁用 MCP client（以验证后的配置为准）。

### `/Users/chengshuai/Work/private/cs-ai-agent/docs/specs/2026-06-18-machine-vision-agent-acceptance.md`

修改类型：验收命令补充。

需要补充：

- 启动前验证 `JAVA_HOME/bin/java -version` 为 17。
- 验证 MCP 子模块 jar 已构建。
- 启动 smoke 命令覆盖 MCP disabled / enabled 两种最小路径（根据实际可用配置确定）。

## 数据流路径

```text
Spring Boot 启动
  -> Spring AI MCP Client AutoConfiguration
  -> 读取 classpath:mcp-servers.json
  -> StdioClientTransport 启动 yu-image-search-mcp-server 子进程
  -> command 指向 Java runtime
  -> Java runtime 执行 cs-image-search-mcp-server-0.0.1-SNAPSHOT.jar
  -> MCP server 初始化并通过 stdio 返回能力列表
  -> mcpSyncClients 初始化完成
  -> mcpToolCallbacks 注入 ToolCallbackResolver
  -> dashscopeChatModel / ToolCallingManager / CsManus 创建成功
  -> 应用启动成功
```

当前断点在：

```text
command=java -> 解析为 Java 11 -> 执行 Java 17 jar 失败 -> MCP 初始化超时
```

## 边界条件与异常处理

- 未设置 `JAVA_HOME`：应在启动前失败得更明确，或文档要求先设置 Java 17；不能静默退回 Java 11。
- MCP jar 未构建：Spring AI 仍可能启动子进程失败；文档需要求先构建 `cs-image-search-mcp-server`。
- `AMAP_MAPS_API_KEY` / `PEXELS_API_KEY` 未配置：不应阻塞应用启动；对应工具可在调用时返回缺配置提示。
- 用户只想启动问答/业务接口：应提供禁用 MCP 的本地启动方式，避免 MCP 外部依赖阻断非 MCP 功能。
- IDE 启动：IDE Run Configuration 的 JDK 不等同于子进程 PATH，需要明确配置 `JAVA_HOME` 或使用可解析的 Java 17 command。

## 验证方式

1. 编译主工程：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./mvnw -q -o compile -DskipTests
```

2. 编译 MCP 子模块：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
cd cs-image-search-mcp-server
../mvnw -q -o package -DskipTests
```

3. 验证 Java 版本：

```bash
$JAVA_HOME/bin/java -version
```

输出应包含 Java 17。

4. 启动应用，观察不再出现：

```text
UnsupportedClassVersionError ... class file version 61.0 ... recognizes up to 55.0
TimeoutException ... mcpSyncClients
```

5. 若 MCP enabled 启动依赖外部工具不可控，则至少验证 MCP disabled 启动路径，确保业务接口可启动。

## 预期结果

- 应用启动不再因为 MCP 子进程 Java 版本不匹配失败。
- Java 17 要求在配置和文档中一致。
- 用户看到“build 不起来”时，有明确可执行的启动前置步骤：设置 Java 17、构建 MCP jar、必要时禁用 MCP。
