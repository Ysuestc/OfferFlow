# Verification
| 检查 | 证据 | 结果 |
|---|---|---|
| change 严格验证 | openspec validate focus-desktop-development --strict | 首轮 FAIL 为直接重命名 Scenario 被判遗漏；用显式旧合同退休及完整迁移修订，复核 PASS |
| 默认测试清单 | npm test -- --list | PASS，退出码 0，desktop 下 10 tests / 3 files；无 mobile 项目 |
| 测试代码保留 | git diff -- frontend/tests | PASS，无业务场景或断言修改 |
| YAML 上下文 | Python PyYAML 读取 openspec/config.yaml | PASS，当前只面向电脑端决定存在 |
| 差异空白 | git diff --check | PASS |
| Java / MySQL / Jar 重建 | 未修改应用实现、迁移或依赖 | NOT_RUN，本次仅范围 / 说明 / 测试项目调整 |
| 实际浏览器操作 | 未修改页面实现或场景逻辑 | NOT_RUN，--list 仅验证配置与测试发现，不当作操作测试通过 |

当前个人本机实例未停止或修改，未查询或变更个人业务记录。

归档后 PASS：OpenSpec archive 成功同步明确的平台合同替换及 Dashboard 平台调整；7 项稳定规范严格验证通过，两项当前业务规范均无手机平台要求；18 个本次文件及 33 个本地 Markdown 链接检查通过，实际 diff 保留业务测试代码、CSS 和应用源码。提交前 fetch 后 HEAD / origin/main 一致，远程为已授权 OfferFlow。
