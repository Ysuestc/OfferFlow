# Proposal

## Why

用户按文档启动本地实例，在 `/api/v1/workspace` 探测得到 WinError 10061 后收到 Python 堆栈。`scripts/run_local.py` 读取到 Tomcat 启动日志后只请求一次，网络异常逃出等待循环并触发 finally 关闭自己的 Java / MySQL。实际日志包含 Tomcat 和应用启动完成标记；代理或瞬时连接失败的具体来源尚不能据此唯一确定。

## What Changes

- 本机就绪探测明确直连 loopback，不经过系统或环境 HTTP 代理。
- 在有限等待时间内重试暂时连接拒绝、超时和 HTTP 未就绪，并检查子进程是否退出。
- 保留数据与原有实例锁 / 关闭规则；失败给出简短提示，不输出网络堆栈。
- 补充重启电脑后的启动步骤和针对性回归验证。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- application-workspace：可靠启动已打包的持久化本机系统。

## Success Criteria

1. 已配置不可用 HTTP 代理时，本机探测仍成功；不会改变全局代理设置。
2. 短暂连接拒绝、超时或 HTTP 未就绪之后可以启动成功；持续失败在规定时间内退出并给出安全提示。
3. Java / 自有 MySQL 退出或业务模式不可用时不报告 ready，原有清理路径和保存目录保留。
4. 用真实 Jar 和独立 MySQL 数据空间验证启动、停止、再次启动与数据保留；恢复用户 local 实例并确认页面 / API 可访问。

## Non-goals

不开机自启，不安装 Windows 服务，不修改网络 / 防火墙 / 系统代理，不升级邮件识别或其他业务功能。

## Impact

仅涉及 Python 本地入口及其标准库测试、启动文档、相关规范；无依赖、字段、迁移或 REST 合同变化。按已授权流程完成审查、归档后一次推送。
