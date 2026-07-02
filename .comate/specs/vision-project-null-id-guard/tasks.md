# 采集项目工作区创建项目 500 修复任务计划

- [x] Task 1: 修复 VisionRecordStore 项目持久化空 ID 防护
    - 1.1: 在 `saveProject` 中校验入参项目和项目 ID，避免内部写入无效项目
    - 1.2: 在 `listProjects` 中过滤 `null` 项目、`id == null` 项目和空白 ID 项目
    - 1.3: 在 `findProject` 中处理空白查询 ID，并使用空安全比较
    - 1.4: 新增私有项目有效性判断方法，集中表达过滤规则

- [x] Task 2: 补充脏 projects.json 场景回归测试
    - 2.1: 定位现有测试约定和本地文件目录隔离方式
    - 2.2: 构造 `projects.json` 含 `{}` 的脏数据场景
    - 2.3: 验证 `saveProject` 在脏数据存在时不抛出空指针异常
    - 2.4: 验证 `listProjects` 不返回空 ID 项目
    - 2.5: 验证 `findProject` 对空 ID 和不存在 ID 返回 `Optional.empty()`

- [x] Task 3: 运行后端测试并修正发现的问题
    - 3.1: 运行针对新增测试的 Maven 命令
    - 3.2: 如单测失败，基于失败输出修复实现或测试问题
    - 3.3: 运行项目后端测试命令确认没有回归
    - 3.4: 记录最终验证结果
