---
name: offerflow-workflow
description: 在 OfferFlow 仓库中创建或继续 OpenSpec 阶段变更，生成方案与行为规范，执行审查、受控实现、验证、归档及已授权阶段推送。用于模块开发和工作流维护。
---

# OfferFlow Workflow

读取 [AGENTS.md](../../../AGENTS.md)、[项目上下文](../../../docs/project-context.md) 和 [工作流](../../../docs/workflow.md)。只加载本次能力相关规范和角色，避免读取所有历史 change。

1. 明确本次 change、实际阶段和已有授权。优先继续相关活动 change；多个匹配且不能判断时请求必要澄清。
2. 按本次工作需要预检。新建时使用 [本地 schema](../../../openspec/schemas/offerflow-spec-driven/schema.yaml) 与其模板；CLI 不可用时按同一格式手动创建并记录验证方式。
3. 根据工作流状态依次承担 [Coordinator](../../roles/coordinator.md)、[Designer](../../roles/designer.md)、[Spec Reviewer](../../roles/spec-reviewer.md)、[Implementer](../../roles/implementer.md)、[Code Reviewer](../../roles/code-reviewer.md)、[Verifier / Archiver](../../roles/verifier-archiver.md) 的相关职责。
4. 首次业务代码实现需要整体设计确认；工作流文档初始化不代表该确认。已经授权的阶段连续完成，只有实质业务歧义或新增范围需要输入。
5. 将真实任务进度、审查方式和验证证据写入 processing 文件；每次转换前重读状态。纯文档做格式和语义检查，不机械补 Java 测试。
6. 有阻断问题时修复并针对性复核；没有必要工具时保留 NOT_RUN 和续跑条件，不能改验收让它通过。
7. 验收通过后同步稳定规范、归档，并按既有授权提交、推送；报告实际结果。

角色文件是职责契约，不会自动注册或启动子 agent。默认顺序执行；用户明确请求多 agent 后才使用当前环境可用的协作工具，不能假定参考项目的工具已安装。

不复制参考附件的内部业务、通知配置或企业任务编号，不安装全局工具，不改变其他项目配置。工具或权限缺失时继续独立工作并说明实际限制。
