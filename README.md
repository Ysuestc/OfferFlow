# OfferFlow

个人秋招投递管理系统，用于统一管理公司、岗位、投递进度、面试复盘和待办事项，后续逐步接入 AI Agent。

## 当前进度

首个开发 change `initialize-backend-foundation` 提供可运行的单体后端工程、统一 JSON 响应、错误处理、Jakarta Validation 支持和进程存活接口。公司、岗位、投递等业务能力按后续阶段开发。

当前使用 Java 21、Spring Boot 3.5.16、Maven Wrapper 3.3.4 / Maven 3.9.16。MyBatis-Plus、MySQL、Flyway 和实体工具在对应模块加入，Redis 按实际用途启用。后续前端考虑 Vue 3、TypeScript、Element Plus。

## 本地运行

需要 JDK 21，设置 JAVA_HOME 或确保 java 在 PATH 中。首次构建需要网络，Wrapper 自动获取固定版本 Maven；无需使用系统 Maven。

Windows PowerShell：

```powershell
.\mvnw.cmd -B -ntp verify
.\mvnw.cmd spring-boot:run
```

macOS / Linux：

```bash
./mvnw -B -ntp verify
./mvnw spring-boot:run
```

打包后也可以直接运行：

```bash
java -jar target/offerflow-0.1.0-SNAPSHOT.jar
```

默认地址为 http://127.0.0.1:8080。SERVER_ADDRESS / SERVER_PORT 环境变量可覆盖监听地址和端口。当前阶段启动不需要 MySQL 或 Redis。

```powershell
Invoke-RestMethod http://127.0.0.1:8080/api/v1/health
```

## HTTP 基础合同

GET /api/v1/health 返回：

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": { "status": "UP" }
}
```

此接口表示 HTTP 进程存活，不代表数据库或全部业务能力已就绪。

普通 JSON API 保持 code、message、data 字段。失败返回真实 HTTP 状态，不统一包装为 200；错误 data 为 null。DTO 字段校验返回 HTTP 400，例如：

```json
{
  "code": "VALIDATION_ERROR",
  "message": "请求参数校验失败",
  "data": null,
  "errors": [{ "field": "name", "message": "名称不能为空" }]
}
```

校验示例用于解释公共合同，当前没有公司创建接口。测试辅助端点仅存在于测试源码，不进入生产 Jar。

| HTTP | code | 语义 |
|---|---|---|
| 400 | BAD_REQUEST | JSON、类型或必要参数格式错误 |
| 400 | VALIDATION_ERROR | DTO 或方法输入约束失败 |
| 404 | NOT_FOUND | 路由或资源不存在 |
| 405 | METHOD_NOT_ALLOWED | 不支持的方法，保留 Allow 头 |
| 406 | NOT_ACCEPTABLE | 客户端不接受 JSON，可无响应体 |
| 409 | CONFLICT | 已声明的业务冲突 |
| 415 | UNSUPPORTED_MEDIA_TYPE | 不支持的请求内容类型 |
| 500 | INTERNAL_ERROR | 服务端异常或返回值约束缺陷 |

未知异常原文、堆栈和拒绝的输入值不进入响应。详细错误留在服务端日志。公共实现见 [响应类型](src/main/java/io/github/ysuestc/offerflow/common/api/ApiResponse.java) 和 [异常处理](src/main/java/io/github/ysuestc/offerflow/common/exception/GlobalExceptionHandler.java)。

## 验证

`verify` 编译、执行行为测试并打包可执行 Jar：

- Spring MVC 测试覆盖成功响应、DTO 与方法约束、绑定错误、协议错误、业务异常和错误信息隔离。
- 随机端口 HTTP 测试覆盖实际存活、404、405、406，以及测试辅助接口未暴露。

当前没有数据库，因此这些测试不代表 MySQL 事务或约束验证；数据库 change 使用独立 MySQL 测试环境。

## 工作流入口

- [AGENTS.md](AGENTS.md)：仓库协作与实现约定。
- [项目上下文](docs/project-context.md)：需求、决定和阶段计划。
- [工作流说明](docs/workflow.md)：角色、状态、审查、验证与交付。
- [经验记录](docs/lessons.md)：有实际证据支持的经验。
- [OpenSpec 配置](openspec/config.yaml) 与 [schema](openspec/schemas/offerflow-spec-driven/schema.yaml)。
- [项目技能](.agents/skills/offerflow-workflow/SKILL.md)：可显式使用 `$offerflow-workflow`。
- [工作流初始化记录](openspec/changes/archive/2026-10-06-bootstrap-workflow/proposal.md)。
- [首个开发 change](openspec/changes/archive/2026-10-06-initialize-backend-foundation/proposal.md)：范围、设计和验证。

默认一个助手顺序承担角色，自审标为 SELF_REVIEW。角色契约不自动启动 agent；项目尚未接入业务 AI 功能。

## 后续阶段

1. 数据库迁移与核心表。
2. 公司与岗位管理。
3. 投递与阶段历史。
4. 待办与面试管理。
5. Dashboard 和 MVP 联调。

**一个 change 推送 GitHub 一次**：实施、修复、审查和验证在本地完成，验收后同步规范、归档，再统一提交并普通推送。未明确的业务决定在相应 change 中复核，不重复索要已有授权。

## 许可证

项目代码采用 [MIT](LICENSE)，公开示例使用虚构数据。官方 Maven Wrapper 脚本保留 [Apache 2.0 许可证](.mvn/wrapper/LICENSE) 和原始版权头。
