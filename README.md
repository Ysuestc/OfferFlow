# OfferFlow

个人秋招投递管理系统，用于统一管理公司、岗位、投递进度、面试复盘和待办事项，后续逐步接入 AI Agent。

## 当前进度

基础 MVP 已完成：Vue 3 / TypeScript / Element Plus 页面连接真实 MySQL 后端，支持公司、岗位、投递档案与完整阶段历史、独立面试复盘、待办完成 / 重开和首页 Dashboard。数据保存到 MySQL，刷新或重启后保留。

后端使用 Java 21、Spring Boot 3.5.16、MyBatis-Plus 3.5.17、MySQL、Flyway、Lombok 和 Maven Wrapper。前端生产文件随 Jar 打包，页面和 API 共用一个端口。默认本机使用，没有登录或 AI；Redis 按实际需要启用。面试 / 待办不隐式改变招聘阶段，已发生面试和未来七天截止事项分别展示。

## 本地使用

构建需要 JDK 21、Node 22.12+（推荐 24）、npm 和 Python 3.11+；使用打包 Jar 仅需 Java 和 MySQL。

```powershell
python scripts/build.py
python scripts/run_local.py --mysql-bin '<MySQL 安装目录>/bin'
```

打开 http://127.0.0.1:8080，先添加公司和岗位，再建立投递。辅助脚本使用自己的 MySQL 实例，不修改已有服务；数据保留在被忽略的 private-data/local。停止可以按 Ctrl+C 或执行：

```powershell
python scripts/run_local.py --stop
```

再次启动复用原数据。重建前先停止实例，避免 Windows 的运行文件锁。系统 Node 较旧时可给 build.py 指定 --node '<Node executable>'，无需更改全局安装。

使用已有专用空 MySQL 数据库：

```powershell
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/offerflow'
$env:DB_USERNAME = 'offerflow_app'
$env:DB_PASSWORD = '<本地配置的数据库密码>'
java -jar target/offerflow-0.1.0-SNAPSHOT.jar --spring.profiles.active=mysql
```

账户由使用者准备，不默认使用 root。Flyway 首次建立六张业务表，之后校验迁移，连接 / 校验失败阻止启动；禁止 clean 和自动 baseline，已使用迁移不修改。默认仅 loopback，未实现认证，不直接开放公网。

后端基础验证和无需数据库的存活模式：

```powershell
.\mvnw.cmd -B -ntp verify
.\mvnw.cmd spring-boot:run
```

默认 standalone 仅提供健康检查和业务模式说明，不提供持久化业务。其他平台使用 ./mvnw。详细构建、前端开发、数据目录与浏览器测试见 [本地开发说明](docs/local-development.md)；接口见 [API 文档](docs/api.md)，模型见 [数据库说明](docs/database.md)。

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

公司创建接口等业务接口使用此公共合同。测试辅助端点仅存在于测试源码，不进入生产 Jar。

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

真实 MySQL 验证需要 Python 3.11+ 和 mysqld / mysql / mysqladmin 二进制。隔离脚本自动运行 mysql-integration verify，再检查可执行 Jar：

```powershell
python scripts/verify_mysql.py --mysql-bin '<MySQL 安装目录>/bin'
```

脚本初始化自身临时 datadir 和随机测试库，使用独立 loopback 端口，完成后停止自己的实例并清理目录；不连接已有 MySQL 服务。--probe 仅验证实例启动 / 清理。72 项真实 MySQL 测试覆盖迁移、六 Mapper、外键、唯一键、状态 / 结束原因、未知日期、UTC 微秒、乐观锁、事务和工作台 HTTP 行为；加上 19 项基础 HTTP 测试，共 91 项。前端另有 14 项桌面 / 手机浏览器用例，覆盖公司 / 投递 / 面试复盘 / 六类待办完整操作、完成重开、关联保护、失败输入保留、并发冲突和断线恢复。已验证 Windows / MySQL 8.0.34；MySQL 8.4 与其他操作系统的隔离脚本尚未验证。显式 CI 测试设置要求见 [数据库说明](docs/database.md#数据库验收)。

## 工作流入口

- [AGENTS.md](AGENTS.md)：仓库协作与实现约定。
- [项目上下文](docs/project-context.md)：需求、决定和阶段计划。
- [工作流说明](docs/workflow.md)：角色、状态、审查、验证与交付。
- [经验记录](docs/lessons.md)：有实际证据支持的经验。
- [OpenSpec 配置](openspec/config.yaml) 与 [schema](openspec/schemas/offerflow-spec-driven/schema.yaml)。
- [项目技能](.agents/skills/offerflow-workflow/SKILL.md)：可显式使用 `$offerflow-workflow`。
- [工作流初始化记录](openspec/changes/archive/2026-10-06-bootstrap-workflow/proposal.md)。
- [首个开发 change](openspec/changes/archive/2026-10-06-initialize-backend-foundation/proposal.md)：范围、设计和验证。
- [数据库 change](openspec/changes/archive/2026-10-07-initialize-persistence-model/proposal.md) 与 [验证证据](openspec/changes/archive/2026-10-07-initialize-persistence-model/processing/verification.md)。

默认一个助手顺序承担角色，自审标为 SELF_REVIEW。角色契约不自动启动 agent；项目尚未接入业务 AI 功能。

- [首个可操作版本](openspec/changes/archive/2026-10-07-deliver-application-workspace/proposal.md) 与 [验收证据](openspec/changes/archive/2026-10-07-deliver-application-workspace/processing/verification.md)。
- [完整基础 MVP](openspec/changes/archive/2026-10-07-complete-core-mvp/proposal.md) 与 [验收证据](openspec/changes/archive/2026-10-07-complete-core-mvp/processing/verification.md)。

## 后续阶段

1. 根据个人真实使用反馈完善基础体验。
2. 明确登录 / 数据备份等需求后逐步扩展。
3. 按独立 change 接入邮件解析、面试准备和自然语言查询等 AI 能力。

**一个 change 推送 GitHub 一次**：实施、修复、审查和验证在本地完成，验收后同步规范、归档，再统一提交并普通推送。未明确的业务决定在相应 change 中复核，不重复索要已有授权。

## 许可证

项目代码采用 [MIT](LICENSE)，公开示例使用虚构数据。官方 Maven Wrapper 脚本保留 [Apache 2.0 许可证](.mvn/wrapper/LICENSE) 和原始版权头。
