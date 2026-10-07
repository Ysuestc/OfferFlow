# Verification
- 环境: Windows / Java 21.0.6 / Node 24.19.0 / MySQL 8.0.34 / 已安装 Chrome。
- 审查方式: SELF_REVIEW。
- 测试数据: verify_mysql.py 自建临时实例及随机空测试库；浏览器使用独立 browser-review 实例，只写虚构记录。个人 local 实例不运行业务测试。

| 检查 | 命令或证据 | 结果 |
|---|---|---|
| OpenSpec | openspec validate simplify-application-entry --strict | PASS |
| 生产构建 | python scripts/build.py --node '<Node 24 executable>' --npm-cli '<npm-cli.js>' | PASS，vue-tsc、Vite、Wrapper -Pfrontend verify，19 基础 HTTP 测试；最终构建含校验期间提交保护 |
| 真实 MySQL | python scripts/verify_mysql.py --mysql-bin '<MySQL bin>' | PASS，19 基础 + 80 MySQL = 99 Java 测试，0 失败 / 错误 / 跳过 |
| 打包 Jar | 隔离脚本检查 standalone / mysql 健康、静态页面、业务模式及无效数据库拒绝启动 | PASS |
| 既有浏览器回归 | npm test -- tests/application-entry.spec.ts tests/workspace.spec.ts | workspace 6 PASS；新增 4 PASS、2 FAIL 为精确文本断言错误，见 code-review C2 |
| 新入口复核 | npm test -- tests/application-entry.spec.ts | PASS，桌面 / 手机各 3 项，共 6 项；修订断言后退出码 0 |
| 布局与图像 | target/visual-review/entry-desktop.png、entry-mobile.png（忽略文件） | PASS，实际查看默认文本输入与模式切换布局，浏览器断言无页面横向溢出 |
| 无关面试 / 待办浏览器用例 | tests/mvp.spec.ts | NOT_RUN，本次未修改其组件；上阶段验收记录保留，不声称本次完整 20 项运行 |
| 差异空白 | git diff --check | PASS |

## 验收对应
1. 默认无搜索录入并刷新保留：浏览器记录目录搜索次数为 0；HTTP 验证四表记录、阶段 / 日期和归一化。
2. 既有 / 预选入口：workspace 桌面 / 手机回归通过；无结果按钮可以切回直接录入并保留名称、岗位、备注。
3. 复用 / 重复 / 歧义：MySQL 8 个新行为测试覆盖目录资料不覆盖、相同身份重复、不同地点 / 方向 / 批次、同名公司指定 ID、重复岗位明确选择；浏览器覆盖同名选择及重复错误后笔记保留。
4. 原子失败：真实 HTTP 校验失败、无效阶段、未投递填日期、故意注入历史 trigger 故障，四表无残留；错误不暴露数据库信息。
5. 失败恢复与手机操作：模拟连接中断，恢复后可保存；六个新增用例覆盖两个屏幕，既有六个工作台用例覆盖编辑 / 阶段 / 冲突和查询恢复。

## 本地运行
- PASS：在原 private-data/local 数据空间重启，8082 health=UP、workspace.available=true；仅读取健康、模式和静态资源，没有查询个人业务数据。
- PASS：运行首页及其 3 个引用资源与最终生产构建逐字节 / SHA-256 一致；原 MySQL Windows 服务仍 Running。
- PASS：schema 验证与 change 严格验证通过；archive 仅给 application-workspace 增加一条需求，保留其他规范。
- PASS：Git 远程为已授权 OfferFlow，提交前 fetch 后 HEAD 与 origin/main 一致；提交只包含本次源码、测试、说明与归档，无凭据或个人数据。
