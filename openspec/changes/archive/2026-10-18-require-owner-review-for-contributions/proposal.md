# Proposal

## Why

用户明确要求设置 GitHub 协作模式，使别人的代码必须经过自己审核。实际 API 预检：公开仓库 Ysuestc/OfferFlow，默认 main，当前凭据有管理权限；无 rulesets，main 无经典保护；仓库无 CODEOWNERS。

## What Changes

- main 启用必须 PR、至少 1 个批准、必须代码所有者批准、新提交撤销旧批准、会话需解决、禁止强推 / 删除的规则。
- CODEOWNERS 全部文件归 @Ysuestc；贡献指南和 PR 模板说明 Fork / 分支 / PR / 审查。
- 仅所有者保留 PR 内维护例外，解决自有 PR 不能自审；不允许普通贡献者绕过。自动化不擅自合并其他贡献者的 PR。
- 更新阶段交付约定为一次推送 change 分支并创建 PR，禁止之后直推 main。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- development-workflow：代码所有者审查与分支交付规则。

## Success Criteria

1. Active main 规则 API 回读包含 PR、1 个批准、代码所有者批准、旧批准失效、会话解决和强推 / 删除限制。
2. CODEOWNERS 全文件归 Ysuestc，指南 / 模板 / AGENTS / workflow 一致，普通贡献者无绕过。
3. 本 change 一次分支推送、维护 PR 安装；合并后 main 的 CODEOWNERS 和 effective rules 复核，工作区与远程一致。

## Non-goals / Impact

不改产品代码、数据库、数据、依赖或启用 CI；不更改可见性、协作者权限和其他仓库。无现有 CI 检查不设置强制检查，避免锁死 PR。
