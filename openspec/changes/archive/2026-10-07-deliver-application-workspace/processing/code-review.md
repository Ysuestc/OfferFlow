# Code Review

方式：SELF_REVIEW。主助手检查实际 diff、新增服务 / DTO / VO / 前端、脚本和测试证据，不声称独立审查。

## 第 1 轮
- FAIL C1 / CODE：[App.vue](../../../../../frontend/src/App.vue)，原第 58 行 refresh 吞掉 loadCounts 的错误；当前视图查询成功、其他目录失败时可能显示错误的零计数而没有提示。改为未知计数与明确重试反馈，复核前端构建和浏览器。
- PASS：ApplicationService 创建 / 阶段变更使用真实事务，版本更新以条件写入和 MyBatis-Plus 乐观锁保护；真实 HTTP 触发器失败验证快照回滚。
- PASS：DTO 字段长度、URL、外键 / 唯一键冲突、NULL 清空与安全响应已用真实 MySQL HTTP 检查。
- PASS：V1 未修改，当前不增加字段或数据库迁移。持久脚本只使用已核对的自身 datadir，数据目录和凭据被忽略，停止不清空数据。
- WARN C2 / CODE：[CompanyService.java](../../../../../src/main/java/io/github/ysuestc/offerflow/company/service/CompanyService.java)，第 51 行；公司 / 岗位资料编辑采用完整 PUT，尚无版本冲突提示。接受理由：个人资料整理首版，投递的关键状态与备注已保护；多窗口资料并发编辑可后续扩展。
- FAIL C3 / CODE：[run_local.py](../../../../../scripts/run_local.py)，原第 22 行 Windows 对已锁定字节的读取会先抛 PermissionError，导致 --stop 探测失败。改为 stat 判断空文件并直接尝试非阻塞锁，再验证停止和重启。

## 验证中发现并解决
- 动态 SQL 首行多余字符已修复；测试客户端对 URL 重复编码已改为绝对 URI。随后 52 项 MySQL 测试通过。
- 隔离实例禁用自身 binlog，使仅限测试库的账户能创建失败触发器；未修改已有服务。
- Playwright 明确区分详情抽屉与编辑对话框，非搜索选择器采用支持的键盘交互；6 项桌面 / 手机测试通过。
- 按需注册 Element Plus，JavaScript 从约 1010 kB 降至 478 kB，未关闭体积警告。
- 重打包前停止测试实例，消除 Windows Jar 文件锁；重启后 9 条虚构记录及阶段 / 版本 / 备注与原快照一致。

## 第 2 轮
- PASS C1：失败计数置为未知并显示重试提示，不吞掉概览异常；桌面 / 手机断线用例新增概览失败与恢复断言，6 项全部通过。
- PASS C3：使用 stat 代替读取锁字节；--stop 返回 0，持有实例正常返回 0；重复启动返回“already running”，未启动第二个实例。
- 结果 PASS；无未解决 FAIL。C2 保留为明确非阻断限制。最终检查没有无关模块改动、真实投递数据或凭据。
- 样式源文件已展开为便于维护的排版，重新构建产物与已验证 Jar 逐字节一致，无行为变化。
