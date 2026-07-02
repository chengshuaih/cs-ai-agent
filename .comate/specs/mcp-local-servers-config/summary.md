# MCP 本地服务器配置修复总结

## 根因

`mcp-servers.json` 的 `env` 字段中使用的 `${local-api-keys.amap.maps-api-key}` 等 Spring 占位符**不会被 Spring AI MCP client 解析**。MCP client 直接将 JSON 中的字符串原样传给 `ProcessBuilder` 作为子进程环境变量，导致子进程收到的是字面占位符字符串而非真实 key，MCP server 无法正常启动，最终触发 `mcpSyncClients` 20 秒超时。

## 修改内容

### 新建 `src/main/resources/mcp-servers-local.json`

含真实 API key 的本地专用 MCP 配置文件。已加入 `.gitignore`，不会提交到仓库。

### 修改 `.gitignore`

追加了 `mcp-servers-local.json` 忽略规则。

### 修改 `src/main/resources/application-local.yml`

新增覆盖项：

```yaml
spring:
    ai:
        mcp:
            client:
                stdio:
                    servers-configuration: classpath:mcp-servers-local.json
```

本地启动时，MCP client 读取 `mcp-servers-local.json` 而非公共的 `mcp-servers.json`，env 中的值为真实 key。

## 验证结果

不额外 export 任何环境变量，仅设置 `JAVA_HOME` 指向 Java 17，直接启动主应用（端口 8124，因 8123 被旧进程占用）。

关键日志：

```
Server response ... Implementation[name=cs-image-search-mcp-server, version=0.0.1]
Server response ... Implementation[name=mcp-server/amap-maps, version=0.1.0]
Started CsAiAgentApplication in 41.282 seconds
```

两个 MCP server 均正常初始化，`mcpSyncClients` 超时问题已解决。

## 注意事项

- `mcp-servers-local.json` 含真实 API key，已由 `.gitignore` 保护，**禁止提交**。
- `mcp-servers.json` 保留占位符结构，作为配置说明用途，不影响本地运行。
- 图片搜索 MCP 的 Nacos/Logback stderr 碰撞日志仍会出现，但属于日志噪音，不影响 MCP 功能正常运行。
