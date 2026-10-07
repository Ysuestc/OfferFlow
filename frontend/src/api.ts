export interface Company { id: string; name: string; type: string; website: string | null; notes: string | null }
export interface Position { id: string; companyId: string; companyName: string; name: string; location: string | null; direction: string | null; jd: string | null; recruitmentBatch: string | null }
export interface Application { id: string; jobPositionId: string; companyId: string; companyName: string; positionName: string; location: string | null; direction: string | null; recruitmentBatch: string | null; channel: string | null; appliedOn: string | null; submitted: boolean; currentStage: string; currentStageOn: string | null; endReason: string | null; notes: string | null; version: number; updatedAt: string }
export interface History { id: string; stage: string; stageOn: string | null; endReason: string | null; remark: string | null; recordedAt: string }
export interface Page<T> { items: T[]; total: number; page: number; size: number }
export interface InterviewSummary { id: string; applicationId: string; companyName: string; positionName: string; roundName: string; interviewAt: string | null; format: string | null; version: number; updatedAt: string }
export interface Interview extends InterviewSummary { questions: string | null; answers: string | null; review: string | null; result: string | null }
export interface Todo { id: string; applicationId: string; companyName: string; positionName: string; interviewId: string | null; interviewRound: string | null; kind: string; title: string; dueAt: string | null; completed: boolean; completedAt: string | null; notes: string | null; version: number; updatedAt: string }
export interface Dashboard { counts: { totalSubmitted: number; active: number; interviewing: number; offers: number; rejected: number }; generatedAt: string; upcomingUntil: string; upcomingTodoCount: number; overdueTodoCount: number; undatedTodoCount: number; recentApplications: Application[]; recentInterviews: InterviewSummary[]; upcomingTodos: Todo[]; overdueTodos: Todo[] }
export const interviewFormats: Record<string, string> = { ONLINE: '线上', OFFLINE: '线下', AI: 'AI 面' }
export const todoKinds: Record<string, string> = { WRITTEN_TEST: '笔试', INTERVIEW: '面试', ASSESSMENT: '测评', MATERIAL: '材料提交', OFFER_DEADLINE: 'Offer 截止', OTHER: '其他' }
export function eventTime(value: string | null) { return value ? localTime(value) : '时间未定' }
interface Envelope<T> { code: string; message: string; data: T; errors?: {field: string; message: string}[] }
export class ApiError extends Error {
  constructor(message: string, readonly status: number) { super(message) }
}
export async function api<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
  let response: Response
  try {
    response = await fetch('/api/v1' + path, {
      method, headers: { Accept: 'application/json', ...(body === undefined ? {} : {'Content-Type': 'application/json'}) },
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError('连接失败，请检查本地服务并重试', 0)
  }
  let envelope: Envelope<T>
  try { envelope = await response.json() as Envelope<T> } catch {
    throw new ApiError('服务暂时无法响应，请稍后重试', response.status)
  }
  if (!response.ok) {
    const details = envelope.errors?.map(item => item.message).join('；')
    throw new ApiError(details || envelope.message || '操作失败，请重试', response.status)
  }
  return envelope.data
}
export const companyTypes: Record<string, string> = { INTERNET: '互联网', BANK: '银行', STATE_OWNED: '央国企', RESEARCH_INSTITUTE: '研究所', FOREIGN: '外企', OTHER: '其他' }
export const stages: Record<string, string> = { TO_APPLY: '待投递', SUBMITTED: '已投递', ASSESSMENT: '测评', WRITTEN_TEST: '笔试', FIRST_INTERVIEW: '一面', SECOND_INTERVIEW: '二面', THIRD_INTERVIEW: '三面', HR_INTERVIEW: 'HR 面', OFFER: 'Offer', REJECTED: '已拒绝', ENDED: '已结束' }
export const reasons: Record<string, string> = { ACCEPTED_OFFER: '接受 Offer', DECLINED_OFFER: '主动拒绝 Offer', WITHDRAWN: '撤回投递', POSITION_CLOSED: '岗位关闭', OTHER: '其他' }
export function stageTone(stage: string): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  if (stage === 'OFFER') return 'success'
  if (stage === 'REJECTED') return 'danger'
  if (stage === 'TO_APPLY' || stage === 'ENDED') return 'info'
  return stage.includes('INTERVIEW') ? 'warning' : 'primary'
}
export function day(value: string | null) { return value || '日期未知' }
export function localTime(value: string) { return new Date(value).toLocaleString('zh-CN', { hour12: false }) }
export function safeWebsite(value: string | null) {
  if (!value) return undefined
  try {
    const url = new URL(value)
    if (['http:', 'https:'].includes(url.protocol) && !url.username && !url.password) return url.href
  } catch { /* Imported legacy values must not become executable links. */ }
  return undefined
}
