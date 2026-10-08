# Verification

## 范围与环境

2026-10-08（Asia/Shanghai），Windows / Python 3.11.7 标准库 / Java 21 / MySQL 8.0.34。仅修改启动脚本；实际运行既有前端打包 Jar，测试数据仅写入独立 startup-review 实例。个人 local 只恢复运行并访问 health、workspace 和根页面，不读取求职记录、邮件或凭据。

## PASS

1. `python -X utf8 -m unittest discover -s scripts/tests -v`：8 项测试，0 失败。包括真实 HTTP 验证默认 opener 受坏代理影响、修复后直连成功；连接拒绝 / 超时 / 503 后重试恢复；持续拒绝和无启动日志有期限；Java / MySQL 退出、false / 非预期 JSON、探测时进程退出不误报 ready。
2. `python -X utf8 -m py_compile scripts/run_local.py scripts/tests/test_run_local.py`：退出 0。
3. 将 HTTP / HTTPS / ALL_PROXY 指向不可用的 loopback 测试代理并清空 NO_PROXY，再执行 `python scripts/run_local.py --mysql-bin '<MySQL bin>' --instance startup-review --port 8083`：实际 MySQL / Java 成功、workspace / health 返回 200。创建一条虚构公司，再用 `--instance startup-review --stop` 正常停止。
4. 同样坏代理环境以 `--instance startup-review --port 0` 重新启动：动态端口可用、原虚构公司 ID 和名称保持一致、打包首页 200。随后正常停止，两个 helper 均退出 0。
5. 在同一个独立实例中用测试 socket 占据将分配给 Java 的端口：入口返回预期简短 RuntimeError；跟踪的所有自有子进程均已退出，datadir / credentials 文件保留。第一轮验收断言把短命 mysql 客户端也算作两项服务，测试断言失败；修正按可执行文件筛选 Java / mysqld，重跑通过。产品代码没有因此修改。
6. `python scripts/run_local.py --mysql-bin '<MySQL bin>' --instance local --port 8082`：ready；真实 `/api/v1/health` UP、`/api/v1/workspace` available=true、根页面均 HTTP 200，留运行供用户使用。
7. OpenSpec 缓存 CLI 使用现有 Node 24：`validate fix-local-startup-readiness --strict` 退出 0。首次直接用系统 Node 18 因不支持依赖的正则标记退出 1，改用既有 Node 24 后通过，没有升级全局工具。
8. `git diff --check`：退出 0；CRLF 提醒不是空白错误。

## NOT_RUN 与限制

- Maven / Java / MySQL 业务完整测试、前端完整浏览器套件：NOT_RUN；没有 Java、SQL、迁移或前端代码变化，本次针对入口做标准库行为测试与真实 Jar / MySQL 验收。
- 真正重启整台电脑：NOT_RUN；以正常停止 / 重新启动同一持久化实例验证入口，不代替用户执行系统重启。
- 用户当时连接拒绝的具体代理 / 网络来源不能由堆栈唯一确定；本次修复已证实的单次探测容错缺陷并覆盖坏代理条件，未更改电脑代理或防火墙。

## 验收对应

| 条件 | 证据 |
|---|---|
| 1：坏代理可用、本机直连 | PASS 1、3、4 |
| 2：重试与期限 | PASS 1、2 |
| 3：失败清理、保留数据 | PASS 1、5 |
| 4：真实停启与恢复个人实例 | PASS 3、4、6 |

最终验收：34 处相关 Markdown 文件引用通过；本地 schema 校验通过；归档前 `validate --all --strict` 9 项通过（8 稳定规范 + 本 change）。归档后 `validate --all --strict` 8 项稳定规范全部通过；34 处相关文件引用复核通过。
