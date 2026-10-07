# Proposal

## Why
用户要求继续开发并尽早得到有前端、可实操的系统。当前仅有存活接口和六表持久化基础，不能录入业务数据。本 change 交付公司 → 岗位 → 投递 → 阶段历史的完整操作闭环，作为首个可用版本。

## What Changes
- 公司、岗位增删改查；搜索、分页及引用保护。
- 建立单岗位投递档案，编辑主要渠道 / 日期 / 备注，更新阶段并保留历史。
- Vue 3 / TypeScript / Element Plus 工作台，真实调用 MySQL 后端，处理加载、空数据、错误及并发冲突。
- 前端可打入可执行 Jar；提供本地持久化运行入口和可复现验证。
- 既有 standalone 存活模式保持可用；业务 API 在 mysql 模式启用。

## Capabilities
### New Capabilities
- application-workspace：业务接口、阶段历史与可运行前端。
### Modified Capabilities
无；沿用 backend-foundation 和 persistence-model 合同。

## Success Criteria
1. 浏览器从空数据库创建公司、岗位和投递，刷新页面后数据仍存在；可编辑、搜索及分页。
2. 更新到二面或结束，展示当前阶段和完整追加历史；未知日期留空，结束必须选择原因。
3. 同岗位重复投递、被引用资源删除及陈旧版本写入返回安全 409；错误操作不留下部分历史。
4. Java / 真实 MySQL 行为测试、前端类型检查与生产构建、浏览器操作闭环通过。
5. 打包 Jar 从同一端口提供界面及 API，本地启动说明和数据保留方式可执行。

## Non-goals
面试复盘、待办、完整 Dashboard、登录 / 公网部署、历史删除或独立补录 API、AI、Redis。当前适用于个人本机，默认 loopback。

## Impact
新增紧密关联的 service / controller / dto / vo 与 frontend；无需新增数据库字段或修改 V1。增加前端构建及浏览器测试依赖，用途见 design.md。按用户既有授权完成审查、归档后统一普通推送一次。
