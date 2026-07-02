# IDE 点击运行默认启用 MCP 本地配置修复任务计划

- [x] Task 1: 更新 application-local.yml 本地启动配置
    - 1.1: 将 `spring.ai.dashscope.api-key` 增加本地默认 key 回退值
    - 1.2: 保持 `spring.ai.mcp.client.enabled=true`
    - 1.3: 保留高德地图 API Key 本地配置项
    - 1.4: 保留 Pexels API Key 本地配置项
    - 1.5: 确认 `application-local.yml` 仍为 `.gitignore` 忽略文件

- [x] Task 2: 更新 mcp-servers.json 子进程环境变量
    - 2.1: 将高德 MCP 的 `AMAP_MAPS_API_KEY` 改为引用 `local-api-keys.amap.maps-api-key`
    - 2.2: 给图片搜索 MCP 增加 `PEXELS_API_KEY` 环境变量占位
    - 2.3: 给图片搜索 MCP 增加 `DASHSCOPE_API_KEY` 环境变量占位
    - 2.4: 给图片搜索 MCP 增加 `SPRING_AI_DASHSCOPE_API_KEY` 环境变量占位
    - 2.5: 确认 `mcp-servers.json` 不包含真实 API key

- [x] Task 3: 验证默认启动配置
    - 3.1: 检查 `8123` 是否已有旧进程监听
    - 3.2: 如有旧进程，记录 PID 并使用备用端口或停止旧进程
    - 3.3: 不额外 export API key，直接启动主应用验证本地配置是否生效
    - 3.4: 确认是否出现 DashScope key 缺失
    - 3.5: 确认是否出现 `mcpSyncClients` 20 秒超时
    - 3.6: 记录启动成功或失败原因

- [x] Task 4: 生成修复总结
    - 4.1: 记录本地配置修改内容
    - 4.2: 记录 MCP 子进程 env 修改内容
    - 4.3: 记录密钥未写入公开配置的检查结果
    - 4.4: 记录最终启动验证结果
    - 4.5: 如仍失败，记录下一步处理建议
