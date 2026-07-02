# 业务逻辑补全设计文档

## 背景与目标

项目为**机器视觉智能问答与采集辅助系统**，主要功能链路为：
```
用户输入采集任务 → 智能规划 → 生成采集计划/实验计划 → 导出 PDF → 报告查看
```

当前核心 API 与数据层已完整，但以下业务功能缺失，导致前端页面逻辑无法闭环：

1. **报告中心**：前端 `ReportCenter.vue` 显示 `projectId` 原始字符串而非项目名称
2. **项目工作区**：报告列表只显示 `r.pdfPath` 原始路径，无法直接下载；项目创建后无法关联采集/实验计划（缺少 `listPlans`、`listExperiments` 后端接口）
3. **用户登录系统**：软著展示需要完整的登录注册流程
4. **知识库初始化**：RAG 功能需要向量数据库中有文档，目前缺少文档上传/导入 API

---

## 功能一：采集计划 & 实验计划列表接口

### 需求说明
前端 `ProjectWorkspace.vue` 中项目详情聚合已显示 `collectionPlans`、`experimentPlans`，但没有独立的列表接口供前端在独立场景下查询。

### 技术方案
新增两个 GET 接口到 `VisionCollectionController` 和新增 `VisionExperimentController`：
- `GET /vision/collection/plan/list` — 返回所有采集计划列表
- `GET /vision/experiment/plan/list` — 返回所有实验计划列表

**影响文件**：
- 新增：无（在已有 Controller 中添加方法）
- 修改：`/src/main/java/cn/chengshuai/csaiagent/controller/VisionCollectionController.java`
- 修改：`/src/main/java/cn/chengshuai/csaiagent/controller/VisionExperimentController.java`

**实现细节**：
```java
@GetMapping("/collection/plan/list")
public ApiResponse<List<CollectionPlan>> listPlans() {
    return ApiResponse.success(recordStore.listPlans());
}
```

---

## 功能二：项目工作区 - 报告下载链接修复

### 需求说明
`ProjectWorkspace.vue` 中报告列表展示 `r.pdfPath`（服务器本地路径），用户无法直接下载。需改为可点击的下载链接，与 `ReportCenter.vue` 保持一致。

### 技术方案
前端修改 `ProjectWorkspace.vue` 的报告列表渲染：将 `{{ r.pdfPath }}` 改为 `<a>` 下载链接，复用 `/vision/report/download/{id}` 接口。

**影响文件**：
- 修改：`/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/ProjectWorkspace.vue`（第 77 行附近）

---

## 功能三：报告中心 - 显示项目名称

### 需求说明
`ReportCenter.vue` 表格中"所属项目"列显示 `r.projectId`（原始 ID 字符串），用户体验差。

### 技术方案
**方案 A（前端）**：加载报告列表时同步加载项目列表，建立 `projectId → projectName` 映射表，渲染时做 lookup。

选择方案 A，无需改后端。

**影响文件**：
- 修改：`/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/src/views/ReportCenter.vue`

**实现细节**：
```js
async loadReports() {
  const [reportRes, projRes] = await Promise.all([
    api.get('/vision/report/list'),
    api.get('/vision/project/list')
  ])
  this.projects = (projRes.data?.data || []).reduce((m, p) => { m[p.id] = p.name; return m }, {})
  this.reports = reportRes.data?.data || []
}
```

---

## 功能四：用户登录/注册系统

### 需求说明
软著材料展示需要完整软件系统，登录模块是基础能力。实现简单的用户名+密码登录（不依赖数据库，使用本地 JSON 存储，与现有 `VisionRecordStore` 风格一致）。

### 技术方案

#### 后端
- 新增 `UserStore`：JSON 持久化用户信息（`tmp/vision/users.json`），密码用 BCrypt 哈希
- 新增 `AuthController`：
  - `POST /auth/register` — 注册（用户名/密码，自动生成 userId）
  - `POST /auth/login` — 登录（验证密码，返回 token）
  - `GET /auth/me` — 获取当前用户信息（通过 token 请求头）
- Token 方案：简单实现，使用 `UUID` 作为 session token，存储在内存 Map（重启失效，软著展示足够）
- 用户模型：`UserInfo(id, username, passwordHash, createdAt)`

**影响文件**：
- 新增：`/src/main/java/cn/chengshuai/csaiagent/auth/UserInfo.java`
- 新增：`/src/main/java/cn/chengshuai/csaiagent/auth/UserStore.java`
- 新增：`/src/main/java/cn/chengshuai/csaiagent/controller/AuthController.java`

#### 前端
- 新增 `LoginPage.vue`：用户名+密码表单，支持登录/注册切换
- 修改 `router/index.js`：添加 `/login` 路由，设置路由守卫（未登录跳转登录页）
- 修改 `api.js`：请求拦截器自动添加 `Authorization: Bearer {token}` 请求头
- 修改 `Home.vue`：顶部添加用户信息显示和退出按钮

**影响文件**：
- 新增：`/cs-ai-agent-fronted/src/views/LoginPage.vue`
- 修改：`/cs-ai-agent-fronted/src/router/index.js`
- 修改：`/cs-ai-agent-fronted/src/utils/api.js`
- 修改：`/cs-ai-agent-fronted/src/views/Home.vue`

---

## 功能五：知识库文档上传接口

### 需求说明
RAG 功能需要向量数据库中有文档数据，目前缺少文档导入入口，导致 `/ai/vision/chat/rag` 接口总返回无上下文的回答。

### 技术方案
新增文档管理接口：
- `POST /vision/knowledge/upload` — 接收文本内容，分块嵌入存入 PGVector
- `GET /vision/knowledge/list` — 列出已入库的文档

**影响文件**：
- 新增：`/src/main/java/cn/chengshuai/csaiagent/controller/KnowledgeController.java`
- 已有：`VectorStore`（Spring AI PGVector 已配置）

前端新增简单的知识库管理页：
- 新增：`/cs-ai-agent-fronted/src/views/KnowledgePage.vue`（文本输入 + 上传按钮 + 已上传列表）
- 修改：`/cs-ai-agent-fronted/src/router/index.js`（添加 `/knowledge` 路由）
- 修改：`/cs-ai-agent-fronted/src/views/Home.vue`（添加入口卡片）

---

## 数据流总览

```
登录 → token → 所有 API 请求携带 token
                ↓
        Home → 采集工作台 / 对话助手 / 知识库管理
                ↓
        采集规划 → POST /vision/collection/plan → CollectionPlan
                ↓
        导出 PDF → POST /vision/report/pdf → VisionReport
                ↓
        报告中心 → GET /vision/report/list → 显示项目名 + 下载链接
```

---

## 边界条件与异常处理

- 登录：用户名重复注册返回 409；密码错误返回 401；token 无效返回 401
- 知识库上传：内容为空返回 400；向量存储异常返回 500 并记录日志
- 报告下载：文件不存在返回 404（已有实现）
- 所有接口统一使用 `ApiResponse<T>` 包装

## 预期结果

完成后，系统主流程完整可演示：
1. 注册/登录
2. 新建项目
3. 输入采集任务 → 智能规划 → 导出 PDF
4. 在项目工作区查看关联计划和报告（可下载）
5. 在报告中心查看所有报告（显示项目名）
6. 上传知识库文档 → RAG 问答有效回答
