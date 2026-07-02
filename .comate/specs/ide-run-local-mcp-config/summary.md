# IDE 点击运行默认启用 MCP 本地配置修复总结

## 本地配置修改内容

已修改本地配置文件：

```text
/Users/chengshuai/Work/private/cs-ai-agent/src/main/resources/application-local.yml
```

修改内容：

- `spring.ai.dashscope.api-key` 已增加本地默认 key 回退值。
- `spring.ai.mcp.client.enabled` 保持为 `true`。
- 保留本地高德地图 API Key：

```yaml
local-api-keys.amap.maps-api-key
```

- 保留本地 Pexels API Key：

```yaml
local-api-keys.pexels.api-key
```

说明：`application-local.yml` 已在仓库根目录 `.gitignore` 中忽略，用作本地开发配置，不应提交真实密钥。

## MCP 子进程 env 修改内容

已修改公开 MCP 配置文件：

```text
/Users/chengshuai/Work/private/cs-ai-agent/src/main/resources/mcp-servers.json
```

修改内容：

- 高德 MCP env 从环境变量占位改为引用本地配置占位：

```json
"AMAP_MAPS_API_KEY": "${local-api-keys.amap.maps-api-key}"
```

- 图片搜索 MCP env 增加：

```json
"PEXELS_API_KEY": "${local-api-keys.pexels.api-key}"
```

```json
"DASHSCOPE_API_KEY": "${spring.ai.dashscope.api-key}"
```

```json
"SPRING_AI_DASHSCOPE_API_KEY": "${spring.ai.dashscope.api-key}"
```

## 密钥未写入公开配置的检查结果

已检查 `mcp-servers.json`，未发现真实 API key 明文：

- 未发现真实高德地图 key。
- 未发现真实 Pexels key。
- 未发现真实 DashScope key。

真实 key 仍只保存在本地忽略文件 `application-local.yml` 中。

## 启动验证结果

验证方式：

- 不额外 export API key。
- 仅设置 Java 17。
- 由于 `8123` 已有旧进程监听，使用备用端口 `8124`。
- 执行主应用启动验证。

验证日志文件：

```text
/Users/chengshuai/.comate-engine/store/terminals/un5xka.output
```

结果：启动失败。

关键现象：

- `8124` 当前没有监听进程，说明启动进程已退出。
- 日志中未再出现明确的：

```text
DashScope API key must be set
```

- 但仍出现：

```text
mcpSyncClients
java.util.concurrent.TimeoutException: Did not observe any item or terminal signal within 20000ms
```

同时图片搜索 MCP 子进程仍持续输出 Nacos/Logback appender collision 相关 stderr：

```text
RollingFileAppender[CONFIG_LOG_FILE] - Collisions detected
RollingFileAppender[NAMING_LOG_FILE] - Collisions detected
RollingFileAppender[REMOTE_LOG_FILE] - Collisions detected
```

## 当前结论

本次修改解决了 IDE/默认启动时主应用本地 DashScope key 的缺失问题，但没有完全解决 MCP 初始化超时。

更准确地说：

1. `application-local.yml` 中的 `spring.ai.dashscope.api-key` 已能被主应用读取。
2. 但 `mcp-servers.json` 中 env 字段的 `${local-api-keys...}` 与 `${spring.ai.dashscope.api-key}` 是否被 Spring AI MCP 正确解析，仍未从日志中得到成功证据。
3. 图片搜索 MCP 子进程仍有大量 Nacos/Logback stderr 输出，且最终主应用仍等待 MCP 初始化超时。

## 下一步建议

如果继续修复“IDE 点击运行即可启动 MCP”，建议下一步单独做更明确的根因验证与修复：

1. 验证 `mcp-servers.json` 的 env 占位符是否支持引用 `application-local.yml` 中的自定义配置。
2. 如果不支持，改用 `.gitignore` 忽略的本地 MCP 配置文件，例如：

```text
src/main/resources/mcp-servers-local.json
```

在该文件中写真实本地 env，并让 `application-local.yml` 指向它。

3. 或者修改图片搜索 MCP server，使其完全不加载 DashScope/Rerank/Nacos 相关自动配置，只保留图片搜索工具本身，减少 stdio MCP 子进程启动依赖。

4. 进一步清理图片搜索 MCP 模块依赖，避免 Nacos/Logback appender collision 持续污染 stderr。

## 本轮任务状态

- Task 1 已完成。
- Task 2 已完成。
- Task 3 已完成，但验证结果为失败。
- Task 4 已完成，本文件记录了失败原因和下一步建议。