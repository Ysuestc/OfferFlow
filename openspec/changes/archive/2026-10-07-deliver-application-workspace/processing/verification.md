# Verification

2026-10-07，Windows，Java 21.0.6 / Node 24.19.0 / MySQL 8.0.34；只使用虚构记录、脚本新建测试库和本项目忽略的数据目录。审查 SELF_REVIEW。

| 检查 | 实际命令 / 入口 | 结果 |
|---|---|---|
| 方案规范 | openspec validate deliver-application-workspace --strict | PASS |
| 完整构建 | python scripts/build.py --node '<已有 Node 24 executable>' | PASS，退出 0；npm ci / vue-tsc / Vite / Wrapper -Pfrontend verify |
| 基础 HTTP | Wrapper verify | PASS，19 项，0 失败 / 错误 / 跳过 |
| MySQL 全回归 | python scripts/verify_mysql.py --mysql-bin '<MySQL binary directory>' | PASS，52 项：迁移 1、持久化 39、工作台 HTTP 12 |
| Jar | 隔离脚本内启动 standalone / mysql，实际 HTTP | PASS，存活、前端入口、业务模式标志；无效连接阻止启动 |
| 桌面 / 手机 | OFFERFLOW_E2E_URL 指向独立实例，Chrome，frontend npm test | PASS，6 项，0 失败；最终运行 25.6 秒 |
| 持久重启 | 同一实例重启，比较全部原 9 条档案 id / 关联 / 阶段 / 日期 / 备注 / 原因 / submitted / version | PASS，原数据一致 |
| 停止 / 单实例 | run_local.py --instance browser-review --stop；重复启动同一目录 | PASS，停止命令和实例退出均 0；重复启动明确拒绝，没有第二个数据库 |
| 资源门控 | 暂移 target/frontend/static/index.html 后 Wrapper -Pfrontend validate，并 finally 恢复 | PASS，预期退出 1、RequireFilesExist 指向缺失入口；不会打包半成品 |
| 迁移与忽略 | git diff --exit-code V1；git check-ignore private-data / target 证据 | PASS，V1 未改，数据 / 凭据 / 日志 / 截图均忽略 |
| 既有服务 | Get-Service MySQL，前后核验 | PASS，Running，未停止 / 改配置 / 改账户 |

## 验收对应
1. 公司 → 岗位 → 投递、编辑、搜索、分页与刷新：12 项 MySQL HTTP + 两个视口浏览器操作；所有数据真实保存。
2. 二面跳过、结束原因、历史保留、未知日期：HTTP 验证初始 / 追加 / 回退 / NULL；浏览器三条时间线、结束必选原因和刷新。
3. 重复投递 / 引用删除 / 陈旧版本 / 部分写入：HTTP 409 与真实并发；触发器使第二步失败后快照回滚；浏览器失败表单保留并可加载最新版本。
4. 编译、SQL、前端和浏览器：71 项 Java / MySQL + 6 项浏览器，最终代码检查通过。C1 概览异常修复新增真实断线与恢复断言，C3 Windows 文件锁修复通过实际停止 / 重启。
5. 同端口 Jar 和数据持久化：Jar 内含页面，MySQL 业务可操作，停启后恢复。Node 构建 JS 478 kB（gzip 156 kB），无体积警告；系统字体无需外网。

## 修复和验证边界
首轮 SQL 文本首行、测试 URL 编码、触发器 binlog 权限、浏览器控件定位、Jar 文件锁、概览错误反馈与 Windows 锁字节探测问题均已修复并针对性复核，详见 code-review.md。测试输出保存于忽略的 target；不把包含环境属性的完整报告提交公开仓库。

NOT_RUN：MySQL 8.4、其他操作系统、Safari / 实体手机；手机验证为 Chrome 的 iPhone 13 尺寸与触摸模拟。当前本机单用户，无公网认证验收；公司 / 岗位资料 PUT 暂无版本保护，为明确首版限制。面试 / 待办 / 完整 Dashboard 不在本次验收范围。

归档核验 PASS：schema / change strict，稳定规范 4/4、修改文件的 Markdown 本地引用、git diff / staged diff --check，公开提交清单无数据 / 凭据。样式源文件展开为可维护格式后重新构建，最终前端产物与已验证 Jar 逐字节相同。交付 SHA 与远程结果在交付消息报告，不递归写入自身提交。
