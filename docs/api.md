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
