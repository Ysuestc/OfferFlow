# 验证记录 — bootstrap-workflow

日期：2026-10-06（Asia/Shanghai）。验证对象仅为工作流与仓库基础。

## 工具与方法

Python 3.11.7 / PyYAML 6.0.1；固定 OpenSpec 1.14.1，使用工具环境的 Node.js 24.19.0 和临时 npm 缓存内 CLI 执行。系统 Node.js 18.20.4 不满足 CLI 要求，未修改系统 Node 或全局安装 OpenSpec。

下列 OpenSpec 命令经显式 Node 入口执行，命令表省略机器本地路径；CLI 遥测在后续检查进程中关闭。未把缓存或原始参考附件提交到仓库。

## 已运行检查

| 检查 / 命令 | 结果 | 证据 |
|---|---|---|
| skill-creator `quick_validate.py .agents/skills/offerflow-workflow` | PASS，退出码 0 | Skill is valid |
| PyYAML 与 schema 依赖 / 模板检查 | PASS，退出码 0 | 三个 YAML 可解析，四个 artifact 引用有效，依赖无环 |
| Markdown 本地链接和锚点检查 | PASS，退出码 0 | 首轮 21 个 Markdown、32 个本地引用，errors=[] |
| `openspec schema validate offerflow-spec-driven` | PASS，退出码 0 | 本地 schema valid |
| `openspec validate bootstrap-workflow --strict` | PASS，退出码 0 | Change valid，无格式或场景错误 |
| `openspec status --change bootstrap-workflow --json` | PASS，退出码 0 | 识别本地 schema，四个 artifact 均存在 |
| `openspec instructions proposal --change bootstrap-workflow --json` | PASS，退出码 0 | 返回 OfferFlow context、三条规则及项目模板 |
| `openspec instructions apply --change bootstrap-workflow --json` | PASS，退出码 0 | 当时 total=9、complete=5、remaining=4；文件存在不等于实施完成 |
| `git diff --cached --check` | PASS，退出码 0 | 无空白错误 |
| `git check-ignore` 五类样例 | PASS，退出码 0 | IDE、环境、本地配置、私有数据、参考压缩包均被忽略 |
| 参考内部标识及常见凭据标记扫描 | PASS | 无匹配；另已人工检查提交范围 |
| 四种代表请求的规则走查 | PASS，SELF_REVIEW | 见 code-review.md 的走查表；不是运行时模型评测 |

## 不适用检查

Java 编译、JUnit、MySQL 迁移和接口测试：NOT_RUN / 不适用。本 change 不创建业务工程、表或接口。

独立 agent 审查：NOT_RUN / 未请求多 agent，报告均如实使用 SELF_REVIEW。

## 验收对应

SC-1 对应本地引用检查；SC-2 对应 schema、config 和 CLI 指令；SC-3 对应技能校验和角色说明；SC-4 对应项目上下文及授权走查；SC-5 对应审查方式和实际检查记录。

SC-6 的规范同步、归档及最终清单复核是验收后的交付动作，在执行后追加结果，不提前声明成功。

## 归档交付结果

- `openspec instructions apply --change bootstrap-workflow --json`：退出码 0，state=all_done，九项任务完成，remaining=0。
- 归档前已验证源和目标均位于仓库 openspec/changes 内，目标不存在，没有目录碰撞。
- `openspec archive bootstrap-workflow --yes`：退出码 0，同步九条新增 Requirement 到稳定规范，归档到 `openspec/changes/archive/2026-10-06-bootstrap-workflow/`。
- `openspec validate --specs --strict`：退出码 0，稳定规范 1 passed、0 failed。
- README 引用更新为真实归档路径；归档后复核 25 个 Markdown、32 个本地引用和三个 YAML，errors=[]，退出码 0。
- 归档状态为 ARCHIVED，九项任务完成；确认无 pom.xml 或 src，保持纯文档阶段，检查退出码 0。

SC-6 的规范同步与归档已完成。提交及远程推送结果由本阶段交付消息报告，不在包含自身 SHA 的文件内重复写回。
