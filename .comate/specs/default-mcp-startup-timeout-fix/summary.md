# 默认启用 MCP 启动超时修复总结

## 根因判断

默认启用 MCP 后，主应用曾在创建 `mcpSyncClients` 时出现 20 秒初始化超时：

```text
java.util.concurrent.TimeoutException: Did not observe any item or terminal signal within 20000ms
```

经系统化排查，根因集中在本地图片搜索 MCP 子进程的 stdio 初始化环境：

1. 高德 MCP 单独启动可进入 stdio 运行态：

```text
Amap Maps MCP Server running on stdio
```

2. 本地图片搜索 MCP 子进程单独启动时，原先会输出：

```text
PatternLayout("") - Empty or null pattern
```

原因是 `mcp-servers.json` 中使用了空的：

```text
-Dlogging.pattern.console=
```

3. 图片搜索 MCP 还会输出 Nacos/Logback appender collision 日志，但修复后它已经能够完成 MCP 初始化握手，不再阻塞主应用启动。

最终判断：

- 直接导致原始超时的关键问题是图片搜索 MCP stdio 子进程日志配置不当，尤其是空 `logging.pattern.console` 触发 Logback 错误并影响初始化时序。
- 修复后虽然仍能看到 Nacos appender collision stderr，但 MCP server 已能在超时前完成初始化响应，因此主应用可启动。

## 配置修改内容

### 1. 图片搜索 MCP stdio 配置

修改文件：

```text
/Users/chengshuai/Work/private/cs-ai-agent/cs-image-search-mcp-server/src/main/resources/application-stdio.yml
```

修改内容：

- 保持 stdio 模式：

```yaml
spring.ai.mcp.server.stdio: true
```

- 保持 non-web 模式和关闭 banner：

```yaml
spring.main.web-application-type: none
spring.main.banner-mode: off
```

- 增加日志关闭配置：

```yaml
logging.level.root: off
logging.level.com.alibaba.nacos: off
logging.level.org.springframework: off
```

### 2. 主应用 MCP server 启动命令

修改文件：

```text
/Users/chengshuai/Work/private/cs-ai-agent/src/main/resources/mcp-servers.json
```

修改内容：

- 移除图片搜索 MCP 启动命令中的空参数：

```text
-Dlogging.pattern.console=
```

- 增加明确的 JVM 日志关闭参数：

```text
-Dspring.main.banner-mode=off
-Dlogging.level.root=OFF
-Dlogging.level.com.alibaba.nacos=OFF
-Dlogging.level.org.springframework=OFF
```

- 保持高德 MCP 的 API key 使用环境变量占位，不写入真实 key：

```json
"AMAP_MAPS_API_KEY": "${AMAP_MAPS_API_KEY}"
```

## jar 重建结果

首次构建图片搜索 MCP jar 失败，原因是 Maven 运行时使用了 Java 11，而 Spring Boot 3.5.4 Maven 插件需要 Java 17：

```text
class file version 61.0, this version of the Java Runtime only recognizes class file versions up to 55.0
```

随后使用 Java 17 重新构建：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./mvnw package -DskipTests
```

执行目录：

```text
/Users/chengshuai/Work/private/cs-ai-agent/cs-image-search-mcp-server
```

构建结果：

```text
BUILD SUCCESS
```

已重新生成：

```text
cs-image-search-mcp-server/target/cs-image-search-mcp-server-0.0.1-SNAPSHOT.jar
```

## MCP 子进程验证结果

### 高德 MCP

单独启动结果：

```text
running_after_8s=true
stderr_head=Amap Maps MCP Server running on stdio
```

说明：高德 MCP 可进入 stdio 运行态。

### 图片搜索 MCP

单独启动结果：

```text
running_after_8s=true
```

修复后结果：

- 不再出现 `PatternLayout("") - Empty or null pattern`。
- 仍有 Nacos/Logback appender collision stderr 输出。
- 后续主应用验证证明它已能完成 MCP 初始化握手。

## 最终主应用启动验证结果

启动命令使用：

- Java 17
- DashScope API Key 环境变量
- 高德 API Key 环境变量
- Pexels API Key 环境变量
- 默认启用 MCP
- 默认端口 8123

验证结果：主应用启动成功。

关键日志：

```text
Server response with Protocol: 2024-11-05 ... Implementation[name=cs-image-search-mcp-server, version=0.0.1]
Server response with Protocol: 2024-11-05 ... Implementation[name=mcp-server/amap-maps, version=0.1.0]
Tomcat started on port 8123 (http) with context path '/api'
Started CsAiAgentApplication in 36.276 seconds
```

结论：

- 两个 MCP server 都完成初始化响应。
- 未再出现 `mcpSyncClients` 20 秒超时。
- 默认启用 MCP 的主应用已能启动成功。

## 仍需注意的问题

1. 图片搜索 MCP 启动时仍会输出 Nacos/Logback appender collision 日志，但当前不再阻塞 MCP 初始化。
2. 如果后续要进一步清理 stderr，需要单独处理图片搜索 MCP 模块的依赖与日志系统，尤其是 Alibaba/Nacos 相关依赖带来的 logback appender 冲突。
3. 默认启用 MCP 仍依赖：
   - Java 17
   - npx/npm 可用
   - 高德 API Key 环境变量
   - Pexels API Key 环境变量
   - DashScope API Key 环境变量
4. 当前主应用进程仍在 8123 运行，占用端口；再次启动前需先停止该进程或改用备用端口。

## 最终结论

本次修复已达成目标：默认启用 MCP 后主应用可以启动，且两个 MCP server 都完成初始化，不再因为 `mcpSyncClients` 20 秒超时导致启动失败。