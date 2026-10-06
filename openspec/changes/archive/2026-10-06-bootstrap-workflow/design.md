# Design

## Context

检查时项目只有 `.idea`，没有 Git 元数据或业务工程。用户提供了 GitHub 地址并授权后续按阶段推送，当前又明确要求先创建工作流文件。

参考资料将设计、方案审查、实现、代码审查、验证和归档分为角色，并保存 change 状态。OfferFlow 复用这一结构，重新编写与个人项目相关的规则。

本次是工作流自身的 bootstrap：先建立工作流定义，再补齐本 change 制品和审查记录。记录实际发生的检查，不声称此前已经通过新流程运行独立 agent。

## Goals / Non-Goals

目标是让下一阶段能够从仓库恢复上下文、生成方案、跟踪任务并记录验收。

不初始化业务工程、调整全局设置、接入通知服务或自动多 agent 编排。

## Decisions

1. **共享入口**：AGENTS.md 只保留仓库关键规则，详细流程放 docs/workflow.md，需求和设计确认状态放 docs/project-context.md。
2. **角色契约**：`.agents/roles/` 定义六个角色的输入、范围和产物；默认由主助手顺序执行，并标明自审。
3. **项目技能**：`.agents/skills/offerflow-workflow/SKILL.md` 按需链接角色和流程，不依赖参考项目专有工具。
4. **本地 schema**：`offerflow-spec-driven` 定义四类制品及模板，specs 与 design 依赖 proposal，tasks 依赖两者。
5. **授权分离**：首次业务代码实现仍需整体设计确认；当前工作流文档可以按明确请求完成；已有推送授权不重复询问。
6. **适当验证**：文档验证使用实际 YAML、链接、技能和 CLI 检查，不强制 Java 测试；WARN 可有理由接受，FAIL 必须修复。
7. **Git 基础**：忽略本地 IDE、配置、归档输入和个人数据，文本行尾规范化；阶段交付使用普通提交，不强推。

## Change Scope

| 范围 | 内容与原因 |
|---|---|
| 仓库根目录 | AGENTS、README、许可证、Git 规则，提供入口和公开仓库基础 |
| docs | 流程、项目事实与设计状态、少量有依据的经验 |
| .agents | 角色职责与项目技能，用于后续助手读取 |
| openspec/config.yaml | 项目上下文和四类 artifact 规则 |
| openspec/schemas | 本地 schema 与可复用制品模板 |
| openspec/changes | 本阶段记录、审查、验证和最终归档 |
| openspec/specs | 只同步已完成的 development-workflow 规范 |

未增加数据库字段或后端依赖。OpenSpec CLI 只作为可选开发验证工具；若临时获取，使用缓存内固定版本，不写入业务依赖或全局安装。

## Data / API / Transactions

没有业务数据、接口或事务变更。status.md 是工作流恢复记录，不是权限来源；CLI artifact 完成状态不代替审查或确认状态。

## Verification

- 使用 Python / PyYAML 检查配置、schema、change 元数据及依赖引用。
- 使用 skill-creator 的 quick_validate 检查技能。
- 检查 Markdown 相对文件、目录和锚点引用。
- CLI 可用时执行 schema 和 change 严格验证、指令和状态检查。
- 对“仅分析”“明确批准某模块”“已授权推送”“缺少 CLI 的文档修改”四种请求逐项走查确认规则。
- 审查待提交文件、忽略规则和原始参考资料隔离；不运行不存在的 Java 工程。

## Risks / Rollback

角色文档可能被误认为运行时自动注册，入口与角色中明确说明其性质。业务建议可能被误当成确认，项目上下文单独标记待确认。

所有改动为新建文档和配置，可通过普通 Git revert 回退。归档前检查仓库内源目标和目录碰撞，归档后复核引用。

## Open Questions

本次工作流范围无阻断问题。整体业务设计仍待确认，属于后续业务阶段的输入，不阻断此文档交付。
