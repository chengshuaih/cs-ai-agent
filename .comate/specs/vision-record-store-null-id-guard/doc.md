# 采集辅助规划空 ID 脏数据 500 修复设计

## 背景与问题分类

用户在采集辅助规划生成采集计划时再次遇到失败。终端日志显示：

```text
VisionCollectionController       : 生成采集计划失败
java.lang.NullPointerException: Cannot invoke "String.equals(Object)" because the return value of "cn.chengshuai.csaiagent.vision.model.CollectionPlan.id()" is null
    at cn.chengshuai.csaiagent.vision.service.VisionRecordStore.savePlan(VisionRecordStore.java:81)
    at cn.chengshuai.csaiagent.vision.service.CollectionPlanService.plan(CollectionPlanService.java:72)
    at cn.chengshuai.csaiagent.controller.VisionCollectionController.plan(VisionCollectionController.java:40)
```

该问题分类为：**后端本地 JSON 持久化脏数据兼容缺陷 / 采集计划空 ID 防护缺失导致的 500**。

## 当前上下文

上一次已修复项目工作区创建项目 500：

- 规格目录：`/Users/chengshuai/Work/private/cs-ai-agent/.comate/specs/vision-project-null-id-guard/`
- 已修复 `VisionProject` 相关路径：`saveProject`、`listProjects`、`findProject`
- 已将 `VisionRecordStore` 通用 JSON 读写从 Hutool JSON 切换为 Jackson `ObjectMapper`，避免 Java record 被序列化为 `{}`。

但当前 `VisionRecordStore` 里其他数据类型仍然有同类空 ID 风险：

- `savePlan` / `findPlan`
- `saveExperiment` / `findExperiment`
- `findReport`

其中本次实际触发的是 `savePlan`。

## 根因分析

### 直接根因

当前采集计划持久化文件存在脏数据：

- `/Users/chengshuai/Work/private/cs-ai-agent/tmp/vision/plans.json:1`

```json
[{}]
```

`CollectionPlan` 是 Java record，没有构造校验：

- `/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/vision/model/CollectionPlan.java:18`

```java
public record CollectionPlan(
        String id,
        String title,
        VisionTask task,
        List<CollectionSite> sites,
        String routeSummary,
        List<String> checklist,
        List<String> annotationGuide,
        List<String> privacyTips,
        String pdfPath
) {
}
```

`VisionRecordStore.savePlan()` 当前写法为：

- `/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/vision/service/VisionRecordStore.java:78`

```java
public void savePlan(CollectionPlan plan) {
    synchronized (lock) {
        List<CollectionPlan> list = listPlans();
        list.removeIf(p -> p.id().equals(plan.id()));
        list.add(plan);
        writeList(PLANS_FILE, list);
    }
}
```

当 `listPlans()` 读取 `[{}]` 后，会得到 `id == null` 的 `CollectionPlan`。随后执行 `p.id().equals(plan.id())` 时触发 NPE。

### 数据流路径

```text
POST /api/vision/collection/plan
  -> VisionCollectionController.plan
  -> CollectionPlanService.plan
  -> 生成新的 CollectionPlan（新 plan.id 非空）
  -> VisionRecordStore.savePlan
  -> listPlans 读取 tmp/vision/plans.json: [{}]
  -> removeIf 中 p.id().equals(plan.id())
  -> NPE
  -> Controller 捕获异常并返回 INTERNAL_ERROR
```

## 技术方案

### 方案原则

- 在 `VisionRecordStore` 持久化层统一修复同类问题，不在 Controller 或 Service 层做分散兜底。
- 保持接口契约不变。
- 只过滤业务上无效的空 ID 历史脏记录，不影响有效计划/实验/报告数据。
- 延续上次修复的最小改动风格和 Jackson 读写策略。

### 推荐修复范围

必修：

- `/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/vision/service/VisionRecordStore.java`

新增/修改：

1. `savePlan(CollectionPlan plan)`：校验 `plan` 与 `plan.id()`，使用空安全去重。
2. `listPlans()`：过滤 `null`、`id == null`、空白 ID 的 `CollectionPlan`。
3. `findPlan(String id)`：对 `null` / blank id 返回 `Optional.empty()`，使用 `id.equals(p.id())`。
4. `saveExperiment(ExperimentPlan plan)` / `listExperiments()` / `findExperiment(String id)`：做同类空 ID 防护，避免实验计划后续出现同样问题。
5. `listReports()` / `findReport(String id)`：对报告记录做同类过滤和空安全查询，避免报告中心后续出现同样问题。
6. 将 ID 有效性抽成通用 `hasText(String id)` 或按类型保留 `isValidPlan/isValidExperiment/isValidReport/isValidProject`，保持可读性。

### 建议代码形态

```java
public void savePlan(CollectionPlan plan) {
    if (!isValidPlan(plan)) {
        throw new IllegalArgumentException("采集计划 id 不能为空");
    }
    synchronized (lock) {
        List<CollectionPlan> list = listPlans();
        list.removeIf(p -> plan.id().equals(p.id()));
        list.add(plan);
        writeList(PLANS_FILE, list);
    }
}

public List<CollectionPlan> listPlans() {
    List<CollectionPlan> plans = readList(PLANS_FILE, CollectionPlan.class);
    plans.removeIf(plan -> !isValidPlan(plan));
    return plans;
}

public Optional<CollectionPlan> findPlan(String id) {
    if (id == null || id.isBlank()) {
        return Optional.empty();
    }
    return listPlans().stream().filter(p -> id.equals(p.id())).findFirst();
}

private boolean isValidPlan(CollectionPlan plan) {
    return plan != null && plan.id() != null && !plan.id().isBlank();
}
```

同理扩展至 `ExperimentPlan`、`VisionReport`，并保留已经存在的 `VisionProject` 防护。

## 受影响文件

| 文件 | 修改类型 | 影响函数 |
| --- | --- | --- |
| `/Users/chengshuai/Work/private/cs-ai-agent/src/main/java/cn/chengshuai/csaiagent/vision/service/VisionRecordStore.java` | 修改 | `savePlan`、`listPlans`、`findPlan`、`saveExperiment`、`listExperiments`、`findExperiment`、`listReports`、`findReport`，新增/调整有效性判断方法 |
| `/Users/chengshuai/Work/private/cs-ai-agent/src/test/java/cn/chengshuai/csaiagent/vision/service/VisionRecordStoreTest.java` | 修改 | 增加 `plans.json`、`experiments.json`、`records.json` 脏数据回归测试 |

## 边界条件与异常处理

1. `plans.json` 不存在或为空：保持返回空列表。
2. `plans.json` 为 `[{}]`：过滤无效计划，生成新采集计划不再 NPE。
3. `findPlan(null)` 或 `findPlan("")`：返回 `Optional.empty()`。
4. `savePlan(null)` 或 `savePlan(id 为空)`：抛出明确 `IllegalArgumentException`，暴露内部调用错误。
5. `experiments.json`、`records.json` 中存在 `{}`：后续对应列表/查找/保存路径也不应 NPE。
6. 有效历史数据：保持原有 JSON 结构与接口响应结构。

## 验证方式

### 自动化测试

新增或扩展 `VisionRecordStoreTest`：

1. `plans.json` 为 `[{}]` 时：
   - `savePlan(validPlan)` 不抛异常。
   - `listPlans()` 仅返回有效计划。
   - `findPlan(null/blank/missing)` 返回 empty。
2. `experiments.json` 为 `[{}]` 时：
   - `saveExperiment(validExperiment)` 不抛异常。
   - `listExperiments()` 仅返回有效实验计划。
   - `findExperiment(null/blank/missing)` 返回 empty。
3. `records.json` 为 `[{}]` 时：
   - `listReports()` 不返回空 ID 报告。
   - `findReport(null/blank/missing)` 返回 empty。

运行命令：

```bash
JAVA_HOME="/Users/chengshuai/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home" ./mvnw -Dtest=VisionRecordStoreTest test
```

后端全量测试：

```bash
JAVA_HOME="/Users/chengshuai/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home" ./mvnw test
```

### 手工验证

1. 保留或构造：

```json
// tmp/vision/plans.json
[{}]
```

2. 触发采集辅助规划生成采集计划。
3. 预期：接口不再返回 `生成采集计划失败`，后端不再出现 `CollectionPlan.id()` NPE。
4. 再次查看 `plans.json`，预期无效 `{}` 不再参与业务列表，成功保存的新计划具有非空 `id`。

## 预期结果

- 采集辅助规划不再因为 `plans.json` 中的 `[{}]` 返回失败。
- `VisionRecordStore` 对项目、采集计划、实验计划、报告记录的空 ID 脏数据处理一致。
- 后续类似 `ExperimentPlan.id()`、`VisionReport.id()` 空指针风险同步降低。
- 自动化测试覆盖同类持久化脏数据场景。
