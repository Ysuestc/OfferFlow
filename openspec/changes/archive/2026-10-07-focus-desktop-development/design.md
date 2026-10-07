# Design
- 用户明确缩小平台范围，实施无需再次确认；SELF_REVIEW，按既有授权完成一次 change 推送。
- frontend/playwright.config.ts 删除 devices 导入和 mobile 项目，desktop 1440×1000 保留；业务测试代码和断言完全保留，不以范围调整隐藏业务失败。
- AGENTS.md、docs/project-context.md、openspec/config.yaml、README 和本地开发文档记录电脑端范围；稳定 application-workspace 与 recruitment-dashboard 完整替换受影响需求，仅改平台场景。
- 既有响应式 CSS 直接保留；已归档记录中的手机测试是历史事实，不修改历史，也不把尺寸模拟说成实体手机或可用手机入口。
- 无应用代码、字段、接口、迁移或依赖变化。用 Playwright --list 检查真实测试发现与平台配置，用严格 OpenSpec、Markdown 引用、YAML 和 diff 检查验收；不重建 Jar、不重启个人实例、不重复 Java / MySQL 或实际浏览器操作测试。
- 回滚测试配置和当前规范 / 文档即可恢复此前验收范围，不涉及业务数据。
- OpenSpec 对 MODIFIED 禁止遗漏旧 Scenario 标题，不能直接重命名手机场景；用明确 REMOVED + ADDED 退休原含手机平台合同并迁移全部业务场景到电脑端合同，附 Reason / Migration。Dashboard 场景标题未变，仍用 MODIFIED；不禁用验证。
