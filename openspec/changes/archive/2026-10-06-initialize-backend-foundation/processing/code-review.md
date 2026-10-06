# Code Review

- Round: 1
- Review mode: SELF_REVIEW
- Result: PASS
- Scope: 实际暂存 diff 的 pom、官方 Wrapper/校验摘要、8 个生产类、运行配置、2 个测试类、规则更新与文档。

## Evidence

公共响应保持 code/message/data，errors 仅含字段和安全约束消息。框架异常走 ResponseEntityExceptionHandler，保留实际状态及协议头；返回值缺陷按 500 处理；未知和声明的内部异常均使用通用响应。绑定失败不直接采用可能含拒绝值的框架消息。

生产源码没有测试辅助接口、空业务包、数据库自动配置排除或无用途依赖。测试依赖为 test scope，打包与实际 HTTP 证明辅助 Controller 未暴露。Wrapper 官方脚本、Apache 许可证与 SHA-256 固定分发保留，Unix 脚本 Git 模式为 100755。

Wrapper verify 的实际结果为 18 项通过，无失败、错误或跳过；可执行 Jar 检查通过。没有独立 agent 审查，不能将本自审解释为独立审查。

## Findings

| ID | Severity | Route | Location | Impact | Resolution |
|---|---|---|---|---|---|
| R-001 | FAIL | CODE | src/test/java/io/github/ysuestc/offerflow/health/HealthHttpTest.java:27 | 首轮错误使用 AssertJ MediaType 断言 API，导致 testCompile 失败。 | 已改为 MediaType 兼容性布尔断言；完整 verify 及 18 项测试通过。 |
| R-002 | FAIL | CODE | src/main/java/io/github/ysuestc/offerflow/common/exception/GlobalExceptionHandler.java:51 | 实施早期发现绑定错误默认消息可能含拒绝值，违反响应隔离合同。 | 已对绑定错误使用安全消息，真实 MVC 绑定失败测试验证拒绝值和异常类名均不返回。 |
| R-003 | WARN | CODE | README.md:115 | 候选 diff 检查发现 EOF 多余空行。 | 已规范化，git diff --cached --check 通过。 |
| R-004 | WARN | CODE | src/test/java/io/github/ysuestc/offerflow/common/ApiContractTest.java:36 | Spring 测试栈在 JDK 21 下发出 Mockito 动态 agent 提示。 | 当前测试通过且无生产 Mockito 依赖；不为未来 JDK 提示添加无用途运行配置。 |

无未解决阻断项。数据库、认证和业务行为没有在本次实现中声称已完成。

已核对 errors 是 DTO 字段/绑定校验的可选细节；方法级输入当前提供 400/VALIDATION_ERROR。行为规范与设计和 README 保持这一范围，不推定所有框架错误均有字段细节。
