<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { api, eventTime, todoKinds, type Page, type Todo } from '../api'
import { addDays, atHour, calendarDays, clockTime, dateKey, localDate, navigateDate, type CalendarView } from '../todo-calendar'

const props = defineProps<{ q: string; category: string; completion: string; timing: string; pending: Set<string> }>()
const emit = defineEmits<{ create: [date: Date]; edit: [id: string]; complete: [todo: Todo]; remove: [todo: Todo]; application: [id: string] }>()
const view = ref<CalendarView>('week')
const anchor = ref(dateKey(new Date()))
const now = ref(new Date())
const dated = ref<Todo[]>([])
const undated = ref<Todo[]>([])
const loading = ref(false)
const error = ref('')
const selected = ref<Todo>()
const timeline = ref<HTMLElement>()
const hours = Array.from({ length: 24 }, (_, index) => index)
const weekdays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const days = computed(() => calendarDays(localDate(anchor.value), view.value))
const miniDays = computed(() => calendarDays(localDate(anchor.value), 'month'))
const todayKey = computed(() => dateKey(now.value))
const heading = computed(() => {
  const date = localDate(anchor.value)
  if (view.value === 'month') return `${date.getFullYear()} 年 ${date.getMonth() + 1} 月`
  const start = days.value[0]!
  const end = days.value[days.value.length - 1]!
  const label = (day: Date) => `${day.getFullYear()} 年 ${day.getMonth() + 1} 月 ${day.getDate()} 日`
  const endYear = start.getFullYear() === end.getFullYear() ? '' : `${end.getFullYear()} 年 `
  return view.value === 'day' ? label(start) : `${label(start)} — ${endYear}${end.getMonth() + 1} 月 ${end.getDate()} 日`
})
const groups = computed(() => {
  const result = new Map<string, Todo[]>()
  for (const todo of dated.value) {
    if (!todo.dueAt) continue
    const key = dateKey(new Date(todo.dueAt))
    if (!result.has(key)) result.set(key, [])
    result.get(key)!.push(todo)
  }
  for (const items of result.values()) items.sort((a, b) => Date.parse(a.dueAt!) - Date.parse(b.dueAt!))
  return result
})
function onDay(date: Date) { return groups.value.get(dateKey(date)) || [] }
function inHour(date: Date, hour: number) { return onDay(date).filter(todo => new Date(todo.dueAt!).getHours() === hour) }
function currentMonth(date: Date) { return date.getMonth() === localDate(anchor.value).getMonth() }
function showDay(date: Date) { anchor.value = dateKey(date); view.value = 'day' }
function navigate(direction: number) { anchor.value = dateKey(navigateDate(localDate(anchor.value), view.value, direction)) }
function today() { now.value = new Date(); anchor.value = todayKey.value }
function chooseDate(event: Event) {
  const value = (event.target as HTMLInputElement).value
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) anchor.value = value
}
let sequence = 0
async function allPages(params: URLSearchParams, request: number) {
  const items: Todo[] = []
  for (let page = 1; ; page++) {
    params.set('page', String(page))
    const result = await api<Page<Todo>>('/todos?' + params)
    if (request !== sequence) return undefined
    if (result.total > 10000) throw new Error('当前事项过多，请缩小日期范围或增加筛选条件')
    items.push(...result.items)
    if (items.length >= result.total) return items
    if (!result.items.length || page >= 100) throw new Error('日历数据正在变化，请刷新后重试')
  }
}
async function load() {
  const request = ++sequence
  loading.value = true
  error.value = ''
  dated.value = []
  undated.value = []
  const params = new URLSearchParams({ q: props.q, size: '100' })
  if (props.category) params.set('kind', props.category)
  if (props.completion) params.set('completed', props.completion)
  const range = new URLSearchParams(params)
  range.set('from', days.value[0]!.toISOString())
  range.set('to', addDays(days.value[days.value.length - 1]!, 1).toISOString())
  if (props.timing) range.set('timing', props.timing)
  const unknown = new URLSearchParams(params)
  unknown.set('timing', 'UNDATED')
  try {
    const [scheduled, unscheduled] = await Promise.all([
      props.timing === 'UNDATED' ? Promise.resolve([]) : allPages(range, request),
      !props.timing || props.timing === 'UNDATED' ? allPages(unknown, request) : Promise.resolve([]),
    ])
    if (request !== sequence) return
    dated.value = scheduled || []
    undated.value = unscheduled || []
    if (selected.value) selected.value = [...dated.value, ...undated.value].find(todo => todo.id === selected.value!.id)
  } catch (cause) {
    if (request === sequence) error.value = cause instanceof Error ? cause.message : '日历加载失败'
  } finally { if (request === sequence) loading.value = false }
}
async function scrollToMorning() {
  await nextTick()
  const container = timeline.value
  const row = container?.querySelector('[data-hour="7"]')
  if (container && row) container.scrollTop += row.getBoundingClientRect().top - container.getBoundingClientRect().top - 70
}
watch([anchor, view, () => props.q, () => props.category, () => props.completion, () => props.timing], load)
watch(loading, value => { if (!value && !error.value && view.value !== 'month') scrollToMorning() })
let timer: ReturnType<typeof setInterval> | undefined
onMounted(() => {
  load()
  scrollToMorning()
  timer = setInterval(() => { now.value = new Date() }, 60000)
})
onUnmounted(() => { sequence++; if (timer) clearInterval(timer) })
defineExpose({ load })
</script>

<template>
  <div class="todo-calendar">
    <div class="calendar-toolbar">
      <div class="calendar-navigation">
        <button class="calendar-control" @click="today">今天</button>
        <button class="calendar-arrow" aria-label="上一段日程" @click="navigate(-1)">‹</button>
        <button class="calendar-arrow" aria-label="下一段日程" @click="navigate(1)">›</button>
        <h2>{{ heading }}</h2>
        <input type="date" :value="anchor" aria-label="跳转到日期" @change="chooseDate" />
      </div>
      <div class="calendar-view-switch" aria-label="日历显示范围">
        <button v-for="(label, key) in { day: '日', week: '周', month: '月' }" :key="key" :aria-label="label + '视图'" :aria-pressed="view === key" @click="view = key as CalendarView">{{ label }}</button>
      </div>
    </div>
    <div class="calendar-layout">
      <aside class="calendar-sidebar">
        <div class="mini-month-title">{{ localDate(anchor).getFullYear() }} 年 {{ localDate(anchor).getMonth() + 1 }} 月</div>
        <div class="mini-calendar">
          <span v-for="weekday in weekdays" :key="weekday" class="mini-weekday">{{ weekday.slice(1) }}</span>
          <button v-for="date in miniDays" :key="dateKey(date)" :class="{ muted: !currentMonth(date), selected: dateKey(date) === anchor, today: dateKey(date) === todayKey }" :aria-label="dateKey(date) + ' 查看日程'" :aria-pressed="dateKey(date) === anchor" @click="anchor = dateKey(date)">{{ date.getDate() }}<i v-if="onDay(date).length" /></button>
        </div>
        <div class="calendar-legend"><span v-for="(label, key) in todoKinds" :key="key" :class="'kind-' + key"><i />{{ label }}</span></div>
        <div class="undated-heading"><h3>待安排</h3><span>{{ undated.length }}</span></div>
        <p class="sidebar-note">未完成 · 时间未定</p>
        <div class="undated-list">
          <button v-for="todo in undated" :key="todo.id" class="undated-card" @click="selected = todo"><strong>{{ todo.title }}</strong><small>{{ todo.companyName }}</small></button>
          <p v-if="!undated.length && !loading" class="sidebar-note">暂无符合筛选的未定事项</p>
        </div>
      </aside>
      <div class="calendar-main" v-loading="loading">
        <div v-if="error" class="calendar-error" role="alert"><el-alert :title="error" type="error" :closable="false" show-icon /><el-button @click="load">重新加载日历</el-button></div>
        <template v-else>
          <div class="calendar-caption"><span>按待办截止或开始时间展示</span><span>{{ dated.length }} 项日程 · 点击空白处新增</span></div>
          <div v-if="view === 'month'" class="month-calendar">
            <div v-for="weekday in weekdays" :key="weekday" class="month-weekday">{{ weekday }}</div>
            <div v-for="date in days" :key="dateKey(date)" class="month-cell" :class="{ muted: !currentMonth(date), 'is-today': dateKey(date) === todayKey }" :data-date="dateKey(date)">
              <button class="month-add" :aria-label="dateKey(date) + ' 新增待办'" @click="emit('create', atHour(date, 9))"><span>{{ date.getDate() }}</span><small>＋</small></button>
              <button v-for="todo in onDay(date).slice(0, 3)" :key="todo.id" class="calendar-event month-event" :class="['kind-' + todo.kind, { completed: todo.completed }]" :title="clockTime(todo.dueAt!) + ' ' + todo.companyName + ' · ' + todo.title" @click="selected = todo"><time>{{ clockTime(todo.dueAt!) }}</time><span>{{ todo.companyName }} · {{ todo.title }}</span></button>
              <button v-if="onDay(date).length > 3" class="more-events" @click="showDay(date)">还有 {{ onDay(date).length - 3 }} 项，查看全部</button>
            </div>
          </div>
          <div v-else ref="timeline" class="time-calendar" :style="{ '--day-count': days.length }">
            <div class="time-head">
              <span class="time-zone">本机<br />时间</span>
              <button v-for="date in days" :key="dateKey(date)" :class="{ 'is-today': dateKey(date) === todayKey }" @click="showDay(date)"><span>{{ weekdays[(date.getDay() + 6) % 7] }}</span><strong>{{ date.getMonth() + 1 }}/{{ date.getDate() }}</strong></button>
            </div>
            <div v-for="hour in hours" :key="hour" class="hour-row" :data-hour="hour" :class="{ 'current-hour': hour === now.getHours() && days.some(date => dateKey(date) === todayKey) }">
              <time class="hour-label">{{ String(hour).padStart(2, '0') }}:00</time>
              <div v-for="date in days" :key="dateKey(date)" class="hour-cell" :class="{ 'is-today': dateKey(date) === todayKey }" :data-date="dateKey(date)">
                <button class="hour-add" :aria-label="dateKey(date) + ' ' + String(hour).padStart(2, '0') + ':00 新增待办'" @click="emit('create', atHour(date, hour))" />
                <button v-for="todo in inHour(date, hour)" :key="todo.id" class="calendar-event time-event" :class="['kind-' + todo.kind, { completed: todo.completed }]" @click="selected = todo"><strong>{{ todo.title }}</strong><span>{{ clockTime(todo.dueAt!) }} · {{ todo.companyName }}</span></button>
              </div>
            </div>
          </div>
          <p v-if="!dated.length && !loading" class="calendar-empty">这段时间没有符合筛选的日程，可点击日期或时段安排待办。</p>
        </template>
      </div>
    </div>
    <el-dialog v-if="selected" :model-value="true" title="待办日程" width="min(540px, calc(100% - 28px))" @close="selected = undefined">
      <div class="calendar-detail">
        <span class="detail-kind" :class="'kind-' + selected.kind">{{ todoKinds[selected.kind] }} · {{ selected.completed ? '已完成' : '未完成' }}</span>
        <h3>{{ selected.title }}</h3><p>{{ selected.companyName }} · {{ selected.positionName }}</p>
        <dl><dt>截止或开始时间</dt><dd>{{ eventTime(selected.dueAt) }}</dd><template v-if="selected.interviewRound"><dt>关联面试</dt><dd>{{ selected.interviewRound }}</dd></template></dl>
        <p v-if="selected.notes" class="preserve-text">{{ selected.notes }}</p>
        <el-button text type="primary" @click="emit('application', selected.applicationId); selected = undefined">查看投递档案</el-button>
      </div>
      <template #footer>
        <el-button :disabled="pending.has(selected.id)" type="danger" plain @click="emit('remove', selected)">删除</el-button>
        <el-button :disabled="pending.has(selected.id)" @click="emit('edit', selected.id); selected = undefined">编辑</el-button>
        <el-button type="primary" :loading="pending.has(selected.id)" @click="emit('complete', selected)">{{ selected.completed ? '重新打开' : '完成' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.todo-calendar { --calendar-border: #e8ede6; padding-top: 6px; }
.calendar-toolbar { display: flex; justify-content: space-between; align-items: center; gap: 16px; padding: 14px 24px 20px; flex-wrap: wrap; }
.calendar-navigation { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.calendar-navigation h2 { font-size: 16px; font-weight: 600; margin: 0 10px; }
.calendar-navigation input { color: #63766a; padding: 5px 7px; font: inherit; font-size: 12px; border: 1px solid var(--calendar-border); border-radius: 6px; background: white; }
.calendar-control, .calendar-arrow { background: white; border: 1px solid var(--calendar-border); border-radius: 6px; padding: 7px 12px; color: #36594b; cursor: pointer; font: inherit; font-size: 12px; }
.calendar-arrow { border-color: transparent; font-size: 24px; padding: 0 9px; }
.calendar-view-switch { display: flex; padding: 3px; background: #f1f5ef; border-radius: 8px; }
.calendar-view-switch button { background: none; border: 0; border-radius: 6px; color: #708174; padding: 7px 17px; cursor: pointer; font: inherit; font-size: 12px; }
.calendar-view-switch button[aria-pressed="true"] { color: #17695a; background: white; box-shadow: 0 1px 5px #183e2310; font-weight: 600; }
.calendar-layout { display: grid; grid-template-columns: 198px minmax(0, 1fr); border-top: 1px solid var(--calendar-border); }
.calendar-sidebar { background: #f9fbf7; padding: 22px 15px; border-right: 1px solid var(--calendar-border); }
.mini-month-title { font-size: 12px; font-weight: 600; padding: 0 2px 14px; }
.mini-calendar { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); gap: 3px 0; }
.mini-weekday { text-align: center; color: #a1aba1; font-size: 10px; padding-bottom: 7px; }
.mini-calendar button { position: relative; border: 0; border-radius: 6px; background: none; font: inherit; font-size: 11px; height: 26px; color: #425c4c; cursor: pointer; padding: 0; }
.mini-calendar button.muted { color: #bcc5b9; }
.mini-calendar button.today { color: #17695a; font-weight: 700; outline: 1px solid #96b9a9; outline-offset: -2px; }
.mini-calendar button.selected { background: #17695a; color: white; }
.mini-calendar i { position: absolute; width: 3px; height: 3px; border-radius: 50%; background: currentColor; bottom: 2px; left: calc(50% - 1px); }
.calendar-legend { display: grid; grid-template-columns: 1fr 1fr; gap: 12px 4px; margin: 25px 0; }
.calendar-legend span { font-size: 10px; display: flex; align-items: center; gap: 6px; color: #738572; }
.calendar-legend i { width: 7px; height: 7px; border-radius: 2px; background: var(--event-color); }
.undated-heading { border-top: 1px solid var(--calendar-border); padding-top: 20px; display: flex; justify-content: space-between; align-items: center; }
.undated-heading h3 { margin: 0; font-size: 12px; }.undated-heading span { background: #e9eee4; padding: 2px 6px; font-size: 10px; border-radius: 5px; }
.sidebar-note { color: #94a18f; font-size: 10px; line-height: 1.8; }
.undated-list { max-height: 280px; overflow-y: auto; }
.undated-card { width: 100%; text-align: left; border: 1px solid #e5ebdf; background: white; border-radius: 8px; padding: 10px; margin: 4px 0; cursor: pointer; color: #425c4c; font: inherit; }
.undated-card strong { font-size: 11px; display: block; overflow-wrap: anywhere; }.undated-card small { font-size: 10px; color: #93a08c; display: block; margin-top: 6px; }
.calendar-main { min-width: 0; position: relative; }
.calendar-caption { display: flex; justify-content: space-between; gap: 8px; padding: 14px 16px; color: #8b9987; font-size: 10px; }
.calendar-error { padding: 24px; }.calendar-error .el-button { margin-top: 14px; }
.month-calendar { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); }
.month-weekday { color: #8a9a84; font-size: 11px; text-align: center; padding: 12px 0; border-bottom: 1px solid var(--calendar-border); background: #fbfcf9; }
.month-cell { min-height: 123px; padding: 5px; border-bottom: 1px solid var(--calendar-border); border-right: 1px solid var(--calendar-border); min-width: 0; }
.month-cell.muted { background: #fafbf8; }.month-cell.is-today { background: #f3f8ed; }
.month-add { width: 100%; display: flex; align-items: center; justify-content: space-between; background: none; border: 0; color: #55704e; padding: 4px 5px 8px; font: inherit; font-size: 11px; cursor: pointer; }
.month-add small { color: #acb7a3; }.muted .month-add { color: #b6c0ad; }.is-today .month-add span { background: #17695a; border-radius: 6px; color: white; padding: 3px 5px; }
.calendar-event { border: 0; border-left: 3px solid var(--event-color); background: var(--event-background); border-radius: 5px; color: #355141; text-align: left; font: inherit; cursor: pointer; width: 100%; position: relative; }
.calendar-event:hover { filter: brightness(.97); }.calendar-event.completed { opacity: .55; }.calendar-event.completed strong, .calendar-event.completed > span:first-of-type { text-decoration: line-through; }
.month-event { display: flex; gap: 4px; align-items: center; font-size: 10px; padding: 5px 3px; margin-bottom: 3px; }.month-event time { font-size: 9px; flex-shrink: 0; color: var(--event-color); }.month-event span { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.more-events { color: #17695a; padding: 4px; background: none; border: 0; font-size: 10px; cursor: pointer; text-align: left; }
.time-calendar { height: 590px; overflow-y: auto; scroll-padding-top: 70px; }
.time-head, .hour-row { display: grid; grid-template-columns: 49px repeat(var(--day-count), minmax(0, 1fr)); }
.time-head { position: sticky; top: 0; z-index: 3; background: white; border-bottom: 1px solid var(--calendar-border); }
.time-head button { background: white; border: 0; border-left: 1px solid var(--calendar-border); padding: 12px 7px; text-align: left; font: inherit; cursor: pointer; color: #76886f; }.time-head button span { font-size: 10px; display: block; }.time-head button strong { display: block; font-size: 19px; font-weight: 500; padding-top: 5px; }.time-head button.is-today { color: #17695a; background: #f2f7ed; }
.time-zone { font-size: 9px; color: #a3ae9b; padding: 15px 7px; line-height: 1.7; }
.hour-row { min-height: 65px; border-bottom: 1px solid #edf0e9; }.hour-label { font-size: 9px; color: #9aa894; padding: 5px 5px 0; text-align: center; }
.hour-cell { position: relative; padding: 3px; min-width: 0; border-left: 1px solid var(--calendar-border); }.hour-cell.is-today { background: #fcfdf9; }
.hour-add { position: absolute; inset: 0; border: 0; background: none; cursor: pointer; }.hour-add:hover { background: #eaf2e550; }
.time-event { display: block; padding: 8px 5px; margin-bottom: 3px; }.time-event strong { display: block; font-size: 11px; font-weight: 600; overflow-wrap: anywhere; }.time-event span { font-size: 9px; display: block; margin-top: 5px; color: var(--event-color); overflow-wrap: anywhere; }
.current-hour .hour-label { color: #bc7755; font-weight: 700; }.current-hour .hour-cell.is-today { box-shadow: inset 0 2px #cc936d; }
.calendar-empty { padding: 2px 18px 14px; color: #8b9987; font-size: 11px; line-height: 1.7; }
.kind-INTERVIEW { --event-color: #288c79; --event-background: #eaf5ef; }.kind-WRITTEN_TEST { --event-color: #aa8242; --event-background: #fbf3df; }.kind-ASSESSMENT { --event-color: #5b87ad; --event-background: #edf4fc; }.kind-MATERIAL { --event-color: #8673ac; --event-background: #f1edf8; }.kind-OFFER_DEADLINE { --event-color: #bc715b; --event-background: #fbede6; }.kind-OTHER { --event-color: #7c8972; --event-background: #eff2e9; }
.calendar-detail h3 { font-size: 22px; margin: 16px 0 8px; overflow-wrap: anywhere; }.calendar-detail > p { color: #8a9a81; font-size: 12px; }.detail-kind { color: var(--event-color); background: var(--event-background); font-size: 11px; padding: 5px 8px; border-radius: 5px; }.calendar-detail dl { border-radius: 10px; background: #f6f8f2; padding: 15px; }.calendar-detail dt { font-size: 10px; color: #94a18b; margin-bottom: 7px; }.calendar-detail dd { margin: 0 0 10px; font-size: 13px; }
button:focus-visible, input:focus-visible { outline: 2px solid #17695a; outline-offset: -2px; }
@media (max-width: 1250px) { .calendar-layout { grid-template-columns: 163px minmax(0, 1fr); }.calendar-sidebar { padding: 18px 10px; }.calendar-navigation h2 { font-size: 13px; }.calendar-toolbar { padding-inline: 16px; } }
</style>
