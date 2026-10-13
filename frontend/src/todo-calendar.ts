export type CalendarView = 'day' | 'week' | 'month'

export function dateKey(date: Date) {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}
export function localDate(key: string) {
  const [year, month, day] = key.split('-').map(Number)
  return new Date(year!, month! - 1, day!)
}
export function addDays(date: Date, count: number) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + count)
}
export function weekStart(date: Date) {
  return addDays(date, -((date.getDay() + 6) % 7))
}
export function calendarDays(date: Date, view: CalendarView): Date[] {
  const start = view === 'month' ? weekStart(new Date(date.getFullYear(), date.getMonth(), 1))
    : view === 'week' ? weekStart(date) : addDays(date, 0)
  return Array.from({ length: view === 'month' ? 42 : view === 'week' ? 7 : 1 }, (_, index) => addDays(start, index))
}
export function navigateDate(date: Date, view: CalendarView, direction: number) {
  if (view !== 'month') return addDays(date, direction * (view === 'week' ? 7 : 1))
  const month = new Date(date.getFullYear(), date.getMonth() + direction, 1)
  const lastDay = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate()
  return new Date(month.getFullYear(), month.getMonth(), Math.min(date.getDate(), lastDay))
}
export function atHour(date: Date, hour: number) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate(), hour)
}
export function clockTime(value: string) {
  return new Date(value).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })
}
