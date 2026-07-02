# VisionRecordStore 空 ID 脏数据统一防护修复总结

## 问题背景

采集辅助规划生成采集计划时，后端接口返回 500。终端日志显示异常发生在 `VisionRecordStore.savePlan`：

```text
java.lang.NullPointerException: Cannot invoke "String.equals(Object)" because the return value of "cn.chengshuai.csaiagent.vision.model.CollectionPlan.id()" is null
```

直接原因是 `tmp/vision/plans.json` 中存在 `[{}]` 形式的历史脏数据，反序列化为 `CollectionPlan` 后 `id()` 为 `null`，旧逻辑在保存新计划时执行 `p.id().equals(plan.id())`，导致对 `null` 调用 `equals`。

## 修改内容

### 1. 采集计划空 ID 防护

在 `VisionRecordStore` 中补齐采集计划持久化的空安全处理：

- `savePlan`：校验入参和 `plan.id()`，拒绝写入无效采集计划。
- `listPlans`：读取后过滤 `null` 计划、`id == null` 计划和空白 ID 计划。
- `findPlan`：对查询 ID 做空值/空白判断，并使用 `id.equals(plan.id())` 的空安全比较方式。
- 新增 `isValidPlan` 作为统一有效性判断。

### 2. 实验计划与报告记录同类风险防护

为避免同类 `[{}]` 脏数据继续在其他记录类型中触发空指针，补齐：

- `saveExperiment` / `listExperiments` / `findExperiment`
- `saveReport` / `listReports` / `findReport`
- `isValidExperiment`
- `isValidReport`

项目记录此前已具备同类防护，本次保持一致的空 ID 过滤与保存校验策略。

### 3. 回归测试扩展

扩展 `VisionRecordStoreTest`，覆盖四类本地 JSON 数据文件：

- `projects.json`
- `plans.json`
- `experiments.json`
- `records.json`

测试通过写入 `[{}]` 脏数据，验证列表读取会过滤无效记录，保存有效记录不会触发 NPE，查询空白或不存在 ID 返回 `Optional.empty()`。

## 验证结果

### 定向测试

```bash
JAVA_HOME="/Users/chengshuai/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home" ./mvnw -Dtest=VisionRecordStoreTest test
```

结果：

```text
BUILD SUCCESS
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```

### 后端全量测试

```bash
JAVA_HOME="/Users/chengshuai/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home" ./mvnw test
```

结果：

```text
BUILD SUCCESS
Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
```

## 审查结果

- 规格符合性审查：PASS
- 代码质量审查：PASS
- 最终 QA 评估：PASS

代码质量审查中仅发现一个 Minor：测试运行 `store.init()` 后可能留下 `tmp/vision/reports/` 空目录。该目录不影响四个 JSON 数据文件的备份恢复，不影响运行时行为和本次修复目标。

## 最终结果

采集辅助规划失败的根因已修复。`plans.json`、`experiments.json`、`records.json`、`projects.json` 中即使存在 `[{}]` 形式的空 ID 脏数据，也不会再导致 `VisionRecordStore` 保存、列表读取或查询流程触发空指针异常。后端定向测试和全量测试均通过。