# focus-desktop-development
## Why
此前浏览器验收同时覆盖电脑和手机宽度，容易让用户误以为已有手机访问入口。用户现在明确要求后续只考虑电脑端，需要统一开发范围、稳定规范和默认测试配置。
## What Changes
- 当前迭代仅面向电脑浏览器，优先本机使用，不安排手机适配或手机访问工作。
- 默认 Playwright 只保留 desktop，完整保留十个业务测试场景。
- 项目约定、上下文、使用文档和 OpenSpec 中的平台验收同步；历史手机模拟证据保留为历史事实。
## Capabilities
### Modified Capabilities
- application-workspace: 将平台验收限定为电脑端，保留全部业务行为。
- recruitment-dashboard: 电脑端完成首页、面试和待办操作闭环。
## Acceptance
1. 默认测试清单只有 desktop 项目和十个既有业务场景。
2. 当前项目说明与稳定规范均不再要求手机端验收。
3. 不删除现有 CSS 或功能，不修改 API、数据库、依赖及本地实例。
