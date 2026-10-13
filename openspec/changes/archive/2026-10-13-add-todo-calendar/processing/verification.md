# Verification

## 环境与范围

2026-10-13（Asia/Shanghai），Windows、Java 21.0.6、MySQL 8.0.34、Node 24.19.0、Chrome。仅 calendar-review 和脚本独占随机测试库写入虚构数据；个人 local 停止后保留，未对个人求职内容运行测试。

## 已执行 PASS

- 完整 `python scripts/build.py --node '<现有 Node24>' --npm-cli '<npm CLI>'`：npm ci / vue-tsc / Vite / Wrapper -Pfrontend verify 与可执行 Jar 成功；30 基础测试，0 失败 / 错误 / 跳过。
- `python -X utf8 scripts/verify_mysql.py --mysql-bin '<MySQL bin>'`：30 基础 + 91 真实 MySQL，共 121 项，0 失败 / 错误 / 跳过；独占实例正常停止清理；mysql / standalone Jar health / 页面 / workspace 与坏数据库阻止启动检查通过。Mvp IT 新增 2 项覆盖区间半开边界、分页 / 计数 / 状态、无范围兼容、缺端 / 逆序 / 超长 / 非法时间及 62 天可用。
- 前端最终跨年标签 / 月格公司名调整后，再跑 vue-tsc / Vite 与 Wrapper -Pfrontend -DskipTests package；最终静态生成目录经过边界核实清理再复制，避免旧块混入。Java 未再变化，已通过的 Java 套件不机械重跑。
- `--instance calendar-review --port 8083`：实际 Jar ready，浏览器测试只用此独立空间。
- 手工 OpenSpec：schema / metadata YAML 解析、对应名称与必要制品、Requirement SHALL / MUST、四级 Scenario 和 WHEN / THEN 检查通过；git diff --check 通过。

## 修复与边界

首次编译发现 Dashboard 使用原 TodoMapper 方法；实现保留原签名委托无范围查询，未修改 Dashboard 文件，随后完整构建 / 真实数据库验证通过。

OpenSpec CLI 临时缓存已清理且当前未提供 executable：严格 CLI 校验 NOT_RUN，采用仓库允许的手工制品与 delta 检查；此检查不声称等价于 CLI。

## 前端与视觉 PASS

`playwright test tests/todo-calendar.spec.ts tests/mvp.spec.ts`：11 项通过（1 日期函数检查、6 新日历浏览器场景、4 原活动 / 首页回归），0 失败。覆盖日期 / 跨月 / 跨年、午夜 / 同小时 / 未定事项、月溢出展开、完整分页 101 项、真实时间预填 / 新建 / 编辑 / 完成 / 重开 / 删除、过滤和原列表、网络恢复、旧请求不能覆盖新周。原面试复盘、关联保护、冲突保留与首页恢复同时通过。

初版月截图截到加载过渡状态；加强等待遮罩消失和月标题断言后，单独重跑该场景 PASS，再等待绘制并截取完整日历区域，复核 PASS。已查看最终 week.png / month.png：没有横向页面溢出，周 / 月、类型颜色、同小时两项、待安排侧栏均清晰可见。截图只包含虚构验收内容，位于被忽略的 target/calendar-evidence。

最终 Jar 的 14 项静态资源逐个比对构建产物一致，无旧静态资源。个人 local 用最终 Jar 恢复到 8082；health=UP、workspace.available=true、首页及新日历 JS / CSS 都 HTTP 200。测试实例已正常停止；个人内容未查询或修改。移动端、外部飞书接入、真实系统重启不在范围。

## NOT_RUN

其他未受影响前端场景（9 项）本轮未重跑；本轮执行 11 项及日历截图针对性复核。严格 OpenSpec CLI 因工具缓存不可用未执行；手工 YAML / 制品 / 行为规范检查与 delta 同步会保留实际证据。

## 最终规范验收 PASS

手工核对 8 份稳定规范的 Purpose / Requirements / 去重需求名 / SHALL-MUST / 四级 Scenario / WHEN-THEN，全部通过；todo-management 原 2 个需求保留，追加本 change 的 3 个需求。归档目录边界与目标不存在检查通过；归档后的相关 Markdown 文件引用 37 处通过。git diff --check 通过；没有数据库迁移、依赖、App / 全局样式 / 其他业务模块变更。
