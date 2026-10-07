# recruitment-dashboard Specification

## Purpose
以明确统计口径整合秋招概览、最近投递、最近已发生的面试与七天待办，并突出逾期和未知截止事项，使个人可在首页理解真实招聘进展和行动安排。

## Requirements

### Requirement: Defined dashboard metrics
系统 SHALL 展示历史已投递数、当前进行中、面试中、Offer、被拒数量，面试中属于进行中；接受或主动拒绝 Offer 后 MUST 按已结束而非当前 Offer 统计。
#### Scenario: Unsubmitted and ended applications
- **WHEN** 存在待投递档案、已投递后结束和当前 Offer 档案
- **THEN** 待投递不计总投递数，已结束仍计历史投递事实，只有当前 Offer 计入 Offer 数。

### Requirement: Bounded recent and deadline lists
系统 MUST 使用同一次服务端时刻返回最近投递、最近已发生面试、未完成七天待办、逾期待办及对应待办总数，列表最多各五条，未知时间不猜测。
#### Scenario: Seven day boundaries
- **WHEN** 待办在 now、now+7天、now 之前和未知时刻截止
- **THEN** now 属于七天列表，now+7天不属于，now 之前属于逾期，未知计入未定数量而不加入任何时间窗口。
#### Scenario: Future interviews
- **WHEN** 存在未来预约、已发生和未知时间的面试
- **THEN** 最近面试仅按已发生的已知时间排序，不用记录创建时间替代面试时间。

### Requirement: Usable complete MVP workspace
电脑浏览器 SHALL 提供首页、面试与待办导航，以及投递详情内的关联记录与新增入口；保留公司、岗位、投递、历史功能。
#### Scenario: Daily operation loop
- **WHEN** 用户在电脑浏览器建立面试复盘及待办，完成后重新打开并返回首页
- **THEN** 内容持久保存，首页及时反映当前数量，错误可重试，未知数据和空状态明确显示。
