# 机器视觉智能问答与采集辅助系统 接口示例

> 配套：`technical-solution.md` §6 接口契约。本文给出完整请求/响应样例，供前后端联调与 `CollectionPlanner.vue` 对接。
> 约定：`server.servlet.context-path=/api`；问答/智能体在 `@RequestMapping("/ai")` 下，业务接口在 `@RequestMapping("/vision")` 下。

## 0. 通用约定

- 字符编码 UTF-8；POST body 为 `application/json`。
- SSE 接口 `Content-Type: text/event-stream`，逐条 `data:` 推送。
- **响应格式双轨制**：聊天/智能体接口（`/api/ai/**`）返回纯文本/流，不包装；vision 业务接口（`/api/vision/**`）统一返回 `ApiResponse<T> = { code, message, data }`。
- 统一错误响应格式（仅适用 `/api/vision/**` 业务接口）：

```json
{
  "code": 40001,
  "message": "taskType 不能为空",
  "data": null
}
```

| code | 含义 |
|---|---|
| 0 | 成功 |
| 40001 | 参数校验失败 |
| 40400 | 资源不存在（如 projectId/reportId 未找到） |
| 50000 | 服务内部错误（模型/MCP/PDF 失败） |
| 50001 | 依赖能力不可用（地图 MCP 超时，已降级估算） |

## 1. 采集规划 `POST /api/vision/collection/plan`

请求：

```json
{
  "task": {
    "taskType": "目标检测",
    "targetObjects": ["车辆", "行人", "交通灯"],
    "sceneTypes": ["十字路口", "校园道路"],
    "location": "北京市海淀区中关村",
    "timeBudget": "半天",
    "transportMode": "骑行"
  },
  "needPdf": false,
  "projectId": "proj-20260618-001"
}
```

> `projectId` 可选，不传则不归档到任何项目。**采集规划与 PDF 生成为两步流程**：本接口仅生成结构化计划并落库，响应中 `pdfPath` 恒为 `null`；`needPdf` 字段为兼容保留，当前实现不在此步生成 PDF。如需 PDF，请在拿到 `plan.id` 后调用 `POST /api/vision/report/pdf`（见 §2）。

响应（`CollectionPlan`）：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": "plan-20260618-093012",
    "title": "中关村目标检测数据采集计划",
    "task": {
      "taskType": "目标检测",
      "targetObjects": ["车辆", "行人", "交通灯"],
      "sceneTypes": ["十字路口", "校园道路"],
      "location": "北京市海淀区中关村",
      "timeBudget": "半天",
      "transportMode": "骑行"
    },
    "sites": [
      {
        "name": "中关村大街与北四环交叉口",
        "address": "北京市海淀区中关村大街",
        "distance": "1.2km",
        "sceneTags": ["十字路口", "主干道"],
        "suitableTasks": ["车辆检测", "行人检测", "交通灯识别"],
        "score": 86,
        "reason": "典型十字路口，车流人流密集，交通灯齐全，适合多目标检测采集",
        "captureSuggestions": ["早晚高峰各拍一组", "覆盖红绿灯切换全过程", "斜 45 度俯拍减少遮挡"],
        "riskTips": ["临主干道注意人身安全", "勿进入机动车道"]
      },
      {
        "name": "中关村某高校南门",
        "address": "北京市海淀区学院路",
        "distance": "2.4km",
        "sceneTags": ["校园道路", "非机动车道"],
        "suitableTasks": ["行人检测", "骑行者检测", "小目标采集"],
        "score": 78,
        "reason": "人流以行人/骑行为主，适合非机动车道与小目标样本补充",
        "captureSuggestions": ["上下课时段采集", "兼顾远近不同尺度目标"],
        "riskTips": ["校园内拍摄注意隐私，避免清晰人脸特写"]
      }
    ],
    "routeSummary": "建议骑行路线：中关村大街交叉口 → 沿学院路向北 → 高校南门，全程约 3.6km，半天可覆盖两点早晚两个时段。",
    "checklist": ["相机/手机已充电", "存储空间 ≥ 32GB", "备用电源", "记录每段采集时间地点", "天气与光照备注"],
    "annotationGuide": ["按 VOC/COCO 格式建目录", "类别：vehicle/person/traffic_light", "遮挡目标标 difficult"],
    "privacyTips": ["不采集可识别人脸特写", "不拍摄车牌清晰特写", "公共区域采集，避开私人住宅"],
    "pdfPath": null
  }
}
```

> 字段说明：`task` 内即 `VisionTask`（采集接口与 `CollectionPlan` 内字段名均为 `task`；项目工作区接口 §6 中对应字段名为 `visionTask`）。`sceneTypes` 为用户可选输入，留空时服务端按目标对象/任务类型规则推导默认场景（见 `CollectionPlanService.inferScenes`）。

> 降级说明：地图 MCP 不可用时 `distance` 标注"（估算）"，`code` 仍为 0，`message` 提示"部分指标为规则估算"。

## 2. 报告生成 `POST /api/vision/report/pdf`

请求：

```json
{
  "type": "collection_plan",
  "title": "中关村目标检测数据采集计划",
  "sourceId": "plan-20260618-093012",
  "projectId": "proj-20260618-001"
}
```

> `type` 取值：`collection_plan` / `experiment_plan` / `research_summary` / `stage_summary`。`sourceId` 指向已有采集计划或实验计划 id；研究/阶段摘要类可改传 `content` 文本字段。

响应（`VisionReport`）：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": "rpt-20260618-093530",
    "title": "中关村目标检测数据采集计划",
    "type": "collection_plan",
    "projectId": "proj-20260618-001",
    "createdAt": "2026-06-18 09:35:30",
    "pdfPath": "tmp/vision/reports/collection_plan_20260618093530.pdf"
  }
}
```

## 3. 同步问答 `GET /api/ai/vision/chat/sync`

请求：`GET /api/ai/vision/chat/sync?message=什么是目标检测的mAP指标&chatId=abc123`

响应：纯文本字符串（沿用现有 `AiController.doChatWithLoveAppSync` 直接 `return String` 的行为，**不包装** `ApiResponse`）：

```
mAP（mean Average Precision）是目标检测的核心指标……
```

## 4. 流式问答 `GET /api/ai/vision/chat/sse`

请求：`GET /api/ai/vision/chat/sse?message=图像分割和目标检测有什么区别&chatId=abc123`

响应（`text/event-stream`，逐 token）：

```
data: 图像分割
data: 与目标检测
data: 的主要区别在于……
data: [DONE]
```

> 前端用 `utils/api.js` 的 `createSSEConnection` 消费；约定结束标记 `[DONE]`（沿用现有实现）。

## 5. 视觉智能体 `GET /api/ai/vision-agent/chat`

请求：`GET /api/ai/vision-agent/chat?message=帮我在中关村规划一次半天的目标检测采集并生成PDF`

响应（SSE）：按现有 `BaseAgent.runStream` 行为，**每个 ReAct 步骤推送一条该步的文本结果**（即模型在该步的可见输出），工具调用的名称/参数等执行细节留在内部上下文与日志中，不推给前端；最后以 `[DONE]` 结束。示例：

```
data: 我先确认采集任务：目标检测，地点中关村，时长半天。
data: 已找到 2 个候选采集点并完成评估，正在整理路线与清单。
data: 采集计划已生成，并已导出 PDF：tmp/vision/reports/collection_plan_xxx.pdf
data: [DONE]
```

> 说明：上述每条 `data:` 对应一次 `step()` 的结果（源码 `BaseAgent.runStream` 中 `sseEmitter.send(formatStepResult(...))`）。文案由模型生成、步数不固定，**不存在** `[步骤N]` 这类固定前缀；达到 `maxSteps` 时会追加"执行结束：达到最大步骤"。SseEmitter 超时沿用 5 分钟。

## 6. 项目工作区

新建 `POST /api/vision/project`：

```json
{ "name": "毕设采集项目", "description": "无人驾驶视觉感知数据采集", "visionTask": { "taskType": "目标检测", "location": "北京海淀" } }
```

响应：返回创建的 `VisionProject`（含生成的 `id`、`createdAt`）。

列表 `GET /api/vision/project/list` → `data` 为 `VisionProject[]`。

详情 `GET /api/vision/project/{id}` → 聚合返回项目下采集计划、实验计划、报告：

```json
{
  "code": 0,
  "data": {
    "project": { "id": "proj-20260618-001", "name": "毕设采集项目", "...": "..." },
    "collectionPlans": [ { "id": "plan-...", "title": "..." } ],
    "experimentPlans": [ { "id": "exp-...", "taskType": "目标检测" } ],
    "reports": [ { "id": "rpt-...", "type": "collection_plan", "pdfPath": "..." } ]
  }
}
```

项目不存在时返回 `code=40400`。
