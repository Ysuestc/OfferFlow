# Spec Review

- 方式：SELF_REVIEW；第 1 轮。
- 独立工程部分：PASS。依赖用途、独立实例、UTC 配置、两种启动模式、失败行为和测试边界可验证，已有实施授权。
- 业务约束部分：NEEDS_INPUT，不作为已批准模型实施。

| 编号 | 级别 / 归属 | 位置 | 影响与处理 |
|---|---|---|---|
| S1 | NEEDS_INPUT | design.md:24 | 同岗位多渠道档案数量决定唯一键，已向用户给出具体选项；对应表约束和测试等待输入 |
| S2 | NEEDS_INPUT | design.md:50 | 拒绝方向决定 ENDED 原因规则，已向用户给出具体选项；对应枚举与 CHECK 等待输入 |
| S3 | PASS / SPEC | specs/backend-foundation/spec.md:6 | 初次 strict 提示遗漏既有场景标题，已恢复 Check a started application，strict 退出 0 |

可先执行 tasks 1.2 的构建与模式配置、独立实例运行入口；不写或执行依赖 S1 / S2 的业务模型、迁移及业务测试。收到输入后复核完整方案，完成 tasks 1.1，再实施模型。

## 第 2 轮：完整方案复核

- SELF_REVIEW，PASS。用户“都按推荐”明确解除 S1 / S2；设计和 spec 现已定义岗位单档案唯一键、不同批次独立岗位、拒绝方向和结束原因 CHECK。
- 六个实体职责、组合外键、NULL 日期、UTC 时刻、审计字段、乐观锁及真实事务测试均有验收对应。当前阶段不实现 API / 自动同步，不将数据库事务基础声称为阶段服务。
- CASE / 尾随空格使用精确 CHECK 避免 Java 枚举回读错误；无其他待输入项。strict 退出 0，准许完整模型实施。

## 第 3 轮：事务措辞与文档复核

- SELF_REVIEW，PASS。处理 C3（spec.md:82）：明确“显式分组的 Spring 事务”保证共同提交 / 回滚，不把持久化基础误描述为业务阶段服务。原成功 / 回滚场景、验收和真实测试全部保留。
- README、database.md 说明已实现关系、约束、时间与运行模式；未实现接口和未验证平台清楚标记。schema / change strict、文档本地链接检查通过；没有新增范围或必要输入。
