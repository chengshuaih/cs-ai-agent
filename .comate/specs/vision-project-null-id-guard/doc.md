# 采集项目工作区创建项目 500 修复设计

## 背景与问题分类

用户在采集项目工作区点击“创建项目”时，前端显示：

```text
Request failed with status code 500
```

终端日志显示后端抛出空指针异常：

```text
java.lang.NullPointerException: Cannot invoke "String.equals(Object)" because the return value of "cn.chengshuai.csaiagent.vision.model.VisionProject.id()" is null
    at cn.chengshuai.csaiagent.vision.service.VisionRecordStore.lambda$6(VisionRecordStore.java:117)
    at cn.chengshuai.csaiagent.vision.service.VisionRecordStore.saveProject(VisionRecordStore.java:117)
    at cn.chengshuai.csaiagent.vision.service.VisionProjectService.create(VisionProjectService.java:44)
    at cn.chengshuai.csaiagent.controller.VisionProjectController.create(VisionProjectController.java:39)
```

同时查询项目详情也出现同类异常：

```text
at cn.chengshuai.csaiagent.vision.service.VisionRecordStore.lambda$7(VisionRecordStore.java:128)
at cn.chengshuai.csaiagent.vision.service.VisionRecordStore.findProject(VisionRecordStore.java:128)
at cn.chengshuai.csaiagent.vision.service.VisionProjectService.detail(VisionProjectService.java:115)
at cn.chengshuai.csaiagent.controller.VisionProjectController.detail(VisionProjectController.java:50)
```

该问题分类为：**后端持久化脏数据兼容缺陷 / 空值防护缺失导致的 500**。

## 当前项目上下文

- 后端是 Spring Boot 3.4.4 + Java 17 项目，配置见 `/Users/chengshuai/Work/private/cs-ai-agent/pom.xml`。
- 前端是 Vue 3 项目，采集项目工作区接口见 `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/README.md:82` 到 `/Users/chengshuai/Work/private/cs-ai-agent/cs-ai-agent-fronted/README.md:95`。
- 项目工作区后端接口入口为 `/api/vision/project`、`/api/vision/project/list`、`/api/vision/project/{id}`。
- 视觉项目数据使用本地 JSON 文件持久化，根目录为 `tmp/vision/`。

## 根因分析

### 直接根因

`VisionRecordStore` 在按项目 ID 去重或查找时，使用了如下写法：

```java
list.removeIf(p -> p.id().equals(project.id()));
return listProjects().stream().filter(p -> p.id().equals(id)).findFirst();
```

对应文件：

- `/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/vision/service/VisionRecordStore.java:114`
- `/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/vision/service/VisionRecordStore.java:127`

当前本地项目持久化文件中存在脏数据：

- `/Users/chengshuai/Work/private/cs-ai-agent/tmp/vision/projects.json:1`

```json
[{}]
```

`VisionProject` 是 Java record，没有构造校验：

- `/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/vision/model/VisionProject.java:18`

```java
public record VisionProject(
        String id,
        String name,
        String description,
        VisionTask visionTask,
        List<String> collectionPlanIds,
        List<String> experimentPlanIds,
        List<String> reportIds,
        String createdAt,
        String updatedAt
) {
}
```

因此 `{}` 可以被 Hutool 反序列化为字段全空的 `VisionProject`。当 `VisionRecordStore.saveProject()` 或 `VisionRecordStore.findProject()` 遍历到该对象时，`p.id()` 返回 `null`，调用 `p.id().equals(...)` 触发 NPE。

### 创建项目路径

1. 前端调用 `POST /vision/project`。
2. 后端进入 `/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/controller/VisionProjectController.java:34` 的 `create()`。
3. Controller 校验 name 后调用 `/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/vision/service/VisionProjectService.java:31` 的 `create()`。
4. Service 生成的新项目 ID 非空，例如 `proj-20260624170702`。
5. `recordStore.saveProject(project)` 读取历史 `projects.json`。
6. 历史列表中存在 `id == null` 的脏项目，`removeIf(p -> p.id().equals(project.id()))` 抛出 NPE。
7. Controller 未捕获，Spring 返回 HTTP 500。

### 查询详情路径

1. 前端调用 `GET /vision/project/{id}`。
2. 后端进入 `/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/controller/VisionProjectController.java:48` 的 `detail()`。
3. Service 调用 `/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/vision/service/VisionProjectService.java:114` 的 `detail()`。
4. `recordStore.findProject(id)` 遍历项目列表。
5. 遇到 `id == null` 的脏项目，`p.id().equals(id)` 抛出 NPE。
6. 原本应该返回项目详情或 404，但实际返回 HTTP 500。

## 技术方案

### 方案原则

- 保持最小改动，不修改 Controller API 契约。
- 修复持久化层对脏数据的兼容能力，避免 `{}`、缺失 `id` 的历史数据继续导致 500。
- 不引入额外依赖。
- 不删除用户有效数据，只过滤业务上无效的项目记录。

### 推荐修复点

主要修改：

- `/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/vision/service/VisionRecordStore.java`

建议改动：

1. 在 `saveProject()` 中使用空安全比较，并过滤无效项目记录。
2. 在 `listProjects()` 中对 `readList(PROJECTS_FILE, VisionProject.class)` 返回结果做有效性过滤，避免列表接口继续返回 `id == null` 的项目。
3. 在 `findProject()` 中改为 `id != null && id.equals(p.id())`，避免目标 ID 或记录 ID 为空时抛异常。
4. 可选：对 `saveProject()` 的入参做保护，如果传入项目或项目 ID 为空，直接抛出明确的 `IllegalArgumentException`，因为系统内部新建项目时 ID 应始终非空。

### 建议代码形态

```java
public void saveProject(VisionProject project) {
    if (project == null || project.id() == null || project.id().isBlank()) {
        throw new IllegalArgumentException("项目 id 不能为空");
    }
    synchronized (lock) {
        List<VisionProject> list = listProjects();
        list.removeIf(p -> project.id().equals(p.id()));
        list.add(project);
        writeList(PROJECTS_FILE, list);
    }
}

public List<VisionProject> listProjects() {
    return readList(PROJECTS_FILE, VisionProject.class).stream()
            .filter(this::isValidProject)
            .toList();
}

public Optional<VisionProject> findProject(String id) {
    if (id == null || id.isBlank()) {
        return Optional.empty();
    }
    return listProjects().stream()
            .filter(p -> id.equals(p.id()))
            .findFirst();
}

private boolean isValidProject(VisionProject project) {
    return project != null && project.id() != null && !project.id().isBlank();
}
```

该写法有两个效果：

- `projects.json` 中已有 `[{}]` 不会再参与业务逻辑。
- 下一次成功保存项目时，`writeList(PROJECTS_FILE, list)` 会将过滤后的有效列表写回文件，从而自然清理脏记录。

## 受影响文件

### 必改文件

| 文件 | 修改类型 | 影响函数 |
| --- | --- | --- |
| `/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/vision/service/VisionRecordStore.java` | 修改 | `saveProject`、`listProjects`、`findProject`，新增私有校验方法 |

### 建议新增或补充测试

根据现有测试结构，可补充一个针对 `VisionRecordStore` 或 `VisionProjectService` 的测试，验证脏数据场景。

候选测试文件：

| 文件 | 修改类型 | 验证内容 |
| --- | --- | --- |
| `/Users/chengshuai/Work/private/cs-ai-agent/src/test/java/cn/chengshuai/csaiagent/vision/service/VisionRecordStoreTest.java` | 新增 | 当 `projects.json` 含 `{}` 时，创建项目不抛 NPE，列表过滤无效项目，详情查询稳定返回 |

如果不新增专用测试文件，也可通过现有 Maven 测试和手工接口验证覆盖。但从回归稳定性看，建议新增后端单元测试。

## 边界条件与异常处理

1. `projects.json` 不存在或为空：保持当前逻辑，返回空列表。
2. `projects.json` 整体 JSON 损坏：保持当前逻辑，重置为 `[]`。
3. `projects.json` 含 `{}` 或缺少 `id` 的对象：过滤，不返回给业务层。
4. `projects.json` 含 `id` 为空字符串的对象：过滤。
5. `findProject(null)` 或 `findProject("")`：返回 `Optional.empty()`，不抛异常。
6. `saveProject(null)` 或 `saveProject(id 为空)`：抛出明确 `IllegalArgumentException`，暴露内部调用错误。
7. 有效项目数据：保持原有读写格式和接口响应结构不变。

## 数据流路径

### 修复前

```text
POST /api/vision/project
  -> VisionProjectController.create
  -> VisionProjectService.create
  -> VisionRecordStore.saveProject
  -> listProjects 读取 projects.json: [{}]
  -> removeIf 中 p.id().equals(project.id())
  -> NPE
  -> HTTP 500
```

### 修复后

```text
POST /api/vision/project
  -> VisionProjectController.create
  -> VisionProjectService.create
  -> VisionRecordStore.saveProject
  -> listProjects 读取 projects.json: [{}]
  -> isValidProject 过滤 id 为空记录
  -> removeIf 使用 project.id().equals(p.id())
  -> add 新项目
  -> writeList 写回仅包含有效项目的 projects.json
  -> HTTP 200 + 新项目数据
```

## 验证方式

### 自动验证

优先运行与后端相关的测试：

```bash
mvn test
```

如果新增了专用测试，可优先运行：

```bash
mvn -Dtest=VisionRecordStoreTest test
```

### 手工验证

1. 保留或构造 `tmp/vision/projects.json` 为：

```json
[{}]
```

2. 启动后端。
3. 在采集项目工作区点击“创建项目”。
4. 预期：接口不再返回 500，创建成功。
5. 查询项目列表。
6. 预期：列表不包含 `id == null` 的项目。
7. 打开新建项目详情。
8. 预期：详情接口返回成功，不再出现 `VisionProject.id()` NPE。

## 预期结果

- 采集项目工作区创建项目不再因历史 `{}` 脏数据返回 500。
- 项目详情接口不再因脏数据返回 500。
- 项目列表不再返回无效项目记录。
- 有效历史项目数据不受影响。
- 下一次成功保存项目时，`projects.json` 中的 `{}` 脏记录会被自然清理。
