<script setup lang="ts">
import { computed, defineAsyncComponent, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { api, companyTypes, stages, stageTone, day, safeWebsite, type Company, type Position, type Application, type Page } from './api'
import RecordEditor from './components/RecordEditor.vue'
import ApplicationDetail from './components/ApplicationDetail.vue'
import DashboardView from './components/DashboardView.vue'
const RecruitmentRecords = defineAsyncComponent(() => import('./components/RecruitmentRecords.vue'))
const InterviewDetail = defineAsyncComponent(() => import('./components/InterviewDetail.vue'))

type View = 'dashboard' | 'applications' | 'interviews' | 'todos' | 'positions' | 'companies'
type Kind = 'application' | 'position' | 'company'
const views = { dashboard: { label: '首页概览', hint: '看清进展，安排下一步。', kind: 'application' },
  applications: { label: '投递台账', hint: '把每一次投递，推进得更有把握。', kind: 'application' },
  interviews: { label: '面试记录', hint: '记下每一轮问题，把复盘变成下一次的准备。', kind: 'application' },
  todos: { label: '待办事项', hint: '把时间记清楚，让重要的截止事项不再遗漏。', kind: 'application' },
  positions: { label: '岗位库', hint: '留下 JD 与招聘批次，为每个机会做好准备。', kind: 'position' },
  companies: { label: '公司库', hint: '整理目标公司，让机会有迹可循。', kind: 'company' } } as const
const view = ref<View>('dashboard')
const activity = ref<{ create: () => void; load: () => Promise<void> }>()
const dashboard = ref<InstanceType<typeof DashboardView>>()
const interviewDetail = ref<string>()
const todoTiming = ref('')
const q = ref('')
const stage = ref('')
const page = ref(1)
const size = 12
const total = ref(0)
const companies = ref<Company[]>([])
const positions = ref<Position[]>([])
const applications = ref<Application[]>([])
const counts = ref<{ companies: number | null; positions: number | null; applications: number | null }>({
  companies: null, positions: null, applications: null,
})
const countsError = ref('')
const available = ref(false)
const booting = ref(true)
const loading = ref(false)
const error = ref('')
const editor = ref<{ kind: Kind; item?: Company | Position | Application; presetPosition?: Position }>()
const detail = ref<string>()
const currentView = computed(() => views[view.value])
const year = new Date().getFullYear()
let requestSequence = 0
async function load() {
  if (!available.value) return
  if (view.value === 'dashboard' || view.value === 'interviews' || view.value === 'todos') return
  const sequence = ++requestSequence
  const selected = view.value
  loading.value = true
  error.value = ''
  const params = new URLSearchParams({ q: q.value, page: String(page.value), size: String(size) })
  if (selected === 'applications' && stage.value) params.set('stage', stage.value)
  try {
    const result = await api<Page<Company | Position | Application>>('/' + selected + '?' + params)
    if (sequence !== requestSequence) return
    if (selected === 'companies') companies.value = result.items as Company[]
    if (selected === 'positions') positions.value = result.items as Position[]
    if (selected === 'applications') applications.value = result.items as Application[]
    total.value = result.total
    if (!result.items.length && page.value > 1) { page.value--; await load() }
  } catch (cause) {
    if (sequence === requestSequence) error.value = cause instanceof Error ? cause.message : '加载失败'
  } finally { if (sequence === requestSequence) loading.value = false }
}
async function loadCounts() {
  try {
    const results = await Promise.all(['companies', 'positions', 'applications'].map(path => api<Page<unknown>>('/' + path + '?size=1')))
    counts.value = { companies: results[0]!.total, positions: results[1]!.total, applications: results[2]!.total }
    countsError.value = ''
  } catch {
    counts.value = { companies: null, positions: null, applications: null }
    countsError.value = '资料概览加载失败，请点击刷新重试'
  }
}
async function refresh() {
  await Promise.all([load(), loadCounts(), dashboard.value?.load()])
}
async function boot() {
  booting.value = true
  error.value = ''
  try {
    available.value = (await api<{available: boolean}>('/workspace')).available
    if (available.value) await refresh()
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '连接失败' }
  finally { booting.value = false }
}
watch(view, () => { q.value = ''; stage.value = ''; page.value = 1; load() })
function search() { page.value = 1; load() }
function create() {
  if (view.value === 'interviews' || view.value === 'todos') activity.value?.create()
  else editor.value = { kind: currentView.value.kind }
}
function openTodos(timing = '') { todoTiming.value = timing; view.value = 'todos' }
function selectView(key: View) { if (key === 'todos') todoTiming.value = ''; view.value = key }
function openApplication(id: string) { interviewDetail.value = undefined; detail.value = id }
async function saved() { editor.value = undefined; await refresh() }
async function remove(kind: 'company' | 'position', item: Company | Position) {
  try { await ElMessageBox.confirm('删除“' + item.name + '”？已被引用的记录会保留。', '确认删除', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }) }
  catch { return }
  try {
    await api('/' + (kind === 'company' ? 'companies' : 'positions') + '/' + item.id, 'DELETE')
    ElMessage.success('已删除'); await refresh()
  } catch (cause) { ElMessage.error(cause instanceof Error ? cause.message : '删除失败') }
}
onMounted(boot)
</script>

<template>
  <ElConfigProvider :locale="zhCn">
  <div class="workspace">
    <aside class="sidebar">
      <a class="brand" href="/" aria-label="OfferFlow 首页"><span class="brand-mark"><svg width="26" height="26" viewBox="0 0 28 28" aria-hidden="true"><path d="M5 20V8h7v5h6V5h5v18H5Z" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linejoin="round" /></svg></span><span>Offer<span class="brand-flow">Flow</span></span></a>
      <div class="sidebar-caption">我的秋招空间</div>
      <nav aria-label="工作台导航"><button v-for="(item, key) in views" :key="key" :class="{ active: view === key }" :aria-current="view === key ? 'page' : undefined" @click="selectView(key)"><span class="nav-symbol" aria-hidden="true">{{ { dashboard: '◉', applications: '↗', interviews: '◷', todos: '✓', positions: '▤', companies: '◈' }[key] }}</span>{{ item.label }}</button></nav>
      <div class="sidebar-note"><span class="small-dot"></span> 少一点遗忘，多一点从容<p>每一个机会，都值得认真记录。</p></div>
      <div class="sidebar-footer">PERSONAL WORKSPACE <span>v0.1</span></div>
    </aside>
    <main>
      <header class="topbar"><span>工作空间 <span class="breadcrumb"> / {{ currentView.label }}</span></span><span class="season"><span class="small-dot"></span>{{ year }} 秋招</span></header>
      <div class="content">
        <section class="hero"><div><span class="eyebrow">YOUR NEXT CHAPTER</span><h1>{{ currentView.label }}</h1><p>{{ currentView.hint }}</p></div><el-button type="primary" size="large" :disabled="!available || booting" @click="create">＋ {{ view === 'interviews' ? '新增面试' : view === 'todos' ? '新增待办' : view === 'positions' ? '新增岗位' : view === 'companies' ? '新增公司' : '新增投递' }}</el-button></section>
        <div v-if="view !== 'dashboard'" class="overview" aria-label="资料概览">
          <button @click="view = 'applications'"><span>投递档案</span><strong>{{ available ? counts.applications ?? '—' : '—' }}</strong><small>每个岗位一份记录</small></button>
          <button @click="view = 'positions'"><span>已整理岗位</span><strong>{{ available ? counts.positions ?? '—' : '—' }}</strong><small>保留 JD 与招聘批次</small></button>
          <button @click="view = 'companies'"><span>目标公司</span><strong>{{ available ? counts.companies ?? '—' : '—' }}</strong><small>积累你的公司资料库</small></button>
        </div>
        <el-alert v-if="countsError" :title="countsError" type="warning" show-icon :closable="false" class="form-alert" />
        <section v-if="!available && !booting" class="setup-state"><h2>{{ error ? '暂时无法连接工作台' : '开启你的秋招工作台' }}</h2><p>{{ error || '请按照仓库 README 的本地运行步骤启动数据库模式，即可保存公司、岗位和投递记录。' }}</p><el-button @click="boot">重新连接</el-button></section>
        <DashboardView v-else-if="available && view === 'dashboard'" ref="dashboard" @application="openApplication" @interview="interviewDetail = $event" @todos="openTodos" @applications="view = 'applications'" />
        <RecruitmentRecords v-else-if="available && (view === 'interviews' || view === 'todos')" :key="view" ref="activity" :kind="view === 'interviews' ? 'interview' : 'todo'" :initial-timing="todoTiming" @updated="refresh" @application="openApplication" />
        <section v-else class="records-panel" v-loading="loading || booting">
          <div class="toolbar"><div class="search"><el-input v-model="q" clearable maxlength="100" :placeholder="view === 'companies' ? '搜索公司名称' : '搜索公司或岗位名称'" aria-label="搜索记录" @keyup.enter="search" @clear="search" /><el-button @click="search">搜索</el-button></div>
            <div class="filters"><el-select v-if="view === 'applications'" v-model="stage" clearable placeholder="全部阶段" aria-label="筛选招聘阶段" @change="search"><el-option v-for="(label, value) in stages" :key="value" :label="label" :value="value" /></el-select><el-button text :disabled="loading" @click="refresh">刷新</el-button></div>
          </div>
          <div class="list-caption"><span>{{ q || stage ? '筛选结果' : '全部记录' }}</span><small>共 {{ total }} 条</small></div>
          <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="list-error" />
          <template v-else-if="total">
            <div class="record-list" v-if="view === 'applications'">
              <article v-for="item in applications" :key="item.id" class="record application-record">
                <button class="record-title" @click="detail = item.id"><span class="company-avatar">{{ item.companyName.slice(0, 1) }}</span><span><strong>{{ item.positionName }}</strong><span>{{ item.companyName }}<span v-if="item.recruitmentBatch"> · {{ item.recruitmentBatch }}</span></span></span></button>
                <div class="record-meta">{{ item.location || '地点待补充' }}<span v-if="item.direction"> · {{ item.direction }}</span><small>{{ item.channel || '渠道待补充' }} · {{ day(item.appliedOn) }}</small></div>
                <div class="record-stage"><el-tag :type="stageTone(item.currentStage)" effect="light">{{ stages[item.currentStage] }}</el-tag><small>{{ day(item.currentStageOn) }}</small></div>
                <el-button text type="primary" @click="detail = item.id">查看档案 ↗</el-button>
              </article>
            </div>
            <div class="record-list" v-if="view === 'positions'">
              <article v-for="item in positions" :key="item.id" class="record catalog-record">
                <div class="catalog-title"><span class="company-avatar">{{ item.companyName.slice(0, 1) }}</span><div><strong>{{ item.name }}</strong><span>{{ item.companyName }} · {{ item.recruitmentBatch || '批次未填写' }}</span><small>{{ item.location || '地点待补充' }} · {{ item.direction || '方向待补充' }}</small></div></div>
                <div class="row-actions"><el-button type="primary" plain size="small" @click="editor = { kind: 'application', presetPosition: item }">建立投递</el-button><el-button text size="small" @click="editor = { kind: 'position', item }">编辑</el-button><el-button text size="small" @click="remove('position', item)">删除</el-button></div>
              </article>
            </div>
            <div class="record-list" v-if="view === 'companies'">
              <article v-for="item in companies" :key="item.id" class="record catalog-record">
                <div class="catalog-title"><span class="company-avatar">{{ item.name.slice(0, 1) }}</span><div><strong>{{ item.name }}</strong><span>{{ companyTypes[item.type] }}</span><small v-if="item.notes" class="truncate">{{ item.notes }}</small></div></div>
                <div class="row-actions"><a v-if="safeWebsite(item.website)" :href="safeWebsite(item.website)" target="_blank" rel="noopener noreferrer" class="website-link">官网 ↗</a><el-button text size="small" @click="editor = { kind: 'company', item }">编辑</el-button><el-button text size="small" @click="remove('company', item)">删除</el-button></div>
              </article>
            </div>
            <div class="pagination"><el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev, pager, next" @current-change="load" /></div>
          </template>
          <div v-else-if="!loading && !booting" class="empty-state"><div class="empty-mark">↗</div><h2>{{ q || stage ? '还没有匹配的记录' : '从第一个机会开始' }}</h2><p>{{ q || stage ? '试试其他关键词或招聘阶段。' : view === 'companies' ? '添加目标公司，逐步整理你的秋招资料。' : view === 'positions' ? '先到公司库添加公司，再记录感兴趣的岗位。' : '点击新增投递，直接填写公司和岗位即可保存。' }}</p><el-button v-if="!q && !stage" type="primary" plain @click="create">{{ view === 'applications' ? '新增投递' : view === 'positions' ? '新增岗位' : '新增公司' }}</el-button></div>
        </section>
        <footer class="page-footer">每一步进展，都有迹可循。<span>OfferFlow · 个人秋招工作台</span></footer>
      </div>
    </main>
    <RecordEditor v-if="editor" :kind="editor.kind" :item="editor.item" :preset-position="editor.presetPosition" @close="editor = undefined" @saved="saved" />
    <ApplicationDetail v-if="detail" :id="detail" @close="detail = undefined" @updated="refresh" />
    <InterviewDetail v-if="interviewDetail" :id="interviewDetail" @close="interviewDetail = undefined" @updated="refresh" @application="openApplication" />
  </div>
  </ElConfigProvider>
</template>
