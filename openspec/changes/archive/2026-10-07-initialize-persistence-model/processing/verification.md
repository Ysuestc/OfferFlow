# Verification

按实际阶段记录验证。首轮为等待业务输入期间的独立工程检查；完整交付结论见下方“完整验收”。

## 首轮：独立工程检查（历史快照）

| 检查 | 结果 | 证据 |
|---|---|---|
| initialize-persistence-model strict | PASS，退出 0 | 恢复既有 Check a started application 场景后通过；初次遗漏提示已处理 |
| MyBatis-Plus 3.5.17 依赖预检 | PASS，退出 0 | 官方 Boot 3 starter 可从 Maven Central 解析；首次 PowerShell 未引用 -D 参数失败，引用完整参数后成功 |
| wrapper verify（依赖与配置） | PASS，退出 0 | 18 tests，0 failures，0 errors，0 skipped；生成可执行 Jar；默认 standalone |
| 独立 MySQL 初始化探测 | PASS，正常关闭后退出 0 | MySQL 8.0.34，独立 datadir / 随机 loopback 端口；确认 @@datadir 为本次目录，未使用原服务 |
| Windows 初次进程清理 | 修复后 PASS | 首次终止父进程留下监控子进程，文件占用；按专属 datadir 查明测试实例，mysqladmin 正常关闭并核验边界后清理 |
| verify_mysql.py --probe | PASS，退出 0 | 自有实例初始化、创建专用随机测试库与临时账户、正常关闭、删除专属临时目录；Windows 加 --no-monitor 避免孤儿子进程 |
| 新 Jar standalone HTTP / 产物隔离 | PASS，退出 0 | java -jar target/offerflow-0.1.0-SNAPSHOT.jar --server.port=0；真实 health 200 / UP；Jar 无 Lombok 和测试类 |
| 新 Jar mysql 无效连接 | PASS，退出 0 | mysql profile 使用专属不可连接的 loopback 端口；进程非零退出，有 Communications link failure，未输出成功启动标志 |
| XML / 应用 YAML / Markdown 链接 / diff | PASS，退出 0 | Python 解析及本地链接检查，git diff --check；仅工程部分自审完成 |
| 六表迁移 / Mapper / 业务约束 / UTC / 事务 | NOT_RUN | S1 / S2 的用户答案未到达；没有创建或执行相关模型迁移 |
| mysql Jar 的迁移验收 | NOT_RUN | 业务迁移尚未建立，不能以无迁移启动代替验收 |
| MySQL 8.4 | NOT_RUN | 本次二进制为 8.0.34；不声称已经验证 8.4 |
| 完整 Code Review / 验收 / 归档 / Git 推送 | NOT_RUN | 当前 change 未完成，无中间提交或推送 |

## 首轮检查边界

普通 verify 只证明现有 HTTP 合同与 standalone 构建；不证明数据库行为。隔离脚本的 --probe 只证明启动和清理流程，不代表 SQL / 事务测试已执行。mysql-integration 尚未新增 IT，failIfNoTests=true 会阻止空 profile 伪通过。

## 首轮续跑条件

收到输入后完成方案复核、模型与 IT；执行完整 mysql-integration、mysql / standalone Jar 和无效连接检查、实际 diff 自审、文档 / OpenSpec / 任务证据核对，再同步规范、归档和一次普通推送。

## 完整验收（2026-10-07）

用户“都按推荐”后落定唯一键与拒绝方向。命令中的 MySQL 路径以占位符展示；实际二进制为本机 MySQL 8.0.34。

| 命令 / 检查 | 实际结果 | 覆盖 |
|---|---|---|
| ./mvnw.cmd -B -ntp verify | PASS，退出 0；27 个主源码、5 个测试源码编译，18 tests，0 failures / errors / skipped，生成 Jar | 构建与原 HTTP 合同 |
| python scripts/verify_mysql.py --mysql-bin <mysql>/bin | PASS，退出 0；内部 Wrapper -Pmysql-integration verify：18 HTTP + 40 MySQL tests 均通过；自有进程关闭、临时目录移除 | 全数据库验收与完整入口 |
| MySqlMigrationIT | PASS，1 test | 空库建六表、拒绝非空自动基线、重复 migrate=0 保留数据、checksum 不符 validate / migrate 失败、clean 禁用 |
| MySqlPersistenceIT | PASS，39 cases | 六 Mapper / 中文与 Unicode、审计时间、日期未知、单档案 / 不同批次、孤儿 / 删除 / 跨档案关联、19 项非法静态事实、5 类结束原因、NULL 清空、UTC 微秒、过期 version=0 rows、事务提交 / 回滚并保留原历史 |
| 脚本内实际 Jar standalone / mysql | PASS，health 200 / UP；产物无 Lombok 和测试类 | 运行模式与可执行产物；mysql 实际连接已迁移的自有库 |
| 脚本内不可连接的专属 loopback 端口 | PASS，Jar 非零退出；Communications link failure，未成功启动 | mysql 失败时不退回 standalone |
| 缺少 OFFERFLOW_TEST_* 的 failsafe:integration-test failsafe:verify | 预期失败 PASS；Maven exit 1，Missing required integration setting，在连接前失败 | 防止缺失设置时静默跳过或访问应用库 |
| 实际报告读取 | PASS | Surefire 13 + 5，Failsafe 1 + 39，均 0 failures / errors / skipped；最后正向执行覆盖负向测试报告 |
| schema validate offerflow-spec-driven / validate initialize-persistence-model --strict | PASS，退出 0 | schema 和修订后的事务支持规范 |
| XML / YAML / Python AST / Markdown 本地链接 | PASS，退出 0，检查 95 个版本管理及待新增文件 | 文档与配置 |
| git diff --check / ignore | PASS；target 日志、.env、本地配置均被忽略 | 提交卫生；未加入真实求职数据或凭据 |
| MySQL 原有服务状态 | Running | 测试未停止或使用原有服务 |

## 验收条件对应

1. 版本化初始化 / 重跑 / checksum：MySqlMigrationIT、应用上下文实际空库迁移。
2. 六实体读写：MySqlPersistenceIT roundTripsAllSixMappersAndPreservesDatabaseAuditTimes / unknown dates。
3. 外键 / 唯一性 / 状态：MySqlPersistenceIT 的 5 项孤儿、父删除、跨档案、重复档案、19 项静态约束和 5 类结束原因。
4. 并发与事务：过期版本更新影响 0 行；失败事务快照、版本和新历史一起回滚；成功事务保留原历史并提交新条目。
5. 启动模式：18 HTTP tests、standalone / mysql 实际 Jar、不可用数据库失败。
6. 工程交付：完整脚本、实际报告、SELF_REVIEW、README / database 文档、格式与 OpenSpec 检查。归档后的稳定规范 / 路径检查继续记录为交付检查。

## 限制和修复记录

- MySQL 8.4 与其他操作系统隔离脚本 NOT_RUN。本次实际所需 MySQL 验收使用 8.0.34，未用 Mock / H2 替代。
- Windows 初次清理子进程问题已修复并验证。首次完整脚本隐藏子进程没有转发 Maven 输出，但 XML 证实 40 cases 通过；改为显式输出管道后重跑完整入口，58 tests 与三个 Jar 检查通过。
- 事务 spec 在代码审查中澄清为显式 Spring 事务内的共同提交 / 回滚；不宣称已实现阶段推进服务，没有删除或跳过必要验证。
- JDK 21 有 Lombok 处理器发现提示与既有 Mockito 动态 agent 提示，未影响编译 / 测试；本次未扩展到其他 JDK。

## 稳定规范同步与交付清单

- 新增 persistence-model：九项需求与已验证场景完整同步。
- 修改 backend-foundation 的 Independent HTTP liveness，完整保留 Check a started application 场景，并增加 mysql 正常 / 失败启动；保留其他五项原需求。
- schema / change strict、所有任务、审查与验收逐项核对完成；归档路径为 Asia/Shanghai 的 2026-10-07，源 / 目标在本仓库 changes 内，目标不得已存在。
- 归档后检查稳定规范、README 引用、状态和实际 Git 清单；提交只含本 change，不包含 target 报告、凭据、参考原件或个人数据。
- 推送前 git fetch origin main 成功，HEAD...origin/main 为 0 / 0，无远程变更需要整合。

## 归档后检查

- OpenSpec archive 正常退出 0：新增 persistence-model 九项需求，修改 backend-foundation 一项，归档名称为 2026-10-07-initialize-persistence-model。
- 补全新稳定规范 Purpose，README / 项目上下文使用实际归档路径；validate --specs --strict 三份规范全部通过。
- 确认活动目录消失、归档目录存在、八项任务全部完成；稳定 backend-foundation 保留六项需求，persistence-model 九项，无 TBD 占位符。
- XML / YAML / Python AST / Markdown 本地链接与 git diff --check 在移动后再次通过。后续 Git 提交和一次普通推送只属于交付动作，实际 SHA / 远程结果由消息报告，不回写自身 SHA。
- 暂存后完整 diff 检查发现 23 个新增文件有多余 EOF 空行；此前未暂存的新文件不在普通 git diff 范围内。已仅规范末尾空行，不改 SQL / Java 行为，并使用实际 Flyway 11.7.2 ChecksumCalculator 对 V1 修正前后计算，两者均为 943653108。迁移校验和保持一致；临时比对代码 / SQL 副本在忽略的 target 中，后续以暂存区 check 作为最终格式证据。
- 重新暂存后 git diff --cached --check 与 git diff --check 均退出 0；没有未暂存修改。最终清单 46 个直接相关文件，未暂存任何 target / 本地凭据 / 原始参考文件。
