# 机器视觉智能问答与采集辅助系统 技术方案

> 配套文档：`2026-06-18-machine-vision-agent-system-design.md`（设计）、`2026-06-18-machine-vision-agent-system-tasks.md`（任务）
> 本文聚焦"怎么改"：结合现有源码给出每个改动点的具体实现方案、接口契约、数据结构与关键代码片段。

## 0. 现状基线（基于源码核对）

| 关注点 | 现状 | 对方案的影响 |
|---|---|---|
| 问答应用 | `app/LoveApp.java`，普通 `@Component`，`doChat/doChatByStream/doChatWithRag/...` | 改造为 `VisionQaApp`，保留方法结构，换 Prompt 与向量库引用 |
| RAG 向量库 | `rag/LoveAppVectorStoreConfig` 构建内存 `SimpleVectorStore`，启动时 `loadMarkdowns()` + 关键词增强 | 内存库每次启动重建，**换文档+重启即完成"清库重建"**，无需手工清理 |
| 文档加载 | `rag/LoveAppDocumentLoader` 第 38 行用 `filename.substring(len-6,len-4)` 取"状态" | 必须改元数据策略（按 domain/taskType），否则中文文件名截取无意义甚至越界 |
| RAG 调用 | `LoveApp.doChatWithRag` 用 `new QuestionAnswerAdvisor(loveAppVectorStore)` | 改为注入 `visionVectorStore` |
| 智能体 | `agent/CsManus extends ToolCallAgent`，请求级 `new CsManus(...)` | 新增 `agent/VisionAgent extends ToolCallAgent`，同样请求级构造 |
| 工具集 | `tools/ToolRegistration.allTools()` 注册 7 个工具；MCP 工具由 `toolCallbackProvider` 提供 | 工具本身复用；新增"领域服务"封装层，收敛终端工具 |
| 控制器 | `controller/AiController`，`/api/ai/love_app/chat/*`、`/api/ai/manus/chat` | 新增 `/api/ai/vision/*`、`/api/ai/vision-agent/chat`，新增 vision 业务控制器 |
| PDF | `tools/PDFGenerationTool`：`@Tool generatePDF(fileName, content)` 默认存 `tmp/pdf`；新增重载 `generatePDF(fileName, content, fileDir)` 供报告中枢指定目录 | 报告构建器产出结构化文本后，由 `VisionReportService` 调 3 参重载统一落盘到 `tmp/vision/reports/` |
| 持久化 | 无 DB schema；`FileConstant.FILE_SAVE_DIR = user.dir/tmp` | 本地 JSON 持久化落在 `tmp/vision/` |
| 密钥 | `mcp-servers.json:10` 高德 key、`application-local.yml:4` DashScope key、`application.yml:39` search-api key 均明文 | 统一外置为环境变量占位 |
| 前端 | `views/{Home,LoveApp,ManusApp,DebugPage}.vue`，`router/index.js` 路由 `/love-app`、`/manus-app` | 改名 + 新增采集/报告/项目页 |
| 包名 | `cn.chengshuai.csaiagent` | 新增子包 `vision` |

## 1. 目标包结构

```text
cn.chengshuai.csaiagent
├── app/VisionQaApp.java                 (由 LoveApp 改造)
├── agent/VisionAgent.java               (新增, extends ToolCallAgent)
├── rag/
│   ├── VisionDocumentLoader.java        (由 LoveAppDocumentLoader 改造)
│   ├── VisionVectorStoreConfig.java     (由 LoveAppVectorStoreConfig 改造)
│   └── VisionRagAdvisorFactory.java     (由 LoveAppRagCustomAdvisorFactory 改造)
├── controller/
│   ├── AiController.java                (改路由)
│   ├── VisionCollectionController.java  (新增)
│   ├── VisionReportController.java      (新增)
│   └── VisionProjectController.java     (新增)
└── vision/
    ├── model/      VisionTask / VisionSceneType / CollectionSite / CollectionPlan
    │               / ExperimentPlan / VisionReport / VisionProject
    ├── service/    VisionTaskParser / VisionSceneClassifier / CollectionSiteEvaluator
    │               / CollectionPlanService / ExperimentPlanService / VisionReportService
    │               / VisionProjectService / VisionRecordStore
    └── report/     CollectionPlanReportBuilder / ExperimentPlanReportBuilder
```

## 2. 数据模型（Java record / class）

第一版用 record 表达不可变 DTO，集合字段用 List。

```java
// VisionSceneType: 枚举 + 适配任务
public enum VisionSceneType {
    INTERSECTION("十字路口", List.of("车辆检测","行人检测","交通灯识别")),
    PARKING("停车场", List.of("车辆检测","遮挡场景")),
    CAMPUS("校园道路", List.of("行人检测","小目标采集")),
    INDUSTRIAL_PARK("工业园区", List.of("园区道路","低速场景")),
    PEDESTRIAN_PATH("非机动车道", List.of("行人","骑行者")),
    LOW_LIGHT("低光照", List.of("夜间视觉","明暗变化")),
    REFLECTIVE_ROAD("反光路面", List.of("积水反光","浮空伪影"));
    // label, suitableTasks 字段 + 构造
}

public record VisionTask(
    String taskType,            // 目标检测/图像分割/小目标检测/深度估计/三维重建
    List<String> targetObjects, // 车辆、行人、交通灯、路沿...
    List<String> sceneTypes,    // 文本场景偏好
    String location,            // 城市/学校/园区/地址
    String timeBudget,          // 2小时/半天...
    String transportMode        // 步行/骑行/驾车
) {}

public record CollectionSite(
    String name, String address, String distance,
    List<String> sceneTags, List<String> suitableTasks,
    int score, String reason,
    List<String> captureSuggestions, List<String> riskTips
) {}

public record CollectionPlan(
    String id, String title, VisionTask task,
    List<CollectionSite> sites, String routeSummary,
    List<String> checklist, List<String> annotationGuide,
    List<String> privacyTips, String pdfPath
) {}

public record ExperimentPlan(
    String id, String taskType, String objective,
    List<String> dataPrep, List<String> annotationDesign,
    String datasetSplit, List<String> recommendedModels,
    List<String> trainingSteps, List<String> metrics,
    List<String> risks, String pdfPath
) {}

public record VisionReport(
    String id, String title, String type,   // collection_plan/experiment_plan/research_summary/stage_summary
    String projectId, String createdAt, String pdfPath
) {}

public record VisionProject(
    String id, String name, String description, VisionTask visionTask,
    List<String> collectionPlanIds, List<String> experimentPlanIds,
    List<String> reportIds, String createdAt, String updatedAt
) {}
```

## 3. 关键改造点（贴合现有代码）

### 3.1 VisionQaApp（由 LoveApp 改造）

保留 `doChat / doChatByStream / doChatWithRag / doChatWithTools / doChatWithMcp` 方法签名，仅改：

- 类名、`SYSTEM_PROMPT` 改为机器视觉助手人设。
- `doChatWithReport` + record `LoveReport` 删除或改为通用 `VisionQaReport`（第一版可直接删，无引用方）。
- RAG 注入由 `loveAppVectorStore` → `visionVectorStore`。

新 Prompt（要点）：

```text
你是机器视觉智能助手，专注于机器视觉概念、目标检测、图像分割、深度估计、
三维重建、数据采集与标注、实验流程与评价指标等问题。
回答先正面回应问题，必要时结合知识库；遇到与机器视觉无关的问题，
说明系统边界后再给一般性解释。不暴露工具调用细节。
```

### 3.2 VisionDocumentLoader（修复元数据策略）

现有第 38 行 `substring(len-6, len-4)` 对中文文件名无意义，改为基于文件名前缀约定 domain：

```java
// 文件名约定：detection_目标检测常见问题.md → domain=detection
String filename = resource.getFilename();
String domain = filename.contains("_") ? filename.substring(0, filename.indexOf('_')) : "general";
MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
    .withHorizontalRuleCreateDocument(true)
    .withAdditionalMetadata("filename", filename)
    .withAdditionalMetadata("domain", domain)   // 替代原 status
    .build();
```

> 折中：若不想给文件名加前缀，可统一写 `domain=machine-vision` 常量，先保证不越界、不报错。最终选哪种在 Task 2 执行时定。

### 3.3 VisionVectorStoreConfig

仅改 Bean 名 `loveAppVectorStore` → `visionVectorStore`、loader 引用。内存库特性意味着**换掉 `resources/document/*.md` 并重启，旧恋爱向量自动消失**，这是设计文档 9.4.2"清空重建"的最简实现。若启用 PgVector 才需手工清表（第一版默认 SimpleVectorStore，不涉及）。

### 3.4 VisionAgent（由 CsManus 模式新建）

```java
@Component
public class VisionAgent extends ToolCallAgent {
    public VisionAgent(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("visionAgent");
        this.setSystemPrompt(/* 机器视觉实验智能体 Prompt，含采集/地图/报告策略 */);
        this.setNextStepPrompt(/* 工具选择策略：采集->地图MCP；报告->PDF；资料->搜索 */);
        this.setMaxSteps(20);
        this.setChatClient(ChatClient.builder(dashscopeChatModel)
            .defaultAdvisors(new MyLoggerAdvisor()).build());
    }
}
```

请求级实例化沿用 `AiController.doChatWithManus` 写法（合并 `allTools` + `toolCallbackProvider.getToolCallbacks()`）。

**5.7.1 单一中枢落地（明确实现方式）**：新增 `tools/CollectionPlanTool`，以 `@Tool` 注解封装 `CollectionPlanService.plan(...)`，注册进 `ToolRegistration.allTools()` 成为 Spring AI `ToolCallback`。VisionAgent 通过标准工具调用触发它，**不自行拼接地图逻辑**。这样对话入口（VisionAgent）与结构化入口（`POST /vision/collection/plan` 控制器）共用同一份 `CollectionPlanService`，杜绝两套逻辑。

```java
public class CollectionPlanTool {
    private final CollectionPlanService planService;
    public CollectionPlanTool(CollectionPlanService planService) { this.planService = planService; }

    @Tool(description = "根据视觉任务与位置生成数据采集规划（点位/路线/清单）")
    public CollectionPlan planCollection(
            @ToolParam(description = "任务类型，如目标检测") String taskType,
            @ToolParam(description = "目标对象，逗号分隔") String targetObjects,
            @ToolParam(description = "位置/城市") String location,
            @ToolParam(description = "采集时长，如半天") String timeBudget,
            @ToolParam(description = "交通方式，如骑行") String transportMode) {
        VisionTask task = VisionTask.of(taskType, targetObjects, location, timeBudget, transportMode);
        return planService.plan(task, false);   // Agent 链路默认不直接出 PDF，由报告工具单独触发
    }
}
```

> 注册：`ToolRegistration.allTools()` 增加 `new CollectionPlanTool(planService)`。因 `ToolRegistration` 现为 `@Configuration`，将 `CollectionPlanService` 注入其 `allTools(...)` 方法参数即可。

### 3.5 CsManus 处理

保留类但去除 date-route 等旧场景痕迹，或在 `AiController` 中下线 `/manus/chat`，统一走 `/vision-agent/chat`。建议：保留 `CsManus` 不动作为历史能力，前端入口切到 `VisionAgent`，避免大改 ReAct 基类。

## 4. 采集规划核心链路（Task 5）

```text
VisionCollectionController.plan(req)
  -> VisionTaskParser.parse(req)               // DTO 已结构化时直接映射；自然语言时调用大模型结构化输出
  -> 候选点检索（地图 MCP / Agent）
  -> VisionSceneClassifier.classify(poi)       // POI/文本 -> VisionSceneType
  -> CollectionSiteEvaluator.evaluate(sites)   // 四维规则打分
  -> 组织 routeSummary + checklist
  -> CollectionPlan
  -> VisionRecordStore.save(plan)              // 落本地 JSON（采集规划到此结束，pdfPath=null）

// 第二步（独立请求）：POST /api/vision/report/pdf
  -> CollectionPlanReportBuilder + VisionReportService.generate(...) -> tmp/vision/reports/*.pdf -> VisionReport
```

**评估字段 ↔ 地图能力来源**（落地约束，缺失项标注"估算"）：

| 评分项 | 数据来源 |
|---|---|
| 场景匹配 0-40 | POI 类型/名称关键词 → SceneClassifier |
| 可达性 0-20 | 距离/路径耗时（地图测距/路径规划）；不可得则按直线距离估算 |
| 多样性 0-20 | 周边 POI 类型分布聚合 |
| 安全性 0-20 | POI 类型 + 规则（临主干道降分、园区/校园加分） |

第一版若地图 MCP 路径接口不稳定，`CollectionSiteEvaluator` 用"场景匹配 + 规则估算"产出分数并标注，保证链路可跑通。

## 5. 轻量持久化（VisionRecordStore，Task 1.3）

```text
tmp/vision/
├── records.json     // List<VisionReport> 索引（含 type/projectId/pdfPath/createdAt）
├── projects.json    // List<VisionProject>
└── reports/         // 生成的 PDF，统一落盘于此（VisionReportService 经 generatePDF 3 参重载指定）
```

`VisionRecordStore` 用 Hutool `JSONUtil`（pom 已含 hutool-all）读写，加 `synchronized` 防并发写。`VisionProjectService`、`VisionReportService` 依赖它。报告中心、项目工作区读它获取列表，闭合"6.4 报告中心需要数据"的矛盾。

## 6. 接口契约

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/ai/vision/chat/sync` | 同步问答（替换 love_app/chat/sync） |
| GET | `/api/ai/vision/chat/sse` | SSE 流式问答 |
| GET | `/api/ai/vision-agent/chat` | 视觉智能体（工具/地图/PDF） |
| POST | `/api/vision/collection/plan` | 采集规划，body 见设计 7.3，可选 `projectId` |
| POST | `/api/vision/report/pdf` | 报告生成 |
| POST | `/api/vision/project` | 新建项目 |
| GET | `/api/vision/project/list` | 项目列表 |
| GET | `/api/vision/project/{id}` | 项目详情（聚合采集/实验/报告） |

> 注意：`server.servlet.context-path=/api`，控制器 `@RequestMapping("/ai")` 实际暴露 `/api/ai/...`。vision 业务控制器用 `@RequestMapping("/vision")` 即对外 `/api/vision/...`。

**响应格式约定（统一口径，消除两种说法并存）**：

- **聊天/智能体接口保持原样**：`/api/ai/vision/chat/sync` 返回纯 `String`、`/chat/sse` 与 `/vision-agent/chat` 返回流（`Flux<String>` / `SseEmitter`），沿用现有 `AiController` 行为，不包装。理由：SSE 逐 token 推送无法套 `{code,message,data}`，前端 `createSSEConnection` 也按纯文本消费。
- **vision 业务接口统一用 `ApiResponse<T>`**：`/api/vision/**`（采集规划、报告、项目）一律返回 `ApiResponse<T> = { code, message, data }`，新增 `cn.chengshuai.csaiagent.common.ApiResponse<T>` 与 `ResultCode`（见 api-examples §0 的 code 表）。
- 即"聊天纯文本、业务包装体"的双轨制，按接口性质区分，不做全局强制包装。

## 7. 密钥外置（Task 11.3，安全项）

| 位置 | 现状 | 改为 |
|---|---|---|
| `application-local.yml:4` | `api-key: sk-2fe8...` 明文 | `${DASHSCOPE_API_KEY:}`，本地用环境变量 |
| `application.yml:39` | `search-api.api-key: WixbY...` | `${SEARCH_API_KEY:}` |
| `mcp-servers.json:10` | `AMAP_MAPS_API_KEY: 38a05...` | 读环境变量；提交版用占位符 `${AMAP_MAPS_API_KEY}` |

`application.yml:8` 的 DashScope 已是占位符写法，可参照。提交软著前确认仓库内无真实 key。

## 8. 前端改造（Task 10）

- `router/index.js`：`/love-app`→`/vision-qa`，`/manus-app`→`/vision-agent`，新增 `/collection-planner`、`/report-center`、`/project`。
- `LoveApp.vue`→`VisionQaApp.vue`、`ManusApp.vue`→`VisionAgentApp.vue`：复用 SSE 逻辑（`utils/api.js` 的 `createSSEConnection`），仅改 URL、欢迎语、示例问题。
- 新增 `CollectionPlanner.vue`（表单→`POST /vision/collection/plan`→展示点位/路线/清单+导出PDF）、`ReportCenter.vue`、`ProjectWorkspace.vue`。
- `Home.vue`：四张入口卡片改为问答/采集推荐/实验规划/报告中心。
- `utils/api.js`：`API_BASE_URL` 不变（已是 `/api`）。

## 9. 实施顺序与验证

与 tasks.md 一致，按 Task 1→13。每个后端任务以"`mvn compile` 通过 + 关键接口手测"为验证；核心服务（Task 5）补 Task 12 单元测试；前端以 `npm run dev` 跑通页面为验证。最终 Task 13 走最小演示闭环（首页→采集推荐→PDF→问答→报告中心），并按 9.4.5 全仓检索确认无 `love/恋爱` 残留、无明文密钥。

## 10. 报告构建器契约（Task 6，补全）

`PDFGenerationTool.generatePDF` 现状：单段落 `Paragraph`、内置字体 `STSongStd-Light`、`sanitizePdfContent` 过滤 BMP 外字符与控制符。2 参 `@Tool` 重载默认存 `tmp/pdf`；报告中枢 `VisionReportService` 调用 3 参重载 `generatePDF(fileName, content, recordStore.reportsDir())` 统一落盘到 `tmp/vision/reports/`。因此**报告构建器只需产出一段纯文本字符串**（用 `\n` 分段），不能依赖富文本/表格排版。

```text
CollectionPlanReportBuilder.build(CollectionPlan) : String   // 纯文本，章节用标题行+换行
VisionReportService.generate(type, title, content, projectId) : VisionReport
  -> fileName = type + "_" + yyyyMMddHHmmss + ".pdf"
  -> PDFGenerationTool.generatePDF(fileName, content)         // 复用，不改其签名
  -> new VisionReport(id, title, type, projectId, createdAt, pdfPath)
  -> VisionRecordStore.saveReport(report)                     // 写 records.json
```

采集计划报告固定章节（构建器按序拼接为文本）：
1. 任务概述（taskType / targetObjects / location / timeBudget）
2. 推荐采集点位（逐个：名称、地址、距离、得分、推荐理由）
3. 路线与时段建议（routeSummary）
4. 采集清单（checklist）
5. 标注与隐私提示（annotationGuide + privacyTips）

实验计划报告章节：目标 → 数据准备 → 标注设计 → 数据集划分 → 推荐模型 → 训练步骤 → 评价指标 → 风险。两个 Builder 输出结构对齐各自 record 字段，保证"字段→章节"一一可追溯。

> 约束：`VisionReportService` 是写 PDF 的唯一入口，VisionAgent 经此服务生成报告而非直接调 `PDFGenerationTool`，与 5.7.1 单一中枢一致。

## 11. 实验规划模块（Task 7，补全）

`ExperimentPlanService` 不依赖外部接口，是**模板 + 任务参数填充**的纯本地逻辑，可独立编译与单测。

```text
ExperimentPlanTemplate (enum/Map)         // 四类模板
  DETECTION / SEGMENTATION / DEPTH_ESTIMATION / RECONSTRUCTION
    -> 预置 dataPrep / annotationDesign / datasetSplit
       / recommendedModels / trainingSteps / metrics / risks

ExperimentPlanService.plan(taskType, objective) : ExperimentPlan
  1. 按 taskType 选模板（无匹配 -> DETECTION 兜底并提示）
  2. 用 objective/targetObjects 微调（替换占位、补目标类别）
  3. 组装 ExperimentPlan record
  4. (可选) VisionRecordStore 关联 projectId
```

四类模板要点（第一版固定文案，后续可外置到 resources）：

| taskType | recommendedModels | metrics |
|---|---|---|
| 目标检测 | YOLO 系列 / Faster R-CNN | mAP@0.5、mAP@0.5:0.95、FPS |
| 图像分割 | U-Net / Mask R-CNN / DeepLab | mIoU、Dice、像素准确率 |
| 深度估计 | MiDaS / Monodepth2 | AbsRel、RMSE、δ<1.25 |
| 三维重建 | COLMAP / NeRF 系 | 重投影误差、点云完整度 |

接入 VisionAgent：实验规划意图统一调用 `ExperimentPlanService.plan(...)`，需要导出时经 `ExperimentPlanReportBuilder` + `VisionReportService` 生成 PDF，不在 Agent 内拼模板，保持单一中枢。

## 12. 工具与安全边界（Task 9，补全 / 安全项）

现状风险：`TerminalOperationTool` 直接 `ProcessBuilder("/bin/sh","-c", command)` 执行任意命令，`redirectErrorStream` 回显，**无任何白名单/黑名单**。这是面向系统的高危直通能力，软著演示前必须收敛。

落地方案（改 `TerminalOperationTool`，不动 ReAct 基类）：

```text
executeTerminalCommand(command):
  1. 取命令首 token 作为可执行名
  2. 白名单放行：ls / pwd / cat / head / tail / mkdir / mv / cp / find / tree / echo / wc
  3. 危险模式拦截（命中即拒绝，返回提示而非执行）：
     rm  rmdir  mkfs  dd  shutdown  reboot  kill  chmod 777  > /dev  sudo
     管道/重定向/命令链符号 | & ; `$(` 默认拒绝（仅放行白名单单命令）
  4. 限定工作目录在项目 tmp/ 下，禁止绝对路径与 ..
  5. 不通过则返回 "命令被安全策略拦截：<原因>"，不抛出、不执行
```

工具门面（收敛对外语义，复用底层工具）：

| 门面 | 复用底层 | 对外语义 |
|---|---|---|
| `VisionSearchService` | WebSearchTool + 图片搜索 MCP | "视觉样例图/资料检索" |
| `VisionReportService` | PDFGenerationTool | "报告生成"（见 §11） |
| `VisionCollectionToolService` | 地图 MCP | "采集点位检索/路线"（仅供 CollectionPlanService 调用） |

图片搜索 MCP（`cs-image-search-mcp-server`）由"调试能力"提为正式"视觉样例图检索"，在 VisionAgent Prompt 中明确其用途。终端工具按上述收敛后，仅保留视觉数据整理用途（目录结构、分场景归档、批量重命名）。

> 安全提示：白名单方式仍允许 `cat` 读任意可达文件，第一版限定工作目录在 `tmp/` 下可缓解；若软著演示不需要终端能力，最稳妥是从 `ToolRegistration.allTools()` 中移除该工具（一行删除），彻底消除直通 shell 风险。两种取舍在 Task 9 执行时按演示需要二选一。

## 13. 痕迹清理补充（Task 11.1 / 11.2）

- **love 残留检索**：全仓 `grep -ri "love\|恋爱\|约会\|单身\|已婚\|date"` 检查代码/注释/日志/测试/资源，逐项改名或删除（注意 `date` 误命中日期，需人工甄别）。Bean 名 `loveAppVectorStore`、`@Qualifier`、方法引用同步改 `visionVectorStore`。
- **历史会话文件**：`FileBasedChatMemory`（Kryo 序列化）会在 `tmp/` 下产生 `.kryo` 会话文件，含旧恋爱对话。清理动作：删除 `tmp/` 下历史 `.kryo` 与旧 `tmp/pdf` 产物，确保提交物/演示环境无旧业务数据。
- 配合 §7 密钥外置与 9.4.5 验收：提交前全仓再次检索确认无 `love/恋爱` 残留、无明文 key、无 `.kryo` 历史。

## 14. 风险与取舍

- **地图 MCP 能力不确定**：评估打分对路径接口有依赖，第一版用规则估算兜底，不阻塞主链路。
- **结构化输出稳定性**：`VisionTaskParser` 大模型结构化可能失败，需规则兜底（关键词抽取 location/taskType）。
- **CsManus 去留**：建议保留不动、仅切换入口，降低改动 ReAct 基类的回归风险。
- **PgVector**：第一版坚持 SimpleVectorStore，避免引入清表/迁移复杂度；如需 PgVector 再单列任务。
