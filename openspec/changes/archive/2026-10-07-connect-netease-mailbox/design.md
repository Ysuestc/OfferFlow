# Design

## Context

单模块 Java 21 / Boot 3.5.16 / MyBatis-Plus / MySQL，Vue 电脑工作台随 Jar 打包。mysql profile 提供业务；standalone 仍仅健康接口。现有版本保护与六表事务规则不因邮件采集改变。

## Goals / Non-Goals

实现 proposal 的七项验收。只接入单个免费网易邮箱，手动采集；识别、MailEvent 和自动维护留给后续 change。

## Decisions

- 提供商枚举固定 IMAPS 主机：imap.163.com / imap.126.com / imap.yeah.net，993，证书及主机校验，连接 / 读取 / 写入超时；不允许接口指定任意主机。支持 ID 时发送真实 OfferFlow 客户端标识。只读打开文件夹、BODY.PEEK，关闭不 expunge。
- 授权码由页面仅写提交，数据库仅存 AES-256-GCM 密文；随机 nonce，每次重新加密。256-bit 本机密钥保存在私有目录，通过原子创建避免覆盖旧密钥。密钥缺失且已有密文时失败，不悄悄生成替代密钥。默认 private-data/mailbox；run_local 使用 private-data/<instance>/mailbox。Linux 密钥权限 600；Windows 使用本机目录继承权限，不声称提供操作系统密钥托管。备份需保留密钥与数据库。密钥、密文均不作为 API 输出或异常文本。
- 单账号固定 id=1，设置 version 防止旧表单覆盖。已有采集记录后冻结账号身份、文件夹与起点，仅可换授权码；首次采集前允许修正并清空游标。账号类与请求禁止生成含凭据的 toString。
- 同一进程的 ReentrantLock.tryLock 保护设置 / 测试 / 同步。当前单实例入口已有实例文件锁；不宣称跨进程分布式互斥。同步状态 RUNNING 可记录中断，重启不阻止重试。
- 同步独立于外部网络事务；首次 / UIDVALIDITY 改变时按 received date 搜索起点当天及以后，再按精确 Instant 本地过滤。最多处理 100 条，按 UID 升序；每条邮件或明确过滤结果与游标同事务提交，末尾推进到已确定扫描结束的位置。网络或数据库失败保留之前已提交游标，记录 PARTIAL / FAILED，下一次继续。读取失败不跳过该 UID。
- UID 元组唯一；另存 SHA-256 内容摘要并唯一防止 UID 重建时完全相同内容重复采集。摘要计算有界 RFC822 内容；超限邮件用消息头 / 大小摘要，明确不是附件下载器。邮件移动和服务端修改头部可能使摘要不同，本阶段不保证跨邮箱语义去重。失败只存固定错误类别，不存 Throwable / 协议原文。
- 输入大小 2 MiB，正文 20000 字符；优先 multipart/alternative 的 text/plain，否则 HTML 经 jsoup 转文本。限制 MIME 深度，跳过附件与转发 message/rfc822；异常解析中断以便重试，超限可确定地保留邮件头和状态。详情使用 Vue 文本插值，不使用 v-html、远程图片或自动打开链接。

## Change Scope

新增 mailbox 的 controller / dto / entity / mapper / service / vo 与 config 类；前端邮件组件独立，App 仅挂导航。追加 V3 不改旧迁移。

依赖：jakarta.mail-api / org.eclipse.angus:angus-mail 由当前 Boot BOM 管理（2.1.5 / 2.0.5），实现 MIME 与 IMAPS；jsoup 1.23.2 提取惰性 HTML 文本；com.icegreen:greenmail 2.1.14 仅 test，用于实际本地 TLS / IMAP 协议验证。无需 SMTP starter、Redis、MQ 或 Agent SDK。

官方参考：[网易协议开启](https://help.mail.126.com/faqDetail.do?code=d7a5dc8471cd0c0e8b4b8f4f8e49998b374173cfe9171305fa1ce630d7f67ac2a5feb28b66796d3b)、[服务器设置](https://help.mail.126.com/faqDetail.do?code=d7a5dc8471cd0c0e8b4b8f4f8e49998b374173cfe9171305fa1ce630d7f67ac22b85ac2e7c90cd63)、[Angus](https://eclipse-ee4j.github.io/angus-mail/)、[jsoup](https://jsoup.org/download)、[GreenMail](https://greenmail-mail-test.github.io/greenmail/)。

## Data / API / Transactions

V3 mailbox_account：id=1、email VARCHAR(254)、provider VARCHAR(24)、folder VARCHAR(128)、sync_from DATETIME(6)、credential VARBINARY(1024)、uid_validity / last_uid BIGINT 可空、last_status VARCHAR(16) 默认 NEVER、last_attempt_at / last_success_at DATETIME(6) 可空、last_error VARCHAR(32) 固定类别可空、last_imported INT 默认 0、version INT 默认 0、审计。字段分别承载配置、仅写凭据、恢复游标、操作结果与设置并发；不预建 AI 字段。

V3 mail_message：账号外键 RESTRICT、uid_validity / uid BIGINT、fingerprint CHAR(64)、message_id VARCHAR(998) 可空、subject VARCHAR(1000)、sender VARCHAR(1000)、received_at / sent_at DATETIME(6) 可空、body_text MEDIUMTEXT 可空、content_status VARCHAR(24)、body_truncated BOOLEAN、审计。唯一 UID 元组与账号 / 摘要；账号 / id 索引用于列表，不放岗位外键。正文状态 AVAILABLE / TOO_LARGE。

API（统一 JSON，mysql profile）：

- GET /api/v1/mailbox：未配置返回 data=null，否则安全设置 VO 与真实操作状态。
- PUT /api/v1/mailbox：email、provider、folder、syncFrom、authorizationCode（首次必填，后续空则保留）、version（首次 0）。返回安全 VO；非法 400、旧版本 / 身份冻结 / 忙 409。
- POST /api/v1/mailbox/connection-tests：读取保存配置，返回 status=CONNECTED 或 FAILED / errorCode / message；外部失败作为操作结果返回 200，不将异常原文透传；未设置 404。
- POST /api/v1/mailbox/syncs：返回 status / imported / scanned / hasMore / errorCode / message；部分失败 200 但 status 显式，未设置 404、忙 409。
- GET /api/v1/mailbox/messages?q=&page=1&size=20：字面量 LIKE、大小上限 100；摘要 VO 不带正文。
- GET /api/v1/mailbox/messages/{id}：安全文本详情，缺失 404。

同步开始记录 RUNNING / attemptAt，按条 TransactionTemplate 原子写邮件与游标；结果事务写固定错误类别与统计。配置更新在版本匹配的事务中完成；加密密钥创建先于数据库提交，回滚不删除已有密钥。未来 MailEvent 单独迁移，不提前追加投递来源字段。

## Verification

单测验证密钥重启 / 丢失 / 损坏、MIME 边界、真实 TLS IMAP / 未读旗标 / 认证及文件夹失败；新增隔离 MySQL 邮箱测试 schema 覆盖 V3、HTTP、加密、安全响应、去重、UID 重置、游标失败回滚、并发与六表不变。电脑端 Playwright 用隔离后端并拦截仅外部同步响应的必要场景，真实列表由数据库验证。Vue / Java 构建、完整 SQL 回归与 OpenSpec 严格验证。

真实网易端到端因未提供账号授权码记 NOT_RUN，不把 GreenMail 通过写成网易联调通过。

## Risks / Rollback

本机密钥与数据库须共同备份；丢失密钥需要重新输入授权码。网易账户可能有客户端限制，真实凭据需用户本机配置后验证。不修改个人邮箱或求职记录。回滚应用时保留 V3 数据与密钥，不能修改旧迁移或删除个人库。

## Open Questions

无阻断项。用户明确授权第一阶段；具体邮箱由运行页面配置，不索要真实凭据。定时同步和解析策略不属于本阶段。
