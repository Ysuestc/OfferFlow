# Verification
环境：Windows、Node 24.19.0、Java 21.0.6、已安装 Chrome、隔离浏览器 MySQL 8.0.34。仅写虚构验收数据，个人 local 实例不运行测试。

| 检查 | 证据 | 结果 |
|---|---|---|
| OpenSpec change | openspec validate add-company-type-tags --strict | PASS |
| 生产构建 | python scripts/build.py --node '<Node executable>' --npm-cli '<npm-cli.js>' | PASS，vue-tsc、Vite、Maven Wrapper -Pfrontend verify；19 基础测试，无失败 / 错误 / 跳过 |
| 浏览器 | npm test -- tests/application-entry.spec.ts tests/workspace.spec.ts | 10 PASS；2 FAIL 为只读 group 断言错误，已修订 |
| 针对性复核 | npm test -- tests/application-entry.spec.ts --grep 'ambiguous company' | 2 PASS，退出码 0；合计 12 个受影响场景均有通过证据 |
| 图像 / 布局 | target/visual-review/entry-desktop.png、entry-mobile.png | PASS，实际查看；默认可见六个标签，手机换行，原有无页面横向溢出断言通过 |
| 后端 MySQL 套件 | 80 项 MySQL 集成测试 | NOT_RUN，后端、API、迁移均未变；真实类型保存由隔离浏览器 HTTP + 查询验证 |
| 无关面试 / 待办浏览器 | tests/mvp.spec.ts | NOT_RUN，未修改相关组件 |
| 差异空白 | git diff --check | PASS |

验收覆盖：默认其他 → 央国企单选 → 保存 / 刷新查询 STATE_OWNED；互联网选择在切换 / 网络失败后保留；公司库键盘选银行、编辑为外企后刷新保留；已有银行公司显示只读标签并保持 BANK；桌面与手机各自完成以上场景。
本地恢复 PASS：以原 private-data/local 重启 8082，health=UP / workspace.available=true；首页及 3 个资源与生产构建逐字节 / SHA-256 一致。仅读健康 / 模式 / 静态资源，未查询个人记录。原 MySQL 服务仍 Running。

归档 PASS：源 / 目标边界验证、严格 change 验证和 archive 完成；仅新增 application-workspace 标签需求。归档时 Why 长度出现非阻断提示，补充原入口操作背景以完善说明，不改变范围。
