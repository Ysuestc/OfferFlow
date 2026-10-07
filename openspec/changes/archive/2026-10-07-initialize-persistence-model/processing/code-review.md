# Code Review

- 方式：SELF_REVIEW；第 1 轮，仅独立工程部分，不是完整模型验收。
- 已审查实际 pom.xml / application.yml / 两个 profile、新脚本及对应 README / 方案 / 验证记录。
- 依赖由固定 MyBatis-Plus 版本和 Boot BOM 管理；未添加无关框架。默认 standalone 原有 18 tests 通过；真实 Jar 同样通过 HTTP 检查。mysql 无效连接实际使 Jar 非零退出，未静默退回 standalone。Lombok 与测试类未进入 Jar。
- 脚本 --no-defaults 排除原配置，先验证 @@datadir，再对本次新实例创建库；生成的账户密码只在内存和子进程环境中；没有输出密码。清理检查临时目录边界且只停止自己启动的进程。--probe 实际通过，Windows 监控子进程问题已处理。
- XML、应用 YAML、Markdown 本地链接和 git diff --check 通过；公共文件未写本机凭据或个人求职数据。

| 编号 | 结果 / 归属 | 位置 | 影响与处理 |
|---|---|---|---|
| C1 | PASS / CODE | scripts/verify_mysql.py:66 | Windows 使用二进制帮助确认支持的 --no-monitor，正常 shutdown / wait 后清理；真实探测退出 0 |
| C2 | WARN / CODE | pom.xml:89 | IT 尚未加入，完整数据库验证不可执行；failIfNoTests 防止空 profile 通过。待 S1 / S2 输入后完成，并重审模型 |

尚未形成六表迁移，不能审查外键 / CHECK、快照历史或跨实体 SQL。完整代码审查、数据库验收、稳定规范同步、归档、提交及推送保持未完成。

## 第 2 轮：完整实现

- SELF_REVIEW，PASS。审查 V1 实际 SQL、六个实体 / Mapper、枚举、BaseEntity、PersistenceConfiguration、MySQL 测试和隔离 / Jar 验证入口，读取实际 Surefire / Failsafe 报告。
- 18 HTTP + 40 MySQL tests，0 failures / errors / skipped。SQL 中外键、组合唯一键、单档案唯一键及精确代码 CHECK 与场景一致。所有父表删除 RESTRICT，无历史级联删除。
- 乐观锁实际拒绝过期版本；审计字段由数据库维护；NULL 阶段日期 / 原因可以清空。Test 的显式 Spring 事务证明快照及历史共同提交 / 回滚，保留旧条目；尚未提供业务阶段服务。
- 默认 standalone 与真实 mysql Jar 均 health 200 / UP，无效数据库连接非零退出。缺失测试设置的负向验证在连接前失败（预期 Maven exit 1）。已有本机 MySQL 服务仍 Running；测试实例正常关闭、临时目录移除。

| 编号 | 结果 / 归属 | 位置 | 影响与处理 |
|---|---|---|---|
| C2 | CLOSED / CODE | pom.xml:89 | 已新增并执行 40 项真实数据库测试，空 profile 不再是当前状态 |
| C3 | FIXED / SPEC | specs/persistence-model/spec.md:82 | 原措辞可能把事务基础误读为已完成阶段服务；明确显式分组事务的共同提交 / 回滚，保留原测试和验收，没有删除所需检查 |
| C4 | FIXED / CODE | scripts/verify_mysql.py | 首次 Windows 隐藏子进程未显示 Maven 输出，报告证实 40 项执行成功；改为显式管道转发，再执行完整脚本、58 tests 与三个 Jar 检查通过 |
| C5 | WARN / VERIFY | design.md:20 | 本次真实验证 MySQL 8.0.34，8.4 NOT_RUN；不宣称 8.4 已验收，部署前需要对应版本验证。用户需求未限定 8.4，此限制不阻断本次 MySQL 交付 |

没有未解决 FAIL。没有个人记录、原始参考资料或真实密码进入提交范围。Mapper 只作持久化；业务校验、历史写入和更新冲突的 HTTP 映射由后续 Service / DTO / VO change 实现。

## 提交前格式复核

C6（CODE / 格式）：暂存后的完整 diff 检查覆盖了此前未暂存的新文件，发现多余 EOF 空行。规范这些空行后，用实际 Flyway ChecksumCalculator 确认 V1 校验和 943653108 修正前后相同；没有改变 SQL 语句或 Java 行为，不需要重复数据库测试。最终暂存区检查继续核对，不使用普通未暂存 diff 代替完整交付检查。
