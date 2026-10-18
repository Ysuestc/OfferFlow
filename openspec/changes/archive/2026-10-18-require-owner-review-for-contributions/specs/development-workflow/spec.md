## ADDED Requirements

### Requirement: 贡献代码由仓库所有者审查
仓库 MUST 通过 main 的 Active 规则要求 PR、至少一个批准和代码所有者审核，CODEOWNERS 全部文件归 @Ysuestc；新提交撤销旧批准，合并前解决审查会话，禁止强推和删除受保护分支。
#### Scenario: 外部贡献合并
- **WHEN** 其他贡献者提交修改 main 的非草稿 PR
- **THEN** 自动请求 Ysuestc 审查，没有其有效批准不能由普通贡献者合并，不能通过直接推送绕开。
#### Scenario: 所有者维护
- **WHEN** 所有者自己的 PR 无法自审
- **THEN** 仅所有者具有 PR 内维护例外，普通贡献者无例外；助手不能据此自行合并外部贡献。

## MODIFIED Requirements

### Requirement: 阶段交付和公开数据隔离
阶段交付 MUST 只提交当前 change 的公开文件，采用普通 Git 推送，并报告实际结果；原始参考资料、凭据和个人求职数据不得提交。一个 change 在实现、审查、验收和归档完成后统一推送 GitHub 的 codex/ 变更分支一次，再通过 PR 交付 main；不在中间阶段推送，不直接推送 main。
#### Scenario: 准备公开推送工作流阶段
- **WHEN** 已授权阶段完成验证、审查及归档
- **THEN** 核对候选文件和远程后提交、普通推送变更分支一次并创建 PR；成功或失败均据实报告，不夹带原始附件或本地配置，是否合并按用户当次授权执行。
#### Scenario: 当前 change 正在实施或修复
- **WHEN** 同一 change 的方案、代码或验证仍在进行
- **THEN** 相关修改留在本地，完成完整交付后统一推送一次。
