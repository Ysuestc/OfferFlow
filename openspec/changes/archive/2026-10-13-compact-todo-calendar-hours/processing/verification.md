# Verification

2026-10-13（Asia/Shanghai），Windows / Node 24 / Chrome；独立 calendar-review 虚构数据空间。个人 local 只停止 / 升级 / 恢复，不查询或修改求职内容。

## PASS

- `npm run build`（现有 Node24）：vue-tsc / Vite 通过，无新依赖。
- Wrapper `-Pfrontend -DskipTests package`：生产 Jar 成功；target/classes/static 经绝对边界核实后清理再复制，最终 Jar 与全部生成静态资源逐项一致。
- `playwright test tests/todo-calendar.spec.ts`：7 项全部通过，0 失败；1 日期工具与 6 电脑浏览器场景。
- 1440×1000 和 1366×768，两尺寸各日 / 周：通过实际 boundingClientRect 验证 09 和 20 行同时完整落在日历窗口内；日历窗口可容纳于电脑视口，页面无横向溢出，24 行仍存在。
- 同小时两项：首项与 +1，展开列表全部两项且可打开另一项详情；同小时 101 项：+100 打开全部 101，尾项可找到，完整分页正确，行高不增长。
- 原日期 / 月、录入 / 编辑 / 完成重开 / 删除、网络恢复及旧请求竞争通过。
- 查看完整 week.png：09–20 清楚可见，密集计数入口明确；截图仅虚构记录，留被忽略的 target/calendar-evidence。

## NOT_RUN

- Java / MySQL 套件：NOT_RUN，本轮无后端 / SQL / 迁移修改。
- 无关其他前端模块测试：NOT_RUN，仅运行受影响日历。
- OpenSpec 严格 CLI：NOT_RUN，当前临时 CLI 缓存仍不可用；采用手工 YAML / 完整 MODIFIED requirement / 场景和引用校验。

个人 local 8082 已用最终 Jar 恢复；health UP、workspace available=true、首页全部 HTTP 200。手工 YAML / 完整 MODIFIED 与三场景结构通过；归档后的 11 处文档文件引用通过，稳定需求名无重复，git diff --check 通过。
