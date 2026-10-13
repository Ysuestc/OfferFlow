# Status

- Change: compact-todo-calendar-hours
- 当前阶段: ARCHIVED
- 授权: 2026-10-13 用户要求日历同屏至少 09:00–20:00，仅日历显示调整；沿用 change 一次推送授权。
- 设计确认: 无新业务规则，密集时段显式展开是固定行高的必要适配。
- 审查方式: SELF_REVIEW，按仓库用户约定顺序承担角色。
- 阻断项: 无。
- 最近证据: 两电脑尺寸日 / 周 09–20 可见及 7 日历测试通过，最终生产 Jar 一致，本机 8082 恢复。

| 日期（Asia/Shanghai） | 转换 | 证据 |
|---|---|---|
| 2026-10-13 | INIT → DESIGNING → REVIEWING_SPEC | 小范围日历密度修复，制品生成 |
| 2026-10-13 | REVIEWING_SPEC → IMPLEMENTING | SELF_REVIEW PASS，用户已授权此显示修复 |
| 2026-10-13 | IMPLEMENTING → REVIEWING_CODE | 前端 / 生产打包通过，7 日历检查通过，两个尺寸日 / 周矩形范围符合 09–20 |
| 2026-10-13 | REVIEWING_CODE → VERIFYING | SELF_REVIEW PASS，继续本机恢复与最终制品检查 |
| 2026-10-13 | VERIFYING → DONE | 全部任务及验收完成，个人 local 正常恢复；手工 OpenSpec 完整需求检查通过 |
| 2026-10-13 | DONE → ARCHIVED | 完整替换日历显示需求，其他需求保留，目录边界核实后归档 |
