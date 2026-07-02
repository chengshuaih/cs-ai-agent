# 机器视觉智能问答与采集辅助系统 改造任务计划

> 依据：`2026-06-18-machine-vision-agent-system-design.md`
> 原则：先建领域骨架并去恋爱化，再做地图采集融合，然后 PDF 报告与实验规划，最后串联采集项目工作区与前端演示闭环。每个顶层任务尽量可独立编译、可验证。

- [x] Task 1: 建立 vision 业务包骨架与数据模型
    - 1.1: 新建 `cn.chengshuai.csaiagent.vision` 包及 agent/controller/model/service/report 子包
    - 1.2: 定义 `VisionTask`、`VisionSceneType`、`CollectionSite`、`CollectionPlan`、`ExperimentPlan`、`VisionReport`、`VisionProject`（record/DTO，字段以 technical-solution §2 为准）
    - 1.3: 新增 `VisionRecordStore`：本地 JSON 轻量持久化，统一存储根目录 `tmp/vision/`（records.json / projects.json / reports/）
    - 1.4: 新增 `common.ApiResponse<T>` 与 `ResultCode`，供 vision 业务接口统一返回
    - 1.5: 编译通过，确认模型与持久化骨架可用

- [x] Task 2: 机器视觉知识问答改造（去恋爱化第一刀）
    - 2.1: 将 `LoveApp` 改造为 `VisionQaApp`，替换系统 Prompt 为机器视觉助手人设
    - 2.2: `LoveAppDocumentLoader` → `VisionDocumentLoader`，元数据改为 domain/taskType/sceneType/source
    - 2.3: `LoveAppVectorStoreConfig` → `VisionVectorStoreConfig`，Bean `loveAppVectorStore` → `visionVectorStore` 并同步 `@Qualifier`
    - 2.4: `LoveAppRagCustomAdvisorFactory` → `VisionRagAdvisorFactory`、相关 Augmenter/QueryRewriter 同步改名
    - 2.5: `AiController` 路由 `/ai/love_app/chat/*` → `/ai/vision/chat/*`（sync/sse）

- [x] Task 3: 机器视觉 RAG 知识文档替换
    - 3.1: 删除/归档三篇恋爱文档
    - 3.2: 新增机器视觉基础、采集规范、目标检测/图像分割/深度估计/三维重建指南、无人驾驶视觉感知、采集安全与隐私规范等 Markdown（≥6 篇 + 安全规范）
    - 3.3: 可选扩充视觉评价指标说明、数据集与开源项目速览
    - 3.4: 清空并重建向量库 embedding（SimpleVectorStore/PgVector），确保旧恋爱向量不再命中

- [x] Task 4: 智能体领域化（VisionAgent）
    - 4.1: 新增 `VisionAgent extends ToolCallAgent`，写入机器视觉领域 Prompt 与工具选择策略
    - 4.2: 新增 `CollectionPlanTool`（`@Tool` 封装 `CollectionPlanService`）并注册进 `ToolRegistration.allTools()`，VisionAgent 经此工具调用，不自行直接调地图（5.7.1 单一中枢）
    - 4.3: `AiController` 新增 `/ai/vision-agent/chat`
    - 4.4: 保留/收敛 `CsManus`（作为底层能力或废弃入口），去除 date-route 等旧场景痕迹

- [x] Task 5: 场景化数据采集核心服务
    - 5.1: 实现 `VisionTaskParser`：自然语言 → 结构化 VisionTask（大模型结构化输出 + 规则兜底）
    - 5.2: 实现 `VisionSceneClassifier`：POI/文本特征 → 视觉场景类型映射规则
    - 5.3: 实现 `CollectionSiteEvaluator`：四维规则打分（场景匹配/可达性/多样性/安全），对照地图 MCP 能力来源，缺失项标注"估算"
    - 5.4: 实现 `CollectionPlanService`：任务→候选点→分类→评估→路线→CollectionPlan，并写入 VisionRecordStore
    - 5.5: 新增 `VisionCollectionController` 与 `POST /api/vision/collection/plan`，返回统一 `ApiResponse<CollectionPlan>`

- [x] Task 6: PDF 报告场景化生成
    - 6.1: 新增 `CollectionPlanReportBuilder`，固定采集计划报告章节结构
    - 6.2: 新增 `VisionReportService`，封装 `PDFGenerationTool` 并落库报告元数据
    - 6.3: 新增 `ExperimentPlanReportBuilder`（实验计划报告）
    - 6.4: 新增报告接口 `POST /api/vision/report/pdf`

- [x] Task 7: 视觉实验流程规划模块
    - 7.1: 新增 `ExperimentPlanTemplate`，提供目标检测/图像分割/深度估计/三维重建四类模板
    - 7.2: 实现 `ExperimentPlanService`：任务类型 → 数据/标注/训练/评估/报告流程
    - 7.3: 接入 VisionAgent，使其能输出可执行实验流程并可导出实验计划 PDF

- [x] Task 8: 采集项目（工作区）聚合能力
    - 8.1: 实现 `VisionProjectService`（基于 projects.json）
    - 8.2: 新增 `VisionProjectController`：`POST /api/vision/project`、`GET /list`、`GET /{id}`
    - 8.3: 采集规划/报告接口增加可选 `projectId`，不传归入"未分类"
    - 8.4: 项目详情聚合返回其采集计划、实验计划、报告

- [x] Task 9: 工具与安全边界收敛
    - 9.1: 新增领域工具门面 `VisionSearchService`、`VisionReportService`、`VisionCollectionToolService`
    - 9.2: 收敛 `TerminalOperationTool`：限定视觉用途（目录结构/批量重命名/分场景归档）或从产品能力隐藏
    - 9.3: 将图片搜索 MCP 提为正式"视觉样例图检索"能力
    - 9.4: 终端命令白名单与危险命令屏蔽（删除/格式化/系统级修改）

- [x] Task 10: 前端页面改造
    - 10.1: `Home.vue` 改为机器视觉系统首页（标题/副标题/入口卡片）
    - 10.2: `LoveApp.vue` → `VisionQaApp.vue`，更新欢迎语与示例问题；路由 `/love-app` 改名
    - 10.3: `ManusApp.vue` → `VisionAgentApp.vue`
    - 10.4: 新增 `CollectionPlanner.vue` 采集辅助页（表单 + 推荐点位/路线/清单 + 导出 PDF）
    - 10.5: 新增 `ReportCenter.vue` 报告中心、`ProjectWorkspace.vue` 项目工作区
    - 10.6: 更新 `router/index.js` 与 `utils/api.js` 接口路径

- [x] Task 11: 数据与历史痕迹清理（按 9.4 清单）
    - 11.1: 全仓检索 love/Love/恋爱/约会/单身/已婚，清理代码、注释、日志、测试、资源残留
    - 11.2: 删除历史 `.kryo` 会话文件与旧业务产物
    - 11.3: 高德 API Key、模型 Key、数据库口令外置为环境变量/本地配置，源码用占位符
    - 11.4: 更新 README、Knife4j 接口描述、前端文案

- [x] Task 12: 关键单元测试
    - 12.1: `VisionTaskParser` 测试：自然语言/缺字段输入 → 规则兜底正确抽取 taskType/location
    - 12.2: `VisionSceneClassifier` 测试：典型 POI/关键词 → 预期 VisionSceneType
    - 12.3: `CollectionSiteEvaluator` 测试：四维打分边界（满分/缺数据估算/安全降分）与总分范围 0-100
    - 12.4: `CollectionPlanReportBuilder` 测试：给定 CollectionPlan，输出文本含全部必备章节标题

- [x] Task 13: 验收与演示闭环
    - 13.1: 编写冒烟测试/演示脚本，跑通问答 + 采集规划 + PDF 导出
    - 13.2: 预置一份示例采集计划数据，保证演示可复现
    - 13.3: 按 9.4.5 与各模块验收标准核对：无恋爱残留、向量库纯净、提交物无密钥
    - 13.4: 走通最小演示路径（首页→采集推荐→生成 PDF→问答→报告中心）
