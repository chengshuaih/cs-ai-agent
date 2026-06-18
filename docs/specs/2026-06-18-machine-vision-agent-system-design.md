# 机器视觉智能问答与采集辅助系统设计规划

## 1. 系统定位

本系统建议从原有的通用 AI 智能体项目，改造为一个面向机器视觉学习、实验和数据采集场景的智能辅助系统。系统不直接声明自己实现某个具体视觉算法，而是围绕机器视觉项目常见流程提供问答、资料检索、采集规划、工具调用、实验辅助和报告生成能力。

推荐系统名称：

1. 机器视觉智能问答与采集辅助系统 V1.0
2. 基于大模型智能体的机器视觉实验辅助系统 V1.0
3. 面向机器视觉任务的智能问答与数据采集辅助平台 V1.0

推荐优先使用：

> 机器视觉智能问答与采集辅助系统 V1.0

该名称比“无人驾驶路面小目标三维重建智能实验管理系统”更宽泛，能够覆盖机器视觉、无人驾驶视觉感知、图像采集、数据标注、实验管理等方向，同时仍然能和毕业论文中的“小目标视觉先验、无人驾驶场景、三维重建、机器视觉”建立自然关联。

系统一句话描述：

> 本系统面向机器视觉学习与实验场景，基于大语言模型智能体、RAG 知识检索、地图 MCP 与工具调用能力，为用户提供视觉任务问答、采集地点推荐、数据采集方案生成、实验流程规划、资料检索、文件处理和 PDF 报告生成等智能化辅助服务。

## 2. 设计原则

### 2.1 宽领域、强关联

系统不局限于某一个论文实验，也不只服务于 3DGS，而是覆盖机器视觉项目中的通用环节：

- 视觉任务理解
- 数据集采集与规划
- 标注策略建议
- 模型与算法资料问答
- 实验流程拆解
- 实验日志与指标解释
- 结果报告生成

这样系统和毕业论文的关系是“机器视觉方向的实验辅助工具”，而不是强行把 Java 项目包装成三维重建核心算法。

### 2.2 保留现有技术亮点

当前 `cs-ai-agent` 已经具备较好的 AI 应用骨架，改造时应优先复用已有能力：

- Spring Boot 3 后端服务
- Spring AI / Spring AI Alibaba 大模型接入
- RAG 知识库问答
- PgVector 向量存储
- Tool Calling 工具调用
- MCP Client
- 高德地图 MCP
- 文件操作工具
- 网页抓取工具
- 联网搜索工具
- 资源下载工具
- PDF 生成工具
- Manus 风格分层智能体
- Vue 前端聊天页面

核心策略是“换业务语义 + 增加机器视觉场景能力 + 收敛工具调用边界”，而不是重写项目。

### 2.3 让地图和 PDF 成为核心功能

高德地图 MCP 不应只是一个孤立的“地图查询工具”，而应成为机器视觉数据采集流程的一部分：

- 采集地点推荐
- 采集路线规划
- 场景类型筛选
- 采集任务清单生成
- 采集风险提示
- 采集报告导出

PDF 工具也不只做普通文档导出，而应服务于机器视觉实验材料沉淀：

- 数据采集计划书
- 采集路线与点位清单
- 标注规范说明
- 实验流程说明
- 实验阶段报告
- 论文资料摘要

## 3. 用户故事

### 3.1 机器视觉学习者

作为机器视觉方向学生，我希望系统能够回答目标检测、图像分割、深度估计、三维重建、数据标注等问题，并能结合本地知识库给出更贴近项目实践的解释，帮助我快速理解实验方案。

### 3.2 数据采集者

作为需要采集机器视觉数据的用户，我希望系统能根据任务类型推荐合适的采集地点、路线和注意事项，例如十字路口、停车场、校园道路、工业园区道路、路沿区域、反光路面等，帮助我更高效地规划采集任务。

### 3.3 实验执行者

作为视觉实验执行者，我希望系统能把一个模糊实验目标拆成可执行步骤，生成数据准备、标注、训练、评估和结果整理流程，减少实验过程中的遗漏。

### 3.4 论文写作者

作为毕业论文作者，我希望系统能根据知识库、采集记录和实验结果生成阶段性 PDF 报告，作为论文实验记录、开题后续材料和软著演示材料的支撑。

## 4. 系统功能总览

系统建议划分为六个核心模块：

1. 机器视觉知识问答模块
2. 场景化数据采集辅助模块
3. 视觉实验流程规划模块
4. 工具调用与资料处理模块
5. 报告生成与材料沉淀模块
6. 智能体任务编排模块

## 4.1 基于现有代码的自然性审查

### 4.1.1 现有能力基础

当前 `cs-ai-agent` 已经具备以下真实能力，适合作为机器视觉系统的底座：

- `LoveApp.java`：已实现基于 Spring AI 的多轮对话、SSE 流式输出、RAG 问答、工具调用。
- `CsManus.java`：已实现类 Manus 的分层智能体，具备多步思考和工具调用能力。
- `ToolRegistration.java`：已集中注册文件、搜索、网页抓取、资源下载、终端、PDF、终止工具。
- `mcp-servers.json`：已接入高德地图 MCP 和图片搜索 MCP。
- `LoveAppDocumentLoader.java` / `LoveAppVectorStoreConfig.java`：已有 Markdown 文档加载和向量库初始化流程。
- `PDFGenerationTool.java`：已有 PDF 生成工具。
- 前端 `Home.vue`、`LoveApp.vue`、`ManusApp.vue`：已有首页入口和两个聊天页面。

这些能力能支撑“智能问答 + 工具调用 + 地图推荐 + 报告生成”的产品主线，因此系统改造不是凭空新建，而是在已有 AI Agent 能力上替换领域和补齐场景对象。

### 4.1.2 如果只改名会显得强行的地方

如果只把“恋爱大师”改成“机器视觉助手”，系统会有明显换皮感，原因如下：

1. **缺少机器视觉任务对象**
   - 现有系统只有自然语言聊天，没有 `VisionTask`、`CollectionPlan`、`CollectionSite` 这类结构化对象。
   - 用户问“我要采集十字路口数据”时，系统只能聊天式回答，不能沉淀成系统功能。

2. **地图 MCP 只是工具，不是业务流程**
   - 现有 `CsManus` Prompt 中地图能力偏向约会、路线、地点 PDF 等通用场景。
   - 如果不新增“视觉采集场景分类”和“采集地点评估规则”，地图推荐会像临时拼接出来的功能。

3. **PDF 只是通用导出，不是机器视觉报告**
   - 当前 `PDFGenerationTool` 只能把一段文本写成 PDF。
   - 如果不增加采集计划、实验计划、资料摘要等固定模板，报告能力会显得泛泛。

4. **前端缺少采集辅助入口**
   - 现有页面是两个聊天应用入口。
   - 如果软著截图只有聊天框，很难证明这是“机器视觉采集辅助系统”，更像普通 AI 聊天壳。

5. **RAG 文档元数据仍带恋爱业务痕迹**
   - `LoveAppDocumentLoader` 从文件名末尾截取“状态”元数据，这适合原来的“单身/恋爱/已婚”文档，但不适合机器视觉。
   - 机器视觉知识库应有 `domain`、`taskType`、`sceneType`、`source` 等元数据。

### 4.1.3 自然化改造结论

要让系统看起来“本来就是为机器视觉问答与采集辅助设计的”，第一版至少需要新增三类能力：

1. **机器视觉任务结构化能力**
   - 将用户的自然语言需求解析为任务类型、目标对象、场景偏好、位置和约束。

2. **地图点位到视觉采集价值的评估能力**
   - 不只是返回附近地点，而是说明该地点适合采集什么视觉数据、为什么适合、怎么采。

3. **采集计划与实验计划的模板化报告能力**
   - 将问答结果沉淀为可下载、可展示、可作为软著截图材料的 PDF 报告。

这三类能力是“必须新增”，否则系统会更像普通 AI Agent 换了机器视觉 Prompt。

## 4.2 新增功能分级

### 4.2.1 必须新增功能

| 新增功能 | 为什么必须加 | 代码落点 |
|---|---|---|
| 视觉任务结构化解析 | 让用户需求从聊天变成系统对象，避免换皮感 | 新增 `vision/model/VisionTask.java`、`vision/service/VisionTaskParser.java` |
| 采集场景分类规则 | 让地图地点能映射到机器视觉采集场景 | 新增 `vision/model/VisionSceneType.java`、`vision/service/VisionSceneClassifier.java` |
| 采集点位评估 | 解释地点为什么适合采集某类数据 | 新增 `vision/model/CollectionSite.java`、`vision/service/CollectionSiteEvaluator.java` |
| 采集计划生成 | 形成系统主流程：任务 -> 地点 -> 点位 -> 路线 -> 清单 | 新增 `vision/model/CollectionPlan.java`、`vision/service/CollectionPlanService.java` |
| 采集计划 PDF 模板 | 让 PDF 工具变成领域能力，而不是普通文本导出 | 新增 `vision/report/CollectionPlanReportBuilder.java` |
| 机器视觉知识库文档 | RAG 必须有领域内容支撑 | 替换 `src/main/resources/document/*.md` |
| 智能体领域 Prompt | 让工具调用策略围绕视觉采集和实验规划 | 修改 `CsManus.java` 或新增 `VisionAgent.java` |
| 前端采集辅助入口 | 软著展示需要结构化页面，不只聊天框 | 新增或改造 `CollectionPlanner.vue` |

### 4.2.2 建议新增功能

| 新增功能 | 价值 | 代码落点 |
|---|---|---|
| 实验流程规划 DTO | 支撑“机器视觉实验辅助系统”的宽泛定位 | 新增 `vision/model/ExperimentPlan.java` |
| 实验计划 PDF 模板 | 和毕业论文实验流程关联更自然 | 新增 `vision/report/ExperimentPlanReportBuilder.java` |
| 报告中心页面 | 展示已生成 PDF，增强系统完整度 | 新增 `ReportCenter.vue` |
| 资料摘要生成 | 复用搜索、网页抓取和文件工具 | 新增 `vision/service/ResearchSummaryService.java` |
| 机器视觉示例问题库 | 前端首屏更像专业系统 | 新增前端常量或后端接口 |

### 4.2.3 可选增强功能

| 新增功能 | 价值 | 说明 |
|---|---|---|
| 简单报告历史记录 | 可展示“系统管理”能力 | 可先用本地 JSON 文件保存 |
| 地图推荐结果缓存 | 避免重复查询 | 第一版不是必须 |
| 采集任务 checklist 状态 | 支持采集前/采集中/采集后流程 | 适合后续版本 |
| 图片搜索 MCP 场景化 | 搜索某类视觉样例图或数据集示例 | 可作为特色增强 |
| 安全命令白名单 | 收敛终端工具风险 | 如果展示终端工具调用，建议加 |

## 5. 模块设计

### 5.1 机器视觉知识问答模块

#### 功能目标

将原“恋爱知识库问答”改造成“机器视觉知识库问答”。用户可以围绕机器视觉概念、算法路线、实验步骤和论文相关内容提问。

#### 知识库内容建议

`src/main/resources/document/` 下的恋爱文档应替换为机器视觉相关文档，例如：

- 机器视觉基础知识.md
- 目标检测常见问题.md
- 图像分割常见问题.md
- 单目深度估计常见问题.md
- 三维重建与 3DGS 常见问题.md
- 无人驾驶视觉感知场景说明.md
- 数据采集与标注规范.md
- 机器视觉实验流程指南.md
- 毕业论文开题报告摘要.md
- 论文实验整体完成流程.md

#### 典型问题

- 目标检测和图像分割有什么区别？
- SAM 适合做什么类型的标注辅助？
- 单目深度估计为什么存在尺度不确定性？
- 机器视觉项目采集数据时应该注意什么？
- 无人驾驶视觉感知为什么重视十字路口、路沿和反光路面？
- 视觉实验中 PSNR、SSIM、LPIPS 分别表示什么？

#### 和现有代码的映射

- `LoveApp` 改造为 `VisionQaApp` 或 `MachineVisionApp`
- `LoveAppDocumentLoader` 改造为 `VisionDocumentLoader`
- `LoveAppRagCustomAdvisorFactory` 改造为 `VisionRagAdvisorFactory`
- `LoveAppVectorStoreConfig` 改造为 `VisionVectorStoreConfig`
- `loveAppVectorStore` 改造为 `visionVectorStore`
- `/ai/love_app/chat/*` 改造为 `/ai/vision/chat/*`

#### 必须新增/调整

- **必须新增：机器视觉知识文档集**
  - 删除或归档原恋爱文档。
  - 新增机器视觉基础、数据采集、标注规范、实验流程、无人驾驶视觉感知相关 Markdown。

- **必须调整：知识库元数据规则**
  - 当前 `LoveAppDocumentLoader` 通过文件名末尾截取“状态”字段，不适合机器视觉。
  - 建议改为按目录或文件名前缀写元数据，例如：
    - `domain`: detection / segmentation / depth / reconstruction / collection
    - `taskType`: 目标检测 / 图像分割 / 深度估计 / 三维重建
    - `sceneType`: road / campus / parking / intersection / reflective-road

- **建议新增：领域问答系统 Prompt**
  - `VisionQaApp` 的系统提示词应明确其边界：机器视觉、数据采集、实验规划、论文资料辅助。
  - 对非机器视觉问题可回答但应回到专业边界。

#### 验收标准

- 系统不再出现恋爱、情感、约会等业务文案。
- 用户提问机器视觉相关问题时，回答能结合知识库内容。
- 用户提问和知识库无关的问题时，系统能说明边界，并给出一般性解释。

### 5.2 场景化数据采集辅助模块

#### 功能目标

这是系统与高德地图 MCP 深度结合的核心模块。系统根据用户的视觉任务、所在城市或具体位置，推荐适合采集的数据场景，并生成采集路线、采集点位、采集注意事项和采集计划。

该模块是让系统“自然”的关键。它不能只是让大模型调用地图后写一段建议，而应有明确的业务链路：

> 采集需求解析 -> 地图候选点检索 -> 视觉场景分类 -> 点位适配度评估 -> 采集路线组织 -> 采集计划生成 -> PDF 导出

#### 采集场景类型

系统内置机器视觉常见采集场景分类：

- 城市道路：十字路口、丁字路口、人行横道、公交站、路口转弯区域
- 校园道路：校门口、教学楼周边道路、停车区、行人密集区域
- 停车场：露天停车场、地下停车场入口、车位线区域
- 工业园区：厂区道路、仓储门口、园区十字路口、物流通道
- 路面小目标场景：路沿、减速带、井盖、碎石、落叶、散落物
- 光照复杂场景：树荫道路、夜间路灯区域、逆光道路、反光地面
- 特殊结构场景：桥下、隧道口、坡道、环岛、匝道

#### 地图 MCP 融合方式

高德地图 MCP 可以作为以下能力的底层支持：

1. **地点检索**
   - 根据“武汉理工大学附近十字路口”“附近停车场”“附近工业园区道路”等需求检索候选点。

2. **POI 场景分类**
   - 将地图返回的学校、停车场、公园、商业区、道路交叉口、工业园等 POI 映射为机器视觉采集场景。

3. **距离与可达性评估**
   - 按距离、交通便利程度、采集安全性和场景多样性对候选点排序。

4. **采集路线规划**
   - 将多个候选采集点串联成一条半日或一日采集路线。

5. **点位说明生成**
   - 为每个推荐点生成“适合采集什么数据、建议拍摄角度、注意风险、建议图片数量”等说明。

#### 必须新增：采集需求结构化解析

新增 `VisionTaskParser`，负责把用户自然语言解析成结构化任务。第一版可以用大模型结构化输出，也可以用简单规则兜底。

示例输入：

```text
我想在武汉理工大学附近采集十字路口车辆和行人数据，最好步行 2 小时内完成。
```

示例输出：

```text
taskType: 目标检测
targetObjects: 车辆、行人、交通灯
sceneTypes: 十字路口、人行横道、校园道路
location: 武汉理工大学
timeBudget: 2 小时
transportMode: 步行
```

建议代码落点：

- `cn.chengshuai.csaiagent.vision.model.VisionTask`
- `cn.chengshuai.csaiagent.vision.service.VisionTaskParser`

#### 必须新增：视觉场景分类规则

地图 MCP 返回的是 POI 或路径信息，但系统要能解释“为什么这个地方适合机器视觉采集”。因此需要新增场景分类规则。

规则示例：

| 地图/文本特征 | 视觉场景类型 | 适合任务 |
|---|---|---|
| 路口、交叉口、十字路口 | intersection | 车辆检测、行人检测、交通灯识别 |
| 停车场、停车区、地下车库入口 | parking | 车辆检测、遮挡场景、低速车辆行为 |
| 学校、校门、教学楼周边 | campus | 行人检测、校园道路、小目标采集 |
| 工业园、物流园、仓储 | industrial_park | 园区道路、无人驾驶低速场景 |
| 公园、绿道、非机动车道 | pedestrian_path | 行人、骑行者、弱交通参与者 |
| 桥下、隧道、地下通道 | low_light | 低光照、明暗变化 |

建议代码落点：

- `cn.chengshuai.csaiagent.vision.model.VisionSceneType`
- `cn.chengshuai.csaiagent.vision.service.VisionSceneClassifier`

#### 必须新增：采集点位适配度评估

新增 `CollectionSiteEvaluator`，把地图候选点转换为机器视觉采集点位说明。评分不需要复杂，第一版可以用规则打分：

- 与目标场景匹配：0-40 分
- 距离或可达性：0-20 分
- 场景多样性：0-20 分
- 采集安全性：0-20 分

输出字段：

```text
siteName: 点位名称
address: 地址
sceneTags: 场景标签
suitableTasks: 适合任务
score: 推荐分
reason: 推荐理由
captureSuggestions: 采集建议
riskTips: 风险提示
```

建议代码落点：

- `cn.chengshuai.csaiagent.vision.model.CollectionSite`
- `cn.chengshuai.csaiagent.vision.service.CollectionSiteEvaluator`

#### 必须新增：采集计划服务

新增 `CollectionPlanService`，将任务、点位和路线组织成完整采集计划。

职责：

- 接收 `VisionTask`
- 调用地图 MCP 或智能体获得候选地点
- 使用 `VisionSceneClassifier` 分类
- 使用 `CollectionSiteEvaluator` 评估
- 生成 `CollectionPlan`
- 可选调用 PDF 报告模板

建议代码落点：

- `cn.chengshuai.csaiagent.vision.model.CollectionPlan`
- `cn.chengshuai.csaiagent.vision.service.CollectionPlanService`

#### 自然化设计重点

为了不像强行加地图功能，系统回答中必须始终体现“机器视觉采集价值”：

- 不只说“这里离你近”，还要说“这里适合采集车辆、行人、交通灯和遮挡样本”。
- 不只说“推荐某停车场”，还要说“停车场入口适合采集低速车辆、转弯车辆和遮挡场景”。
- 不只说“路线从 A 到 B”，还要说“每个点位采什么、采多少、从什么角度采”。

#### 示例交互

用户：

> 我想采集十字路口的车辆和行人数据，人在武汉理工大学附近，推荐几个地方。

系统应执行：

1. 调用地图 MCP 检索附近道路交叉口或相关 POI。
2. 筛选适合机器视觉采集的点位。
3. 输出候选点列表，包括地点名称、距离、适合采集的视觉元素。
4. 生成采集建议，例如采集时段、角度、样本数量、安全注意事项。
5. 询问是否需要生成 PDF 采集计划。

系统输出示例结构：

```text
推荐采集点 1：XXX 路口
- 适合任务：车辆检测、行人检测、交通灯识别、车道线场景采集
- 采集建议：早晚高峰各采集一组，固定机位 + 缓慢移动机位结合
- 注意事项：不要站在机动车道内，避免拍摄清晰人脸和车牌

推荐采集点 2：XXX 停车场入口
- 适合任务：车辆检测、低速行驶场景、遮挡场景
- 采集建议：采集进出场车辆、转弯车辆、局部遮挡样本
```

#### 进一步增强功能

- 根据任务类型自动推荐地点：
  - 行人检测：校园门口、地铁口、商业街
  - 车辆检测：停车场、道路交叉口、园区道路
  - 路面小目标：校园道路、路沿、停车场边缘、非机动车道
  - 夜间视觉：路灯覆盖道路、停车场、校园主干道
  - 反光/积水：低洼路段、停车场、雨后路面

- 根据采集约束给出路线：
  - “我只有 2 小时”
  - “我只能步行”
  - “我想采集 3 种场景”
  - “我不想去太远的地方”

- 根据采集目标生成任务卡：
  - 点位名称
  - 采集目标
  - 建议拍摄方向
  - 建议图片/视频数量
  - 标注类别
  - 注意事项

#### 验收标准

- 用户输入地理位置和采集目标后，系统能调用地图能力给出地点建议。
- 输出内容必须包含机器视觉任务解释，而不是单纯地图推荐。
- 系统能将多个点位组织成采集路线或采集清单。
- 系统能将采集计划导出为 PDF。
- 采集建议中必须包含目标类别、建议采集角度、样本数量建议和安全隐私提示。

### 5.3 视觉实验流程规划模块

#### 功能目标

用户给出一个机器视觉任务，系统将任务拆解为数据、标注、模型、训练、评估和报告几个阶段。

#### 支持任务类型

- 目标检测实验
- 图像分割实验
- 小目标检测实验
- 单目深度估计实验
- 三维重建实验
- 无人驾驶视觉感知实验
- 数据集构建实验

#### 输出内容

系统生成的实验流程应包含：

- 任务目标
- 数据采集方案
- 数据清洗规则
- 标注类别设计
- 数据集划分方式
- 推荐模型或算法
- 训练流程
- 评价指标
- 结果整理方式
- 风险与注意事项

#### 示例

用户：

> 我想做一个校园道路小目标检测实验，应该怎么开始？

系统输出：

1. 明确检测类别：碎石、落叶、井盖、路沿、减速带等。
2. 推荐采集场景：校园主干道、停车场边缘、路沿区域、树荫道路。
3. 调用地图 MCP 推荐附近点位。
4. 生成采集计划：每个点位 100-200 张图片，覆盖不同光照和角度。
5. 给出标注规范：目标框最小尺寸、遮挡目标处理、模糊样本处理。
6. 推荐实验指标：mAP、Recall、小目标分组 AP。
7. 生成 PDF 实验计划。

#### 验收标准

- 系统能将用户的模糊目标拆成可执行步骤。
- 实验流程和机器视觉任务类型匹配。
- 地点推荐、采集方案和报告生成能在同一条任务链中衔接。

#### 建议新增：实验流程模板

为了让实验规划不像纯大模型自由发挥，建议新增 `ExperimentPlanTemplate` 或静态模板配置。

第一版支持四类模板：

1. 目标检测实验模板
2. 图像分割实验模板
3. 深度估计实验模板
4. 三维重建实验模板

每个模板包含：

- 数据采集建议
- 标注建议
- 训练输入
- 常用模型
- 常用指标
- 常见风险
- 报告章节结构

建议代码落点：

- `cn.chengshuai.csaiagent.vision.model.ExperimentPlan`
- `cn.chengshuai.csaiagent.vision.service.ExperimentPlanService`
- `cn.chengshuai.csaiagent.vision.template.ExperimentPlanTemplate`

### 5.4 工具调用与资料处理模块

#### 功能目标

将现有文件、网页、搜索、下载、终端和 PDF 工具转化为机器视觉场景下的实用工具集。

#### 工具能力映射

| 现有工具 | 机器视觉场景中的用途 |
|---|---|
| WebSearchTool | 搜索论文、模型、数据集、开源项目 |
| WebScrapingTool | 抓取论文介绍、项目 README、数据集说明 |
| ResourceDownloadTool | 下载公开资料、说明文档、示例数据 |
| FileOperationTool | 保存采集计划、实验记录、知识文档 |
| TerminalOperationTool | 生成或执行受控的实验辅助命令 |
| PDFGenerationTool | 生成采集计划、实验报告、论文资料摘要 |
| MCP Client | 调用高德地图等外部工具能力 |

#### 安全边界建议

当前终端工具能力较强，后续改造时应收敛边界：

- 默认只生成命令，不直接执行高风险命令。
- 若需要执行命令，应设置白名单。
- 禁止执行删除、格式化、系统级修改等危险命令。
- 采集数据相关命令应优先限制在项目工作目录内。

#### 必须新增：领域工具门面

现有工具都是通用工具。为了系统自然，建议新增机器视觉领域工具门面，不直接把所有通用工具暴露为产品能力。

新增门面示例：

- `VisionSearchService`
  - 内部调用 `WebSearchTool` / `WebScrapingTool`
  - 面向用户呈现为“视觉资料检索”

- `VisionReportService`
  - 内部调用 `PDFGenerationTool`
  - 面向用户呈现为“采集计划/实验计划报告生成”

- `VisionCollectionToolService`
  - 内部组织地图 MCP、场景分类、点位评估
  - 面向用户呈现为“采集地点推荐”

这样在代码结构和功能描述上，工具调用就不再像临时拼装，而是被机器视觉业务服务封装。

#### 典型工具链任务

1. 资料检索链路：
   - 搜索“YOLO 小目标检测 数据采集 注意事项”
   - 抓取相关页面摘要
   - 整理为知识卡片
   - 保存为 Markdown
   - 生成 PDF 摘要

2. 采集规划链路：
   - 用户输入位置和任务
   - 地图 MCP 检索候选点
   - Agent 评估点位适配度
   - 生成采集路线
   - 生成 PDF 采集计划

3. 实验准备链路：
   - 用户输入任务类型
   - 系统生成数据目录结构建议
   - 生成标注类别表
   - 生成实验流程 checklist
   - 输出 Markdown / PDF 文档

### 5.5 报告生成与材料沉淀模块

#### 功能目标

让 PDF 生成能力成为系统的展示亮点。系统应能将问答、采集规划、实验流程和资料整理结果导出为正式文档。

#### 报告类型

1. **数据采集计划书**
   - 任务目标
   - 推荐地点
   - 采集路线
   - 点位说明
   - 采集样本要求
   - 安全与隐私注意事项

2. **机器视觉实验计划书**
   - 实验背景
   - 数据准备
   - 标注规范
   - 模型选择
   - 训练流程
   - 评价指标
   - 预期结果

3. **论文资料摘要报告**
   - 资料来源
   - 关键概念
   - 方法对比
   - 可用于论文的表述
   - 后续阅读建议

4. **阶段性实验总结报告**
   - 已完成工作
   - 遇到问题
   - 解决方案
   - 下一步计划

#### 必须新增：报告模板构建器

当前 `PDFGenerationTool` 只负责“把文本写进 PDF”，不负责报告结构。建议新增报告模板构建器，由它生成结构化文本，再交给 PDF 工具。

建议新增：

- `CollectionPlanReportBuilder`
- `ExperimentPlanReportBuilder`
- `ResearchSummaryReportBuilder`

其中第一版最重要的是 `CollectionPlanReportBuilder`。

采集计划报告建议固定章节：

1. 采集任务概述
2. 目标类别与视觉任务
3. 推荐采集点位
4. 建议采集路线
5. 点位采集说明
6. 标注类别建议
7. 样本数量建议
8. 安全与隐私注意事项
9. 后续实验建议

建议代码落点：

- `cn.chengshuai.csaiagent.vision.report.CollectionPlanReportBuilder`
- `cn.chengshuai.csaiagent.vision.report.ExperimentPlanReportBuilder`
- `cn.chengshuai.csaiagent.vision.service.VisionReportService`

#### 和软著材料的关系

这些报告功能可以直接用于软著说明书截图和功能说明，但文档本身聚焦系统功能，不写软著申请流程。

#### 验收标准

- 用户能从一次对话中生成结构化报告。
- 报告内容与机器视觉任务相关。
- PDF 文件命名、保存路径和返回提示清晰。

### 5.6 智能体任务编排模块

#### 功能目标

将 `CsManus` 从“通用超级智能体”改造为“机器视觉实验智能体”，让它能够根据任务目标自动选择知识库、地图、搜索、文件和 PDF 工具。

#### 智能体角色设定

建议系统 Prompt 改为：

> 你是机器视觉实验智能体，擅长帮助用户完成视觉任务分析、数据采集规划、地点推荐、实验流程拆解、资料检索、指标解释和报告生成。回答应结合机器视觉实践，必要时调用地图、搜索、文件和 PDF 工具。涉及采集地点时，应优先确认城市或当前位置，并给出安全、合规、可执行的采集建议。

#### 必须调整：从通用 Manus 改成领域 Agent

现有 `CsManus` 的 Prompt 仍是“all-capable AI assistant”，且地图任务里有 date-route 等旧场景痕迹。为了自然，需要调整为领域智能体。

推荐两种实现方式：

1. **保守方案：保留类名 `CsManus`，只改 Prompt 和前端展示**
   - 改动小。
   - 但源码中 `CsManus` 和注释仍会显得偏通用。

2. **推荐方案：新增 `VisionAgent`，让 `CsManus` 作为底层能力或废弃入口**
   - 新增 `VisionAgent extends ToolCallAgent`。
   - Prompt、下一步策略、最大步数都围绕机器视觉。
   - `AiController` 新增 `/ai/vision-agent/chat`。
   - 前端 `ManusApp.vue` 改为 `VisionAgentApp.vue`。

推荐使用第二种。这样系统从代码结构上就不是“把 Manus 改个名”，而是新增了一个机器视觉领域智能体。

#### VisionAgent 工具策略

`VisionAgent` 的工具使用策略应写入 Prompt：

- 用户没有给位置时，先追问城市、学校、园区或当前位置描述。
- 用户要求推荐采集地点时，优先调用高德地图 MCP。
- 地图结果返回后，必须结合机器视觉任务解释采集价值。
- 用户要求计划书、报告、清单时，调用 PDF 工具。
- 用户要求资料调研时，调用搜索/网页抓取工具。
- 不直接暴露工具名、参数、原始返回结果。
- 不建议用户进入危险区域采集。

#### 智能体任务类型

- 视觉知识问答
- 数据采集规划
- 采集地点推荐
- 多点路线规划
- 标注规范生成
- 实验流程拆解
- 资料检索总结
- PDF 报告生成

#### 工具选择策略

- 用户问概念：优先 RAG。
- 用户问论文资料：RAG + WebSearch。
- 用户问采集地点：地图 MCP + 机器视觉场景评估。
- 用户问采集方案：地图 MCP + 采集模板 + PDF。
- 用户问实验流程：RAG + 文件生成。
- 用户要求报告：PDFGenerationTool。

#### 验收标准

- 智能体不会再以通用助手口吻泛泛回答。
- 涉及地点的问题能主动使用地图能力。
- 涉及文档沉淀的问题能主动建议生成 Markdown 或 PDF。

## 6. 前端页面规划

### 6.1 首页

首页从通用 AI Workspace 改成机器视觉系统入口。

建议标题：

> 机器视觉智能问答与采集辅助系统

建议副标题：

> 面向视觉学习、数据采集和实验规划的智能助手

入口卡片：

1. 机器视觉问答
   - 目标检测、分割、深度估计、三维重建知识问答

2. 采集地点推荐
   - 基于地图 MCP 推荐附近采集点和路线

3. 实验流程规划
   - 生成数据、标注、训练、评估流程

4. 报告生成中心
   - 导出采集计划、实验计划和阶段总结 PDF

### 6.2 机器视觉问答页

复用现有聊天页结构，但更换欢迎语和示例问题。

欢迎语示例：

```text
你好，我是机器视觉智能助手。
我可以帮你解答目标检测、图像分割、深度估计、三维重建、数据采集和实验设计相关问题。
你也可以让我根据当前位置推荐合适的数据采集地点，并生成采集计划 PDF。
```

### 6.3 采集辅助页

建议新增一个更结构化的页面，包含：

- 采集任务类型选择
- 所在城市或地点输入
- 采集时长
- 交通方式
- 希望覆盖的场景类型
- 一键生成采集点位和路线
- 一键导出 PDF

#### 必须新增：采集辅助页面

如果第一版只保留聊天页，系统演示会像普通 AI 对话应用。建议新增 `CollectionPlanner.vue`，作为系统的核心业务页面。

页面字段：

- 任务类型：目标检测 / 图像分割 / 小目标检测 / 深度估计 / 三维重建
- 目标对象：车辆、行人、交通灯、路沿、井盖、碎石、落叶等
- 所在位置：城市、学校、园区、具体地址
- 场景偏好：十字路口、停车场、校园道路、工业园、反光路面等
- 采集时长：1 小时 / 2 小时 / 半天 / 自定义
- 交通方式：步行 / 骑行 / 公共交通 / 驾车

页面输出：

- 推荐点位列表
- 每个点位的视觉采集价值
- 采集路线摘要
- 采集 checklist
- 导出 PDF 按钮

前端代码落点：

- 新增 `cs-ai-agent-fronted/src/views/CollectionPlanner.vue`
- 修改 `cs-ai-agent-fronted/src/router/index.js`
- 修改 `Home.vue` 入口卡片

后端接口落点：

- 新增 `VisionCollectionController`
- 新增 `POST /api/vision/collection/plan`

### 6.4 报告中心页

用于管理已生成报告：

- 采集计划 PDF
- 实验流程 PDF
- 资料摘要 PDF
- 阶段总结 PDF

## 7. 后端接口规划

### 7.1 问答接口

```text
GET /api/ai/vision/chat/sync
GET /api/ai/vision/chat/sse
```

用途：

- 机器视觉基础问答
- RAG 知识库问答
- 多轮对话

### 7.2 智能体接口

```text
GET /api/ai/vision-agent/chat
```

用途：

- 工具调用
- 地图 MCP
- 搜索资料
- 生成文件
- 生成 PDF

### 7.3 采集规划接口

```text
POST /api/vision/collection/plan
```

用途：

- 输入任务、位置、场景偏好
- 输出采集点位、路线、任务清单

可以先不做复杂数据库，第一版由智能体生成结构化结果即可。

#### 请求字段建议

```text
taskType: 目标检测 / 图像分割 / 小目标检测 / 深度估计 / 三维重建
targetObjects: 车辆、行人、交通灯、路沿等
location: 用户输入的位置
scenePreferences: 十字路口、停车场、校园道路等
timeBudget: 采集时长
transportMode: 步行、骑行、驾车
needPdf: 是否生成 PDF
```

#### 响应字段建议

```text
taskSummary: 任务摘要
recommendedSites: 推荐点位
routeSummary: 路线摘要
collectionChecklist: 采集清单
annotationSuggestions: 标注建议
safetyTips: 安全提示
pdfPath: 如果生成 PDF，则返回路径
```

### 7.4 报告生成接口

```text
POST /api/vision/report/pdf
```

用途：

- 根据采集计划或实验计划生成 PDF

### 7.5 新增接口优先级

第一版建议优先实现：

1. `GET /api/ai/vision/chat/sse`
2. `GET /api/ai/vision-agent/chat`
3. `POST /api/vision/collection/plan`
4. `POST /api/vision/report/pdf`

如果时间有限，可以暂缓数据库和报告中心，只要 `collection/plan` 能返回结构化采集计划并生成 PDF，就足够体现系统特色。

## 8. 数据模型建议

第一版可以先用 Java record / DTO，不强制落库。

### 8.1 VisionTask

```text
taskType: 任务类型，如目标检测、图像分割、深度估计、三维重建
targetObjects: 目标对象，如车辆、行人、路沿、小障碍物
sceneTypes: 场景类型，如十字路口、停车场、校园道路
location: 用户位置或城市
constraints: 时间、交通方式、距离范围、安全要求
```

### 8.2 CollectionSite

```text
name: 点位名称
address: 地址
distance: 距离
sceneTags: 场景标签
suitableTasks: 适合任务
captureSuggestions: 采集建议
riskTips: 风险提示
```

### 8.3 CollectionPlan

```text
title: 采集计划标题
task: 任务信息
sites: 推荐点位列表
routeSummary: 路线摘要
checklist: 采集清单
annotationGuide: 标注建议
privacyTips: 隐私与安全说明
```

### 8.4 VisionReport

```text
title: 报告标题
sections: 报告章节
sourceType: 采集计划、实验计划、资料摘要、阶段总结
createdAt: 创建时间
filePath: PDF 文件路径
```

## 9. 现有代码改造映射

| 当前文件/模块 | 建议改造方向 |
|---|---|
| `LoveApp.java` | 改为 `VisionQaApp.java`，负责机器视觉 RAG 问答 |
| `LoveAppDocumentLoader.java` | 改为 `VisionDocumentLoader.java`，加载机器视觉知识文档 |
| `LoveAppVectorStoreConfig.java` | 改为 `VisionVectorStoreConfig.java` |
| `LoveAppRagCustomAdvisorFactory.java` | 改为 `VisionRagAdvisorFactory.java` |
| `AiController.java` | 增加 `/vision` 和 `/vision-agent` 路由 |
| `CsManus.java` | 改 Prompt 为机器视觉实验智能体 |
| `ToolRegistration.java` | 保留现有工具，后续增加安全白名单 |
| `mcp-servers.json` | 保留高德地图 MCP，并在 Prompt 中明确采集地点任务优先调用 |
| `Home.vue` | 改为机器视觉系统首页 |
| `LoveApp.vue` | 改为机器视觉问答页 |
| `ManusApp.vue` | 改为视觉实验智能体页 |
| `DebugPage.vue` | 可保留为工具调试页 |

### 9.1 必须新增的代码目录

建议新增以下包，让系统从代码结构上具有机器视觉业务归属：

```text
src/main/java/cn/chengshuai/csaiagent/vision/
  agent/
    VisionAgent.java
  controller/
    VisionCollectionController.java
    VisionReportController.java
  model/
    VisionTask.java
    VisionSceneType.java
    CollectionSite.java
    CollectionPlan.java
    ExperimentPlan.java
    VisionReport.java
  service/
    VisionTaskParser.java
    VisionSceneClassifier.java
    CollectionSiteEvaluator.java
    CollectionPlanService.java
    ExperimentPlanService.java
    VisionReportService.java
  report/
    CollectionPlanReportBuilder.java
    ExperimentPlanReportBuilder.java
```

最小必要集合：

```text
VisionAgent.java
VisionTask.java
CollectionSite.java
CollectionPlan.java
VisionTaskParser.java
VisionSceneClassifier.java
CollectionSiteEvaluator.java
CollectionPlanService.java
CollectionPlanReportBuilder.java
VisionCollectionController.java
```

### 9.2 必须新增的前端文件

```text
cs-ai-agent-fronted/src/views/CollectionPlanner.vue
cs-ai-agent-fronted/src/views/VisionQaApp.vue
cs-ai-agent-fronted/src/views/VisionAgentApp.vue
```

最小方案：

- 将 `LoveApp.vue` 改造或复制为 `VisionQaApp.vue`
- 将 `ManusApp.vue` 改造或复制为 `VisionAgentApp.vue`
- 新增 `CollectionPlanner.vue`

### 9.3 必须新增的知识文档

```text
src/main/resources/document/机器视觉基础知识.md
src/main/resources/document/机器视觉数据采集规范.md
src/main/resources/document/目标检测实验指南.md
src/main/resources/document/图像分割实验指南.md
src/main/resources/document/深度估计实验指南.md
src/main/resources/document/三维重建实验指南.md
src/main/resources/document/无人驾驶视觉感知场景.md
src/main/resources/document/采集安全与隐私规范.md
```

如果要和毕业论文更自然关联，可以增加：

```text
src/main/resources/document/路面小目标视觉采集指南.md
src/main/resources/document/三维重建实验流程摘要.md
```

## 10. 分阶段落地计划

### 第一阶段：领域骨架建立，而不是只做去恋爱化

目标：

- 删除或替换所有恋爱主题文案。
- 将系统定位改为机器视觉智能问答与采集辅助系统。
- 将 RAG 文档替换为机器视觉知识文档。
- 将前端首页、聊天页、欢迎语改为机器视觉场景。
- 新增 `vision` 业务包和最小领域模型。
- 新增 `VisionAgent` 或至少完成 `CsManus` 的领域化改造。

完成标志：

- 页面和接口中不再出现“恋爱大师”。
- 可以通过问答获得机器视觉相关回答。
- 代码结构中出现机器视觉业务模型，而不只是旧类改 Prompt。

### 第二阶段：地图 MCP 与采集辅助融合

目标：

- 改造智能体 Prompt，使其在采集地点问题中主动调用地图 MCP。
- 设计采集场景分类和地点推荐规则。
- 支持根据用户位置推荐采集点位。
- 支持生成采集路线和点位说明。
- 新增 `VisionTaskParser`、`VisionSceneClassifier`、`CollectionSiteEvaluator`、`CollectionPlanService`。

完成标志：

- 用户询问“我在某地附近采集某类视觉数据”时，系统能给出地图点位 + 机器视觉采集建议。
- 系统能返回结构化 `CollectionPlan`，而不是只返回一段聊天文本。

### 第三阶段：PDF 报告生成能力场景化

目标：

- 将 PDF 工具包装为采集计划、实验计划、资料摘要和阶段总结报告生成能力。
- 智能体能在完成采集规划后主动询问是否生成 PDF。
- PDF 内容采用固定结构，避免纯聊天文本堆叠。
- 新增 `CollectionPlanReportBuilder` 和 `VisionReportService`。

完成标志：

- 可以生成一份完整的机器视觉数据采集计划 PDF。
- PDF 至少包含任务目标、推荐点位、路线摘要、采集清单、标注建议、安全提示。

### 第四阶段：实验流程规划能力

目标：

- 支持目标检测、图像分割、深度估计、三维重建等任务流程规划。
- 能输出数据、标注、训练、评估、报告的完整步骤。
- 能根据任务类型推荐指标和注意事项。
- 新增实验流程模板，减少纯大模型自由发挥。

完成标志：

- 用户输入一个视觉任务后，系统能输出可执行实验流程。

### 第五阶段：前端结构化采集辅助页

目标：

- 新增采集辅助页面。
- 用户可以填写任务类型、位置、场景偏好、采集时长。
- 页面展示推荐点位、路线和任务清单。
- 支持一键生成 PDF。

完成标志：

- 软著演示时可以不依赖纯聊天，也能通过页面展示系统功能。

### 第六阶段：可选的报告中心与历史记录

目标：

- 保存已生成报告的文件路径和标题。
- 前端展示最近生成的采集计划和实验计划。
- 支持重新打开或下载 PDF。

完成标志：

- 用户能在报告中心看到历史 PDF 报告。
- 第一版可以使用本地 JSON 或文件目录扫描，不强制数据库。

## 11. 非目标

第一版不建议做以下内容：

- 不实现真实视觉模型训练平台。
- 不直接集成 3DGS/CUDA 训练。
- 不做复杂 MLOps 系统。
- 不做用户权限、多租户和企业级任务调度。
- 不做真实地图可视化大屏，除非后续有时间。
- 不承诺自动采集数据，只做采集辅助规划。
- 不处理敏感地理定位自动获取，位置由用户输入或明确授权。

## 12. 风险与注意事项

### 12.1 地图推荐风险

系统推荐采集地点时必须提示：

- 遵守交通规则。
- 不进入机动车道。
- 不进入禁止拍摄区域。
- 避免拍摄清晰人脸、车牌和隐私信息。
- 校园、园区、商场等地点需要遵守管理规定。

### 12.2 工具调用风险

终端工具能力较强，应避免让大模型自由执行危险命令。后续建议：

- 第一版以“生成命令”为主。
- 执行命令需要用户确认。
- 增加命令白名单。
- 屏蔽删除、格式化、系统修改类命令。

### 12.3 系统边界风险

系统应定位为机器视觉辅助系统，而不是视觉算法本体。表达时应避免：

- “本系统实现了自动驾驶三维重建核心算法”
- “本系统自动完成高精度视觉模型训练”
- “本系统自动采集并标注真实数据”

建议表达：

- “本系统辅助机器视觉数据采集和实验规划”
- “本系统提供视觉知识问答与采集点位推荐”
- “本系统支持实验材料和报告自动生成”

## 13. 推荐最终方案

建议采用以下系统方案：

> 机器视觉智能问答与采集辅助系统 V1.0

系统主线：

1. 以机器视觉知识问答作为基础能力。
2. 以高德地图 MCP 驱动“视觉数据采集地点推荐”作为特色能力。
3. 以智能体工具调用串联搜索、网页、文件、地图和 PDF。
4. 以 PDF 采集计划和实验报告作为可展示成果。
5. 以实验流程规划增强系统与毕业论文方向的关联。

这样改造后，系统既不会过度绑定某个具体论文算法，又能自然覆盖你的毕业论文方向、简历技术栈和现有项目能力。

## 14. 最小自然版本范围

如果只做一个能用于软著演示、又不显得强行换皮的最小版本，建议范围如下：

### 14.1 后端必须完成

1. `VisionQaApp`
   - 机器视觉系统 Prompt。
   - 机器视觉 RAG 文档。
   - `/api/ai/vision/chat/sse`。

2. `VisionAgent`
   - 领域 Prompt。
   - 地图 MCP 优先策略。
   - 搜索、网页、文件、PDF 工具策略。
   - `/api/ai/vision-agent/chat`。

3. `CollectionPlanService`
   - 解析采集任务。
   - 组织采集场景分类。
   - 评估地图候选点。
   - 生成结构化采集计划。

4. `CollectionPlanReportBuilder`
   - 生成采集计划 PDF 文本结构。
   - 调用 PDF 工具导出。

### 14.2 前端必须完成

1. 首页改为机器视觉系统。
2. 机器视觉问答页。
3. 视觉智能体页。
4. 采集辅助页。

### 14.3 文档必须完成

1. 至少 6 篇机器视觉知识库 Markdown。
2. 至少 1 篇采集安全与隐私规范。
3. 至少 1 篇和毕业论文相关的视觉感知/三维重建摘要文档。

### 14.4 演示闭环

最小演示路径：

1. 用户进入首页。
2. 打开“采集地点推荐”。
3. 输入：“我在武汉理工大学附近，想采集十字路口车辆和行人数据，2 小时内步行完成。”
4. 系统调用地图 MCP 获取候选点。
5. 系统生成点位推荐、路线摘要、采集 checklist、标注建议。
6. 用户点击生成 PDF。
7. 系统输出采集计划 PDF。
8. 用户进入机器视觉问答页，询问“为什么十字路口适合车辆和行人检测数据采集？”
9. 系统结合知识库回答。

该闭环能证明系统不是普通聊天助手，而是围绕机器视觉数据采集和实验辅助设计的专业系统。
