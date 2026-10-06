# Verification

- Result: PASS
- Environment: Windows、Java 21.0.6、Maven Wrapper 3.3.4 / Maven 3.9.16、Spring Boot 3.5.16、OpenSpec 1.14.1。
- Review mode: SELF_REVIEW

## Actual checks

| Command / Check | Exit code | Result | Evidence |
|---|---|---|---|
| .\\mvnw.cmd -version | 0 | PASS | 固定 Maven 3.9.16，使用 Java 21；不使用系统 Maven 3.6.1。 |
| 首轮 .\\mvnw.cmd -B -ntp verify | 1 | FAIL, RESOLVED | 生产源码编译成功，测试断言 API 不匹配；已修正，保留失败事实。 |
| 修复后 .\\mvnw.cmd -B -ntp verify | 0 | PASS | BUILD SUCCESS，18 tests / 0 failures / 0 errors / 0 skipped，Jar repackage 成功。 |
| Surefire XML 结果汇总 | 0 | PASS | ApiContractTest 13 项，HealthHttpTest 5 项，与构建输出一致。 |
| 可执行 Jar 内容检查与独立启动 HTTP 检查 | 0 | PASS | 含主类、不含辅助测试类；健康 200/SUCCESS/UP、缺失 404、POST 健康 405/Allow、辅助路由 404；已结束仅本次启动进程。 |
| openspec schema validate offerflow-spec-driven | 0 | PASS | 本地 schema 有效。 |
| openspec validate initialize-backend-foundation --strict | 0 | PASS | backend-foundation 新增与 development-workflow 修改合同有效。 |
| Python 文档/YAML/XML 临时检查 | 0 | PASS | 32 个 Markdown、36 处本地链接、相关 YAML、技能与 pom XML 有效。 |
| skill-creator quick_validate.py .agents/skills/offerflow-workflow | 0 | PASS | Skill is valid。 |
| git diff --cached --check | 0 | PASS | 文档 EOF 已修复；候选范围检查无凭据、求职数据、测试日志或下载缓存。 |
| Linux/macOS 上执行 Wrapper | — | NOT_RUN | 当前验证环境是 Windows；官方 Unix 脚本原样保留，LF 与可执行 Git 模式已核对。 |

本阶段无数据库行为，MySQL 事务、外键、SQL 和 Redis 不属于本次必要验收，不将这些 HTTP 测试当作数据库证明。没有独立审查或部署。

## Acceptance mapping

1. 可复现构建：Wrapper 版本和官方分发 SHA-256 固定，verify 编译 Java 21 并生成可执行 Jar。
2. 独立启动：没有持久化依赖及数据源排除配置，无 MySQL / Redis 时实际启动成功。
3. JSON 与 HTTP 合同：成功、DTO 约束、方法输入/返回值、绑定错误、畸形 JSON、缺参/类型、415、409、500 由实际 MVC 行为测试覆盖；404/405/406 与生产接口隔离由实际服务器测试覆盖。
4. 信息隔离：敏感标记未出现在错误响应，内部异常和返回值缺陷使用通用 INTERNAL_ERROR。
5. 验证与审查：构建、Jar 启动、OpenSpec、文档和自审记录齐全，阻断问题已解决。
6. 交付约定：用户明确授权一个 change 最终推送一次；实施过程尚未推送，验收后同步规范、归档并统一提交推送。

## Archive preparation

tasks 对应实施、审查和验收准备全部完成后，将 backend-foundation 的六项新增需求同步为 stable spec，并将 development-workflow 的两项修改完整替换，保留其余七项需求与原场景。归档目标为 archive/2026-10-06-initialize-backend-foundation，按 Asia/Shanghai 日期检查路径和不存在条件。

归档与一次 Git 推送为验收后的交付动作，最终结果在实际 Git 历史和交付消息中报告，不递归写入自身提交 SHA。

## Post-archive checks

- openspec archive initialize-backend-foundation --yes：退出码 0，同步 6 项新增需求和 2 项修改需求，归档日期为 2026-10-06（Asia/Shanghai）。
- openspec validate --specs --strict：退出码 0，backend-foundation 与 development-workflow 两个规范通过，0 失败。
- Unix Wrapper 使用官方原版脚本，Git 模式 100755；Windows 构建与启动均已执行。
