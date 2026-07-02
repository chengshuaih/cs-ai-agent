# IDE 点击运行默认启用 MCP 本地配置修复设计

## 需求分类

本次属于本地启动配置修复任务。目标是：在 IDE 中直接点击运行 `CsAiAgentApplication` 时，无需手动 export 环境变量，也能默认启用 MCP 并成功启动应用。

## 当前问题

终端命令启动成功，是因为命令显式传入了环境变量：

```bash
DASHSCOPE_API_KEY
AMAP_MAPS_API_KEY
PEXELS_API_KEY
JAVA_HOME
```

但 IDE 点击运行时实际命令类似：

```text
/usr/bin/env ... java ... cn.chengshuai.csaiagent.CsAiAgentApplication
```

IDE 启动进程没有携带上述环境变量，导致：

1. 主应用 `spring.ai.dashscope.api-key` 为空。
2. 图片搜索 MCP 子进程启动时 DashScope 相关自动配置缺 key：

```text
DashScope API key must be set.
Use the connection property: spring.ai.dashscope.api-key or spring.ai.dashscope.rerank.api-key property.
```

3. 图片搜索 MCP 子进程失败后，主应用等待 MCP 初始化，最终表现为：

```text
mcpSyncClients 初始化 20 秒超时
```

## 当前文件状态

### application-local.yml

文件：

```text
/Users/chengshuai/Work/private/cs-ai-agent/src/main/resources/application-local.yml
```

当前已保存本地 key，但只保存在自定义字段：

```yaml
local-api-keys:
    amap:
        maps-api-key: <本地高德 key>
    pexels:
        api-key: <本地 Pexels key>
```

问题：这些自定义字段不会自动注入到：

- Spring AI DashScope 标准配置
- MCP stdio 子进程环境变量

### mcp-servers.json

文件：

```text
/Users/chengshuai/Work/private/cs-ai-agent/src/main/resources/mcp-servers.json
```

当前高德 MCP 仍读取环境变量占位：

```json
"AMAP_MAPS_API_KEY": "${AMAP_MAPS_API_KEY}"
```

图片搜索 MCP 子进程当前没有显式 env：

```json
"env": {}
```

所以 IDE 不设置环境变量时，MCP 子进程拿不到必要 key。

## 安全边界

用户要求修改 `application-local.yml`。该文件已在仓库 `.gitignore` 中忽略，用于本地开发配置，可保存真实本地 key。

本次仍不把真实 key 写入公开跟踪文件：

- 不写入 `application.yml`
- 不写入 `mcp-servers.json`
- 不写入 Java 源码

## 技术方案

### 1. 在 application-local.yml 中配置 DashScope 标准 key

将当前：

```yaml
spring:
    ai:
        dashscope:
            api-key: ${DASHSCOPE_API_KEY:}
```

改为本地默认值回退：

```yaml
spring:
    ai:
        dashscope:
            api-key: ${DASHSCOPE_API_KEY:<本地 DashScope key>}
```

这样 IDE 点击运行时，即使没有 `DASHSCOPE_API_KEY` 环境变量，主应用也能拿到 DashScope key。

### 2. 在 application-local.yml 中继续保存高德和 Pexels key

保留：

```yaml
local-api-keys:
    amap:
        maps-api-key: <本地高德 key>
    pexels:
        api-key: <本地 Pexels key>
```

### 3. 让 MCP 子进程也能从本地配置拿 key

仅修改 `application-local.yml` 还不能改变 `mcp-servers.json` 的子进程 env。Spring AI MCP stdio server 的 env 来源是 `mcp-servers.json`。

为了实现“点击运行即可运行”，需要同时修改 `mcp-servers.json`，但不写真实 key，而是使用占位符引用 Spring 配置值：

```json
"AMAP_MAPS_API_KEY": "${local-api-keys.amap.maps-api-key}"
```

图片搜索 MCP env 增加：

```json
"PEXELS_API_KEY": "${local-api-keys.pexels.api-key}",
"DASHSCOPE_API_KEY": "${spring.ai.dashscope.api-key}",
"SPRING_AI_DASHSCOPE_API_KEY": "${spring.ai.dashscope.api-key}"
```

如果 Spring AI MCP 的 JSON 占位符能解析 Spring Environment，则 IDE 点击运行即可把本地 key 传给子进程。

### 4. 如果 JSON env 不支持 Spring 配置占位符

之前已确认 JSON `command` 不会执行 shell 变量展开，但 `env` 字段通常由 Spring 读取配置后传给子进程。若验证发现 `${local-api-keys...}` 未被解析，备选方案是：

- 在 `mcp-servers.json` 的图片搜索命令中通过 `/bin/sh -c` 从 `application-local.yml` 读取 key 不合适，复杂且脆弱。
- 更推荐新增一个 `.gitignore` 忽略的本地 `mcp-servers-local.json`，写真实 key，并让 `application-local.yml` 指向它。

但本次先采用最小改动方案：`application-local.yml` 保存 key，`mcp-servers.json` 使用配置占位符，不直接写真实 key。

## 受影响文件

### 修改文件

1. `/Users/chengshuai/Work/private/cs-ai-agent/src/main/resources/application-local.yml`
   - 写入本地 DashScope 默认 key。
   - 保持 MCP 默认启用。
   - 保留高德/Pexels 本地 key。

2. `/Users/chengshuai/Work/private/cs-ai-agent/src/main/resources/mcp-servers.json`
   - 不写真实 key。
   - 将 MCP 子进程 env 改为引用本地配置占位符。
   - 给图片搜索 MCP 子进程补充 `DASHSCOPE_API_KEY`、`SPRING_AI_DASHSCOPE_API_KEY`、`PEXELS_API_KEY`。

## 非目标

本次不做以下内容：

- 不修改前端。
- 不修改业务逻辑。
- 不把真实 key 写入 Git 跟踪文件。
- 不删除 MCP。
- 不修改用户 IDE 配置。

## 验收标准

1. IDE 点击运行时主应用不再因为 DashScope key 缺失导致 MCP 子进程失败。
2. 默认启用 MCP 时不再出现 `mcpSyncClients` 20 秒超时。
3. 高德 MCP 和图片搜索 MCP 均能初始化。
4. `application-local.yml` 仍为本地忽略文件。
5. `mcp-servers.json` 不包含真实 API key。

## 验证计划

1. 修改 `application-local.yml` 和 `mcp-servers.json`。
2. 重新启动应用验证。
3. 如当前 8123 已被旧进程占用，先停止旧进程或使用备用端口。
4. 观察日志中是否出现：

```text
Implementation[name=cs-image-search-mcp-server]
Implementation[name=mcp-server/amap-maps]
Started CsAiAgentApplication
```

5. 如仍失败，记录是否为占位符未解析、端口占用或其他 MCP 子进程错误。