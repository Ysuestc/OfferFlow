## ADDED Requirements

### Requirement: Query a bounded calendar interval
系统 SHALL 在现有待办分页接口支持成对可选 from / to 时刻，按 dueAt >= from 且 dueAt < to 筛选，并保留原有过滤和分页规则。
#### Scenario: Range boundary and pagination
- **WHEN** 用户查询一段有效时区时刻区间并逐页读取
- **THEN** 起点事项包含，终点和时间未知事项排除；总数与列表筛选一致，省略区间仍保留原列表行为。
#### Scenario: Invalid range
- **WHEN** 只提供一个边界、起点不早于终点、跨度超过 62 天或时间非法
- **THEN** 返回安全 400，不改变记录。

### Requirement: Inspect todos in a desktop calendar
系统 SHALL 在待办主页面提供列表与日历切换，日 / 周 / 月展示、日期导航与筛选，日历从完整区间分页结果展示时间点而不虚构持续时长。
#### Scenario: Navigate local dates
- **WHEN** 用户切换日 / 周 / 月、前后翻页、跳转或返回今天
- **THEN** 以电脑本机时区显示对应日期，周从星期一开始，月显示完整相邻周；午夜与同小时多项均可见或可展开。
#### Scenario: Filter and recover
- **WHEN** 用户筛选类型、完成状态、时间或关键词，或读取失败、快速连续翻页
- **THEN** 日历遵循筛选，显示无时间未完成事项，失败可重试，旧请求不会覆盖新日期的数据；多页结果不会静默漏掉尾项。

### Requirement: Operate calendar todos through existing records
系统 SHALL 复用现有待办录入、编辑、版本保护、完成 / 重开与删除，日历操作不生成额外记录类型。
#### Scenario: Create from a date or hour
- **WHEN** 用户点击日期或小时新增待办
- **THEN** 录入表单预填所选本机时刻，保存后事项可从日历和列表查询；仍必须选择现有投递档案。
#### Scenario: Inspect and maintain
- **WHEN** 用户点击日历事项查看、编辑、完成、重开、删除或打开投递档案
- **THEN** 实际待办数据和版本规则生效，日历刷新，既有列表和投递档案内嵌操作不受影响。
