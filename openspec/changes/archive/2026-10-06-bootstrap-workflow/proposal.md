# Proposal

## Why

OfferFlow 当前只有 IDE 配置，开发约定仍在聊天中。用户要求参考现有项目的 OpenSpec 流程，先把工作流和 agent 定义沉淀到仓库，方便后续按阶段开发、审查和推送。

## What Changes

- 创建仓库级 AGENTS.md 和项目上下文，区分已确认需求与待确认设计。
- 定义六个流程角色、状态转换、授权规则、审查和验证证据。
- 创建 OpenSpec 项目配置、本地 schema、四类制品模板及项目工作流技能。
- 记录本次初始化的方案、行为规范、任务、审查和验证，完成后归档。
- 补齐 README、忽略规则、行尾约定和 MIT 许可证，并按阶段推送授权交付。

## Capabilities

### New Capabilities

- `development-workflow`：OfferFlow 仓库内可追踪的阶段开发流程、角色职责、确认、验证与交付约定。

### Modified Capabilities

无。

## Success Criteria

1. 仓库入口可定位工作流、项目上下文、六个角色、技能、配置和模板，本地链接可解析。
2. schema 的 proposal → specs / design → tasks 依赖可验证，config 的规则使用相应 artifact ID。
3. 技能名称与 frontmatter 有效，可显式调用；角色文档明确不自动注册或启动子 agent。
4. 首次业务设计确认仍为待确认，现有阶段推送授权被保留，资料中的企业规则不进入项目执行约束。
5. 纯文档阶段有真实审查和检查记录，不声称 Java 编译、业务测试或独立 agent 审查已发生。
6. 已完成流程规范同步到稳定 specs，初始化 change 归档；Git 候选文件不包含 IDE、附件、凭据或个人求职数据。

## Non-goals

不初始化 Spring Boot，不编写业务 Java、前端或 SQL；不把整体业务设计改为已批准；不接入外部通知，不部署业务 AI Agent，不安装全局工具。

## Impact

所有改动属于工作流与仓库基础。角色是文档契约，技能是项目内入口；没有后端运行依赖变更。

许可证采用此前建议的 MIT，作者为仓库拥有者 Ysuestc。原始参考文件只用于归纳通用流程，未将其业务内容或源码复制到公开仓库。
