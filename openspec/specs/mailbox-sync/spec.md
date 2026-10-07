# Mailbox Sync

## Purpose

为单用户电脑端提供网易邮箱配置、只读连接测试、手动增量采集与文本查看能力，保存可去重、可重试的邮件输入；本阶段不识别招聘事件、不修改求职状态，为后续独立的识别和确认功能提供可靠来源。

## Requirements

### Requirement: Configure one private mailbox

系统 SHALL 支持一个免费 163 / 126 / yeah 邮箱，验证邮箱后缀与提供商一致，保存文件夹与首次同步时刻；授权码 MUST 仅写、加密持久化，并在重启后可用。

#### Scenario: Save and read settings

- **WHEN** 用户提交合法设置及授权码，然后读取设置
- **THEN** 设置被保存，读取只返回授权码是否已配置，不返回授权码、密文或密钥

#### Scenario: Protect collected mailbox identity

- **WHEN** 已有邮件后用户修改邮箱、提供商、文件夹或同步起点，或提交旧设置版本
- **THEN** 返回 409 并保留原设置；用户仍可用当前版本更新授权码

### Requirement: Test a read-only TLS connection

系统 SHALL 使用 TLS 验证账号和指定文件夹可读性，并提供固定的认证、文件夹、网络或本地密钥错误提示。

#### Scenario: Connect and fail safely

- **WHEN** 用户测试正确账号，或认证 / 文件夹 / 网络失败
- **THEN** 返回明确成功或固定失败结果，响应与日志不含协议消息、授权码或邮件正文

### Requirement: Synchronize incrementally and recover safely

系统 SHALL 手动按账号、文件夹、UIDVALIDITY 和 UID 采集邮件，只保留起点后的记录；单次最多扫描 100 个邮件，并告知是否有更多。已保存邮件与游标 MUST 同事务提交。UID 有效性变化时重新扫描，并使用内容摘要去重。

#### Scenario: Repeat and reset identifiers

- **WHEN** 重复同步相同邮件，或服务端 UIDVALIDITY 变化后重新扫描同样内容
- **THEN** 不重复建立邮件记录，保留正确的新游标与同步结果

#### Scenario: Recover a partial failure

- **WHEN** 已提交部分邮件后网络或持久化失败，再次手动同步
- **THEN** 首次记录部分失败且保留已提交结果，再次从安全游标恢复，不遗漏失败邮件

#### Scenario: Reject concurrent operations

- **WHEN** 同步或连接测试正在运行时再次同步、测试或保存设置
- **THEN** 返回 409；中断的进程重启后可重新发起同步

### Requirement: Collect bounded inert mail content

系统 SHALL 只读采集邮件头与有界正文，将 HTML 转为文本，不请求远程资源，不解析附件；超限邮件 SHALL 保留头部并标注正文不可用，正文截断 SHALL 明示。

#### Scenario: Keep flags and business records unchanged

- **WHEN** 采集普通邮件、HTML 邮件或含附件邮件
- **THEN** 邮箱已读与删除标记不变，邮件内容仅成为采集记录，六张求职业务表均无新增或更新

#### Scenario: Handle large content

- **WHEN** 邮件超过 2 MiB 或正文超过 20000 字符
- **THEN** 页面分别显示正文未采集或正文截断提示，同步仍可处理后续邮件

### Requirement: Operate mail from the desktop workspace

系统 SHALL 提供邮箱设置、连接测试、立即同步、同步状态、分页搜索和邮件文本详情。

#### Scenario: Set up and inspect collected mail

- **WHEN** 用户进入邮件同步页面，保存设置并同步成功
- **THEN** 可看到采集数量与时间、搜索及翻页，并打开邮件文本详情

#### Scenario: Recover from an operation error

- **WHEN** 保存、测试、同步或列表请求失败
- **THEN** 页面显示错误、恢复可操作状态并保留待修正设置，授权码在成功保存后清除
