## ADDED Requirements

### Requirement: Edit the recruitment cohort label
系统 SHALL 默认显示 2027 届秋招，并允许用户修改四位毕业届别年份、保存到当前浏览器；标签独立于电脑当前年份和求职业务记录。
#### Scenario: Save and reload
- **WHEN** 用户点击标签输入有效四位年份并保存，随后刷新
- **THEN** 标签立即显示新届别，刷新后继续显示保存值。
#### Scenario: Cancel or invalid input
- **WHEN** 用户取消，或输入空白及非四位年份
- **THEN** 取消保留原值，非法输入提示纠正且不保存。
#### Scenario: Browser storage unavailable
- **WHEN** 读取或保存浏览器设置失败
- **THEN** 系统可继续使用，读取失败显示默认届别并提示，保存失败保留原届别并明确报错。
