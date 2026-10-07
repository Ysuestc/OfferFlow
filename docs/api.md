# 工作台 API

业务接口在 mysql profile 启用，/api/v1/workspace 在两种模式可用。响应沿用 ApiResponse，失败使用真实 HTTP 状态和安全消息。关联 id / 返回 id 为十进制字符串；Java 后端按 Long 校验。

| 资源 | 方法和路径 | 内容 |
|---|---|---|
| 业务模式 | GET /api/v1/workspace | available；成功启动 mysql 后为 true |
| 公司 | GET /api/v1/companies | q / page / size，分页 |
| 公司 | POST /api/v1/companies | name / type / website / notes |
| 公司 | GET、PUT、DELETE /api/v1/companies/{id} | 读取、完整替换可编辑字段、删除未引用公司 |
| 岗位 | GET /api/v1/positions | q / companyId / page / size |
| 岗位 | POST /api/v1/positions | companyId / name / location / direction / jd / recruitmentBatch |
| 岗位 | GET、PUT、DELETE /api/v1/positions/{id} | 读取、替换、删除未引用岗位 |
| 投递 | GET /api/v1/applications | q / stage / page / size |
| 投递 | POST /api/v1/applications | jobPositionId / channel / appliedOn / stage / stageOn / endReason / submitted / notes |
| 快捷投递 | POST /api/v1/applications/quick | companyName 或 companyId、positionName、可选目录资料及投递字段；事务建立或复用目录 |
| 投递 | GET、PUT /api/v1/applications/{id} | 读取；PUT 仅 channel / appliedOn / notes / version |
| 阶段 | POST /api/v1/applications/{id}/stages | stage / stageOn / endReason / remark / version |
| 历史 | GET /api/v1/applications/{id}/history | 初始和后续有效阶段记录，按 id 升序；recordedAt 为 UTC 时刻 |

创建返回 201；其余成功 200，删除成功 data=null。未实现投递删除、历史修改、独立历史补录接口。

列表返回 data={items,total,page,size}；page 默认 1、范围 1..100000；size 默认 20、范围 1..100。按 id 降序，q 最多 100 字符，按字面包含匹配（% / _ 不作通配符），公司搜索名称，岗位 / 投递搜索公司或岗位名称。stage 与公司类型使用 [数据库说明](database.md) 中的枚举代码。

## 写入约束

公司 name / type、岗位 companyId / name、投递 jobPositionId / stage 必填。名称最多 255，官网 2048（必须为 http(s) 且有 host，无 URL 用户凭据），公司和投递备注 / 历史备注 16000，地点 255，方向 128，批次 / 渠道 64，JD 40000。可选文本空串规范为 null；PUT 可选字段不传或 null 表示清空，不作 PATCH 合并。

stageOn / appliedOn 为 YYYY-MM-DD，可未知；不使用录入时间推断。初始 TO_APPLY 未投递，其余正常招聘阶段已实际投递；初始 ENDED 由 submitted（默认 false）说明是否投递过。未实际投递不可填写 appliedOn。后续阶段跳过或回退均追加有效历史，submitted 一旦为 true 不随回退清除。

ENDED 必须提供五种 endReason 之一，其他阶段不能提供。REJECTED 表示企业拒绝，主动拒绝 Offer 使用 ENDED / DECLINED_OFFER。

应用元信息或阶段更新需要 version。最新版本检查及写入为乐观并发控制，过期为 409。阶段、日期和原因均无变化且版本有效时不追加历史、版本不递增；历史备注用于有效变更。快照更新和历史插入同一事务，任一步失败均不提交。

## 错误及示例

快捷录入：companyName / companyId 二选一，positionName / stage 必填；companyType 可选，创建新公司时默认 OTHER。可选 location、direction、recruitmentBatch、jd 和投递 channel、appliedOn、stageOn、endReason、submitted、notes 沿用既有长度 / 日期 / 阶段约束。请求示例：

```json
{
  "companyName": "星河示例科技",
  "positionName": "Java 后端",
  "stage": "SUBMITTED",
  "channel": "官网",
  "appliedOn": "2026-09-20"
}
```

公司名称按数据库排序规则等值匹配，唯一匹配复用；多个同名返回 409，可改传 companyId。岗位按公司、名称、地点、方向、批次（包括 null）唯一匹配复用；多个匹配返回 409，使用既有 /applications 接口选择具体 jobPositionId。复用不覆盖已有公司类型、官网、备注或 JD；不同批次 / 地点 / 方向分别建岗。空可选字段转 null，未知日期不推断。

公司、岗位、投递、初始历史在一个事务中保存，任一步失败不留下部分新记录。同岗位已有投递返回既有 409，不更新原档案。目录未增加全局唯一约束，两个并发请求首次创建同名公司或同身份岗位不保证幂等；浏览器保存期间禁止重复提交。

400：参数、日期、类型、必填或业务语义非法；404：公司 / 岗位 / 档案不存在；409：同岗位重复档案、引用保护、陈旧版本；500：未知服务端故障，消息不包含 SQL / 原始异常。

虚构阶段更新示例（id / version 使用当前查询结果）：

```json
{
  "stage": "SECOND_INTERVIEW",
  "stageOn": "2026-09-29",
  "endReason": null,
  "remark": "邮件通知进入二面",
  "version": 0
}
```

同一个岗位使用一份档案，channel 填主要渠道，其他渠道放 notes。不同批次分别创建岗位，避免覆盖原 JD。

## 面试

| 方法 | 路径 | 行为 |
|---|---|---|
| GET | /api/v1/interviews | 分页摘要，不含问题 / 回答 / 复盘长文本 |
| POST | /api/v1/interviews | 建立独立面试记录，201 |
| GET | /api/v1/interviews/{id} | 完整内容 |
| PUT | /api/v1/interviews/{id} | 完整编辑，必须传 version |
| DELETE | /api/v1/interviews/{id}?version=0 | 未被待办引用且版本匹配才删除 |

创建：applicationId 必填正整数；roundName 必填最多 64；interviewAt 为带时区 ISO-8601 或 null；format=ONLINE/OFFLINE/AI 或 null；questions / answers / review 各 40000，result 16000，可空。更新字段相同但无 applicationId，并新增 version（非负必填），关联不能重绑；省略可选字段将清空。

列表额外支持 applicationId、format，q 匹配公司 / 岗位 / 轮次；按 interviewAt DESC、id DESC，未知最后。摘要和详情含公司 / 岗位标签、字符串 ID、version 和 updatedAt。面试 CRUD 不改变投递阶段或历史。删除引用面试返回 409，可先编辑待办清除 interviewId。过期更新 / 删除 409，记录不存在 404。

## 待办

| 方法 | 路径 | 行为 |
|---|---|---|
| GET / POST | /api/v1/todos | 分页 / 新建（201） |
| GET / PUT / DELETE | /api/v1/todos/{id} | 详情 / 完整编辑 / 删除 |
| PUT | /api/v1/todos/{id}/completion | 完成或重开 |

创建：applicationId 必填；interviewId 正整数或 null，仅能关联同投递面试；kind=WRITTEN_TEST/INTERVIEW/ASSESSMENT/MATERIAL/OFFER_DEADLINE/OTHER；title 必填 255；dueAt 带时区 ISO-8601 或 null；notes 16000 或 null。更新无 applicationId、必须带 version，普通编辑不会改变完成状态。DELETE 必须带 ?version=。同投递校验错误 400，引用缺失 404，数据库关联发生变化或版本过期 409。

完成请求示例：
```json
{ "completed": true, "version": 0 }
```
服务端首次完成记录 completedAt；当前版本重复完成是无操作（时间 / 版本不变），旧版本仍 409。completed=false 重新打开并清空完成时间。前端冲突保留笔记，重新加载前可复制输入。

筛选支持 applicationId、kind、completed=true/false、timing=UPCOMING/OVERDUE/UNDATED，q 匹配公司 / 岗位 / 标题。timing 只查询未完成项，组合 completed=true 返回空。UPCOMING=[now, now+7天)，OVERDUE=dueAt<now，UNDATED=dueAt null。按未完成优先、截止时间 ASC、未知最后、id ASC 排序。分页与关键词限制沿用公共合同。

## Dashboard

GET /api/v1/dashboard 返回：

- counts.totalSubmitted：submitted=true，包含投递后结束的历史事实。
- counts.active：submitted=true 且当前非 TO_APPLY / OFFER / REJECTED / ENDED。
- counts.interviewing：submitted=true 且当前 FIRST_INTERVIEW / SECOND_INTERVIEW / THIRD_INTERVIEW / HR_INTERVIEW，是 active 子集。
- counts.offers / rejected：当前 OFFER / REJECTED，主动拒绝 Offer、接受 Offer 后 ENDED 不计当前 Offer。
- generatedAt / upcomingUntil：同一次服务端 UTC 时刻及七天窗口末端。
- upcomingTodoCount / overdueTodoCount / undatedTodoCount：对应未完成事项总数。
- recentApplications：最近实际投递，appliedOn DESC / id DESC，未知日期最后。
- recentInterviews：已知且 interviewAt ≤ generatedAt 的最近面试，时间 DESC / id DESC；排除未来预约和未知。
- upcomingTodos / overdueTodos：对应窗口的未完成待办，各最多 5 条；总数可能大于列表长度。

所有最近列表最多 5 条，未知日期不猜测；响应采用统一 JSON 合同，首页加载失败可重试。默认仅 mysql 模式提供业务接口，standalone 不提供。

## 邮箱采集

仅 mysql profile、单邮箱、默认 loopback，无登录认证。邮箱响应 Cache-Control: no-store。

| 方法 | 路径 | 行为 |
|---|---|---|
| GET / PUT | /api/v1/mailbox | 安全设置 / 保存，首次 GET data=null |
| POST | /api/v1/mailbox/connection-tests | 测试保存的账号与文件夹，只读 TLS |
| POST | /api/v1/mailbox/syncs | 手动采集一批，最多扫描 100 封 |
| GET | /api/v1/mailbox/messages | q 匹配主题 / 发件人，分页摘要，无正文 |
| GET | /api/v1/mailbox/messages/{id} | mail 摘要与 bodyText 纯文本详情 |

PUT：email 必填 254，后缀与 provider=NETEASE_163/NETEASE_126/NETEASE_YEAH 对应；folder 必填 128，无控制字符；syncFrom 必填带时区 ISO-8601，不得为将来；authorizationCode 最多 128，去除空白后 8—128 字母数字，首次必填，后续空则保留；version 非负，首次 0，保存成功递增。响应不含授权码、密文、密钥或游标。已有邮件后禁止改身份 / 文件夹 / 起点，仍可换授权码。

设置 VO 还含 credentialConfigured、identityLocked、busy、version、lastStatus、lastAttemptAt、lastSuccessAt、lastError、lastImported、messageCount。lastStatus=NEVER/RUNNING/SUCCESS/PARTIAL/FAILED；RUNNING 且 busy=false 表示上次中断，不阻止重试。lastSuccessAt 仅完整批次成功时更新，不代表已采集所有后续邮件。

操作 data={status, imported, scanned, hasMore, errorCode, message}；测试成功 status=CONNECTED，同步为 SUCCESS/PARTIAL/FAILED。已执行操作的外部失败返回 HTTP 200 + 显式失败结果；错误类别 AUTHENTICATION/FOLDER/NETWORK/CONTENT/STORAGE/CREDENTIAL，不透传协议异常原文。格式错误 400；未设置 404；忙、旧版本或被冻结字段修改 409；无法访问本地数据库时可为 500。普通 HTTP 错误沿用公共合同。

列表 page / size / q 沿用公共限制，字面量 LIKE，按本地 id DESC；摘要含字符串 id、subject、sender、receivedAt、sentAt、contentStatus、bodyTruncated；详情无附件或原始 HTML。contentStatus=AVAILABLE/TOO_LARGE，未知时间 NULL，正文最多 20000 字符。重复同步与 UID 重建时相同内容不重复建记录；邮件与游标同事务保存，失败不越过未提交项。当前不改变任何投递、历史、面试或待办。
