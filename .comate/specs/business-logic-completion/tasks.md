# 业务逻辑补全任务计划

- [x] Task 1: 补全采集计划与实验计划列表接口
    - 1.1: 在 `VisionCollectionController` 中添加 `GET /vision/collection/plan/list` 接口，注入 `VisionRecordStore`，返回 `recordStore.listPlans()`
    - 1.2: 在 `VisionExperimentController` 中添加 `GET /vision/experiment/plan/list` 接口，返回 `recordStore.listExperiments()`

- [x] Task 2: 修复项目工作区报告下载链接
    - 2.1: 修改 `ProjectWorkspace.vue` 报告列表渲染，将 `{{ r.pdfPath }}` 替换为可点击的下载链接 `<a :href="downloadUrl(r.id)">下载 PDF</a>`
    - 2.2: 在 `ProjectWorkspace.vue` 的 `script` 中引入 `API_BASE_URL`，添加 `downloadUrl(id)` 方法（复用 ReportCenter 逻辑）

- [x] Task 3: 报告中心显示项目名称
    - 3.1: 修改 `ReportCenter.vue`，在 `data()` 中增加 `projectMap: {}` 字段
    - 3.2: 修改 `loadReports()` 方法，用 `Promise.all` 并发请求报告列表和项目列表，构建 `projectId → projectName` 映射
    - 3.3: 修改模板中"所属项目"列，渲染 `projectMap[r.projectId] || r.projectId || '未分类'`

- [x] Task 4: 后端用户登录/注册系统
    - 4.1: 新建 `UserInfo.java` record（id, username, passwordHash, createdAt），放在 `auth/` 包下
    - 4.2: 新建 `UserStore.java`，基于 `tmp/vision/users.json` 做 JSON 持久化（参考 `VisionRecordStore` 风格），提供 `save`、`findByUsername`、`findByToken` 方法；token 存储用内存 `ConcurrentHashMap<String, String>`（token → userId）
    - 4.3: 新建 `AuthController.java`，实现三个接口：
        - `POST /auth/register`：用户名查重 → BCrypt 加密密码 → 保存 → 返回用户信息
        - `POST /auth/login`：验证密码 → 生成 UUID token → 缓存 → 返回 token
        - `GET /auth/me`：从请求头 `Authorization: Bearer {token}` 解析用户信息
    - 4.4: 修改 `CorsConfig.java`，确保 `/auth/**` 路径允许跨域

- [x] Task 5: 前端登录页与路由守卫
    - 5.1: 新建 `LoginPage.vue`，包含用户名/密码输入框，登录/注册切换 tab，调用 `/auth/login` 和 `/auth/register` 接口，成功后将 token 存入 `localStorage`，跳转到首页
    - 5.2: 修改 `router/index.js`，添加 `/login` 路由，并添加全局 `beforeEach` 路由守卫：无 token 时跳转 `/login`，`/login` 路由不需要守卫
    - 5.3: 修改 `api.js` 请求拦截器，自动读取 `localStorage.getItem('token')` 并附加到 `Authorization` 请求头；响应拦截器处理 401 时清除 token 并跳转登录页
    - 5.4: 修改 `Home.vue` 顶部导航栏，添加当前用户名显示和"退出"按钮（退出时清除 localStorage token 并跳转 `/login`）

- [x] Task 6: 知识库文档上传接口与页面
    - 6.1: 新建 `KnowledgeController.java`，注入 Spring AI `VectorStore` 和 `VisionVectorStoreConfig` 中的 bean
        - `POST /vision/knowledge/upload`：接收 `{ title, content }` 请求体，构造 `Document`（带 `title`、`domain=manual` 元数据），调用 `vectorStore.add()`
        - `GET /vision/knowledge/list`：调用 `vectorStore.similaritySearch("")` 取前 50 条返回标题和 id（仅展示用）
    - 6.2: 新建 `KnowledgePage.vue`：包含标题输入框、多行文本内容输入框、上传按钮（调用 `/vision/knowledge/upload`）、已上传记录列表（调用 `/vision/knowledge/list`）
    - 6.3: 修改 `router/index.js`，添加 `/knowledge` 路由
    - 6.4: 修改 `Home.vue`，在功能入口区域添加"知识库管理"卡片，导航到 `/knowledge`
