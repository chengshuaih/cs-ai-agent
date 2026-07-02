# 采集项目工作区创建项目 500 修复总结

## 问题概述

采集项目工作区点击创建项目时，前端显示：

```text
Request failed with status code 500
```

后端日志显示 `VisionProject.id()` 为 `null` 时调用 `equals` 触发 NPE。堆栈指向：

- `VisionRecordStore.saveProject`
- `VisionRecordStore.findProject`
- `VisionProjectService.create/detail`
- `VisionProjectController.create/detail`

## 根因

本地持久化文件 `tmp/vision/projects.json` 中存在脏数据：

```json
[{}]
```

旧实现使用 Hutool JSON 读写 Java record，`{}` 会被反序列化成 `id == null` 的 `VisionProject`。随后项目创建或查询详情时执行：

```java
p.id().equals(project.id())
p.id().equals(id)
```

因此遇到空 ID 历史记录会直接抛出 NPE，接口返回 HTTP 500。

## 完成任务

- [x] 修复 `VisionRecordStore` 项目持久化空 ID 防护
- [x] 补充脏 `projects.json` 场景回归测试
- [x] 运行后端测试并修正发现的问题

任务清单：`/Users/chengshuai/Work/private/cs-ai-agent/.comate/specs/vision-project-null-id-guard/tasks.md`

## 修改内容

### 1. 修复项目持久化读写与空 ID 防护

文件：`/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/vision/service/VisionRecordStore.java`

关键修改：

- 将通用 JSON 读写从 Hutool JSON 切换为 Jackson `ObjectMapper`，避免 Java record 序列化成 `{}`。
- `saveProject` 增加项目和项目 ID 有效性校验。
- `listProjects` 过滤 `null` 项目、`id == null` 项目和空白 ID 项目。
- `findProject` 对 `null` / blank 查询 ID 返回 `Optional.empty()`，并使用 `id.equals(p.id())` 做空安全比较。
- `listProjects` 返回可变列表，保证 `saveProject` 中 `removeIf/add` 可正常执行。

关键位置：

- `VisionRecordStore.java:39`：新增 Jackson `ObjectMapper`
- `VisionRecordStore.java:116`：`saveProject` 入参校验与空安全保存
- `VisionRecordStore.java:128`：`listProjects` 过滤无效项目
- `VisionRecordStore.java:134`：`findProject` 空查询处理
- `VisionRecordStore.java:141`：`isValidProject` 统一有效性判断
- `VisionRecordStore.java:147`：Jackson 读取 JSON 列表
- `VisionRecordStore.java:178`：Jackson 写入 JSON 列表

### 2. 补充回归测试

文件：`/Users/chengshuai/Work/private/cs-ai-agent/src/test/java/cn/chengshuai/csaiagent/vision/service/VisionRecordStoreTest.java`

测试覆盖：

- 测试前备份 `tmp/vision/projects.json`，测试后恢复，避免污染本地数据。
- 构造 `projects.json` 内容为 `[{}]` 的脏数据场景。
- 验证 `saveProject` 在脏数据存在时不抛 NPE。
- 验证 `listProjects` 不返回空 ID 项目。
- 验证 `findProject(null)`、`findProject("   ")`、`findProject("missing-project")` 返回 `Optional.empty()`。

## 执行中发现并修复的问题

### 问题 1：`stream().toList()` 返回不可变列表

第一次运行目标测试时失败：

```text
java.lang.UnsupportedOperationException
at java.base/java.util.ImmutableCollections$AbstractImmutableCollection.removeIf
at VisionRecordStore.saveProject
```

原因是 `listProjects()` 使用 `stream().toList()` 返回不可变列表，而 `saveProject()` 需要执行 `removeIf/add`。

修复：`listProjects()` 改为基于 `readList()` 返回的 `ArrayList` 原地过滤并返回可变列表。

### 问题 2：Hutool JSON 对 Java record 序列化不完整

第二次运行目标测试时失败：

```text
expected: <1> but was: <0>
```

原因是 Hutool JSON 对 Java record 写入可能生成 `{}`，导致刚保存的有效项目又被过滤为空。

修复：`readList/writeList` 切换到 Spring Boot 已有 Jackson 依赖。

## 验证结果

### 定向回归测试

命令：

```bash
JAVA_HOME="/Users/chengshuai/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home" ./mvnw -Dtest=VisionRecordStoreTest test
```

结果：

```text
BUILD SUCCESS
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

### 后端全量测试

命令：

```bash
JAVA_HOME="/Users/chengshuai/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home" ./mvnw test
```

结果：

```text
BUILD SUCCESS
Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
```

说明：当前 shell 默认 Java 为 11，项目要求 Java 17，因此验证命令显式使用 JBR 17。

## 审查与 QA 结论

### 规格审查

结论：PASS

- Critical：0
- Important：0
- Minor：Task 3 勾选状态在审查时尚未更新，后续已更新。

### 代码质量审查

结论：PASS，可以合并

- Critical：0
- Important：0
- Minor：
  - JSON 整体解析失败时会重置为 `[]`，存在非阻塞数据恢复风险。
  - 单条脏记录跳过时无日志，可观测性较弱。
  - 测试触碰真实 `tmp/vision/projects.json`，但有备份恢复机制。
  - 未覆盖 `findProject("project-1")` 正向命中，非阻塞。

### 独立 QA Evaluator

结论：PASS

- Spec Fit：通过
- Runtime Behavior：通过
- Evidence Quality：充分
- Regression Risk：低到中低
- Product UX：正向改善
- Failure Attribution：none

## 预期效果

修复后：

- `projects.json` 中即使存在 `[{}]` 这类历史脏数据，创建项目也不会再返回 500。
- 项目列表不会返回空 ID 项目。
- 项目详情查询不会因历史空 ID 记录抛出 NPE。
- 新项目会被正确序列化为包含 record 字段的 JSON 对象。
- 下一次成功保存项目时，无效项目记录会被过滤，不再继续参与业务逻辑。

## 后续可选改进

以下不是本次修复的阻塞项：

1. JSON 整体解析失败时，先备份损坏文件再重置为 `[]`。
2. 单条脏数据跳过时增加 warn 日志，提升排障可观测性。
3. 为 `VisionRecordStore` 支持注入 base dir，使测试可使用 `@TempDir` 完全隔离本地 `tmp` 目录。
4. 补充 `findProject("project-1")` 正向命中断言。
