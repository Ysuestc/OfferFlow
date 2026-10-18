# Proposal

## Why

用户指出右上角“2026 秋招”应为“2027 届秋招”，并要求能手动更新年份，只改此处。当前标签直接使用系统年份。

## What Changes

标签默认 2027 届秋招，点击后输入四位届别年份，保存到当前浏览器并用于刷新后的展示。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- application-workspace：可编辑秋招届别标签。

## Success Criteria

默认 2027 届；保存 2028 后立即更新并刷新保留；取消或非法输入不改变原值；存储失败明确提示。前端构建和真实浏览器冒烟通过。

## Non-goals / Impact

仅 App 标签、必要说明和工作流制品；不改投递 / 日历 / 邮箱 / 后端 / 数据库，不新增依赖。一次 change 最后统一推送。
