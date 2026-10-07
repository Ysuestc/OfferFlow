# OfferFlow 项目上下文

## 状态与已授权范围

已完成并归档首个开发 change initialize-backend-foundation：Java 21 / Spring Boot 3 工程、公共 HTTP 合同、参数校验和运行验证。18 项测试及可执行 Jar HTTP 检查通过；下一阶段为数据库迁移与模型约束。

2026-10-07 已验收并归档 initialize-persistence-model：六表 V1、实体 / Mapper、MyBatis-Plus、Flyway、UTC 配置、乐观锁和隔离实例入口已实现。18 项 HTTP + 40 项真实 MySQL 测试、mysql / standalone Jar 和无效连接检查通过；MySQL 版本为 8.0.34。记录见 [数据库 change](../openspec/changes/archive/2026-10-07-initialize-persistence-model/proposal.md)。下一阶段为公司与岗位管理。

用户已授权按阶段推送到 [Ysuestc/OfferFlow](https://github.com/Ysuestc/OfferFlow)，并明确要求 **一个 change 推送 GitHub 一次**：实施、审查、验证和归档完成后统一交付，不在中间阶段推送。

确认状态：工程及数据库基础已获明确实施授权；用户本轮“都按推荐”确认下述两项业务规则。其他设计建议在对应 change 复核，不自动视为批准。

## 已确认的数据规则

- 同一具体岗位单档案，多渠道合并，记录主要渠道；不同招聘批次使用不同 JobPosition。
- REJECTED 仅指企业拒绝；主动拒绝 Offer、接受 Offer、撤回、岗位关闭使用 ENDED + end_reason。
- 六表关联、日期 / UTC 时刻、字段和静态约束已落实，见 [数据库说明](database.md)。状态推进、补录纠正、面试待办自动同步和统计接口仍在后续阶段定义。

## 用户需求

| 能力 | 范围 |
|---|---|
| 公司 | 名称、类型、官网、备注 |
| 岗位 | 公司、名称、地点、方向、JD |
| 投递 | 渠道、投递时间、当前阶段和完整阶段历史 |
| 面试 | 独立记录轮次、时间、形式、问题、回答、复盘和结果 |
| 待办 | 笔试、面试、测评、材料提交及 Offer 截止时间 |
| Dashboard | 总投递、进行中、面试中、Offer、拒绝数量与近期列表 |

阶段包括待投递、已投递、测评、笔试、一面、二面、三面、HR 面、Offer、拒绝和结束。

核心实体：Company、JobPosition、Application、ApplicationStageHistory、Interview、Todo。实体按职责分表，不把公司、岗位、面试和历史都塞进 Application。

## 技术与工程要求

- 后端：Java 21、Spring Boot 3、MyBatis-Plus、MySQL、Maven、Lombok；用户原技术栈还包含 Redis。
- RESTful API、JSON、统一响应、异常处理、参数校验；controller、service、mapper、entity、dto、vo 等分层清晰。
- 先单体 MVP，不使用微服务、MQ、Elasticsearch，不引入没有实际用途的复杂架构。
- 没有现有前端，先做后端；后续考虑 Vue 3、TypeScript、Element Plus。
- 每模块说明改动，说明新增字段和依赖用途，编写必要测试，保证可编译运行。

## 整体设计建议，尚待确认

- 单用户、单 Maven 模块；业务按 company、position、application、interview、todo、dashboard 组织。
- 投递渠道和日期属于 Application；招聘批次属于 JobPosition，不同批次保留不同 JD。
- 阶段允许跳跃；阶段纠正、历史补录与正常推进有不同语义。当前快照和历史在事务中协调。
- 安排面试与录入结果不隐式推导招聘阶段；面试和其关联待办通过明确规则同步。
- 总投递以实际投递事实计数；进行中包含面试中；当前 Offer 与累计 Offer 明确区分。
- 当前模型使用 submitted、current_stage_on、version；统计口径和阶段变更服务在业务阶段定义。
- 日期未知保留未知；投递和阶段可按日期记录，面试与待办按带时区的具体时刻记录。
- Redis 当前缺少必要用途，建议 MVP 暂不启用；首次工程设计时明确这一调整。
- 已使用 Spring Boot 3.5.16、Java 21、Maven Wrapper 3.3.4 / Maven 3.9.16、MyBatis-Plus 3.5.17、MySQL JDBC、Flyway 和 Lombok。数据库行为真实验证 MySQL 8.0.34，MySQL 8.4 LTS 尚未运行验证。

## 开发阶段

工作流基础 → 工程初始化 → 数据库迁移 → 公司与岗位 → 投递与历史 → 待办与面试 → Dashboard → MVP 联调。

每阶段保持独立验收、审查和提交；直接相关的修复继续在当前 change 内完成，最终统一推送一次。

## 未来 AI 扩展

规划包括邮件解析、进度更新、待办生成、基于 JD 和简历的面试准备、历史复盘和自然语言查询。

现在保留结构化数据与清晰业务服务；不预建 Agent 框架、向量库、邮件连接器、空扩展表或无限扩张的 JSON 字段。未来自动更新复用业务校验和事务入口。

## 环境检查快照

2026-10-06 检查时：Java 21.0.6、Maven 3.6.1、系统 Node.js 18.20.4；初始目录只有 IDE 配置。此快照是历史证据，后续操作需要重新核实环境，不能作为长期预检缓存。
