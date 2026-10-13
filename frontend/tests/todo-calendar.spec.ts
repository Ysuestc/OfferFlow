import { test, expect, type APIRequestContext, type Page, type Route } from '@playwright/test'
import { mkdir } from 'node:fs/promises'
import { atHour, calendarDays, dateKey, navigateDate } from '../src/todo-calendar'

test.use({ timezoneId: 'Asia/Shanghai' })

async function seed(request: APIRequestContext, name: string) {
  const company = (await (await request.post('/api/v1/companies', { data: { name, type: 'INTERNET' } })).json()).data
  const position = (await (await request.post('/api/v1/positions', { data: { companyId: company.id, name: '日历验收岗位' } })).json()).data
  return (await (await request.post('/api/v1/applications', { data: { jobPositionId: position.id, stage: 'SUBMITTED' } })).json()).data
}
async function todo(request: APIRequestContext, applicationId: string, title: string, dueAt: string | null, kind = 'INTERVIEW') {
  const response = await request.post('/api/v1/todos', { data: { applicationId, title, dueAt, kind, notes: '虚构日历备注' } })
  expect(response.status()).toBe(201)
  return (await response.json()).data
}
async function openCalendar(page: Page, query: string) {
  await page.clock.setFixedTime(new Date('2026-10-13T02:00:00Z'))
  await page.goto('/')
  await page.getByRole('navigation', { name: '工作台导航' }).getByRole('button', { name: '待办事项', exact: true }).click()
  await page.getByRole('textbox', { name: '搜索活动记录' }).fill(query)
  await page.getByRole('button', { name: '搜索', exact: true }).click()
  await page.getByRole('button', { name: '日历', exact: true }).click()
  await expect(page.locator('.todo-calendar')).toBeVisible()
}
const noOverflow = async (page: Page) => expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
async function daytimeFits(page: Page) {
  await expect(page.locator('.calendar-main .el-loading-mask')).toHaveCount(0)
  await page.locator('.time-calendar').scrollIntoViewIfNeeded()
  await expect.poll(() => page.evaluate(() => {
    const box = document.querySelector('.time-calendar')!.getBoundingClientRect()
    const head = document.querySelector('.time-head')!.getBoundingClientRect()
    const first = document.querySelector('.hour-row[data-hour="9"]')!.getBoundingClientRect()
    const last = document.querySelector('.hour-row[data-hour="20"]')!.getBoundingClientRect()
    return box.top >= -1 && box.bottom <= innerHeight + 1 && first.top >= head.bottom - 1 && last.bottom <= box.bottom + 1
  })).toBe(true)
  await expect(page.locator('.hour-row')).toHaveCount(24)
}

test('calendar date calculations preserve local midnight and month end', () => {
  const date = new Date(2026, 9, 18)
  const week = calendarDays(date, 'week')
  expect(dateKey(week[0]!)).toBe('2026-10-12')
  expect(dateKey(week[6]!)).toBe('2026-10-18')
  expect(week.every(day => day.getHours() === 0)).toBe(true)
  expect(calendarDays(date, 'month')).toHaveLength(42)
  expect(dateKey(navigateDate(new Date(2026, 0, 31), 'month', 1))).toBe('2026-02-28')
  expect(dateKey(navigateDate(new Date(2026, 11, 31), 'month', 1))).toBe('2027-01-31')
  expect(atHour(date, 23).getHours()).toBe(23)
})

test('week, day and month retain midnight, same-hour and undated todos', async ({ page, request }) => {
  const name = '日历显示-' + Date.now()
  const app = await seed(request, name)
  await todo(request, app.id, '午夜测评', '2026-10-13T00:00:00+08:00', 'ASSESSMENT')
  await todo(request, app.id, '上午技术面', '2026-10-13T09:00:00+08:00')
  await todo(request, app.id, '同小时笔试', '2026-10-13T09:30:00+08:00', 'WRITTEN_TEST')
  await todo(request, app.id, '深夜材料截止', '2026-10-13T23:59:00+08:00', 'MATERIAL')
  await todo(request, app.id, '尚未定时间', null, 'OTHER')
  await openCalendar(page, name)
  await expect(page.locator('.time-event')).toHaveCount(3)
  await expect(page.locator('.hour-row[data-hour="9"] .time-event')).toHaveCount(1)
  await page.getByRole('button', { name: '2026-10-13 09:00 查看全部 2 项待办', exact: true }).click()
  const crowded = page.getByRole('dialog', { name: '同一时段的待办', exact: true })
  await expect(crowded.locator('.hour-detail-event')).toHaveCount(2)
  await crowded.getByRole('button', { name: /同小时笔试/ }).click()
  const detail = page.getByRole('dialog', { name: '待办日程', exact: true })
  await expect(detail.getByRole('heading', { name: '同小时笔试', exact: true })).toBeVisible()
  await detail.getByRole('button', { name: '关闭此对话框' }).click()
  await expect(page.locator('.hour-row[data-hour="0"] .time-event')).toContainText('午夜测评')
  await expect(page.locator('.undated-card')).toContainText('尚未定时间')
  await page.getByRole('button', { name: '月视图', exact: true }).click()
  const cell = page.locator('.month-cell[data-date="2026-10-13"]')
  await expect(cell.locator('.month-event')).toHaveCount(3)
  await cell.getByRole('button', { name: '还有 1 项，查看全部' }).click()
  await expect(page.getByRole('button', { name: '日视图', exact: true })).toHaveAttribute('aria-pressed', 'true')
  await expect(page.locator('.time-event')).toHaveCount(3)
  for (const viewport of [{ width: 1440, height: 1000 }, { width: 1366, height: 768 }]) {
    await page.setViewportSize(viewport)
    for (const view of ['周视图', '日视图']) {
      await page.getByRole('button', { name: view, exact: true }).click()
      await expect(page.locator('.time-event')).toHaveCount(3)
      await daytimeFits(page)
      await noOverflow(page)
    }
  }
  await page.setViewportSize({ width: 1440, height: 1000 })
  await noOverflow(page)
  await page.getByRole('button', { name: '周视图', exact: true }).click()
  await expect(page.locator('.time-event')).toHaveCount(3)
  await expect(page.locator('.calendar-main .el-loading-mask')).toHaveCount(0)
  await mkdir('../target/calendar-evidence', { recursive: true })
  await page.locator('.todo-calendar').screenshot({ path: '../target/calendar-evidence/week.png', animations: 'disabled' })
  await page.getByRole('button', { name: '月视图', exact: true }).click()
  await expect(page.locator('.month-cell[data-date="2026-10-13"] .month-event')).toHaveCount(3)
  await expect(page.locator('.calendar-main .el-loading-mask')).toHaveCount(0)
  await expect(page.locator('.calendar-navigation h2')).toHaveText('2026 年 10 月')
  await page.evaluate(() => new Promise<void>(resolve => requestAnimationFrame(() => requestAnimationFrame(() => resolve()))))
  await page.locator('.todo-calendar').screenshot({ path: '../target/calendar-evidence/month.png', animations: 'disabled' })
})

test('date navigation and filters use local dates without changing list behavior', async ({ page, request }) => {
  const name = '日历筛选-' + Date.now()
  const app = await seed(request, name)
  await todo(request, app.id, '本周面试', '2026-10-13T12:00:00+08:00')
  await todo(request, app.id, '本周笔试', '2026-10-13T14:00:00+08:00', 'WRITTEN_TEST')
  const done = await todo(request, app.id, '已完成事项', '2026-10-13T16:00:00+08:00', 'OTHER')
  await request.put('/api/v1/todos/' + done.id + '/completion', { data: { completed: true, version: 0 } })
  await openCalendar(page, name)
  await expect(page.locator('.time-event')).toHaveCount(3)
  await page.getByRole('combobox', { name: '筛选事项类型' }).press('ArrowDown')
  await page.getByRole('option', { name: '笔试', exact: true }).click()
  await expect(page.locator('.time-event')).toHaveCount(1)
  await expect(page.locator('.time-event')).toContainText('本周笔试')
  await page.getByRole('button', { name: '下一段日程' }).click()
  await expect(page.locator('.calendar-empty')).toBeVisible()
  await page.getByRole('button', { name: '今天', exact: true }).click()
  await expect(page.locator('.time-event')).toHaveCount(1)
  await page.getByRole('button', { name: '月视图', exact: true }).click()
  await page.getByLabel('跳转到日期').fill('2026-12-31')
  await page.getByRole('button', { name: '下一段日程' }).click()
  await expect(page.getByLabel('跳转到日期')).toHaveValue('2027-01-31')
  await page.getByRole('button', { name: '今天', exact: true }).click()
  await page.getByRole('button', { name: '列表', exact: true }).click()
  await expect(page.locator('.activity-record')).toHaveCount(1)
  await noOverflow(page)
})

test('calendar creation, editing, completion and deletion persist through existing records', async ({ page, request }) => {
  const name = '日历实操-' + Date.now()
  const app = await seed(request, name)
  await openCalendar(page, name)
  await page.getByRole('button', { name: '2026-10-13 14:00 新增待办', exact: true }).click()
  const editor = page.getByRole('dialog', { name: '新增待办事项', exact: true })
  await expect(editor.getByLabel('截止或开始时间', { exact: true })).toHaveValue('2026-10-13 14:00')
  await editor.getByRole('combobox', { name: /投递档案$/ }).fill(name)
  await page.getByRole('option', { name: new RegExp(name) }).click()
  await editor.getByLabel(/待办标题$/).fill('日历新增面试')
  await editor.getByRole('button', { name: '保存', exact: true }).click()
  await expect(editor).not.toBeVisible()
  const event = page.locator('.time-event').filter({ hasText: '日历新增面试' })
  await expect(event).toBeVisible()
  const result = (await (await request.get('/api/v1/todos?applicationId=' + app.id)).json()).data.items[0]
  expect(result.dueAt).toBe('2026-10-13T06:00:00Z')
  await event.click()
  let detail = page.getByRole('dialog', { name: '待办日程', exact: true })
  await detail.getByRole('button', { name: '编辑', exact: true }).click()
  const editing = page.getByRole('dialog', { name: '编辑待办事项', exact: true })
  await editing.getByLabel('待办备注', { exact: true }).fill('日历编辑后保存')
  await editing.getByRole('button', { name: '保存', exact: true }).click()
  await expect(editing).not.toBeVisible()
  await event.click()
  detail = page.getByRole('dialog', { name: '待办日程', exact: true })
  await expect(detail.getByText('日历编辑后保存', { exact: true })).toBeVisible()
  await detail.getByRole('button', { name: '完成', exact: true }).click()
  await expect(detail.getByRole('button', { name: '重新打开', exact: true })).toBeVisible()
  await detail.getByRole('button', { name: '重新打开', exact: true }).click()
  await expect(detail.getByRole('button', { name: '完成', exact: true })).toBeVisible()
  await detail.getByRole('button', { name: '删除', exact: true }).click()
  await page.getByRole('dialog', { name: '确认删除', exact: true }).getByRole('button', { name: '删除', exact: true }).click()
  await expect(event).toHaveCount(0)
  expect((await request.get('/api/v1/todos/' + result.id)).status()).toBe(404)
  await page.getByRole('button', { name: '月视图', exact: true }).click()
  await page.getByRole('button', { name: '2026-10-14 新增待办', exact: true }).click()
  await expect(page.getByRole('dialog', { name: '新增待办事项', exact: true }).getByLabel('截止或开始时间', { exact: true })).toHaveValue('2026-10-14 09:00')
})

test('calendar fetches later pages and does not hide dense schedules', async ({ page }) => {
  const pages: number[] = []
  const items = Array.from({ length: 101 }, (_, index) => ({ id: String(index + 1), applicationId: '1', companyName: '分页虚构公司', positionName: '分页岗位', title: '分页日程-' + index,
    kind: 'INTERVIEW', dueAt: '2026-10-13T01:00:00Z', completed: false, version: 0, notes: null }))
  await page.route('**/api/v1/todos?**', async route => {
    const params = new URL(route.request().url()).searchParams
    if (!params.has('from')) return route.continue()
    const current = Number(params.get('page'))
    pages.push(current)
    await route.fulfill({ json: { code: 'SUCCESS', data: { items: items.slice((current - 1) * 100, current * 100), total: 101, page: current, size: 100 } } })
  })
  await openCalendar(page, '分页虚构公司')
  await expect(page.locator('.time-event')).toHaveCount(1)
  await daytimeFits(page)
  await page.getByRole('button', { name: '2026-10-13 09:00 查看全部 101 项待办', exact: true }).click()
  const crowded = page.getByRole('dialog', { name: '同一时段的待办', exact: true })
  await expect(crowded.locator('.hour-detail-event')).toHaveCount(101)
  await expect(crowded.locator('.hour-detail-event').last()).toContainText('分页日程-100')
  expect(pages).toEqual([1, 2])
})

test('calendar network errors are visible and can be retried', async ({ page }) => {
  await page.route('**/api/v1/todos?**', route => new URL(route.request().url()).searchParams.has('from') ? route.abort() : route.continue())
  await openCalendar(page, '无匹配日历-' + Date.now())
  await expect(page.locator('.calendar-error')).toContainText('连接失败')
  await page.unroute('**/api/v1/todos?**')
  await page.getByRole('button', { name: '重新加载日历' }).click()
  await expect(page.locator('.calendar-error')).toHaveCount(0)
  await expect(page.locator('.calendar-empty')).toBeVisible()
})

test('a late previous-week response cannot replace the new week', async ({ page, request }) => {
  const name = '日历竞争-' + Date.now()
  const app = await seed(request, name)
  await todo(request, app.id, '下一周保留', '2026-10-20T09:00:00+08:00')
  let late: Route | undefined
  await page.route('**/api/v1/todos?**', async route => {
    const params = new URL(route.request().url()).searchParams
    if (!late && params.get('from') === '2026-10-11T16:00:00.000Z') { late = route; return }
    await route.continue()
  })
  await openCalendar(page, name)
  await expect.poll(() => !!late).toBe(true)
  await page.getByRole('button', { name: '下一段日程' }).click()
  await expect(page.locator('.time-event')).toContainText('下一周保留')
  const response = page.waitForResponse(value => new URL(value.url()).searchParams.get('from') === '2026-10-11T16:00:00.000Z')
  await late!.continue()
  await response
  await expect(page.getByLabel('跳转到日期')).toHaveValue('2026-10-20')
  await expect(page.locator('.time-event')).toContainText('下一周保留')
})
