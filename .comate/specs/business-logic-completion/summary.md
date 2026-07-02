# 业务逻辑补全 - 完成总结

## 实现概览

本次共完成 6 个任务，覆盖后端新增接口、前端功能修复、用户认证系统和知识库管理四大方向，使项目主流程完整闭环。

---

## 变更清单

### 后端（Java / Spring Boot）

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `VisionCollectionController.java` | 修改 | 新增 `GET /vision/collection/plan/list` 接口 |
| `VisionExperimentController.java` | 修改 | 新增 `GET /vision/experiment/plan/list` 接口 |
| `auth/UserInfo.java` | 新增 | 用户信息 record（id, username, passwordHash, createdAt） |
| `auth/UserStore.java` | 新增 | 用户 JSON 持久化 + 内存 token 缓存 |
| `controller/AuthController.java` | 新增 | 注册/登录/获取用户/退出接口（`/auth/**`） |
| `controller/KnowledgeController.java` | 新增 | 知识库文档上传和列表接口（`/vision/knowledge/**`） |

### 前端（Vue 3）

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `ProjectWorkspace.vue` | 修改 | 报告列表改为可点击下载链接 |
| `ReportCenter.vue` | 修改 | 并发加载项目列表，"所属项目"列显示项目名称 |
| `LoginPage.vue` | 新增 | 登录/注册页面，token 存 localStorage |
| `KnowledgePage.vue` | 新增 | 知识库文档上传与已入库文档展示页 |
| `router/index.js` | 修改 | 新增 `/login`、`/knowledge` 路由，全局路由守卫（未登录跳转登录页） |
| `utils/api.js` | 修改 | 请求拦截器自动附加 token，响应拦截器处理 401 |
| `Home.vue` | 修改 | 顶部显示用户名和退出按钮，新增知识库管理入口卡片 |

---

## 主流程验证路径

```
1. 访问任意页面 → 未登录 → 自动跳转 /login
2. 注册账号 → 登录 → 返回首页（显示用户名）
3. 首页 → 采集工作台 → 采集辅助规划 → 提交任务 → 生成采集计划
4. 在项目工作区创建项目 → 规划时指定项目 ID → 项目详情中查看关联计划
5. 项目详情中点击"生成报告" → 报告中心显示项目名（非 ID）+ 可下载链接
6. 首页 → 知识库管理 → 上传机器视觉文档 → 对话助手中 RAG 问答有效回答
```

---

## 技术说明

- 密码加密：使用 `cn.hutool.crypto.digest.BCrypt`（`hashpw` / `checkpw`），无需额外引入 Spring Security
- Token：UUID 存内存 Map，软著展示场景下重启失效可接受
- 知识库上传：复用已有 `VectorStore` bean（`SimpleVectorStore`），新文档即时可检索
- 后端编译验证通过（Java 17 + Maven Wrapper）
