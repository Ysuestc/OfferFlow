# Design

## Context

仓库只有已归档工作流基础；main 与 origin/main 同步，工作区开始时干净。Java 21.0.6 可用，系统 Maven 3.6.1 低于 Spring Boot 3.5 的最低 3.6.3 要求。用户本次明确授权开始首个开发 change；工程基础不决定尚待复核的数据模型和状态业务规则。

## Goals / Non-Goals

目标：最小可维护工程、统一 HTTP 边界、明确测试和一次交付。
非目标：空业务包、空 Service/Mapper、无用途配置类、数据库集成或演示业务接口。

## Decisions

### 工程与依赖

- 项目根作为单 Maven 模块，groupId io.github.ysuestc、artifactId offerflow、版本 0.1.0-SNAPSHOT。
- Java release 21；Spring Boot 3.5.16 的 parent/BOM 管理依赖版本。保持用户要求的 Boot 3。
- starter-web：JSON、Spring MVC 和嵌入式 Tomcat；starter-validation：Jakarta 约束；starter-test：JUnit、断言、Spring MVC/HTTP 集成验证。
- 本阶段使用 records 表达不可变响应，无 Lombok 需求。MyBatis-Plus/MySQL/Flyway 在数据库阶段添加，Lombok 在实体阶段复核实际用途。
- 官方 Wrapper 3.3.4 only-script 脚本，Maven 3.9.16 官方分发地址和 SHA-256 固定；不引入 Wrapper Jar 或修改全局 Maven。
- 不增加空 service/mapper/entity/dto/config；业务模块按后续能力逐步建立。

### 包与公共 HTTP 合同

主包 io.github.ysuestc.offerflow。common.api 保存 ApiResponse / ApiErrorCode / FieldViolation；common.exception 保存业务异常和全局处理；health.controller / health.vo 提供实际存活接口。

统一字段为 code（稳定字符串）、message、data；校验时增加 errors（field / message）。成功为 SUCCESS；普通失败 data 为 null。HTTP 状态保持真实，避免总是 HTTP 200。

本阶段错误码：BAD_REQUEST、VALIDATION_ERROR、NOT_FOUND、METHOD_NOT_ALLOWED、NOT_ACCEPTABLE、UNSUPPORTED_MEDIA_TYPE、CONFLICT、INTERNAL_ERROR。框架协议异常保留响应头（例如 405 的 Allow），不输出原始解析异常、拒绝值、SQL 或异常堆栈。

GlobalExceptionHandler 继承 ResponseEntityExceptionHandler，复用框架异常路由并在统一出口转换 JSON。DTO 校验输出字段名和约束消息；Spring MVC 方法参数约束归为 400，返回值约束归为 500。业务异常使用明确错误码；未知 Exception 返回通用 INTERNAL_ERROR，完整故障写服务端日志。

统一 JSON 合同适用于支持 application/json 的普通 API 请求；不接受 JSON 的客户端可以收到 406，无可协商响应体。已提交响应/断开连接不伪造可写 JSON。

### 运行配置

默认绑定 127.0.0.1:8080；SERVER_ADDRESS / SERVER_PORT 可覆盖。应用名 offerflow，HTTP UTF-8。异常默认不包含消息、堆栈或绑定错误。没有数据源自动配置依赖，因此启动不需要关闭数据库自动配置，也不需要虚构 H2 验证。

GET /api/v1/health 返回 data.status=UP，只表示 HTTP 进程存活；不声称数据库、Redis、认证或全部 MVP 已就绪。

### 验证

- Spring MVC 测试使用仅存在于 src/test 的受控 Controller 验证 DTO、方法参数/返回值、未知异常与业务冲突；不将测试辅助接口打进生产 Jar。
- 随机端口嵌入式 HTTP 测试验证真实健康、404、405；验证输出内容和 Allow 头。
- Wrapper verify 完成测试和可执行 Jar；随后单独启动 Jar 验证真实 GET 健康/404，并关闭本次进程。
- 不对不存在的数据库行为声称集成验证；下一 change 使用独立 MySQL 测试环境。
- OpenSpec schema、change 和同步后的 stable specs 严格检查，文档链接与 diff 校验。

## Workflow Adjustment

更新过时“仅授权文档”状态为本次工程基础授权；整体业务规则继续待对应 change 复核。每个 change 只在实现、审查、验收和归档完成后统一提交并普通推送一次，不在中途推送。推送 SHA 在最终消息和 Git 历史体现，不递归写回记录。

## Risks / Rollback

首次构建需要网络下载 Wrapper/Maven 与依赖，缓存保存在本机；不能通过关闭 TLS 或删除测试绕过失败。源码和公共错误合同变更要在后续功能中保持一致。回滚使用正常 revert；不重写远程历史。本阶段没有数据库迁移或用户数据。

## Sources

- [Spring Boot 3.5 系统要求](https://docs.spring.io/spring-boot/3.5/system-requirements.html)。
- [Maven 官方版本记录](https://maven.apache.org/docs/history.html)。
- [Maven Wrapper](https://maven.apache.org/tools/wrapper/)。
- [Spring MVC 校验](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html)。
- [ResponseEntityExceptionHandler](https://docs.spring.io/spring-framework/docs/6.2.x/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/ResponseEntityExceptionHandler.html)。

## Open Questions

本阶段无必要业务输入缺失。全局业务语义不作为本工程 change 的验收前提，也不自动视为已确认。
