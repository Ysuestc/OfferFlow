# Proposal

## Why

OfferFlow 目前只有工作流文档，没有可运行后端。当前用户明确要求使用工作流完成首个开发 change，并要求一个 change 推送 GitHub 一次。先建立后续业务模块可复用、可验证的 Web 工程基础。

## What Changes

- 建立 Java 21 / Spring Boot 3.5.16 单模块 Maven 工程。
- Maven Wrapper 3.3.4 固定 Maven 3.9.16，提供跨平台构建入口。
- 建立统一 JSON 响应、基础错误码、全局异常处理与 Jakarta Validation 支持。
- 提供 GET /api/v1/health 作为进程存活检查。
- 使用真实 Spring MVC 与嵌入式 HTTP 测试验证响应、校验、路由和错误处理。
- 更新授权记录、运行说明，并落实验收归档后每个 change 推送一次。

## Capabilities

### New Capabilities
- `backend-foundation`: 可构建运行的后端工程、公共 HTTP 合同与验证入口。

### Modified Capabilities
- `development-workflow`: 明确当前授权工程基础与业务确认的区别，规定每个 change 统一推送一次。

## Success Criteria

1. JDK 21 下 Maven Wrapper 能构建并运行可执行 Jar；不依赖系统旧 Maven。
2. 无 MySQL / Redis 环境时本阶段能够启动，存活接口返回 HTTP 200 和约定 JSON。
3. 客户端输入或协议错误返回真实 4xx 和统一结构；未知异常返回 500 且不泄露异常原文与堆栈。
4. 验证 DTO 字段、方法参数和返回值约束，返回值缺陷必须是 500。
5. 测试与打包、真实 HTTP 验证、OpenSpec 严格校验和自审有实际证据。
6. 验收后同步规范和归档，最终一次普通推送至指定 OfferFlow 远程。

## Non-goals

数据库表与持久化、公司/岗位 CRUD、投递规则、面试/待办/Dashboard、认证、多用户、前端、AI、Redis、CI 和部署。不提前批准全部业务设计建议。

## Impact

新增 pom、Wrapper、src/main 与 src/test；修改项目上下文、README、当前协作规则和 OpenSpec 授权描述。依赖仅包含 Web、Validation 和测试 starter，后续数据库 change 再引入实际需要的持久化、迁移与实体工具。
