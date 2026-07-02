# MCP 本地服务器配置修复任务计划

- [✓] Task 1: 新增 mcp-servers-local.json 并加入 .gitignore
    - 1.1: 在 `src/main/resources/` 下创建 `mcp-servers-local.json`，写入真实 API key
    - 1.2: 在 `.gitignore` 中追加 `mcp-servers-local.json` 忽略规则

- [✓] Task 2: 更新 application-local.yml 指向本地 MCP 配置
    - 2.1: 在 `application-local.yml` 中覆盖 `spring.ai.mcp.client.stdio.servers-configuration` 为 `classpath:mcp-servers-local.json`

- [✓] Task 3: 验证启动
    - 3.1: 不额外 export 任何环境变量，直接使用 Java 17 启动主应用
    - 3.2: 确认 MCP 子进程正常初始化（不出现 `mcpSyncClients` 超时）
    - 3.3: 记录启动结果

- [✓] Task 4: 生成总结
    - 4.1: 写入 summary.md
