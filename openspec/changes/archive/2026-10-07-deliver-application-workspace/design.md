# Design

## Context and scope
实际工程：Java 21 / Boot 3.5.16、六表、MyBatis-Plus、Flyway V1、18 项 HTTP 和 40 项真实 MySQL 测试；尚无业务接口和前端。当前指令调整原先纯后端顺序，先交付可使用的投递工作台。以下是本 change 的实现决定，不宣称先前所有建议均获用户批准。

## Data and business decisions
- 沿用六表，无新增字段 / 迁移。公司和岗位独立维护，岗位包含招聘批次和 JD；投递保存渠道 / 日期 / 备注。
- 公司和岗位允许同名；岗位选择公司。应用唯一键延续已确认的同岗位单档案规则。引用存在时不能删除公司 / 岗位，删除冲突由数据库保护并返回 409。
- 创建投递时选择初始阶段，也写入首条历史。可导入已经进入面试的岗位，无须编造中间阶段。
- 手动更新当前阶段允许跳过或回退：公司流程不同、用户需要纠正当前阶段；每次有效变更追加历史，保留原条目，不提供改写 / 删除历史或独立历史补录入口。
- 历史按记录顺序（id）展示；stageOn 是业务日期，可未知或早于先前记录，createdAt 是录入时刻。页面不把录入时间当作招聘发生时间。
- submitted 是已实际投递的事实。创建时 TO_APPLY 为 false；SUBMITTED / 测评 / 笔试 / 面试 / OFFER / REJECTED 为 true；初始 ENDED 可显式选择是否投过。更新到上述阶段将 submitted 置 true，后续回退不清除事实。
- appliedOn 仅由用户填写；不从 stageOn 推断。未投递不能有投递日期。首次待投递推进时可继续保留未知日期，通过档案编辑补录 / 清空。
- REJECTED 仅企业拒绝；ENDED 必须提供已确认的五种原因之一，其他阶段 endReason 为 null。
- 更新阶段和投递备注 / 日期携带 version。陈旧版本为 409；阶段快照与历史在同一个 Spring 事务中写入，任一步失败全部回滚。
- 相同版本且阶段 / 日期 / 原因均相同的阶段请求是无变化，不追加历史；备注仅用于本次有效历史记录。陈旧版本仍先返回 409，页面重新加载后再决定。
- 当前阶段历史首条及后续操作保留完整。公司 / 岗位基本信息修改是资料纠正，无须复制到历史快照。

## REST API
全部 /api/v1，JSON、ApiResponse、真实 HTTP 状态；创建 201，普通读取 / 更新 / 删除 200。id 在 VO 中以字符串返回，避免 JavaScript 大整数丢精度；请求关联 id 为十进制字符串，后端 Long 接收。
- GET /workspace：数据库业务模式是否已启用，standalone 也可读取，用于界面说明。
- GET /companies?q=&page=1&size=20，POST /companies，GET /companies/{id}，PUT /companies/{id}，DELETE /companies/{id}。
- GET /positions?q=&companyId=&page=1&size=20，POST /positions，GET /positions/{id}，PUT /positions/{id}，DELETE /positions/{id}。
- GET /applications?q=&stage=&page=1&size=20，POST /applications，GET /applications/{id}，PUT /applications/{id}（渠道 / 投递日期 / 备注 / version）。
- POST /applications/{id}/stages（stage / stageOn / endReason / remark / version），GET /applications/{id}/history。
- 页响应 items / total / page / size；size 1..100，page 1..100000，q 最多 100 字符，确定性 id 降序；查询 q 按字面包含匹配而非 SQL 通配符。
- 创建 / 更新 DTO 和 VO 与 Entity 分开，必填及字段长度与表一致；name trim 后不能为空；website 只接受 http(s) 且有 host，前端链接使用安全协议和 rel=noopener。
- 未知关联 404，绑定 / 参数 / 语义错误 400，唯一键 / 外键 / 并发冲突安全 409；其他数据库异常仍为安全 500，不能伪装成业务成功。
- Service 是事务入口，Controller 只协议 / 校验；Mapper join 返回公司 / 岗位名称，避免列表 N+1。分页使用参数化 LIMIT / OFFSET，无新增分页解析库。

## Frontend and packaging
frontend 单独 npm 项目，Vue 3 Composition API、TypeScript、Element Plus、Vite，保留 package-lock。页面为投递台账、岗位库、公司库和投递详情时间线；可直接在各页面新增 / 编辑，表单有中文校验，操作失败保留输入，刷新不丢已保存数据。
不展示假数据或未实现功能入口。桌面侧栏 + 手机顶部导航，自适应列表和表单，避免手机横向溢出。阶段和类型统一映射中文。
使用 fetch，无 Axios / Pinia / Router；三个小视图本地切换，详情抽屉，不需额外状态管理。避免把后端细节放入正常业务页面；数据库未配置时提示如何查看本地启动说明。
Vite 开发代理 /api 到 loopback 后端，无 CORS。生产构建进入 target/frontend/static，通过 Maven frontend profile 作为资源打入 Jar，后端默认 verify 不依赖 Node。构建脚本先 npm ci / npm run build，再 Maven verify -Pfrontend，缺少静态产物时前端 profile 必须失败，不能打包半成品。
Node 22.12+ / 24；当前机器系统 Node 18 不适合 Vite，使用已有运行时 Node 24 验证，不修改全局安装。
新增依赖：vue 运行时；element-plus 控件；vite + plugin-vue 构建；typescript + vue-tsc 类型检查；@playwright/test 仅开发依赖，用于浏览器闭环。Maven Enforcer 使用 Boot 管理的 3.5.0，仅在 frontend profile 检查静态入口存在。版本由官方 npm 注册表确认，TypeScript 使用兼容的 5.9.3，锁文件固定实际解析。参考：[Vite](https://vite.dev/guide/)、[Vue](https://vuejs.org/guide/quick-start.html)、[Element Plus](https://element-plus.org/en-US/guide/installation.html)。

## Local operation
提供两种入口：已有专用空 MySQL 库的 mysql profile；可选 Python 辅助入口初始化 OfferFlow 独占的 loopback MySQL 实例，并将数据和随机凭据存入 git 忽略的 private-data，保持重启数据。不连接或修改已有 MySQL 服务。仅当 datadir 边界和服务器身份吻合才创建库 / 用户。辅助脚本退出关闭自己启动的 Java / MySQL，但不删除业务数据；再次运行复用。临时测试实例继续自动清理。
正常使用只需构建后的 Jar、Java 21、MySQL；Node / Python 是构建或可选本地启动辅助要求。未实现登录，不开放公网。

## Validation and rollback
包含搜索首版使用参数化 LIKE，前导通配符不能依靠普通 B-tree 实现高效定位；个人台账规模接受此扫描成本，不新增全文搜索引擎或伪称名称索引可加速全部包含搜索。
真实 MySQL HTTP 集成验证关联删除 / 唯一键 / 中文 / 搜索 / 分页 / 乐观锁 / 日期 / 阶段历史，注入历史写入失败验证真实事务回滚。使用第三个新建测试库，严格拒绝非 offerflow_it_ URL 及非空库。
前端执行类型检查、生产构建和 Playwright 桌面 / 移动端操作：公司 → 岗位 → 投递 → 二面 → 结束 / 刷新 / 修改；检查无脚本错误。只使用虚构数据。
Jar 检查 standalone 存活、mysql 前端 / API、无效 DB 启动失败。已有 V1 校验和保持不变；不删除数据作为回滚。若应用回退，既有数据库结构仍兼容。
