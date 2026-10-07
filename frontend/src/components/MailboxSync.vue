<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, localTime, type Page } from '../api'

interface Account {
  email: string; provider: string; folder: string; syncFrom: string; version: number
  credentialConfigured: boolean; identityLocked: boolean; busy: boolean; lastStatus: string
  lastAttemptAt: string | null; lastSuccessAt: string | null; lastError: string | null
  lastImported: number; messageCount: number
}
interface Mail { id: string; subject: string; sender: string; receivedAt: string | null; sentAt: string | null; contentStatus: string; bodyTruncated: boolean }
interface Detail { mail: Mail; bodyText: string | null }
interface Operation { status: string; imported: number; scanned: number; hasMore: boolean; errorCode: string | null; message: string }
const account = ref<Account | null>(null)
const form = ref({ email: '', provider: 'NETEASE_163', folder: 'INBOX', syncFrom: localInput(new Date(Date.now() - 30 * 86400000).toISOString()), authorizationCode: '' })
const initial = ref('')
const working = ref('')
const loading = ref(true)
const setupError = ref('')
const listError = ref('')
const detailError = ref('')
const notice = ref('')
const noticeType = ref<'success' | 'warning'>('success')
const q = ref('')
const page = ref(1)
const total = ref(0)
const mails = ref<Mail[]>([])
const detail = ref<Detail | null>(null)
const detailOpen = ref(false)
let sequence = 0
let detailSequence = 0
function localInput(value: string) {
  const date = new Date(value)
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 19)
}
function identity() { return JSON.stringify([form.value.email, form.value.provider, form.value.folder, form.value.syncFrom]) }
const dirty = computed(() => identity() !== initial.value || !!form.value.authorizationCode)
const disabled = computed(() => loading.value || !!working.value || !!account.value?.busy)
const statusLabel = computed(() => {
  if (account.value?.lastStatus === 'RUNNING' && !account.value.busy) return '上次同步中断，可重新同步'
  return ({ NEVER: '尚未同步', RUNNING: '正在同步', SUCCESS: '同步成功', PARTIAL: '部分失败，可重试', FAILED: '同步失败，可重试' } as Record<string, string>)[account.value?.lastStatus || 'NEVER']
})
async function settings(reset = true) {
  const saved = await api<Account | null>('/mailbox')
  account.value = saved
  if (reset && saved) {
    form.value = { email: saved.email, provider: saved.provider, folder: saved.folder, syncFrom: localInput(saved.syncFrom), authorizationCode: '' }
    initial.value = identity()
  }
}
async function list() {
  const current = ++sequence
  listError.value = ''
  try {
    const result = await api<Page<Mail>>('/mailbox/messages?' + new URLSearchParams({ q: q.value, page: String(page.value), size: '10' }))
    if (current === sequence) { mails.value = result.items; total.value = result.total }
  } catch (error) { if (current === sequence) listError.value = error instanceof Error ? error.message : '邮件列表加载失败' }
}
async function reload() {
  loading.value = true; setupError.value = ''
  try { await settings(); initial.value = identity(); await list() }
  catch (error) { setupError.value = error instanceof Error ? error.message : '邮箱设置加载失败' }
  finally { loading.value = false }
}
async function save() {
  working.value = 'save'; setupError.value = ''; notice.value = ''
  try {
    const saved = await api<Account>('/mailbox', 'PUT', { ...form.value, syncFrom: new Date(form.value.syncFrom).toISOString(), version: account.value?.version ?? 0 })
    account.value = saved
    form.value = { email: saved.email, provider: saved.provider, folder: saved.folder, syncFrom: localInput(saved.syncFrom), authorizationCode: '' }
    initial.value = identity()
    notice.value = '邮箱设置已保存'; noticeType.value = 'success'
  } catch (error) { setupError.value = error instanceof Error ? error.message : '保存失败' }
  finally { working.value = '' }
}
async function operate(kind: 'test' | 'sync') {
  working.value = kind; setupError.value = ''; notice.value = ''
  try {
    const result = await api<Operation>('/mailbox/' + (kind === 'test' ? 'connection-tests' : 'syncs'), 'POST')
    notice.value = result.message + (kind === 'sync' ? ` · 新增 ${result.imported} 封，扫描 ${result.scanned} 封` : '')
    noticeType.value = result.errorCode ? 'warning' : 'success'
    await settings(false); if (kind === 'sync') { page.value = 1; await list() }
  } catch (error) { setupError.value = error instanceof Error ? error.message : '操作失败，请重试' }
  finally { working.value = '' }
}
async function open(id: string) {
  const current = ++detailSequence
  detail.value = null; detailError.value = ''; detailOpen.value = true
  try { const result = await api<Detail>('/mailbox/messages/' + id); if (current === detailSequence) detail.value = result }
  catch (error) { if (current === detailSequence) detailError.value = error instanceof Error ? error.message : '邮件读取失败' }
}
function search() { page.value = 1; list() }
onMounted(async () => {
  try { await settings(); initial.value = identity(); await list() }
  catch (error) { setupError.value = error instanceof Error ? error.message : '邮箱设置加载失败' }
  finally { loading.value = false }
})
</script>

<template>
  <div class="mailbox-space">
    <el-alert title="当前支持手动采集邮件；投递状态、面试和待办仍由你维护。" type="info" :closable="false" show-icon />
    <div class="mailbox-grid">
      <section class="mail-settings records-panel" aria-label="邮箱设置" v-loading="loading">
        <h2>连接你的网易邮箱</h2>
        <p class="mail-help">在网页版邮箱的设置中开启 IMAP，生成客户端授权码。支持免费 163、126、yeah 邮箱。</p>
        <form @submit.prevent="save">
          <label>邮箱类型<select v-model="form.provider" :disabled="disabled || account?.identityLocked"><option value="NETEASE_163">163 邮箱</option><option value="NETEASE_126">126 邮箱</option><option value="NETEASE_YEAH">yeah 邮箱</option></select></label>
          <label>邮箱地址<input v-model="form.email" type="email" maxlength="254" required autocomplete="off" placeholder="例如：yourname@163.com" :disabled="disabled || account?.identityLocked" /></label>
          <label>客户端授权码<input v-model="form.authorizationCode" type="password" maxlength="128" autocomplete="new-password" :required="!account" :placeholder="account?.credentialConfigured ? '已保存，留空保留原授权码' : '请输入授权码'" :disabled="disabled" /></label>
          <p class="mail-help">授权码加密保存在本机，不会显示在邮件列表中。</p>
          <label>邮件文件夹<input v-model="form.folder" maxlength="128" required :disabled="disabled || account?.identityLocked" /></label>
          <label>首次采集起点<input v-model="form.syncFrom" type="datetime-local" step="1" required :disabled="disabled || account?.identityLocked" /></label>
          <p class="mail-help">按本机时区填写，只采集此时刻之后的邮件。已有采集邮件后，邮箱、文件夹和起点会锁定。</p>
          <el-alert v-if="setupError" :title="setupError" type="error" :closable="false" show-icon class="form-alert" />
          <el-button native-type="submit" type="primary" :disabled="disabled" :loading="working === 'save'">保存邮箱设置</el-button>
        </form>
      </section>
      <section class="mail-records records-panel" aria-label="采集邮件">
        <div class="mail-actions"><div><h2>采集邮件</h2><span class="mail-help">{{ statusLabel }} · 已保存 {{ account?.messageCount ?? 0 }} 封</span></div><el-button :disabled="!!working || loading" @click="reload">刷新</el-button></div>
        <div class="mail-operation"><el-button :disabled="disabled || !account || dirty" :loading="working === 'test'" @click="operate('test')">测试连接</el-button><el-button type="primary" :disabled="disabled || !account || dirty" :loading="working === 'sync'" @click="operate('sync')">立即同步</el-button></div>
        <p v-if="dirty && account" class="mail-help">设置已修改，请先保存再测试或同步。</p>
        <p class="mail-help">最近成功：{{ account?.lastSuccessAt ? localTime(account.lastSuccessAt) : '暂无' }}<br />最近尝试：{{ account?.lastAttemptAt ? localTime(account.lastAttemptAt) : '暂无' }}<span v-if="account?.lastError"> · 错误类型 {{ account.lastError }}</span></p>
        <el-alert v-if="notice" :title="notice" :type="noticeType" :closable="false" show-icon class="form-alert" />
        <div class="toolbar"><el-input v-model="q" clearable maxlength="100" placeholder="搜索主题或发件人" aria-label="搜索邮件" @keyup.enter="search" @clear="search" /><el-button @click="search">搜索</el-button></div>
        <el-alert v-if="listError" :title="listError" type="error" :closable="false" show-icon />
        <div v-else-if="!mails.length" class="empty-state"><div class="empty-mark">✉</div><h2>{{ q ? '没有匹配的邮件' : '还没有采集邮件' }}</h2><p>{{ q ? '试试其他主题或发件人关键词。' : '保存邮箱设置后，点击测试连接，再开始同步。' }}</p></div>
        <div v-else class="mail-list"><button v-for="mail in mails" :key="mail.id" class="mail-row" @click="open(mail.id)"><strong>{{ mail.subject }}</strong><span>{{ mail.sender }}</span><small>{{ mail.receivedAt ? localTime(mail.receivedAt) : '收信时间未知' }}<span v-if="mail.contentStatus === 'TOO_LARGE'"> · 正文未采集</span><span v-else-if="mail.bodyTruncated"> · 正文已截断</span></small></button></div>
        <div v-if="total" class="pagination"><el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="total, prev, pager, next" @current-change="list" /></div>
      </section>
    </div>
    <el-dialog v-model="detailOpen" title="采集邮件详情" width="820px" destroy-on-close>
      <el-alert v-if="detailError" :title="detailError" type="error" :closable="false" show-icon />
      <template v-else-if="detail"><h2 class="mail-subject">{{ detail.mail.subject }}</h2><p class="mail-help">{{ detail.mail.sender }}<br />收信：{{ detail.mail.receivedAt ? localTime(detail.mail.receivedAt) : '未知' }} · 发信：{{ detail.mail.sentAt ? localTime(detail.mail.sentAt) : '未知' }}</p><el-alert v-if="detail.mail.contentStatus === 'TOO_LARGE'" title="邮件超过 2 MiB，只采集了邮件头；请到网易邮箱查看完整内容。" type="warning" :closable="false" /><el-alert v-else-if="detail.mail.bodyTruncated" title="正文超过 20000 字符，当前显示已截断的文本。" type="warning" :closable="false" /><pre class="mail-body">{{ detail.bodyText || '暂无可显示正文，附件未解析。' }}</pre></template>
      <p v-else>正在加载邮件…</p>
    </el-dialog>
  </div>
</template>

<style scoped>
.mailbox-space { display: grid; gap: 22px; }
.mailbox-grid { display: grid; grid-template-columns: 360px minmax(0, 1fr); gap: 22px; align-items: start; }
.mail-settings, .mail-records { padding: 24px; min-width: 0; }
h2 { margin: 0 0 12px; font-size: 19px; }
.mail-help { color: var(--muted); font-size: 13px; line-height: 1.7; }
form { display: grid; gap: 14px; }
form p { margin: -5px 0 0; }
label { display: grid; gap: 8px; font-size: 14px; }
input, select { padding: 10px 12px; border: 1px solid var(--border); border-radius: 8px; font: inherit; color: var(--ink); background: white; width: 100%; box-sizing: border-box; }
input:focus, select:focus { outline: 2px solid var(--el-color-primary-light-5); }
input:disabled, select:disabled { background: #f4f6f3; color: var(--muted); }
.mail-actions { display: flex; justify-content: space-between; align-items: start; gap: 12px; }
.mail-operation { margin: 18px 0 10px; }
.mail-records .toolbar { padding: 12px 0 20px; }
.mail-list { display: grid; }
.mail-row { display: grid; gap: 8px; text-align: left; padding: 20px 0; background: transparent; border: 0; border-bottom: 1px solid var(--border); cursor: pointer; color: var(--ink); font: inherit; overflow-wrap: anywhere; }
.mail-row:hover strong { color: var(--el-color-primary); }
.mail-row:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 2px; }
.mail-row span, .mail-row small { color: var(--muted); font-size: 13px; }
.mail-subject { overflow-wrap: anywhere; }
.mail-body { white-space: pre-wrap; overflow-wrap: anywhere; font: inherit; line-height: 1.8; max-height: 55vh; overflow: auto; background: #f6f8f4; padding: 20px; border-radius: 10px; }
</style>
