# 本地默认启用 MCP 与 API Key 配置设计

## 需求分类

本次属于本地运行配置改造与启动验证任务，涉及：

- 将高德地图 API Key 与 Pexels API Key 写入本地配置。
- 将本地 profile 默认从关闭 MCP 改为启用 MCP。
- 尝试启动应用并验证 MCP 默认启动是否存在问题。

## 安全原则

用户提供了真实 API Key。本次不能把真实密钥写入会被 Git 跟踪和提交的文件。

当前仓库 `.gitignore` 已忽略：

```text
application-local.yml
```

因此本次只允许把真实 key 写入：

```text
/Users/chengshuai/Work/private/cs-ai-agent/src/main/resources/application-local.yml
```

不允许把真实 key 写入：

```text
/Users/chengshuai/Work/private/cs-ai-agent/src/main/resources/mcp-servers.json
```

原因：`mcp-servers.json` 是正常资源文件，当前不在 `.gitignore` 中，写入真实 key 会造成密钥泄露风险。

## 当前配置现状

### application-local.yml

当前本地 profile 默认关闭 MCP：

```yaml
spring:
    ai:
        dashscope:
            api-key: ${DASHSCOPE_API_KEY:}
        mcp:
            client:
                enabled: false
```

### mcp-servers.json

当前 MCP server 配置：

```json
"amap-maps": {
  "command": "npx",
  "args": ["-y", "@amap/amap-maps-mcp-server"],
  "env": {
    "AMAP_MAPS_API_KEY": "${AMAP_MAPS_API_KEY}"
  }
}
```

```json
"yu-image-search-mcp-server": {
  "command": "/bin/sh",
  "args": [
    "-c",
    "if [ -z \"$JAVA_HOME\" ]; then ...; fi; exec \"$JAVA_HOME/bin/java\" ... -jar cs-image-search-mcp-server/target/cs-image-search-mcp-server-0.0.1-SNAPSHOT.jar"
  ],
  "env": {}
}
```

Pexels key 当前由图片搜索 MCP 子模块 Java 代码读取环境变量：

```java
System.getenv("PEXELS_API_KEY")
```

## 技术方案

### 1. 本地配置写入真实 key

修改文件：

```text
/Users/chengshuai/Work/private/cs-ai-agent/src/main/resources/application-local.yml
```

计划写入：

```yaml
spring:
    ai:
        dashscope:
            api-key: ${DASHSCOPE_API_KEY:}
        mcp:
            client:
                enabled: true

# 仅本地使用；application-local.yml 已在 .gitignore 中忽略，禁止提交真实密钥
mcp:
    amap:
        maps-api-key: <用户提供的高德地图 API Key>
    pexels:
        api-key: <用户提供的 Pexels API Key>
```

但注意：`mcp-servers.json` 里的 stdio server env 不会自动读取上面自定义的 `mcp.amap.maps-api-key` 和 `mcp.pexels.api-key`。Spring AI MCP stdio 配置从 JSON 里给子进程传 env。

因此仅写 YAML 自定义字段不够，需要让 MCP 子进程能拿到这些 key。

### 2. 让 MCP 子进程读取本地配置

为了避免把真实 key 写进 `mcp-servers.json`，采用启动命令设置环境变量的方式进行验证：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export AMAP_MAPS_API_KEY=<本地配置中的高德地图 API Key>
export PEXELS_API_KEY=<本地配置中的 Pexels API Key>
./mvnw spring-boot:run
```

如果用户坚持“直接运行时不再 export key”，更稳妥的后续方案是增加一个本地启动脚本，但脚本必须被 `.gitignore` 忽略，或者使用 shell profile。本次不新增脚本，先保证安全和可验证。

### 3. 默认启用 MCP

修改：

```yaml
spring.ai.mcp.client.enabled: true
```

这会让本地 `spring-boot:run` 默认启动 MCP client，并尝试拉起：

- 高德地图 MCP：依赖 `npx`、网络、npm 包可用、高德 key。
- 图片搜索 MCP：依赖 Java 17、MCP jar、Pexels key。

## 默认启动 MCP 可能遇到的问题

默认启用 MCP 会增加启动失败概率，主要风险：

1. `npx` 不可用或网络无法访问 npm
   - 高德 MCP 子进程可能无法启动。

2. 高德 MCP 包下载慢
   - Spring AI MCP 初始化可能 20 秒超时。

3. `JAVA_HOME` 不是 Java 17
   - 图片搜索 MCP jar 可能无法运行。

4. `AMAP_MAPS_API_KEY` 或 `PEXELS_API_KEY` 未进入子进程环境
   - 对应工具不可用或返回错误。

5. 端口 8123 被占用
   - Web 服务启动失败，但这不是 MCP 问题。

因此，“默认启动 MCP”可以做，但它比默认关闭 MCP 更容易受本机网络、npm、Java 环境影响。

## 运行验证计划

1. 确认图片搜索 MCP jar 存在：

```text
cs-image-search-mcp-server/target/cs-image-search-mcp-server-0.0.1-SNAPSHOT.jar
```

2. 修改 `application-local.yml`：
   - 默认启用 MCP。
   - 本地保存用户提供的高德和 Pexels key，文件不提交。

3. 使用 Java 17 启动：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export AMAP_MAPS_API_KEY=<用户提供的高德地图 API Key>
export PEXELS_API_KEY=<用户提供的 Pexels API Key>
./mvnw spring-boot:run
```

4. 如遇端口占用，则使用：

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=8124"
```

5. 观察日志：
   - MCP client 是否成功创建。
   - 是否出现 `mcpSyncClients TimeoutException`。
   - 是否出现 npx / npm 下载失败。
   - 是否出现 Java 版本错误。

## 受影响文件

### 修改文件

1. `/Users/chengshuai/Work/private/cs-ai-agent/src/main/resources/application-local.yml`
   - 默认启用 MCP。
   - 写入本地 API key 配置。

### 不修改文件

1. `/Users/chengshuai/Work/private/cs-ai-agent/src/main/resources/mcp-servers.json`
   - 不写入真实 key。
   - 保持 `${AMAP_MAPS_API_KEY}` 占位方式。

2. `cs-image-search-mcp-server` Java 代码
   - 继续通过 `PEXELS_API_KEY` 环境变量读取 key。

## 非目标

本次不做以下内容：

- 不把真实 API key 写入 Git 跟踪文件。
- 不提交真实密钥。
- 不重构 MCP 配置加载机制。
- 不新增后端接口。
- 不修改前端 UI。

## 验收标准

1. `application-local.yml` 中 `spring.ai.mcp.client.enabled` 改为 `true`。
2. 真实 API key 只写入本地忽略文件，不写入 `mcp-servers.json`。
3. 执行启动命令，获得明确结果：
   - 启动成功；或
   - 因 npx/npm/network/MCP timeout/端口占用等原因失败，并记录原因。
4. 不在回复中回显完整真实 API key。

## 结论

默认启动 MCP 可以做，但默认运行确实可能有问题。最常见问题不是代码编译，而是：

- `npx` 下载高德 MCP 慢或失败。
- MCP 初始化超时。
- Java 17 环境未正确传给子进程。
- 端口占用。

本次会按安全方式配置并实际尝试运行。