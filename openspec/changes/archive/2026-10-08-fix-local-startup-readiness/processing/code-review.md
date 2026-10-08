# Code Review

- 方式: SELF_REVIEW，第 1 轮。
- 结论: PASS，无阻断问题。
- 真实 diff: scripts/run_local.py 将原单次 urllib 探测替换为私有无代理 opener；monotonic 60 秒期限、单请求剩余时间、0.25 秒重试间隔明确；网络 / HTTP 暂时错误不会逃出等待函数。
- 成功与失败: 仅 data.available 严格 true 返回 URL，成功前再次检查子进程；无效响应和期限给出安全 RuntimeError。main 的 finally、自有 MySQL SHUTDOWN、实例锁、数据边界保护未改写。
- 验证证据: 8 项回归通过，真实坏代理停启保留记录、随机端口、端口占用失败清理与个人 local 恢复均已执行；第一轮手工进程计数断言问题已修正，非产品缺陷。
- 文档与范围: README / 本地说明给出电脑重启步骤、终端保持、相同数据空间、重复启动处理；不安装开机服务，无 API、依赖或迁移变化。没有将未知代理来源断言为用户故障的唯一原因。
- 隐私: 测试只用临时文件与虚构实例；个人服务仅查健康 / 工作区可用 / 首页，private-data 和 target 未加入提交；完整私有日志未公开。
