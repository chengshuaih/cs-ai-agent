# 本地默认启用 MCP 与 API Key 配置任务计划

- [✓] Task 1: 更新本地 application-local.yml 配置
    - 1.1: 将 `spring.ai.mcp.client.enabled` 从 `false` 改为 `true`
    - 1.2: 在本地配置中记录高德地图 API Key 配置项
    - 1.3: 在本地配置中记录 Pexels API Key 配置项
    - 1.4: 确认真实 key 只写入 `.gitignore` 已忽略的 `application-local.yml`

- [✓] Task 2: 保持 MCP 公开配置不泄露密钥
    - 2.1: 检查 `mcp-servers.json` 是否仍使用 `${AMAP_MAPS_API_KEY}` 占位符
    - 2.2: 确认 `mcp-servers.json` 未写入真实高德地图 API Key
    - 2.3: 确认图片搜索 MCP 仍通过 `PEXELS_API_KEY` 环境变量读取 Pexels API Key

- [✓] Task 3: 尝试默认启用 MCP 启动应用
    - 3.1: 确认 Java 17 环境
    - 3.2: 确认图片搜索 MCP jar 存在
    - 3.3: 使用本地 API Key 环境变量启动 Spring Boot 应用
    - 3.4: 如遇端口占用，改用备用端口启动
    - 3.5: 记录启动成功或失败原因

- [✓] Task 4: 生成验证总结
    - 4.1: 记录本地配置变更
    - 4.2: 记录密钥未写入 Git 跟踪配置的检查结果
    - 4.3: 记录启动验证结果和可能的 MCP 风险
