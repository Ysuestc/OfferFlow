## MODIFIED Requirements

### Requirement: Inspect todos in a desktop calendar
系统 SHALL 在待办主页面提供列表与日历切换，日 / 周 / 月展示、日期导航与筛选，日历从完整区间分页结果展示时间点而不虚构持续时长。
#### Scenario: Navigate local dates
- **WHEN** 用户切换日 / 周 / 月、前后翻页、跳转或返回今天
- **THEN** 以电脑本机时区显示对应日期，周从星期一开始，月显示完整相邻周；午夜与同小时多项均可见或可展开。
#### Scenario: Filter and recover
- **WHEN** 用户筛选类型、完成状态、时间或关键词，或读取失败、快速连续翻页
- **THEN** 日历遵循筛选，显示无时间未完成事项，失败可重试，旧请求不会覆盖新日期的数据；多页结果不会静默漏掉尾项。
#### Scenario: See a compact daytime schedule
- **WHEN** 日 / 周视图在常规电脑浏览器展开，即使同一小时有多项事项
- **THEN** 默认定位 09:00，同一日历可视区完整覆盖 09:00 到 20:00；多项用数量入口展开，24 小时均可滚动查询。
