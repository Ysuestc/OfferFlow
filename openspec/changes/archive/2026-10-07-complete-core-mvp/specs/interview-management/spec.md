# Purpose
为每次面试独立保存轮次、时间、形式、问题、回答、复盘和结果，在投递档案内提供真实可操作的记录与并发保护，支持未知时间和手动阶段管理。

## ADDED Requirements
### Requirement: Independent interview records
系统 SHALL 支持面试分页查询、新建、读取、完整更新和删除，关联已存在的投递，保存完整面试内容；投递关联创建后 MUST 不可重绑。
#### Scenario: Record and clear a review
- **WHEN** 用户保存面试并随后清空可选回答或时间
- **THEN** 完整内容可重新读取，清空值保持 null，不推测时间或招聘阶段。
#### Scenario: Invalid association
- **WHEN** 新建面试引用不存在的投递
- **THEN** 返回 404，面试记录不落库。

### Requirement: Interview concurrency and reference protection
系统 MUST 使用版本条件保护更新和删除，并拒绝删除被待办引用的面试。
#### Scenario: Stale editor
- **WHEN** 另一窗口已保存同一面试而用户提交旧版本
- **THEN** 返回 409，已保存内容保留，前端旧输入仍可阅读和处理。
#### Scenario: Referenced interview
- **WHEN** 删除仍被待办关联的面试
- **THEN** 返回 409，无记录被级联删除；解除关联后可按当前版本删除。
