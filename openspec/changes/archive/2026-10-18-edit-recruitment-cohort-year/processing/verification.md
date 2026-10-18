# Verification

## PASS

- 现有 Node24 `npm run build`：vue-tsc / Vite 成功；Wrapper `-Pfrontend -DskipTests package` 成功，最终 Jar 静态资源与生成产物逐文件一致。
- 在独立 calendar-review 的隔离 Chrome 上进行一次真实 UI 冒烟：默认 2027；保存 2028 后立即生效并刷新保留；非法 20x8 提示、取消 2029 保留 2028；模拟存储写入失败保留旧届别且提示，读取失败使用默认 2027 并提示。全部通过，未新增正式测试文件。
- 产品代码修改仅 App.vue 标签与该偏好逻辑，实际 diff 已 SELF_REVIEW。

## NOT_RUN

Java / MySQL 与其他模块测试 NOT_RUN：小范围前端显示设置，无后端或数据变化。严格 OpenSpec CLI NOT_RUN：当前无可用 CLI，使用手工 YAML / 需求场景 / 引用及 delta 检查。

个人 local 8082 恢复，health / workspace / 首页 HTTP 200。手工 metadata YAML、单 ADDED 需求和三场景结构、12 处归档文件引用通过；git diff --check 通过。浏览器冒烟未读写个人求职内容或个人浏览器存储。
