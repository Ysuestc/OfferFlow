# Design

## Context

RecruitmentRecords.vue 共用面试 / 待办列表，ActivityEditor.vue 为现有表单。Todo 只有 dueAt，支持未知和版本控制。GET /todos 当前无区间参数；分页 size 最大 100，直接把当前 10 条用于日历会漏项。

## Goals / Non-Goals

仅为待办主页面增加直观日历和直接必要的查询；其他页面、面试、阶段、邮件不改变。不添加时长或独立日程模型。

## Decisions

- TodoCalendar.vue 独立封装日历与 scoped 样式，默认周视图；主页面保留默认列表并显式切换，内嵌待办仍列表。
- 本机 Date 构造日期边界，周一开始；月固定 42 个格子。跨月导航保留日数并限制到目标月最后一天。日 / 周采用 24 小时网格，每小时卡片流式堆叠，不互相遮挡；卡片标明具体 HH:mm，不表示事件持续长度。
- 点击日期默认预填 09:00，点击小时预填 HH:00。ActivityEditor 增加可选 initialDueAt，仅对新建 todo 生效；仍保存 ISO-8601 Instant，编辑优先真实记录。
- 六类型颜色、今日强调、当前小时指示、完成弱化；点击卡片打开待办详情并调用已有操作。未定侧栏明确为未完成项，因为现有 timing=UNDATED 只筛未完成；类型 / 搜索 / 状态 / timing 均遵循原语义。
- GET /todos 成对可选 from / to，半开区间、最大 62 天，mapper 查询和 COUNT 同条件。原无区间查询保持不变；保留 Mapper 原查询方法委托无区间新查询，使 Dashboard 调用无需改动；不增 API 路径、字段、索引、依赖或迁移。
- 前端 size=100 依次遍历范围分页，最多 10000 条；若超出则明确提示缩小日期 / 筛选，不显示部分数据冒充完整。请求序号隔离快速翻页结果；失败清空本轮结果并提供重试。

## Change Scope

仅 Todo controller / service / mapper 与 MySqlMvpIT、RecruitmentRecords / ActivityEditor、独立日历组件 / 日期工具 / 日历测试以及直接相关 API / 使用 / change 文档。不要修改 App 布局、全局配色、其他模块或脚本。

## Data / API / Transactions

所有写入复用已有 CRUD 和版本。from / to 是查询时刻，不是新持久化字段；范围与其他筛选取交集。未知事项单独用原 UNDATED 筛选；已完成无时间项仍在原列表可查询。

## Verification

必要日期函数与电脑 Playwright 场景，包含 UTC 转本地、跨月、重叠 / 午夜、区间多页、预填创建 / 修改 / 完成 / 删除、网络失败与请求竞争。MySQL 用独立测试库覆盖半开边界、排序 / 分页计数、过滤、无范围兼容、非法参数。完整生产构建和原活动列表受影响回归；个人 local 只停止 / 升级 / 恢复，不跑测试或读个人内容。

## Risks / Rollback

单时刻不能表示持续时长；界面说明按截止 / 开始时间展示，不用假时长。大数据超过上限明确报错。回退日历前端和可选范围代码即可，数据与 Flyway 不受影响。

## Open Questions

无阻断输入；用户明确限定只做此功能，采用现有时间点模型。
