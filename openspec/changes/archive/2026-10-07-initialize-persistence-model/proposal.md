# Proposal

## Why

已交付的工程能编译和提供 HTTP 基础合同，但没有数据库配置、迁移或持久化模型。公司、岗位、投递历史和面试不能混放在一张表中；先建立关系和真实 MySQL 验证，后续 CRUD 才有可靠基础。

## What Changes

- 添加 MyBatis-Plus、MySQL JDBC、Flyway / MySQL 支持和 Lombok。
- 建立 Company、JobPosition、Application、ApplicationStageHistory、Interview、Todo 六张表及实体 / Mapper。
- 增加 mysql 配置与 standalone 配置；默认仍可无数据库启动，mysql 模式必须连接并通过迁移校验。
- 用隔离 MySQL 实例验证迁移、关联、状态约束、日期精度、乐观锁和事务。
- 沉淀 ER 图、字段用途、运行说明及验证证据。

## Capabilities

### New Capabilities

- persistence-model：版本化数据库、规范化实体及受约束的关系。

### Modified Capabilities

- backend-foundation：明确 standalone 与 mysql 启动模式，保留 HTTP 合同。

## Success Criteria

1. 空 MySQL 数据库自动得到六张表与 Flyway 记录；再次 migrate 不重复创建；校验失败不能忽略。
2. 六个 Mapper 可以写入和读取虚构数据，正确映射枚举、中文及日期 / UTC 时刻。
3. 外键阻止孤儿记录、跨投递的面试待办关联和有子记录的父表删除；业务 CHECK 与已确认唯一性规则实际生效。
4. Application 乐观锁阻止过期版本覆盖；真实 MySQL 事务回滚当前快照和新历史，并保留原历史。
5. standalone 原有 HTTP 测试通过；mysql 的可执行 Jar 完成迁移并可访问健康接口；无效数据库连接启动失败。
6. 包含独立 MySQL 集成测试运行入口、已运行的测试报告、SELF_REVIEW、文档和 OpenSpec 检查；通过后归档并统一推送一次。

## Non-goals

本 change 不提供公司 / 岗位 CRUD、阶段推进服务、自动面试待办同步或 Dashboard 统计。状态跳跃、纠正、补录、API 幂等与统计规则在对应业务 change 定义。不引入 Redis、AI、用户体系、逻辑删除、附件或通用扩展 JSON。

## Impact

只修改持久化相关代码、配置、构建、测试及文档。默认 standalone 无数据库；启用 mysql 后配置和数据库必须有效。用户本轮“都按推荐”确认同一具体岗位单档案、多招聘批次分开记录，以及 REJECTED 仅指企业拒绝、ENDED 记录结束原因。
