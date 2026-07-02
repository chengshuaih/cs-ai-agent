# 默认启用 MCP 启动超时修复设计

## 问题分类

本次属于启动失败 bug 修复，目标是在默认启用 MCP 的情况下让主应用可启动，并尽量保留高德地图 MCP 与本地图片搜索 MCP 能力。

## 现象

用户在默认启用 MCP 后启动主应用，出现：

```text
Error creating bean with name 'mcpSyncClients'
java.util.concurrent.TimeoutException: Did not observe any item or terminal signal within 20000ms
```

主应用启动失败，失败链路为：

```text
mcpSyncClients -> mcpToolCallbacks -> toolCallbackResolver -> toolCallingManager -> dashscopeChatModel -> csManus
```

## 已收集证据

### 1. 主应用 MCP 配置

文件：

```text
/Users/chengshuai/Work/private/cs-ai-agent/src/main/resources/mcp-servers.json
```

当前配置了两个 stdio MCP server：

- `amap-maps`：通过 `npx -y @amap/amap-maps-mcp-server` 启动
- `yu-image-search-mcp-server`：通过 Java jar 启动本地图片搜索 MCP server

### 2. 子进程单独探测结果

已分别启动两个 MCP 子进程观察 8 秒：

#### 高德 MCP

命令：

```text
npx -y @amap/amap-maps-mcp-server
```

结果：

```text
stderr: Amap Maps MCP Server running on stdio
running_after_8s=true
```

说明：高德 MCP 能进入 stdio 运行态，不是当前最可疑对象。

#### 本地图片搜索 MCP

命令：

```text
$JAVA_HOME/bin/java -Dspring.ai.mcp.server.stdio=true -Dspring.main.web-application-type=none -Dlogging.pattern.console= -jar cs-image-search-mcp-server/target/cs-image-search-mcp-server-0.0.1-SNAPSHOT.jar
```

结果：

```text
stderr: logback PatternLayout Empty or null pattern
stderr: Nacos CONFIG_LOG_FILE/NAMING_LOG_FILE/REMOTE_LOG_FILE appender collisions
running_after_8s=true
```

说明：本地图片搜索 MCP 子进程启动后持续向 stderr 输出 Spring/Logback/Nacos 日志，且没有在 20 秒内完成 MCP 初始化握手。

## 根因判断

当前根因不是 Java 版本、jar 缺失或端口占用：

- Java 17 可用。
- MCP jar 存在。
- 端口占用可通过备用端口规避。

当前最可能根因是：

1. 本地图片搜索 MCP 子模块使用 `spring-ai-mcp-server-webmvc-spring-boot-starter`，但 stdio 模式下仍带入较重的 Spring Boot / Alibaba / Nacos 日志初始化。
2. `mcp-servers.json` 中通过 `-Dlogging.pattern.console=` 试图清空控制台日志，但该空 pattern 会触发 logback 错误：

```text
PatternLayout("") - Empty or null pattern
```

3. stdio MCP 对标准输入输出/错误输出很敏感，子进程在初始化期大量输出 logback/nacos stderr 信息，导致主应用的 Spring AI MCP client 等待初始化响应超时。

## 修复方案

采用最小可验证修复：先让本地图片搜索 MCP 子进程的 stdio 启动环境安静、稳定。

### 方案 A：修正本地图片搜索 MCP 的 stdio 日志配置

修改文件：

1. `/Users/chengshuai/Work/private/cs-ai-agent/cs-image-search-mcp-server/src/main/resources/application-stdio.yml`
2. `/Users/chengshuai/Work/private/cs-ai-agent/src/main/resources/mcp-servers.json`

修改点：

- 在图片搜索 MCP 的 `application-stdio.yml` 中显式关闭 banner、降低日志级别，并禁用 nacos 日志相关输出。
- 移除 `mcp-servers.json` 中的 `-Dlogging.pattern.console=` 空 pattern，避免 logback 报 `Empty or null pattern`。
- 改为通过 JVM 参数关闭 console 日志或将日志级别设置到 `OFF`。

预期配置：

```yaml
spring:
  main:
    web-application-type: none
    banner-mode: off
logging:
  level:
    root: off
    com.alibaba.nacos: off
```

`mcp-servers.json` 中图片搜索 MCP 命令改为：

```text
exec "$JAVA_HOME/bin/java" \
  -Dspring.ai.mcp.server.stdio=true \
  -Dspring.main.web-application-type=none \
  -Dspring.main.banner-mode=off \
  -Dlogging.level.root=OFF \
  -Dlogging.level.com.alibaba.nacos=OFF \
  -jar cs-image-search-mcp-server/target/cs-image-search-mcp-server-0.0.1-SNAPSHOT.jar
```

### 方案 B：如果方案 A 不足，临时只启用高德 MCP

如果清理日志后图片搜索 MCP 仍无法握手，则创建一个仅包含高德 MCP 的本地配置文件作为验证：

```text
src/main/resources/mcp-servers-amap-only.json
```

并在本地 profile 指向该配置。

但这是降级方案，因为会暂时失去图片搜索 MCP 能力。本次优先执行方案 A。

## 受影响文件

### 修改文件

1. `/Users/chengshuai/Work/private/cs-ai-agent/cs-image-search-mcp-server/src/main/resources/application-stdio.yml`
   - 增加 stdio 模式日志关闭配置。

2. `/Users/chengshuai/Work/private/cs-ai-agent/src/main/resources/mcp-servers.json`
   - 移除空的 `-Dlogging.pattern.console=`。
   - 增加更明确的日志关闭 JVM 参数。

### 可能执行但不修改的验证项

- 重新构建图片搜索 MCP jar。
- 重新启动主应用，验证默认启用 MCP 是否启动成功。

## 非目标

本次不做以下内容：

- 不修改用户提供的 API key。
- 不把真实 key 写入 `mcp-servers.json`。
- 不重构 MCP server 实现。
- 不删除高德 MCP。
- 不修改前端 UI。

## 验收标准

1. 图片搜索 MCP 子进程单独启动时，不再出现 `PatternLayout("") - Empty or null pattern`。
2. 主应用默认启用 MCP 时，不再因为 `mcpSyncClients` 20 秒初始化超时而失败。
3. 如果仍失败，必须明确记录是哪个 MCP server 仍未完成握手。
4. 真实 API key 不写入 Git 跟踪文件。

## 验证计划

1. 修改配置。
2. 重新构建图片搜索 MCP jar：

```bash
./mvnw package -DskipTests
```

执行目录：

```text
cs-image-search-mcp-server
```

3. 单独探测图片搜索 MCP 子进程 stderr。
4. 使用默认 MCP 启动主应用。
5. 如 `8123` 占用，使用 `8124`。
6. 记录启动结果。
