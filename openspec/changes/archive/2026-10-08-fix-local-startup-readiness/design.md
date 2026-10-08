# Design

## Context

现有入口启动独占 MySQL，再启动 mysql-profile Jar。它从独占 application.log 读取实际 Tomcat 端口，支持 `--port 0`。就绪循环已有进程检测，但 urllib 的单次连接异常未捕获，且默认使用系统 / 环境代理；finally 随后关闭自有服务。用户提供了真实 WinError 10061，当前日志表明 Java 曾完成启动，无法仅凭堆栈判定是短暂连接窗口还是代理。

## Goals / Non-Goals

使已有启动命令在本机代理和暂时未就绪条件下可靠运行，保留数据隔离与退出行为。不开机自启，不更改电脑配置或业务功能。

## Decisions

1. 提取一个小的应用就绪函数，内部创建 `ProxyHandler({})` 的私有 opener；仅影响固定 127.0.0.1 探测，不调用 install_opener，不修改环境或代理注册表。
2. 以 monotonic 计时限制应用等待 60 秒，每次 HTTP 最多 3 秒且不超过剩余时间，失败后间隔 0.25 秒。连接异常、超时和 HTTPError 视为暂未就绪；检查 Java 和自有 MySQL 是否提前退出，避免无意义等待。
3. 只有统一响应中的 `data.available` 确实为 true 才返回 URL；业务模式不可用或非预期 JSON 给出简短 RuntimeError。期限结束提示查看实例 application.log，异常内容不向终端展开。
4. 保留 finally、自有数据库关闭、实例锁和文件边界保护。新增测试只用 Python unittest / 标准库，不引入依赖。

## Change Scope

scripts/run_local.py、scripts/tests/test_run_local.py、README、docs/local-development.md、项目上下文与本 change 制品。新增就绪函数是现有启动步骤的小规模提取，便于回归连接重试和期限；无额外进程框架。

## Data / API / Transactions

无字段、索引、迁移、API 或事务变化。测试写入虚构记录仅限独立 startup-review 实例，不查询个人邮件 / 求职记录；用户 local 实例只启动与访问健康、业务可用信息和页面。

## Verification

标准库行为测试覆盖坏代理直连、连接拒绝 / 超时 / HTTP 错误恢复、持续错误期限、Java / MySQL 提前退出、业务模式 false 与无效响应。真实 Jar / MySQL 独立实例执行停启与虚构记录保留验证，再恢复 local。Python 语法、OpenSpec 严格验证、Markdown 引用与 git diff 检查。Java / 数据库业务实现未改，不重复整个 Java / 前端套件。

## Risks / Rollback

应用若超过 60 秒仍未就绪，会提示失败并保留数据；可读私有日志定位。不解析或公开异常中的请求 / 凭据。回退脚本可回到原有行为，但重新出现单次探测失败风险；数据库无需回滚。

## Open Questions

无必要业务决定。用户已要求修复启动问题；阶段提交和一次推送沿用既有授权。
