# 本地默认启用 MCP 与 API Key 配置验证总结

## 本地配置变更

已修改本地配置文件：

```text
/Users/chengshuai/Work/private/cs-ai-agent/src/main/resources/application-local.yml
```

完成内容：

- 已将本地 profile 的 MCP client 默认启用：

```yaml
spring.ai.mcp.client.enabled: true
```

- 已在 `application-local.yml` 中写入本地使用的高德地图与 Pexels API Key。
- `application-local.yml` 已在仓库根目录 `.gitignore` 中忽略，不应提交到 Git。

## 密钥泄露检查结果

已检查公开 MCP 配置：

```text
/Users/chengshuai/Work/private/cs-ai-agent/src/main/resources/mcp-servers.json
```

结果：

- `mcp-servers.json` 仍使用环境变量占位符：

```json
"AMAP_MAPS_API_KEY": "${AMAP_MAPS_API_KEY}"
```

- 未将真实高德地图 API Key 写入 `mcp-servers.json`。
- 图片搜索 MCP 仍通过环境变量读取 Pexels API Key：

```java
System.getenv("PEXELS_API_KEY")
```

- 全仓 Java/YAML/JSON 范围检查结果显示，真实 key 只出现在已忽略的 `application-local.yml` 中。

## 启动验证结果

### 验证前置条件

已确认：

- Java 17 可用。
- 图片搜索 MCP jar 存在：

```text
cs-image-search-mcp-server/target/cs-image-search-mcp-server-0.0.1-SNAPSHOT.jar
```

### 启动命令

由于默认端口 `8123` 已有 Java 进程监听，本次使用备用端口 `8124` 验证启动。

启动时已设置：

- Java 17 `JAVA_HOME`
- `AMAP_MAPS_API_KEY`
- `PEXELS_API_KEY`
- `--server.port=8124`

### 启动结果

应用未成功启动。

日志显示：

```text
Tomcat initialized with port 8124
...
Failed to instantiate [java.util.List]: Factory method 'mcpSyncClients' threw exception
java.util.concurrent.TimeoutException: Did not observe any item or terminal signal within 20000ms
```

最终失败点：

```text
Error creating bean with name 'mcpSyncClients'
```

结论：

- Java 17 与 jar 前置条件满足。
- 端口占用已通过备用端口规避。
- 默认启用 MCP 后，应用启动失败的直接原因是 MCP client 初始化超时。
- 当前无法仅凭这次日志判断是 `amap-maps` 的 `npx` MCP server 超时，还是图片搜索 MCP server 未在 20 秒内完成握手；但从日志中可见 stdio MCP 子进程有输出 logback/nacos 相关 stderr，说明至少某个 MCP 子进程已被拉起，但未及时完成 MCP 初始化协议。

## 默认启动 MCP 的风险

默认启用 MCP 会让应用启动依赖外部/子进程环境，因此存在以下风险：

1. `npx` 下载或启动高德 MCP 包过慢，导致 20 秒初始化超时。
2. MCP 子进程输出日志或初始化行为干扰 stdio 协议握手。
3. 图片搜索 MCP 子进程虽能启动，但 Spring Boot 日志/nacos 日志输出到 stderr，可能影响 MCP client 初始化判断。
4. 任一 MCP server 初始化失败都会导致 Spring AI MCP auto-configuration 创建 `mcpSyncClients` 失败，从而阻塞整个应用启动。

## 建议后续方向

当前任务只要求配置并尝试运行，已完成。

如果继续修复 MCP 默认启动失败，建议下一步单独拆分新任务：

1. 将 `amap-maps` 和 `yu-image-search-mcp-server` 分别单独启用验证，定位是哪一个 MCP server 超时。
2. 优先保证图片搜索 MCP server 的 stdio 输出干净，避免日志污染 MCP 协议。
3. 如需默认稳定启动主应用，可考虑将 MCP 改为可选/按需启用，避免 MCP 子进程失败拖垮整个 Web 应用。
