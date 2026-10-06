# OfferFlow

个人秋招投递管理系统，用于统一管理公司、岗位、投递进度、面试复盘和待办事项，后续逐步接入 AI Agent。

## 当前进度

目前完成仓库协作规则和 OpenSpec 工作流初始化。尚未初始化 Spring Boot 或前端工程；业务整体设计仍待确认。

计划技术栈：Java 21、Spring Boot 3、MyBatis-Plus、MySQL、Maven、Lombok。后续前端考虑 Vue 3、TypeScript、Element Plus。Redis 是否启用在具体需求中决定。

## 工作流入口

- [AGENTS.md](AGENTS.md)：仓库协作与实现约定。
- [项目上下文](docs/project-context.md)：需求、设计建议和阶段计划。
- [工作流说明](docs/workflow.md)：角色、状态、确认规则、验证与阶段推送。
- [经验记录](docs/lessons.md)：已有证据支持的项目经验。
- [OpenSpec 配置](openspec/config.yaml)：生成制品时使用的项目上下文及规则。
- [本地 schema](openspec/schemas/offerflow-spec-driven/schema.yaml)：proposal、specs、design、tasks 的依赖和模板。
- [项目技能](.agents/skills/offerflow-workflow/SKILL.md)：可复用的变更工作流。
- [已归档的初始化记录](openspec/changes/archive/2026-10-06-bootstrap-workflow/proposal.md)：本阶段的范围、设计与验证记录。

## 使用方式

在仓库中向助手描述本阶段需求即可，也可以显式使用 `$offerflow-workflow`，例如：

> 使用 OfferFlow 工作流，为公司与岗位管理创建 change，先生成并审查方案。

需要 CLI 时安装兼容版本的 [OpenSpec](https://github.com/Fission-AI/OpenSpec)，按照其当前运行要求准备 Node.js。CLI 是开发辅助工具，不是后端运行依赖；没有 CLI 时按仓库内模板生成制品并记录验证方式。

角色定义仅提供职责和产物约定，不表示项目已经接入业务 AI Agent 或启用自动多 agent 编排。

## 阶段计划

1. 工作流与仓库基础。
2. Spring Boot 工程、Maven Wrapper、基础配置。
3. 数据库迁移与核心表。
4. 公司与岗位管理。
5. 投递与阶段历史。
6. 待办与面试管理。
7. Dashboard 和 MVP 联调。

每阶段验证通过后单独提交并推送。运行命令和环境配置在工程初始化后补齐。

## 许可证

[MIT](LICENSE)。公开示例只使用虚构数据。
