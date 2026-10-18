# Design

## Context

已通过既有 Git 凭据在内存中只读访问 GitHub API，确认管理权限。无 gh CLI，直接使用官方 REST API，不安装工具、不显示或落盘凭据。当前仓库阶段发布默认直推 main，需与新保护同步调整。

## Decisions

- 创建 offerflow-main-owner-review 分支 Ruleset，只匹配 refs/heads/main，enforcement=active；规则 pull_request / deletion / non_fast_forward。
- PR 参数：required_approving_review_count=1、require_code_owner_review=true、dismiss_stale_reviews_on_push=true、required_review_thread_resolution=true；require_last_push_approval=false，避免唯一审查者做建议修正后无法完成审查。没有 CI 不增加必需 status checks。
- 优先仅 User=Ysuestc 设置 bypass_mode=pull_request；若服务端不支持 User，个人仓库使用 RepositoryRole=5（管理员，即所有者）的 PR 内例外，不添加普通 Write 权限。明确最终实际接受的设置并回读。不使用 always / exempt。
- CODEOWNERS 的 `* @Ysuestc` 覆盖所有文件及自身；默认分支中的文件用于外部 PR 审查。CONTRIBUTING、PR 模板、AGENTS、workflow 说明开发分支 / PR / 人工审核。
- 实际写入与回读：Ruleset 25342394 已 Active，采用精确 User 181989467（Ysuestc）的 pull_request 例外，无需管理员角色回退。服务端返回全部三条规则及所需 PR 参数；默认保留仓库允许的三种合并方式和额外未归属变更批准要求，不改变其他设置。
- 本次配置是用户已授权的所有者维护：先完整实现、检查并激活规则，再归档和一次推送分支、创建维护 PR，通过所有者 PR 例外安装本次配置。之后未明确授权合并的 PR 留给用户审查，不能把维护例外当作自动合并外部代码的授权。

## Scope / Data

仅 GitHub main 治理设置、.github 公开文件、贡献指南与工作流文档。业务代码、依赖、数据库和个人记录不改；令牌只从精确仓库的 Git credential helper 获取到进程内存，用于 api.github.com，绝不打印或提交。

## Verification

REST 预检 / 写入结果 / 单规则回读 / main effective rules；CODEOWNERS 静态匹配、GitHub default-branch CODEOWNERS errors 和内容回读；Markdown / YAML / 完整 delta 检查、候选文件及远程核实。纯配置不跑 Java / 数据库测试，不制造外部账号或模拟他人身份。

## Risks / Rollback

所有者自己不能批准自有 PR，PR 内维护例外保留其维护能力但不恢复 main 直推。若配置失败保留已产生状态并报告，不创建重复规则；不绕过自动审批或改其他项目。回退须由所有者明确授权调整该规则，不删除其他规则。

## Open Questions

无，用户已明确授权设置审核模式；权限预检通过。
