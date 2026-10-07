# todo-management Specification

## Purpose
以独立待办保存笔试、面试、测评、材料、Offer 截止和其他事项，支持截止未知、手动完成及重开，并保护投递与面试关联，形成可靠的日常计划。

## Requirements

### Requirement: Todo records and valid associations
系统 SHALL 支持六类待办分页查询、CRUD、投递 / 类型 / 完成 / 时间筛选；关联投递不可重绑，面试可留空或清除，但 MUST 属于同一投递。
#### Scenario: Different application interview
- **WHEN** 待办关联了另一投递的面试
- **THEN** 返回 400，无不一致记录写入。
#### Scenario: Optional deadline
- **WHEN** 保存未知截止时间的待办并筛选未定时间
- **THEN** 该事项返回 null 时间且可在未定筛选找到，不计入截止窗口。

### Requirement: Completion and optimistic updates
系统 MUST 以版本保护待办修改、完成、重开和删除，完成时间由服务端设置。
#### Scenario: Complete then reopen
- **WHEN** 用户完成待办、按当前版本重复完成、随后重新打开
- **THEN** 首次保存完成时间，重复完成不改时间或版本，重开清除时间，其他字段保留。
#### Scenario: Concurrent modification
- **WHEN** 用户以旧版本编辑或完成待办
- **THEN** 返回 409，当前记录保留，前端保留输入并允许重新读取。
