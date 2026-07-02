# VisionRecordStore 空 ID 脏数据统一防护任务计划

- [✓] Task 1: 修复采集计划持久化空 ID 防护
    - 1.1: 在 `savePlan` 中校验入参计划和计划 ID，避免内部写入无效采集计划
    - 1.2: 在 `listPlans` 中过滤 `null` 计划、`id == null` 计划和空白 ID 计划
    - 1.3: 在 `findPlan` 中处理空白查询 ID，并使用空安全比较
    - 1.4: 新增或复用私有采集计划有效性判断方法

- [✓] Task 2: 补齐实验计划与报告记录空 ID 防护
    - 2.1: 在 `saveExperiment` 中校验入参实验计划和实验计划 ID
    - 2.2: 在 `listExperiments` 中过滤空 ID 实验计划
    - 2.3: 在 `findExperiment` 中处理空白查询 ID，并使用空安全比较
    - 2.4: 在 `listReports` 中过滤空 ID 报告记录
    - 2.5: 在 `findReport` 中处理空白查询 ID，并使用空安全比较

- [✓] Task 3: 扩展 VisionRecordStore 脏数据回归测试
    - 3.1: 扩展测试文件备份和恢复 `plans.json`、`experiments.json`、`records.json`
    - 3.2: 构造 `plans.json` 含 `{}` 的脏数据场景，验证 `savePlan`、`listPlans`、`findPlan`
    - 3.3: 构造 `experiments.json` 含 `{}` 的脏数据场景，验证 `saveExperiment`、`listExperiments`、`findExperiment`
    - 3.4: 构造 `records.json` 含 `{}` 的脏数据场景，验证 `listReports`、`findReport`
    - 3.5: 保留已有 `projects.json` 脏数据回归覆盖

- [✓] Task 4: 运行后端测试并完成审查收口
    - 4.1: 运行 `VisionRecordStoreTest` 定向测试
    - 4.2: 如测试失败，基于失败输出修复实现或测试问题
    - 4.3: 运行后端全量测试确认没有回归
    - 4.4: 执行规格符合性审查、代码质量审查和最终 QA 评估
    - 4.5: 生成修复总结 `summary.md`
