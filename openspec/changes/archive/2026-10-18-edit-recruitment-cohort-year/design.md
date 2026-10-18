# Design

## Context / Scope

App.vue 当前 const year = new Date().getFullYear()，topbar 的 season 为不可编辑 span。本次只替换标签与该设置逻辑，不改其他功能。

## Decisions

- 默认固定 2027，与用户确认的毕业届别一致，不按系统时钟自动滚动。
- 复用现有 ElMessageBox.prompt 和 ElMessage；点击标签输入四位正整数（1000–9999），去除首尾空白，保存 / 取消。
- 使用 localStorage 键 offerflow.recruitment-year；只保存一项前端显示偏好，无后端接口、数据库字段或依赖。范围为当前浏览器与同一地址，非跨浏览器同步。
- 存储成功后才更新标签；读取失败提示并用 2027，写入失败提示且保留原值。现有损坏值回退默认，用户可重新保存。
- 原配色与位置保留，仅给标签按钮添加 scoped 样式和可访问名称。

## Verification / Rollback

前端 typecheck / 生产构建、浏览器人工流程冒烟（默认、保存刷新、取消、非法输入、存储失败）；本地 Jar 更新并恢复。无业务逻辑改动，不增加形式化测试或重复 Java / DB 套件。撤回标签代码即可，业务数据无回滚。

## Open Questions

无，用户已明确授权此小修复。
