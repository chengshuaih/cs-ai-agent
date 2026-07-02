# 机器视觉智能问答与采集辅助系统 改造总结

> 关联文档：`2026-06-18-machine-vision-agent-system-design.md`、`-technical-solution.md`、`-system-tasks.md`、`-acceptance.md`、`-api-examples.md`

## 一、目标与成果

将原 "恋爱大师" AI 智能体（Spring Boot 3.4.4 + Spring AI 1.0.0 + Spring AI Alibaba + DashScope）整体改造为**机器视觉智能问答与采集辅助系统**，支撑软件著作权申请。系统具备：机器视觉知识问答（RAG）、领域智能体（VisionAgent + 工具）、场景化数据采集规划、视觉实验流程规划、PDF 报告生成、采集项目工作区聚合，以及配套前端演示界面。`tasks.md` 中 Task 1–13 全部完成。

## 二、架构与关键设计

- **单一中枢**：采集规划统一经 `CollectionPlanService`、报告生成统一经 `VisionReportService`、实验规划统一经 `ExperimentPlanService`。对话入口（VisionAgent 经 `@Tool` 工具）与 REST 入口（控制器）共用同一服务路径，避免双套逻辑。
- **响应双轨**：`/api/ai/**` 聊天接口返回 `String`/`Flux`/`SseEmitter`；`/api/vision/**` 业务接口统一返回 `ApiResponse<T> = {code, message, data}`（`code=0` 成功）。`context-path=/api`，端口 `8123`。
- **数据模型**：全部用 Java record（`VisionTask`、`VisionSceneType`、`PoiCandidate`、`CollectionSite`、`CollectionPlan`、`ExperimentPlan`、`VisionReport`、`VisionProject`）。
- **持久化**：`VisionRecordStore` 基于 Hutool 做本地 JSON 轻量持久化，存储根 `tmp/vision/`（`plans.json`/`experiments.json`/`records.json`/`projects.json`/`reports/`），文件级 synchronized 防并发。
- **采集打分**：`CollectionSiteEvaluator` 四维规则打分（场景匹配 0-40 / 可达性 0-20 / 多样性 0-20 / 安全 0-20，总分 0-100），地图能力缺失维度按规则估算并在 `distance`/`reason` 标注"（估算）"，保证地图 MCP 不可用时仍可演示。
- **RAG 向量库**：MVP 默认使用 `SimpleVectorStore`（内存，Bean `visionVectorStore`，加载 7 篇知识文档），无需外部依赖即可运行。PgVector 为**可选/预留**方案（`PgVectorVectorStoreConfig`），仅在配置数据库后启用，非 MVP 必需。
- **PDF**：复用 `PDFGenerationTool`（iText + 内置中文字体 STSongStd-Light），由 `CollectionPlanReportBuilder`/`ExperimentPlanReportBuilder` 产出固定章节结构文本。报告统一经 `VisionReportService` 落盘到 `tmp/vision/reports/`（通过 `PDFGenerationTool.generatePDF(fileName, content, reportsDir)` 指定目录）；`PDFGenerationTool` 作为 `@Tool` 直接调用时默认写入 `tmp/pdf`。

## 三、主要改动

后端：
- 新增 `cn.chengshuai.csaiagent.vision` 业务包（model/service/report）+ `common.ApiResponse`/`ResultCode`。
- `LoveApp → VisionQaApp`，RAG 链路 `LoveApp* → Vision*`，Bean `loveAppVectorStore → visionVectorStore`（含 `@Qualifier`）。
- 新增 `VisionAgent`、`CollectionPlanTool` 等工具并注册进 `ToolRegistration`。
- 新增控制器：`VisionCollectionController`（`POST /vision/collection/plan`）、`VisionExperimentController`（`POST /vision/experiment/plan`）、`VisionReportController`（`POST /vision/report/pdf`、`GET /vision/report/list`）、`VisionProjectController`（`POST /vision/project`、`GET /vision/project/list`、`GET /vision/project/{id}`）。
- `AiController` 路由改为 `/ai/vision/chat/*` 与 `/ai/vision-agent/chat`。
- RAG 知识库替换为 7 篇机器视觉文档（基础/检测/分割/深度三维/无人驾驶/评价指标/采集安全隐私）。

前端（`cs-ai-agent-fronted`）：
- `Home.vue` 改为机器视觉系统首页；`LoveApp.vue→VisionQaApp.vue`、`ManusApp.vue→VisionAgentApp.vue`；新增 `CollectionPlanner.vue`、`ReportCenter.vue`、`ProjectWorkspace.vue`；更新 `router/index.js`、`DebugPage.vue` 与文档。

安全与清理：
- 全仓清除 love/恋爱/约会/单身/已婚 业务残留（代码/注释/测试/资源/前端文案）；个别历史迁移文档可能仍提及旧名词作为背景说明。
- DashScope / Search / 高德 API Key、数据库口令外置为环境变量占位符（`application.yml`、`application-local.yml`、`mcp-servers.json`、`TestApiKey.java`）。
- image-search 子模块 `ImageSearchTool` 的 Pexels API Key 已改为从环境变量 `PEXELS_API_KEY` 读取（未配置时返回明确错误、不调用）；**注意：原硬编码的密钥已泄露在 Git 历史中，必须由维护者在 Pexels 控制台轮换/吊销**，外置改动本身不能消除历史泄露风险。
- MCP 启动命令不再使用不确定的裸 `java`，而是通过 `$JAVA_HOME/bin/java` 启动 Java 17 子进程；若 `JAVA_HOME` 未设置会明确报错。
- 前端聊天消息渲染对 AI 文本做 HTML 转义后再渲染换行，避免 `v-html` XSS；CORS 由通配符改为可配置来源白名单（`app.cors.allowed-origins`）；前端 API 基址改为 `import.meta.env.VITE_API_BASE_URL`，MCP 启动命令改为通过 `$JAVA_HOME/bin/java` 启动 Java 17 子进程。

## 四、测试与验收

- 关键单测（Task 12）：`VisionTaskParser`(3) / `VisionSceneClassifier`(3) / `CollectionSiteEvaluator`(4) / `CollectionPlanReportBuilder`(1)，共 11 项通过（离线经 JDK 17 反射 harness 验证）。其间修复 `VisionTaskParser.extractLocation` 取最具体地名的 bug（"北京市" → "中关村"）。
- 验收脚本（Task 13）：
  - `scripts/smoke-demo.sh`：跑通问答→采集规划→采集 PDF→报告列表→实验规划→实验 PDF，按 `code==0` 校验，统计 PASS/FAIL。
  - `scripts/seed-sample-data.sh` + `scripts/sample-data/plans.json`：预置示例采集计划（`planId=plan-sample-20260618`，2 个点位，字段与 `CollectionPlan`/`CollectionSite` record 完全对齐），保证地图不可用时可复现。
- 残留/密钥/向量核对（acceptance §6）：love/恋爱/约会/单身/已婚 业务代码 0 命中；DashScope/Search/高德密钥为占位符；Pexels 密钥已外置（历史泄露需轮换）；无 `.kryo` 历史会话产物。
- **构建/测试硬性前置**：本工程需 **Java 17**。系统默认 Java 11 会导致编译/测试失败，运行任何 Maven 命令前必须 `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`。MCP 子模块同样是 Java 17 产物，`mcp-servers.json` 会通过 `$JAVA_HOME/bin/java` 启动图片搜索 MCP，避免 IDE/终端 PATH 中裸 `java` 解析到 Java 11。
- 后端编译：JDK 17 下 `./mvnw -q -o compile -DskipTests` 退出码 0；前端 `npm run build` 通过。

## 五、最小演示路径

首页 → 智能问答（命中视觉知识库）→ 采集推荐（提交表单得 ≥2 点位 + 路线 + 清单）→ 导出 PDF（`tmp/vision/reports/`，中文正常）→ 报告中心（查看记录）→ 实验规划（目标检测输出完整流程并可导出 PDF）。

## 六、运行说明

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)   # 必需：Java 17，默认 Java 11 会编译/测试失败
$JAVA_HOME/bin/java -version                       # 确认输出为 17.x
export DASHSCOPE_API_KEY=...   # 必需
export SEARCH_API_KEY=...       # 可选（联网搜索）
export AMAP_MAPS_API_KEY=...    # 可选（地图增强；缺失则走估算）
export PEXELS_API_KEY=...       # 可选（image-search 子模块图片搜索；缺失则该工具返回提示）
(cd cs-image-search-mcp-server && ../mvnw -q package -DskipTests)  # 首次启用 MCP 前需构建子模块 jar
./mvnw spring-boot:run
# 如只调试非 MCP 能力，可临时禁用 MCP client：
# ./mvnw spring-boot:run -Dspring-boot.run.arguments='--spring.ai.mcp.client.enabled=false'
# 另开终端：
bash scripts/seed-sample-data.sh   # 可选：预置示例数据
bash scripts/smoke-demo.sh         # 冒烟验收
# 前端：cd cs-ai-agent-fronted && npm install && npm run dev
#   生产部署可设置 VITE_API_BASE_URL 指定后端基址（默认 http://localhost:8123/api）
```

## 七、后续可选项

- 接入真实地图 MCP 后，将 `CollectionPlanService.buildCandidates` 的规则候选点替换为真实 POI，可达性/多样性维度即转为真实数据（评估器已预留估算降级路径）。
- 单测当前依赖反射 harness 离线执行，网络恢复后可补齐 `junit-platform-launcher` 依赖，纳入 `mvn test` 常规流程。
