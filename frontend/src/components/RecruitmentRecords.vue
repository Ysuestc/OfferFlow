<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api, eventTime, interviewFormats, todoKinds, type Application, type InterviewSummary, type Todo, type Page } from '../api'
import ActivityEditor from './ActivityEditor.vue'
import InterviewDetail from './InterviewDetail.vue'
const props = defineProps<{ kind: 'interview' | 'todo'; application?: Application; initialTiming?: string }>()
const emit = defineEmits<{ updated: []; application: [id: string] }>()
const items = ref<(InterviewSummary | Todo)[]>([])
const q = ref('')
const category = ref('')
const completion = ref('')
const timing = ref(props.initialTiming || '')
const page = ref(1)
const total = ref(0)
const loading = ref(false)
const error = ref('')
const editor = ref<{ id?: string }>()
const detail = ref<string>()
const pending = ref(new Set<string>())
let sequence = 0
async function load() {
  const request = ++sequence
  loading.value = true
  error.value = ''
  const params = new URLSearchParams({ q: q.value, page: String(page.value), size: '10' })
  if (props.application) params.set('applicationId', props.application.id)
  if (category.value) params.set(props.kind === 'interview' ? 'format' : 'kind', category.value)
  if (completion.value) params.set('completed', completion.value)
  if (timing.value) params.set('timing', timing.value)
  try {
    const result = await api<Page<InterviewSummary | Todo>>('/' + props.kind + 's?' + params)
    if (request !== sequence) return
    items.value = result.items
    total.value = result.total
    if (!items.value.length && page.value > 1) { page.value--; await load() }
  } catch (cause) { if (request === sequence) error.value = cause instanceof Error ? cause.message : '加载失败' }
  finally { if (request === sequence) loading.value = false }
}
function search() { page.value = 1; load() }
function create() { editor.value = {} }
async function saved() { editor.value = undefined; await load(); emit('updated') }
async function complete(item: Todo) {
  pending.value.add(item.id)
  try {
    await api('/todos/' + item.id + '/completion', 'PUT', { completed: !item.completed, version: item.version })
    ElMessage.success(item.completed ? '已重新打开' : '已完成')
    await load()
    emit('updated')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '操作失败'
  } finally { pending.value.delete(item.id) }
}
async function remove(item: InterviewSummary | Todo) {
  try {
    await ElMessageBox.confirm('删除这条' + (props.kind === 'interview' ? '面试记录？关联待办需先解除面试关联。' : '待办事项？'),
      '确认删除', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
  } catch { return }
  pending.value.add(item.id)
  try {
    await api('/' + props.kind + 's/' + item.id + '?version=' + item.version, 'DELETE')
    ElMessage.success('已删除')
    await load()
    emit('updated')
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '删除失败' }
  finally { pending.value.delete(item.id) }
}
function openApplication(id: string) { detail.value = undefined; emit('application', id) }
async function detailUpdated() { await load(); emit('updated') }
defineExpose({ create, load })
onMounted(load)
watch(() => props.initialTiming, value => { timing.value = value || ''; search() })
</script>

<template>
  <section class="records-panel activity-panel" :class="{ embedded: !!application }" v-loading="loading">
    <div v-if="application" class="section-heading"><h3>{{ kind === 'interview' ? '面试记录' : '待办事项' }} <span>{{ total }}</span></h3><el-button type="primary" plain size="small" @click="create">{{ kind === 'interview' ? '新增面试' : '新增待办' }}</el-button></div>
    <div class="toolbar activity-toolbar">
      <div class="search"><el-input v-model="q" clearable maxlength="100" :placeholder="kind === 'interview' ? '搜索公司、岗位或轮次' : '搜索公司、岗位或事项'" aria-label="搜索活动记录" @keyup.enter="search" @clear="search" /><el-button @click="search">搜索</el-button></div>
      <el-button text :disabled="loading" @click="load">刷新</el-button>
    </div>
    <div class="activity-filters">
      <el-select v-model="category" clearable :placeholder="kind === 'interview' ? '全部形式' : '全部类型'" :aria-label="kind === 'interview' ? '筛选面试形式' : '筛选事项类型'" @change="search"><el-option v-for="(label, key) in (kind === 'interview' ? interviewFormats : todoKinds)" :key="key" :value="key" :label="label" /></el-select>
      <template v-if="kind === 'todo'">
        <el-select v-model="completion" clearable placeholder="全部状态" aria-label="筛选完成状态" @change="search"><el-option label="未完成" value="false" /><el-option label="已完成" value="true" /></el-select>
        <el-select v-model="timing" clearable placeholder="全部时间" aria-label="筛选待办时间" @change="search"><el-option label="未来七天" value="UPCOMING" /><el-option label="已逾期" value="OVERDUE" /><el-option label="时间未定" value="UNDATED" /></el-select>
      </template>
    </div>
    <div class="list-caption"><span>{{ q || category || completion || timing ? '筛选结果' : '全部记录' }}</span><small>共 {{ total }} 条</small></div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="list-error" />
    <template v-else>
      <div class="record-list">
        <article v-for="item in items" :key="item.id" class="activity-record" :class="{ completed: 'completed' in item && item.completed }">
          <div class="activity-main">
            <strong>{{ 'roundName' in item ? item.roundName : item.title }}</strong>
            <span>{{ item.companyName }} · {{ item.positionName }}</span>
            <small>{{ eventTime('interviewAt' in item ? item.interviewAt : item.dueAt) }}</small>
            <small v-if="'completedAt' in item && item.completedAt">完成于 {{ eventTime(item.completedAt) }}</small>
            <small v-if="'interviewRound' in item && item.interviewRound">关联面试：{{ item.interviewRound }}</small>
          </div>
          <el-tag v-if="'roundName' in item" type="info">{{ interviewFormats[item.format || ''] || '形式未定' }}</el-tag>
          <el-tag v-else :type="item.completed ? 'success' : 'warning'">{{ item.completed ? '已完成' : todoKinds[item.kind] }}</el-tag>
          <div class="row-actions">
            <el-button v-if="'roundName' in item" text type="primary" @click="detail = item.id">查看复盘</el-button>
            <el-button v-else size="small" :loading="pending.has(item.id)" @click="complete(item)">{{ item.completed ? '重新打开' : '完成' }}</el-button>
            <el-button text size="small" @click="editor = { id: item.id }">编辑</el-button>
            <el-button text size="small" :disabled="pending.has(item.id)" @click="remove(item)">删除</el-button>
            <el-button v-if="!application" text size="small" @click="emit('application', item.applicationId)">投递档案</el-button>
          </div>
          <details v-if="'notes' in item && item.notes" class="todo-notes"><summary>查看备注</summary><p class="preserve-text">{{ item.notes }}</p></details>
        </article>
      </div>
      <div v-if="!total && !loading" class="empty-state"><div class="empty-mark">{{ kind === 'interview' ? '◷' : '✓' }}</div><h2>还没有{{ q || category || completion || timing ? '匹配的' : '' }}记录</h2><p>{{ kind === 'interview' ? '记录每次面试，把经验变成下一次的准备。' : '把时间和截止事项记下来，逐项推进。' }}</p><el-button plain type="primary" @click="create">{{ kind === 'interview' ? '新增面试' : '新增待办' }}</el-button></div>
      <div v-if="total" class="pagination"><el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="prev, pager, next" @current-change="load" /></div>
    </template>
    <ActivityEditor v-if="editor" :kind="kind" :id="editor.id" :application="application" @close="editor = undefined" @saved="saved" />
    <InterviewDetail v-if="detail" :id="detail" @close="detail = undefined" @updated="detailUpdated" @application="openApplication" />
  </section>
</template>
