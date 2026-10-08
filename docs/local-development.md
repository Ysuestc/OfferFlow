# 本地构建与使用

当前版本支持公司、岗位、投递及阶段历史、面试复盘、六类待办和 Dashboard。默认只监听 127.0.0.1，适用于个人本机，没有登录认证。

当前开发与验收仅面向电脑浏览器，默认 Playwright 只运行 desktop 项目。手机适配、手机访问和移动端测试暂不纳入工作范围；历史尺寸模拟记录保留为当时的验证证据。

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

### 重启电脑后重新连接

电脑重启会停止 Java 和本地 MySQL，目前没有开机自启。打开 PowerShell，进入项目目录，重新执行启动命令；使用与之前相同的 instance 和端口：

```powershell
cd '<OfferFlow 项目目录>'
python scripts/run_local.py --mysql-bin '<MySQL 安装目录>/bin' --instance local --port 8082
```

看到 `OfferFlow ready: http://127.0.0.1:8082` 后，电脑浏览器访问该地址。**使用期间保持启动终端打开**；每次重启无需重新构建或安装依赖，数据保留在原实例目录。若先前使用默认端口 8080，就继续指定 `--port 8080`。

本机就绪检查直接连接 127.0.0.1，不经过系统或环境 HTTP 代理；应用等待最多 60 秒，期间自动重试暂时连接拒绝、超时与 HTTP 未就绪。持续失败会显示简短提示，并关闭自有进程、保留数据；根据提示查看 `private-data/<instance>/application.log` 或 `mysql.log`，不要公开分享完整私有日志。若显示 `This local instance is already running`，说明已有启动终端在运行，使用它显示的地址；不要重复启动或删除锁文件。浏览器显示连接被拒绝时，先确认终端是否已经出现 ready。

启动脚本回归检查无需 Maven、Node 或数据库：

```powershell
python -m unittest discover -s scripts/tests -v
```

脚本只停止自己启动的进程，**保留保存的数据和随机凭据**，再次使用同一命令会恢复。private-data 已被 Git 忽略，包含个人数据、凭据及本地日志；请自行备份整个实例目录，不提交公开仓库。不要在实例运行时复制 datadir 当作一致数据库备份。

--instance '<name>' 可创建独立数据空间，启动和停止需指定同一个名称；命名限制为小写字母、数字和连字符。文件锁防止同一数据目录启动两个实例；外部链接 / 不匹配的服务器目录会阻止运行。脚本不自动重置已存在的未知数据目录。

可以直接新增投递，也可以先整理公司 / 岗位目录再点“建立投递”；投递台账可搜索、筛选阶段、查看档案及更新时间线。投递日期或阶段日期不清楚时留空，后续可编辑。

首页展示实际投递与当前阶段概览，未来七天、逾期和时间未定的数量可点击筛选待办。进入投递详情可直接新增面试和待办，面试页可读取完整问题 / 回答 / 复盘；待办页支持六种类型、完成 / 重开和关联面试。日期时间按本机时区输入，未知可以留空。阶段仍由自己更新，录入面试结果或完成事项不会自动推进阶段。

面试与待办并发保护由 V2 迁移提供；新增投递体验修复没有数据库迁移。重启使用同一 instance 和端口即可，保留原有记录。不要删除 private-data 或修改旧迁移来升级。

点击“新增投递”默认直接录入公司名称和岗位名称，无需搜索或提前建立目录；企业类型直接点击互联网、银行、央国企、研究所、外企、其他标签单选，未知默认其他。公司库新增 / 编辑也使用相同标签。可展开补充地点、方向、招聘批次和 JD。已有唯一匹配的资料会复用，不覆盖官网、公司类型或 JD；选择已有公司时显示原类型，需改类型到公司库编辑。公司同名时选择已有公司；岗位存在歧义时切换选择已有岗位。搜索模式必须选中具体结果，输入文字本身不能保存。失败保留表单输入，同一岗位已有投递时打开原档案继续维护。

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

可使用已安装 Chrome / msedge；不指定 channel 时需先 npx playwright install chromium 下载测试浏览器。默认运行 13 个电脑端场景；邮箱页面测试真实保存虚构配置，连接 / 同步及邮件样本响应由浏览器拦截，不连接真实网易账号。历史 iPhone 13 尺寸 / 触摸模拟不表示实体手机或手机访问验证。浏览器测试不清空数据库，会保留虚构记录；报告、截图和失败 trace 位于被忽略的 target。最后从项目根目录执行 python scripts/run_local.py --instance browser-tests --stop。

## 网易邮箱接入

进入“邮件同步”，选择免费 163 / 126 / yeah 类型，填写邮箱、客户端授权码、文件夹（默认 INBOX）和首次采集起点。时间按电脑本机时区输入。先保存，再点击“测试连接”，成功后点击“立即同步”。本阶段没有定时任务，需要手动同步。

在网易网页版邮箱“设置 → POP/SMTP/IMAP”开启 IMAP，按账号要求验证后生成单独的客户端授权码，不能填网页登录密码。具体步骤见 [网易官方帮助](https://help.mail.126.com/faqDetail.do?code=d7a5dc8471cd0c0e8b4b8f4f8e49998b374173cfe9171305fa1ce630d7f67ac2a5feb28b66796d3b)。VIP / 企业邮箱不在当前范围。

授权码以 AES-GCM 密文保存于数据库，页面仅显示是否已配置；编辑留空保留，填新值替换。辅助实例密钥在 private-data/<instance>/mailbox/mail.key，各实例独立；直接运行 Jar 默认 private-data/mailbox，也可设置 OFFERFLOW_MAILBOX_PRIVATE_DIR 为自己的私有目录。密钥目录须可写；Linux 密钥创建为 600，Windows 使用本机目录权限。备份须同时保留数据库与密钥，不放入公开仓库。密钥丢失可重新填写授权码保存；密钥损坏先恢复备份，或停止实例后移除损坏密钥，再重新配置授权码。已有邮件不受授权码更换影响。

每次最多扫描 100 封，提示有更多时再次同步。只采集起点之后的邮件，不改变已读 / 删除标记。按 UID 元组及内容摘要去重；UID 有效性改变时重新扫描。服务端改写邮件头可能被视作新内容，当前不承诺跨邮箱语义去重。网络 / 保存失败保留已提交邮件和进度，再次同步可恢复；上次运行中断也可重新同步。

正文以纯文本显示，不加载远程图片或执行 HTML；附件与转发邮件附件不解析。超过 2 MiB 只保存邮件头，正文超过 20000 字符截断并提示，可到网易邮箱查看完整内容。已有采集邮件后，邮箱、提供商、文件夹和起点不能更换，只能更新授权码，避免混淆不同来源的游标。

新增 V3 不改写 V1 / V2，正常启动自动迁移并保留求职记录。已验证本地 GreenMail TLS / IMAP、真实 MySQL；真实网易端到端联调 NOT_RUN，用户在本机配置后可通过连接测试核实账号权限。采集不会更新阶段、面试或待办。
