# 机器视觉智能问答与采集辅助系统 验收与演示脚本

> 配套：`system-tasks.md` Task 12、`technical-solution.md` §9 §14。本文给出可执行的验收清单与软著演示脚本，确保"代码写完能证明完成"。

## 1. 软著演示路径（最小闭环）

| 步骤 | 操作 | 期望现象 |
|---|---|---|
| 1 | 打开首页 | 标题为机器视觉系统，四张入口卡片：智能问答 / 采集推荐 / 实验规划 / 报告中心，无恋爱字样 |
| 2 | 进入智能问答，提问"什么是目标检测的 mAP" | 流式输出专业回答，命中视觉知识库 |
| 3 | 进入采集推荐，填表提交（见 §3 输入样例） | 返回 ≥2 个采集点位 + 路线 + 清单 |
| 4 | 点击"导出 PDF" | 生成 PDF，路径在 `tmp/vision/reports/`，可打开且中文正常 |
| 5 | 进入报告中心 | 列表能看到刚生成的报告记录 |
| 6 | 进入实验规划，选"目标检测" | 输出数据/标注/训练/评估/风险流程，可导出实验计划 PDF |

录屏覆盖步骤 1→6 即为软著演示素材。

## 2. 问答测试问题清单

机器视觉域内（应正面专业回答）：
1. 目标检测和图像分割的区别？
2. mAP@0.5 和 mAP@0.5:0.95 怎么算？
3. 小目标检测有哪些难点和常用方法？
4. 深度估计常用哪些评价指标？
5. 数据采集时如何兼顾样本多样性？
6. 标注 COCO 格式需要注意什么？

边界外（应说明系统边界后给一般性回答，不答恋爱话题）：
7. 帮我追女朋友 / 约会建议 → 应回复"本系统专注机器视觉……"并礼貌引导，**不得**输出恋爱建议。

## 3. 采集规划输入样例

```json
{
  "task": {
    "taskType": "目标检测",
    "targetObjects": ["车辆", "行人", "交通灯"],
    "sceneTypes": ["十字路口"],
    "location": "北京市海淀区中关村",
    "timeBudget": "半天",
    "transportMode": "骑行"
  },
  "needPdf": false
}
```

预置一份该输入的样例输出（`tmp/vision/records.json` 含一条记录），保证演示在地图 MCP 不可用时仍可复现。

## 4. 期望输出检查项

采集规划响应：
- [ ] `sites` 长度 ≥ 2
- [ ] 每个 site 含 `score`（0-100）、`reason`、`captureSuggestions`、`riskTips`
- [ ] `routeSummary` 非空
- [ ] `checklist`、`annotationGuide`、`privacyTips` 均非空
- [ ] 采集规划接口响应中 `pdfPath` 为 `null`（PDF 为独立第二步，经 `POST /api/vision/report/pdf` 生成）
- [ ] 地图能力不可用时，距离类字段标注"（估算）"，接口不报错

实验规划响应：
- [ ] 含 `dataPrep / annotationDesign / datasetSplit / recommendedModels / trainingSteps / metrics / risks` 七项且非空
- [ ] `recommendedModels`、`metrics` 与所选 taskType 对应（检测→mAP；分割→mIoU；深度→AbsRel；重建→重投影误差）

## 5. PDF 必含章节

采集计划 PDF：
- [ ] 任务概述
- [ ] 推荐采集点位（名称/地址/距离/得分/理由）
- [ ] 路线与时段建议
- [ ] 采集清单
- [ ] 标注与隐私提示
- [ ] 中文显示正常（STSongStd-Light），无乱码

实验计划 PDF：
- [ ] 目标 / 数据准备 / 标注设计 / 数据集划分 / 推荐模型 / 训练步骤 / 评价指标 / 风险 八节齐全

## 6. 全仓残留检查（提交软著前必过）

```bash
# 业务痕迹：人工甄别 date（日期）误命中
grep -rni "love\|恋爱\|约会\|单身\|已婚" src/ cs-ai-agent-fronted/src/ src/main/resources/ \
  --include=*.java --include=*.vue --include=*.js --include=*.md --include=*.yml --include=*.json

# 明文密钥
grep -rn "sk-\|AMAP_MAPS_API_KEY\|search-api" src/main/resources/ *.json

# 历史会话产物
ls -la tmp/ | grep -i kryo
```

检查项：
- [ ] 无 `love/恋爱/约会/单身/已婚` 残留（代码/注释/日志/测试/资源/前端文案）
- [ ] Bean 名、`@Qualifier`、方法引用已全部由 `loveAppVectorStore` 改为 `visionVectorStore`
- [ ] `application-local.yml`、`application.yml`、`mcp-servers.json` 三处密钥为占位符/环境变量，无真实 key
- [ ] `tmp/` 下无旧 `.kryo` 会话与旧 `tmp/pdf` 恋爱产物
- [ ] 向量库重建后，问恋爱问题不再命中旧向量

## 7. 编译 / 启动 / 冒烟命令

```bash
# 构建/测试前置：本工程需 Java 17（默认 Java 11 会失败）
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
$JAVA_HOME/bin/java -version
# 后端编译
mvn -q clean compile

# MCP 子模块 jar（启用 MCP 前必须构建；否则 stdio client 会启动失败）
(cd cs-image-search-mcp-server && ../mvnw -q package -DskipTests)

# 后端启动（需先设置环境变量）
export DASHSCOPE_API_KEY=...   # 本地
export SEARCH_API_KEY=...
export AMAP_MAPS_API_KEY=...
mvn spring-boot:run
# 如只验收非 MCP 能力，可临时禁用 MCP client：
# mvn spring-boot:run -Dspring-boot.run.arguments='--spring.ai.mcp.client.enabled=false'

# 接口冒烟
curl "http://localhost:8123/api/ai/vision/chat/sync?message=什么是目标检测&chatId=t1"
curl -X POST "http://localhost:8123/api/vision/collection/plan" \
  -H "Content-Type: application/json" \
  -d '{"task":{"taskType":"目标检测","targetObjects":["车辆"],"location":"中关村","timeBudget":"半天"},"needPdf":false}'

# 前端启动
cd cs-ai-agent-fronted && npm install && npm run dev
```

验收通过标准：上述命令均成功，§4/§5/§6 检查项全部勾选。

## 8. CollectionPlanner.vue 页面结构（轻量）

| 区域 | 元素 | 说明 |
|---|---|---|
| 表单 | taskType | 下拉框：目标检测/图像分割/小目标检测/深度估计/三维重建 |
| 表单 | targetObjects | 多选/标签输入：车辆/行人/交通灯/路沿… |
| 表单 | sceneTypes | 多选下拉：十字路口/停车场/校园道路/工业园区/非机动车道/低光照/反光路面 |
| 表单 | location | 文本输入 |
| 表单 | timeBudget | 下拉框：2小时/半天/1天 |
| 表单 | transportMode | 下拉框：步行/骑行/驾车 |
| 表单 | 提交按钮 | 调 `POST /api/vision/collection/plan` |
| 结果 | 点位卡片 | 每卡展示：名称、地址、距离、得分（进度条/徽标）、推荐理由、采集建议、风险提示 |
| 结果 | 路线摘要 | 文本块展示 `routeSummary` |
| 结果 | 清单区 | checklist / annotationGuide / privacyTips 三组列表 |
| 结果 | 导出 PDF 按钮 | **仅当已有规划结果时显示**；点击后展示 `pdfPath` 或下载链接 |

状态处理：
- 空状态：未提交时显示引导文案与示例输入。
- 加载中：提交后按钮 loading，结果区骨架/转圈。
- 错误：接口非 0 时顶部提示 `message`；地图降级（estimated）时结果区加"部分指标为估算"提示条。
