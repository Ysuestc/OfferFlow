# Context
现有 Spring Boot 3 / Java 21 / MyBatis-Plus 单体，已有六实体和 V1 关系约束，公司 / 岗位 / 投递接口与 Vue / Element Plus 页面可用。Interview / Todo 当前只有 Entity / Mapper，用户本地实例使用私有持久数据目录。沿用用户已确认的单岗位单档案、结束原因和阶段补录规则。

# Decisions
## 模型和迁移
Interview.applicationId 创建后不可改；轮次必填（64），时间和形式可空，问题 / 回答 / 复盘各最多 40000 字，结果 16000。Todo.applicationId 同样不可改；interviewId 可选择 / 清除，但必须属于该投递；kind 六类、title 255 必填，notes 16000，可没有截止时间。
V2 为 interview、todo 各追加 version INT NOT NULL DEFAULT 0 和非负 CHECK。用途：复盘长文本和待办完成状态避免并发覆盖。更新 / 删除必须传当前版本，冲突 409；明确 SET 空值实现清除。没有新增依赖、额外表或索引，现有时间 / 关联索引足够 MVP。

## 状态与时间
面试和待办操作不改变 Application / History。面试结果保留文本，不从结果推断招聘阶段。不自动创建 / 同步待办；用户可在投递详情分别新增。
时间为带时区 ISO-8601 Instant，存储 UTC；前端 datetime 本地选择后 toISOString，回显当前浏览器时区，未知留空。无时区输入 400。钟源注入 Clock.systemUTC，便于固定边界测试。
待办完成独立 PUT /todos/{id}/completion，completed=true 设置服务端 completedAt；当前版本重复请求不更改时间和版本；重开清除时间。普通编辑不改变完成标记。日期排序未知最后，同时间 id 决定次序。

## API
GET /interviews 和 /todos：q（公司 / 岗位及轮次 / 标题，100 字）、applicationId、page/size；interviews 可筛 format，todos 可筛 kind、completed、timing=UPCOMING/OVERDUE/UNDATED，UPCOMING 为 now ≤ dueAt < now+7天且未完成。
POST /interviews、/todos：完整字段，applicationId 必填；PUT /{id}：完整可编辑字段 + version，关联投递不可重绑（DTO 无 applicationId）。GET /{id} 返回公司 / 岗位标签及 string ID；DELETE /{id}?version=，乐观版本条件保护。引用面试删除 409，可先清除待办面试关联或删除待办，不静默级联。
分页约束沿用 1..100000、size 1..100，默认 20；JSON 统一响应和错误保护沿用现有接口。列表用参数化 join SQL，无 N+1。

## Dashboard
GET /dashboard 单个只读事务，一次捕获服务端 now 并返回 generatedAt / upcomingUntil；最多各 5 条最近投递、最近面试、七天待办、逾期待办。
totalSubmitted 为 submitted=true 的历史投递事实；active 为 submitted=true 且当前非 TO_APPLY/OFFER/REJECTED/ENDED；interviewing 为当前四个面试阶段且 submitted=true，是 active 子集；offers 为当前 OFFER；rejected 为当前 REJECTED。采用当前快照，接受 / 主动拒绝 Offer 后 ENDED 不继续计入当前 Offer；不会把投递档案数当实际投递数。
recentApplications 仅 submitted=true，appliedOn DESC（未知最后）/ id DESC；recentInterviews 时间已知且 interviewAt ≤ now，按时间 DESC / id DESC，不把未来预约当最近已发生；未完成逾期 dueAt < now；未来七天左闭右开；unknown 不计入时间窗口，另返回 undatedTodoCount，页面引导待办页补充。返回 upcomingTodoCount / overdueTodoCount 与最多五条列表，避免误以为列表长度是总数。

## 前端
六个导航：首页概览、投递、面试、待办、岗位、公司。保留现有台账页面；新增独立 Dashboard / RecruitmentRecords / ActivityEditor，面试详情可阅读完整问题 / 回答 / 复盘，不在列表输出大文本。表单远程搜索投递，面试关联选择仅同档案；冲突保留输入，用户可重新加载当前记录。投递详情内展示关联面试 / 待办和快捷新增。响应式、明确空 / 错误状态；不预建路由、状态库或自动化框架。

手机导航使用三列两行；关联面试已选择时提供明确“解除面试关联”按钮，避免依赖鼠标悬停才出现的下拉清除图标。非首页模块按需加载，减少首屏 JavaScript。

# Validation / Risks
独立第四个 offerflow_it_* MySQL 库承载新 API 测试，固定 UTC Clock 检查七天和逾期边界、完成 / 重开、并发与关联。迁移库先 target=1 插入虚构记录再 migrate，验证 V2 默认值和数据保留，V1 校验不变。旧回归测试必须保留。
浏览器在独立 browser-review 实例、虚构数据运行，验证完整复盘、六种类型、筛选 / 首页 / 完成 / 重开 / 冲突 / 删除 / 手机宽度。生产构建使用现有 Node 24。
构建前停止助手自有本地实例以释放 Windows Jar 锁；不停止安装的 MySQL 服务、不删除 private-data；升级后重启同实例 / 同端口。回滚代码不得降级已迁移 schema；需修复追加迁移而非编辑 V1 或删除真实数据。Linux / Safari / 外网部署不在本次实际环境验证范围。

# Authorization
当前“完善所有基础功能”覆盖上述基础 MVP 行为；既有一个 change 一次推送授权有效。无需重复批准范围内实施、测试、修复、归档和普通推送。
