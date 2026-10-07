# Design

## 现状与授权
RecordEditor.vue 将新增投递绑定到远程岗位选择器，ApplicationRequest 必须有 jobPositionId。输入公司文本只触发搜索，不能表示公司或岗位。
用户本次问题授权修正新增投递体验，按既有工作流完成验证、归档及一次推送。SELF_REVIEW，顺序承担角色。

## 入口与字段
默认“直接录入”，公司名称、岗位名称必填；公司类型默认 OTHER（未知不猜测），可展开填写地点、方向、批次、JD。这些字段全部复用既有 Company / JobPosition 模型，不新增数据库字段或依赖。
“选择已有岗位”保留搜索，明确必须选中结果，无结果显示“直接录入”按钮；岗位库预选默认此模式。
直接录入也允许显式选择已有公司，以解决同名歧义。模式切换保留已填投递信息，隐藏字段不参与校验，错误不会关闭对话框。
公司类型仅创建新公司时使用，复用公司 / 岗位不覆盖现有类型、官网、备注或 JD。帮助文字说明此行为。

## API 与事务
新增 POST /api/v1/applications/quick，成功 HTTP 201，返回现有 ApplicationView。
请求为 companyName 或 companyId（二选一）、可选 companyType，必填 positionName，以及 location / direction / recruitmentBatch / jd 和已有投递字段 channel / appliedOn / stage / stageOn / endReason / submitted / notes。
DTO 复用既有字段长度和枚举约束；公司关联有条件校验，非法或同时指定名称和 ID 返回 400，ID 不存在返回 404。
ApplicationEntryService 协调已有 CompanyService、PositionService 和 ApplicationService，外层 @Transactional 使公司、岗位、投递、历史一同提交或回滚；不在前端串联三次写入。

## 匹配与限制
名称、可选字段去首尾空白，空可选字段转 null；名称等值匹配采用现有 MySQL 排序规则，非模糊搜索。
公司名零匹配创建、一匹配复用、多匹配返回 409 并引导选择已有公司 ID。
岗位按 companyId + name + location + direction + recruitmentBatch 等值匹配（包含 null）；零匹配创建、一匹配复用、多匹配返回 409 并引导选择具体已有岗位。JD 不作为身份，复用不修改 JD。
已有同岗位投递仍返回既有 409，不覆盖档案、阶段或历史。不同批次 / 地点 / 方向可以分别建岗。这是快捷录入的复用规则，不给公司或岗位增加全局唯一约束。
数据库既有岗位单投递唯一键处理并发重复投递。由于目录本身允许同名，两个并发请求首次录入同名公司或同身份岗位仍可能建立两个目录；本次不引入分布式锁或修改目录全局语义，前端校验和提交期间禁止重复提交。名称已产生歧义时要求选择 ID，不猜测。

## 验证与回滚
隔离 MySQL 测试覆盖全链路、复用资料、不同身份、歧义、校验、重复和历史失败全回滚；浏览器覆盖默认无搜索保存、失败保留输入、切换与预选回归以及手机布局。
重建前停止自有本地实例并等待退出，构建后在隔离浏览器实例验收，再用原 private-data/local 重启用户实例，仅检查健康 / 静态资源。
无需迁移；回滚代码不修改既有数据。运行中的用户页面需刷新加载新资源。
