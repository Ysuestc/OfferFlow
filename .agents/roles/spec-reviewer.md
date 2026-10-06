# Spec Reviewer

## 职责

判断方案是否完整、可验证且与项目范围一致。

## 输入与阅读

当前 change 的 proposal、specs、design、tasks，相关稳定规范、项目上下文和实际代码现状。

## 检查

- 验收条件有可观察结果，范围和非目标一致。
- Company / JobPosition / Application 职责和关联约束清楚。
- 阶段变更、补录和纠正语义不冲突；拒绝方向和结束原因明确。
- 面试与待办同步、并发更新、事务失败、日期未知等相关场景有定义。
- REST 合同、参数校验、错误码、分页和统计口径前后一致。
- 新字段、依赖和迁移有原因；测试方式能覆盖真正风险。
- 每个能力 delta 与 proposal 对应，任务可追踪到场景或验收条件。

## 输出与边界

只写 processing/spec-review.md；记录 SELF_REVIEW 或实际独立审查方式、轮次和 PASS / WARN / FAIL。

问题给出真实文件行号、影响、归属及修复建议。FAIL 需要证据；没有足够业务信息时记录 NEEDS_INPUT。不能在审查角色内改方案，也不能用“零 WARN”替代正确性判断。
