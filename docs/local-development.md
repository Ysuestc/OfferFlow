# 本地构建与使用

当前版本支持公司、岗位、投递及阶段历史、面试复盘、六类待办和 Dashboard。默认只监听 127.0.0.1，适用于个人本机，没有登录认证。

## 构建完整系统

需要 Java 21、Node 22.12+（推荐 24）、npm、Python 3.11+。首次需要网络下载依赖；前端使用项目锁文件。不要使用系统旧 Maven。

```powershell
python scripts/build.py
```

脚本执行 npm ci、vue-tsc、Vite build 和 Maven Wrapper -Pfrontend verify。生成 target/offerflow-0.1.0-SNAPSHOT.jar，页面和 API 共用后端端口。可指定 --node '<Node executable>'，特殊 npm 布局可指定 --npm-cli '<npm-cli.js>'，不改变全局安装。默认 backend verify 不需要 Node。

重新构建前先停止运行的本项目实例；Windows 会锁定正在运行的 Jar。构建脚本清理自身 target/classes/static，避免旧前端文件混入新 Jar。直接 -Pfrontend 构建时，Maven Enforcer 会拒绝缺少前端 index.html 的情况。

## 使用专属本地实例

已有 MySQL 二进制但暂时不想配置数据库账户，可用标准库 Python 辅助入口：

```powershell
python scripts/run_local.py --mysql-bin '<MySQL 安装目录>/bin'
```

默认访问 http://127.0.0.1:8080；可指定 --port。脚本在 private-data/local/mysql-data 初始化独占 MySQL、生成随机账户，在独立 loopback 数据库端口启动，再启动打包 Jar。不连接、改密码或停止已有 MySQL 服务。需要 mysqld 与 mysql；已验证 Windows / MySQL 8.0.34，其他系统尚未运行验证，需遵守当地 mysqld 账户运行要求。

第一次初始化可能需要十几秒；终端出现 OfferFlow ready 后访问页面。关闭可按 Ctrl+C，或在另一个项目终端执行：

```powershell
python scripts/run_local.py --stop
```

脚本只停止自己启动的进程，**保留保存的数据和随机凭据**，再次使用同一命令会恢复。private-data 已被 Git 忽略，包含个人数据、凭据及本地日志；请自行备份整个实例目录，不提交公开仓库。不要在实例运行时复制 datadir 当作一致数据库备份。

--instance '<name>' 可创建独立数据空间，启动和停止需指定同一个名称；命名限制为小写字母、数字和连字符。文件锁防止同一数据目录启动两个实例；外部链接 / 不匹配的服务器目录会阻止运行。脚本不自动重置已存在的未知数据目录。

建议先在公司库新增公司，在岗位库新增岗位，然后点“建立投递”；投递台账可搜索、筛选阶段、查看档案及更新时间线。投递日期或阶段日期不清楚时留空，后续可编辑。

首页展示实际投递与当前阶段概览，未来七天、逾期和时间未定的数量可点击筛选待办。进入投递详情可直接新增面试和待办，面试页可读取完整问题 / 回答 / 复盘；待办页支持六种类型、完成 / 重开和关联面试。日期时间按本机时区输入，未知可以留空。阶段仍由自己更新，录入面试结果或完成事项不会自动推进阶段。

本次升级追加 V2，为面试与待办增加并发版本保护；Flyway 自动升级 V1，保留原有记录。重启使用同一 instance 和端口即可。不要删除 private-data 或修改旧迁移来升级。

## 使用自己的数据库

准备空 MySQL 8.0.16+ 库和专用应用账户，参照 README 设置 DB_URL / DB_USERNAME / DB_PASSWORD 后：

```powershell
java -jar target/offerflow-0.1.0-SNAPSHOT.jar --spring.profiles.active=mysql
```

运行 Jar 无需 Node / npm / Python。首次 Flyway 迁移，连接失败或校验失败阻止启动。实际验证版本为 MySQL 8.0.34。默认 standalone 只提供存活和业务模式说明，不提供业务持久化。

## 前端开发与验收

先运行 mysql 后端（端口 8080）；前端开发：

```powershell
cd frontend
npm ci
npm run dev
```

Vite 仅 loopback，/api 代理到后端；生产环境从同一 Jar 提供资源。使用系统字体，不依赖远程字体或图片。前端生产包按需注册 Element Plus 控件。

真实数据库验收：

```powershell
python scripts/verify_mysql.py --mysql-bin '<MySQL 安装目录>/bin'
```

浏览器测试使用单独的数据空间，仅写虚构验收记录，不要指向个人使用实例：

```powershell
python scripts/run_local.py --mysql-bin '<MySQL 安装目录>/bin' --instance browser-tests --port 8081
```

在另一终端：

```powershell
cd frontend
$env:OFFERFLOW_E2E_URL = 'http://127.0.0.1:8081'
$env:OFFERFLOW_E2E_CHANNEL = 'chrome'
npm test
```

可使用已安装 Chrome / msedge；不指定 channel 时需先 npx playwright install chromium 下载测试浏览器。本次实际使用 Chrome，桌面与 iPhone 13 尺寸 / 触摸模拟均验证，未声称 Safari 或实体手机验证。浏览器测试不清空数据库，会保留虚构记录；报告、截图和失败 trace 位于被忽略的 target。最后从项目根目录执行 python scripts/run_local.py --instance browser-tests --stop。
