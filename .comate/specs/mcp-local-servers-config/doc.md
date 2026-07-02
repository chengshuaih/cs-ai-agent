# MCP 本地服务器配置修复方案

## 问题根因

`mcp-servers.json` 的 `env` 字段中写入的 `${local-api-keys.amap.maps-api-key}` 等占位符，
**不会被 Spring AI MCP client 解析**。

Spring AI MCP client 读取 JSON 后直接用 `ProcessBuilder` 启动子进程，env 字段的值是原始字符串传入，
子进程收到的环境变量值就是字面字符串 `${local-api-keys.amap.maps-api-key}`，导致高德 MCP 拿不到真实 key，
图片搜索 MCP 拿不到 DashScope / Pexels key，子进程启动异常或工具调用失败，进而触发 `mcpSyncClients` 20 秒超时。

## 解决方案

### 方案说明

新增本地专用 MCP 配置文件 `src/main/resources/mcp-servers-local.json`：
- 包含真实 API key 的明文值
- 加入 `.gitignore`，不提交到仓库

在 `application-local.yml` 中覆盖 `servers-configuration`，指向 `mcp-servers-local.json`：
```yaml
spring:
    ai:
        mcp:
            client:
                stdio:
                    servers-configuration: classpath:mcp-servers-local.json
```

公开的 `mcp-servers.json` 保持现状（含占位符注释），用于说明配置结构，实际本地运行由 `mcp-servers-local.json` 接管。

## 受影响文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `src/main/resources/mcp-servers-local.json` | 新建 | 含真实 key，加入 .gitignore |
| `.gitignore` | 修改 | 追加 `mcp-servers-local.json` |
| `src/main/resources/application-local.yml` | 修改 | 覆盖 servers-configuration 路径 |

## mcp-servers-local.json 内容结构

```json
{
  "mcpServers": {
    "amap-maps": {
      "command": "npx",
      "args": ["-y", "@amap/amap-maps-mcp-server"],
      "env": {
        "AMAP_MAPS_API_KEY": "<真实高德key>"
      }
    },
    "yu-image-search-mcp-server": {
      "command": "/bin/sh",
      "args": [
        "-c",
        "if [ -z \"$JAVA_HOME\" ]; then echo 'JAVA_HOME must point to Java 17' >&2; exit 1; fi; exec \"$JAVA_HOME/bin/java\" -Dspring.ai.mcp.server.stdio=true -Dspring.main.web-application-type=none -Dspring.main.banner-mode=off -Dlogging.level.root=OFF -Dlogging.level.com.alibaba.nacos=OFF -Dlogging.level.org.springframework=OFF -jar cs-image-search-mcp-server/target/cs-image-search-mcp-server-0.0.1-SNAPSHOT.jar"
      ],
      "env": {
        "PEXELS_API_KEY": "<真实Pexels key>",
        "DASHSCOPE_API_KEY": "<真实DashScope key>",
        "SPRING_AI_DASHSCOPE_API_KEY": "<真实DashScope key>"
      }
    }
  }
}
```

## 预期结果

IDE 直接点击运行，主应用读取 `application-local.yml`，MCP client 加载 `mcp-servers-local.json`，子进程收到真实环境变量，MCP 初始化成功，不再出现 `mcpSyncClients` 20 秒超时。
