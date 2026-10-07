# Verification

## 环境与边界
Windows、Java 21.0.6、MySQL 8.0.34、现有 Node 24.19.0、Maven Wrapper、已安装 Chrome。测试只操作隔离脚本新建四个 offerflow_it_* 库或 private-data/browser-review 的虚构记录；没有读取 / 导出个人条目。

## 当前执行证据
| 检查 | 结果 | 证据 |
|---|---|---|
| change strict | PASS | openspec validate complete-core-mvp --strict |
| 真实 MySQL / HTTP | PASS（最终） | scripts/verify_mysql.py；19 HTTP + 72 SQL / HTTP，失败 0、错误 0、跳过 0；target/mvp-mysql-final.log |
| V1 → V2 | PASS | MySqlMigrationIT 先 target=1 写虚构复盘 / 待办，升 V2 后原文本及关联保留、version=0、负版本拒绝 |
| 时区、七天、逾期、统计 | PASS | MySqlMvpIT 固定 UTC Clock，微秒左闭右开、最近已发生面试、未知数量、当前 Offer / 历史投递事实 |
| 并发 / 关联 / 完成 | PASS | 两个真实 HTTP 写入者仅一方成功；跨档案引用拒绝；引用删除拒绝；完成重复不改时刻、重开清空 |
| 生产构建 | PASS（最终） | scripts/build.py 指定现有 Node / npm-cli；vue-tsc、Vite、-Pfrontend verify；主包 359.03 KB，target/mvp-build-release.log |
| Jar 实际启动 | PASS | standalone / mysql HTTP health、业务模式及前端，错误 DB 阻止启动；最终 Jar 浏览器入口实际运行 |
| 浏览器 | PASS（全部 14 用例） | target/mvp-browser-final.log 12 成功；相关待办 2 例最终 target/mvp-browser-todo-final.log 成功；手机导航修复后其余 6 例复核成功（target/mvp-browser-mobile-final.log） |
| 视觉检查 | PASS | 实际读取 target/visual-review/desktop-mvp-dashboard.png、mobile-mvp-dashboard.png；手机六入口三列两行、表单与列表无横向溢出 |
| 原本地实例 | PASS | 停止 browser-review 并确认退出，恢复同一 private-data/local / 8082；HTTP 健康 UP、业务可用、最终前端资源 200，Flyway V2 升级；不读取个人记录 |
| 引用 / 隔离 | PASS | 本地 Markdown 文件引用、V1 无 diff，私有凭据 / node_modules / target 保持忽略；安装的 MySQL 服务 Running |

## 失败及修复
- 一次过早启动浏览器验收实例与 Maven repackage 重叠，Windows Jar 文件锁导致构建失败，验收实例也无法就绪。停止自有实例并等待最终 build 完成后再启动，重新构建 PASS；数据保留，安装的 MySQL 服务保持 Running。
- 新增页面带必填标记，测试的 combobox exact 名称未包含星号，导致定位超时；改为语义名称正则。首页默认入口变化需要原有测试刷新后明确导航投递台账。没有放宽断言或增加任意等待。
- 清除关联测试第一次使用了带祖先作用域的 has，定位错误；修正后真实手机 DOM 仍无清除图标，证实控件依赖鼠标悬停。新增明确“解除面试关联”按钮，测试以真实按钮操作；同时按唯一虚构标题筛选，避免保留测试数据影响分页。
- 手机截图显示首页导航文字挤成竖排，改为三列两行并保留桌面布局；重新查看截图确认六入口均横向可读。

## 验收映射
1. 面试完整内容、未知 / 清空和冲突：MySqlMvpIT + 两个尺寸复盘与并发用例。
2. 六类待办、完成 / 重开、关联和引用删除：20 个新 SQL / HTTP 行为测试及两尺寸真实新增 / 清除 / 删除操作。
3. 首页数量、排序和时间边界：固定 UTC Clock SQL 测试、首页实际数量比对、窗口导航与故障恢复。
4. 升级 / 打包 / 回归：V1 target 升级样例保留、最终 91 测试、14 用例、生产 Jar 和原目录重启。
5. 文档和交付准备：SELF_REVIEW 第 2 轮 PASS，schema / change strict PASS，tasks 完成后同步稳定规范并归档；不在中间推送。

## NOT_RUN
Linux / macOS、MySQL 8.4、Safari / 实体手机、外网部署。本次不声称这些环境验证通过。

## 归档
CLI 严格校验 PASS，归档源 / 目标边界已核对；同步面试 2、待办 2、Dashboard 3 条稳定需求，更新实际 Purpose 并归档。最终运行入口与生产构建 HTML / assets SHA-256 一致。Git 交付按既有授权普通提交和推送一次。
