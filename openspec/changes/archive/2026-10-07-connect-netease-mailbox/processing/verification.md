# 验证记录

## 当前证据

- PASS：Maven Wrapper test，30 项（19 原基础 + 3 密钥 + 4 MIME + 4 实际 TLS IMAP）；GreenMail 不连接真实邮箱。
- PASS：scripts/build.py，npm ci / vue-tsc / Vite / frontend Maven verify，生成包含邮件页的 Jar。
- 环境记录：默认沙箱 Vite 子进程 spawn EPERM；使用已授权构建权限重新执行成功，不是代码错误或审批拒绝。
- FAIL（已定位，待复核）：首次隔离 MySQL 87 项中 86 成功，字面量搜索用例把 URL 百分号由 TestRestTemplate String 重复编码。改用 URI 重载保留编码，补充网络恢复和密钥丢失场景后重跑。无放宽搜索断言。
- PASS：OpenSpec validate connect-netease-mailbox --strict。
- NOT_RUN：真实网易账号联调；本次没有账号凭据，不将本地 TLS 测试视为网易端到端成功。

## 交付状态

已完成验收、审查与归档，Git 提交 / 推送由交付环节执行。

## 最终验证证据

1. PASS：`./mvnw.cmd -B -ntp test`：30 测试、0 失败 / 错误 / 跳过。3 密钥、4 MIME、4 TLS IMAP 加原 19 测试。
2. PASS：`python scripts/build.py --node <Node24> --npm-cli <npm-cli.js>`：npm ci、vue-tsc、Vite、frontend Maven verify；包含电脑页面的可执行 Jar。退出码 0。
3. PASS：`python -X utf8 scripts/verify_mysql.py --mysql-bin <MySQL8.0.34/bin>`：30 + 89 = 119 Java / SQL 测试，0 失败 / 错误 / 跳过；standalone / mysql Jar 健康、前端、业务模式及坏数据库拒绝启动通过；独占实例已关闭清理。新 9 项邮箱 SQL 测试全部通过。
4. 环境失败修复：第二轮 SQL 尚在编译阶段时，Windows GBK 标准输出不能打印解码后的告警，driver 中断，未声称该轮测试通过。verify_mysql 增加输出 errors=replace；UTF-8 续跑完成步骤 3。
5. 浏览器首轮 `npm test`：13 场景，11 通过 / 2 失败；原生 datetime-local 把带零秒格式规范化而使 Playwright fill 报 Malformed value，随后用例缺少保存配置。改用规范分钟格式、各用例独立准备配置，保留所有断言。
6. PASS：`npm test -- tests/mailbox.spec.ts`：3 个邮箱场景全部通过。结合首轮 10 个未修改基础场景，13 个唯一电脑场景都有成功证据。页面配置写实际隔离 MySQL；外部连接 / 同步和样本邮件响应为浏览器拦截，不能视为真实网易联调。截图实际查看，1440×1000 桌面布局无遮挡。
7. PASS：`openspec validate connect-netease-mailbox --strict`；实际差异 `git diff --check`。
8. NOT_RUN：真实网易端到端，无用户账号或授权码；未访问真实邮箱、未采集个人邮件。

## 验收映射

| 条件 | 实际证据 |
|---|---|
| SC1 | 密钥单测、数据库密文 / VO / version / 换码 / 丢失恢复、浏览器保存与刷新 |
| SC2 | GreenMail TLS 登录与文件夹测试、SQL 安全错误、浏览器提示 |
| SC3 | TLS 增量 / 起点过滤 / 100 条批次 / 下载期间来信、SQL UID 与摘要去重 |
| SC4 | SQL 游标触发器回滚、网络失败恢复、真实并发 409 / 释放、中断状态重试 |
| SC5 | MIME Unicode / alternative / HTML / 附件 / 超限、SQL 分页搜索、浏览器文本详情 |
| SC6 | TLS 未读 / 删除标记、SQL 六表完整快照未变 |
| SC7 | 119 Java / SQL、13 场景覆盖、生产构建、严格规范检查与明确 NOT_RUN |

日志和截图在被忽略 target 中，只含虚构验证记录。验收报告保留命令及结论，不提交原始日志、私人目录或凭据。

- PASS：schema validate 与最终 change --strict；38 个本地文件引用，V1/V2 无 diff，私有密钥目录被忽略。
- PASS：原本机实例已恢复 http://127.0.0.1:8082，health / workspace 可用；打包 V3、前端全部产物匹配，GreenMail 未进入生产包。隔离 browser 实例已停止。

- FAIL（已修复复核通过）：首次同步稳定规范严格验证发现 Purpose 少于 50 字符。恢复活动目录，补充具体采集范围与后续价值；不改业务场景或放松严格参数。

- PASS：修订 Purpose 后 `openspec validate --all --strict`，活动 change 与 8 个稳定规范共 9 项通过。

- PASS：最终归档后 8 个稳定规范全部严格通过；39 个本地文件引用可解析，归档状态 ARCHIVED，无未完成任务。
