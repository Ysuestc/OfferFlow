import { test, expect, type APIRequestContext, type Page } from '@playwright/test'
import { mkdir } from 'node:fs/promises'

async function seed(request: APIRequestContext, name: string) {
  const company = (await (await request.post('/api/v1/companies', { data: { name, type: 'INTERNET' } })).json()).data
  const position = (await (await request.post('/api/v1/positions', { data: { companyId: company.id, name: '后端实操岗位' } })).json()).data
  const application = (await (await request.post('/api/v1/applications', { data: { jobPositionId: position.id, stage: 'SECOND_INTERVIEW' } })).json()).data
  return application
}
async function navigate(page: Page, name: string) {
  await page.getByRole('navigation', { name: '工作台导航' }).getByRole('button', { name, exact: true }).click()
}
async function chooseApplication(page: Page, dialog: ReturnType<Page['getByRole']>, name: string) {
  await dialog.getByRole('combobox', { name: /投递档案$/ }).fill(name)
  await page.getByRole('option', { name: new RegExp(name) }).click()
}
const noOverflow = async (page: Page) => expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)

test('full interview review persists and appears in the dashboard and application', async ({ page, request }, info) => {
  const errors: string[] = []
  page.on('pageerror', error => errors.push(error.message))
  const name = '面试闭环-' + info.project.name + '-' + Date.now()
  const app = await seed(request, name)
  await page.goto('/')
  await expect(page.getByRole('heading', { name: '首页概览', exact: true })).toBeVisible()
  await navigate(page, '面试记录')
  await page.getByRole('button', { name: '＋ 新增面试', exact: true }).click()
  const dialog = page.getByRole('dialog', { name: '新增面试记录', exact: true })
  await chooseApplication(page, dialog, name)
  await dialog.getByLabel(/面试轮次$/).fill('技术二面')
  await dialog.getByLabel('面试时间', { exact: true }).fill('2026-10-06 10:00')
  await dialog.getByLabel('面试时间', { exact: true }).press('Tab')
  await dialog.getByRole('combobox', { name: '面试形式' }).press('ArrowDown')
  await page.getByRole('option', { name: 'AI 面', exact: true }).click()
  await dialog.getByLabel('面试问题', { exact: true }).fill('事务隔离？\n如何设计 Agent 工具？')
  await dialog.getByLabel('我的回答', { exact: true }).fill('用具体读写场景说明隔离级别。')
  await dialog.getByLabel('面试复盘', { exact: true }).fill('下次补充幻读与工具调用失败的例子。')
  await dialog.getByLabel('面试结果', { exact: true }).fill('等待招聘方反馈')
  await noOverflow(page)
  await dialog.getByRole('button', { name: '保存', exact: true }).click()
  await expect(dialog).not.toBeVisible()
  const row = page.locator('.activity-record').filter({ hasText: name })
  await row.getByRole('button', { name: '查看复盘' }).click()
  const detail = page.getByRole('dialog', { name: '面试详情', exact: true })
  await expect(detail.getByText('下次补充幻读与工具调用失败的例子。', { exact: true })).toBeVisible()
  await expect(detail.getByText('事务隔离？\n如何设计 Agent 工具？', { exact: true })).toBeVisible()
  await detail.getByRole('button', { name: '查看投递档案' }).click()
  const application = page.getByRole('dialog', { name: '投递档案', exact: true })
  await expect(application.locator('.activity-record').filter({ hasText: '技术二面' })).toBeVisible()
  await expect(application.locator('.el-timeline-item')).toHaveCount(1)
  await application.getByRole('button', { name: '关闭此对话框' }).click()
  await page.reload()
  const dashboard = (await (await request.get('/api/v1/dashboard')).json()).data
  await expect(page.locator('.metric-totalSubmitted strong')).toHaveText(String(dashboard.counts.totalSubmitted))
  await expect(page.locator('.metric-interviewing strong')).toHaveText(String(dashboard.counts.interviewing))
  expect((await (await request.get('/api/v1/applications/' + app.id)).json()).data.version).toBe(0)
  await noOverflow(page)
  await mkdir('../target/visual-review', { recursive: true })
  await page.screenshot({ path: '../target/visual-review/' + info.project.name + '-mvp-dashboard.png', animations: 'disabled' })
  expect(errors).toEqual([])
})

test('all todo kinds, completion, reopening and linked interview deletion are usable', async ({ page, request }, info) => {
  const name = '待办闭环-' + info.project.name + '-' + Date.now()
  const app = await seed(request, name)
  const interview = (await (await request.post('/api/v1/interviews', { data: { applicationId: app.id, roundName: '关联二面' } })).json()).data
  await page.goto('/')
  await navigate(page, '待办事项')
  const labels = ['笔试', '面试', '测评', '材料提交', 'Offer 截止', '其他']
  const when = await page.evaluate(() => {
    const value = new Date(Date.now() + 86400000)
    const pad = (n: number) => String(n).padStart(2, '0')
    return value.getFullYear() + '-' + pad(value.getMonth() + 1) + '-' + pad(value.getDate()) + ' ' + pad(value.getHours()) + ':' + pad(value.getMinutes())
  })
  for (const label of labels) {
    await page.getByRole('button', { name: '＋ 新增待办', exact: true }).click()
    const dialog = page.getByRole('dialog', { name: '新增待办事项', exact: true })
    await chooseApplication(page, dialog, name)
    await dialog.getByLabel(/待办标题$/).fill(name + '-' + label)
    await dialog.getByRole('combobox', { name: '事项类型', exact: true }).press('ArrowDown')
    await page.getByRole('option', { name: label, exact: true }).click()
    if (label === '面试') {
      await dialog.getByLabel('截止或开始时间', { exact: true }).fill(when)
      await dialog.getByLabel('截止或开始时间', { exact: true }).press('Tab')
      await dialog.getByRole('combobox', { name: '关联面试（可选）', exact: true }).press('ArrowDown')
      await page.getByRole('option', { name: /关联二面/ }).click()
    }
    await dialog.getByLabel('待办备注', { exact: true }).fill('虚构待办备注')
    await dialog.getByRole('button', { name: '保存', exact: true }).click()
    await expect(dialog).not.toBeVisible()
  }
  await page.getByRole('textbox', { name: '搜索活动记录' }).fill(name)
  await page.getByRole('button', { name: '搜索', exact: true }).click()
  await expect(page.locator('.activity-record')).toHaveCount(6)
  const row = page.locator('.activity-record').filter({ hasText: name + '-面试' })
  await row.getByRole('button', { name: '完成', exact: true }).click()
  await expect(row.getByRole('button', { name: '重新打开' })).toBeVisible()
  await row.getByRole('button', { name: '重新打开' }).click()
  await expect(row.getByRole('button', { name: '完成', exact: true })).toBeVisible()
  await noOverflow(page)
  await navigate(page, '首页概览')
  await page.locator('.deadline-summary').getByRole('button', { name: /未来七天/ }).click()
  await expect(page.getByRole('heading', { name: '待办事项', exact: true })).toBeVisible()
  await page.getByRole('textbox', { name: '搜索活动记录' }).fill(name + '-面试')
  await page.getByRole('button', { name: '搜索', exact: true }).click()
  await expect(page.locator('.activity-record').filter({ hasText: name + '-面试' })).toBeVisible()
  await navigate(page, '面试记录')
  await page.getByRole('textbox', { name: '搜索活动记录' }).fill(name)
  await page.getByRole('button', { name: '搜索', exact: true }).click()
  await page.locator('.activity-record').getByRole('button', { name: '删除', exact: true }).click()
  await page.getByRole('dialog', { name: '确认删除' }).getByRole('button', { name: '删除', exact: true }).click()
  await expect(page.getByText('该记录已被引用，请先处理关联记录')).toBeVisible()
  await navigate(page, '待办事项')
  await page.getByRole('textbox', { name: '搜索活动记录' }).fill(name + '-面试')
  await page.getByRole('button', { name: '搜索', exact: true }).click()
  await page.locator('.activity-record').getByRole('button', { name: '编辑', exact: true }).click()
  const editing = page.getByRole('dialog', { name: '编辑待办事项', exact: true })
  await expect(editing.getByLabel('待办备注', { exact: true })).toHaveValue('虚构待办备注')
  await editing.getByRole('button', { name: '解除面试关联', exact: true }).click()
  await editing.getByRole('button', { name: '保存', exact: true }).click()
  await expect(editing).not.toBeVisible()
  await navigate(page, '面试记录')
  await page.getByRole('textbox', { name: '搜索活动记录' }).fill(name)
  await page.getByRole('button', { name: '搜索', exact: true }).click()
  await page.locator('.activity-record').getByRole('button', { name: '删除', exact: true }).click()
  await page.getByRole('dialog', { name: '确认删除' }).getByRole('button', { name: '删除', exact: true }).click()
  await expect(page.locator('.activity-record')).toHaveCount(0)
  expect((await request.get('/api/v1/interviews/' + interview.id)).status()).toBe(404)
  await navigate(page, '待办事项')
  await page.getByRole('textbox', { name: '搜索活动记录' }).fill(name + '-其他')
  await page.getByRole('button', { name: '搜索', exact: true }).click()
  await page.getByText('查看备注', { exact: true }).click()
  await expect(page.getByText('虚构待办备注', { exact: true })).toBeVisible()
  await page.locator('.activity-record').getByRole('button', { name: '删除', exact: true }).click()
  await page.getByRole('dialog', { name: '确认删除' }).getByRole('button', { name: '删除', exact: true }).click()
  await expect(page.locator('.activity-record')).toHaveCount(0)
})

test('interview and todo conflicts preserve notes and offer reload', async ({ page, request }, info) => {
  const name = '活动并发-' + info.project.name + '-' + Date.now()
  const app = await seed(request, name)
  for (const kind of ['interview', 'todo'] as const) {
    const record = (await (await request.post('/api/v1/' + kind + 's', { data: kind === 'interview'
      ? { applicationId: app.id, roundName: '并发面试', review: '初稿' }
      : { applicationId: app.id, kind: 'OTHER', title: name + '-待办', notes: '初稿' } })).json()).data
    await page.goto('/')
    await navigate(page, kind === 'interview' ? '面试记录' : '待办事项')
    await page.getByRole('textbox', { name: '搜索活动记录' }).fill(name)
    await page.getByRole('button', { name: '搜索', exact: true }).click()
    await page.locator('.activity-record').filter({ hasText: name }).getByRole('button', { name: '编辑', exact: true }).click()
    const dialog = page.getByRole('dialog', { name: kind === 'interview' ? '编辑面试记录' : '编辑待办事项', exact: true })
    const input = dialog.getByLabel(kind === 'interview' ? '面试复盘' : '待办备注', { exact: true })
    await expect(input).toHaveValue('初稿')
    await input.fill('本窗口未保存笔记')
    const response = await request.put('/api/v1/' + kind + 's/' + record.id, { data: kind === 'interview'
      ? { roundName: '并发面试', review: '另一窗口笔记', version: 0 }
      : { kind: 'OTHER', title: name + '-待办', notes: '另一窗口笔记', version: 0 } })
    expect(response.status()).toBe(200)
    await dialog.getByRole('button', { name: '保存', exact: true }).click()
    await expect(input).toHaveValue('本窗口未保存笔记')
    await expect(dialog.getByRole('button', { name: '重新加载最新记录' })).toBeVisible()
    await dialog.getByRole('button', { name: '重新加载最新记录' }).click()
    await expect(input).toHaveValue('另一窗口笔记')
    await input.fill('合并后笔记')
    await dialog.getByRole('button', { name: '保存', exact: true }).click()
    await expect(dialog).not.toBeVisible()
    const latest = (await (await request.get('/api/v1/' + kind + 's/' + record.id)).json()).data
    expect(latest[kind === 'interview' ? 'review' : 'notes']).toBe('合并后笔记')
  }
})

test('dashboard failures and activity empty search recover', async ({ page }) => {
  await page.route('**/api/v1/dashboard', route => route.abort())
  await page.goto('/')
  await expect(page.getByText('连接失败，请检查本地服务并重试')).toBeVisible()
  await page.unroute('**/api/v1/dashboard')
  await page.getByRole('button', { name: '重新加载概览' }).click()
  await expect(page.locator('.metric-grid')).toBeVisible()
  await navigate(page, '面试记录')
  await page.getByRole('textbox', { name: '搜索活动记录' }).fill('no-activity-' + Date.now())
  await page.getByRole('button', { name: '搜索', exact: true }).click()
  await expect(page.getByRole('heading', { name: '还没有匹配的记录' })).toBeVisible()
  await noOverflow(page)
})
