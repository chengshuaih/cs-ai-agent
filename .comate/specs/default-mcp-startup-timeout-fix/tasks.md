# 默认启用 MCP 启动超时修复任务计划

- [✓] Task 1: 修正图片搜索 MCP stdio 日志配置
    - 1.1: 修改 `cs-image-search-mcp-server/src/main/resources/application-stdio.yml`
    - 1.2: 在 stdio profile 中关闭 banner 并保持 non-web 模式
    - 1.3: 降低或关闭 root 日志输出
    - 1.4: 降低或关闭 `com.alibaba.nacos` 相关日志输出

- [✓] Task 2: 修正主应用 MCP 启动命令
    - 2.1: 修改 `src/main/resources/mcp-servers.json`
    - 2.2: 移除图片搜索 MCP 命令中的空 `-Dlogging.pattern.console=` 参数
    - 2.3: 增加安全的日志关闭 JVM 参数
    - 2.4: 保持高德 MCP 配置不写入真实 API key

- [✓] Task 3: 重建图片搜索 MCP jar
    - 3.1: 在 `cs-image-search-mcp-server` 目录执行 Maven package
    - 3.2: 确认 `target/cs-image-search-mcp-server-0.0.1-SNAPSHOT.jar` 已更新
    - 3.3: 如构建失败，记录错误并修复构建问题

- [✓] Task 4: 单独验证两个 MCP 子进程启动状态
    - 4.1: 单独启动高德 MCP 并确认可进入 stdio 运行态
    - 4.2: 单独启动图片搜索 MCP 并确认不再出现空 logging pattern 错误
    - 4.3: 记录两个 MCP 子进程 stdout/stderr 头部信息

- [✓] Task 5: 默认启用 MCP 启动主应用验证
    - 5.1: 检查默认端口 `8123` 是否被占用
    - 5.2: 如 `8123` 被占用，使用备用端口 `8124`
    - 5.3: 设置 Java 17、DashScope、高德和 Pexels 环境变量后启动主应用
    - 5.4: 确认是否仍出现 `mcpSyncClients` 20 秒超时
    - 5.5: 记录启动成功或失败原因

- [✓] Task 6: 生成修复总结
    - 6.1: 记录根因判断
    - 6.2: 记录配置修改内容
    - 6.3: 记录 jar 重建结果
    - 6.4: 记录最终启动验证结果
    - 6.5: 记录如仍失败时的下一步建议
