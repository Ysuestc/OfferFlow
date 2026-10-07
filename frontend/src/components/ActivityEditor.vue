<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { api, ApiError, interviewFormats, todoKinds, eventTime, type Application, type Interview, type InterviewSummary, type Todo, type Page } from '../api'

const props = defineProps<{ kind: 'interview' | 'todo'; id?: string; application?: Application }>()
const emit = defineEmits<{ close: []; saved: [] }>()
const formRef = ref<FormInstance>()
const busy = ref(false)
const loading = ref(false)
const error = ref('')
const conflict = ref(false)
const choices = ref<Application[]>([])
const interviews = ref<InterviewSummary[]>([])
const choiceLoading = ref(false)
const interviewLoading = ref(false)
const recordReady = ref(!props.id)
const current = ref<Interview | Todo>()
const form = reactive({
  applicationId: props.application?.id || '', roundName: '', interviewAt: null as Date | null,
  format: '' as string | null, questions: '', answers: '', review: '', result: '',
  title: '', kind: 'INTERVIEW', dueAt: null as Date | null, interviewId: '' as string | null, notes: '', version: 0,
})
if (props.application) choices.value = [props.application]
const title = computed(() => (props.id ? '编辑' : '新增') + (props.kind === 'interview' ? '面试记录' : '待办事项'))
const rules = {
  applicationId: [{ required: true, message: '请选择投递档案', trigger: 'change' }],
  roundName: [{ required: true, whitespace: true, message: '请填写面试轮次', trigger: 'blur' }],
  title: [{ required: true, whitespace: true, message: '请填写待办标题', trigger: 'blur' }],
}
let sequence = 0
async function searchApplications(q = '') {
  const request = ++sequence
  choiceLoading.value = true
  try {
    const result = await api<Page<Application>>('/applications?' + new URLSearchParams({ q, size: '30' }))
    if (request !== sequence) return
    const selected = choices.value.find(item => item.id === form.applicationId)
    choices.value = selected && !result.items.some(item => item.id === selected.id) ? [selected, ...result.items] : result.items
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '档案选项加载失败' }
  finally { if (request === sequence) choiceLoading.value = false }
}
let interviewSequence = 0
async function searchInterviews(q = '') {
  const request = ++interviewSequence
  if (!form.applicationId) { interviews.value = []; return }
  interviewLoading.value = true
  try {
    const result = await api<Page<InterviewSummary>>('/interviews?' + new URLSearchParams({ applicationId: form.applicationId, q, size: '30' }))
    if (request !== interviewSequence) return
    const selected = interviews.value.find(item => item.id === form.interviewId)
    interviews.value = selected && !result.items.some(item => item.id === selected.id) ? [selected, ...result.items] : result.items
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '面试选项加载失败' }
  finally { if (request === interviewSequence) interviewLoading.value = false }
}
function applicationChanged() {
  form.interviewId = ''
  interviews.value = []
  searchInterviews()
}
function fill(item: Interview | Todo) {
  current.value = item
  form.applicationId = item.applicationId
  form.version = item.version
  if ('roundName' in item) {
    form.roundName = item.roundName
    form.interviewAt = item.interviewAt ? new Date(item.interviewAt) : null
    form.format = item.format || ''
    form.questions = item.questions || ''
    form.answers = item.answers || ''
    form.review = item.review || ''
    form.result = item.result || ''
  } else {
    form.title = item.title
    form.kind = item.kind
    form.dueAt = item.dueAt ? new Date(item.dueAt) : null
    form.interviewId = item.interviewId || ''
    form.notes = item.notes || ''
  }
}
async function reload() {
  loading.value = true
  try {
    const item = await api<Interview | Todo>('/' + props.kind + 's/' + props.id)
    fill(item)
    recordReady.value = true
    choices.value = [await api<Application>('/applications/' + item.applicationId)]
    if (props.kind === 'todo') {
      const todo = item as Todo
      interviews.value = todo.interviewId ? [await api<Interview>('/interviews/' + todo.interviewId)] : []
      await searchInterviews()
    }
    error.value = ''
    conflict.value = false
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '加载失败' }
  finally { loading.value = false }
}
onMounted(async () => {
  if (props.id) await reload()
  else {
    if (!props.application) await searchApplications()
    if (props.kind === 'todo' && form.applicationId) await searchInterviews()
  }
})
async function save() {
  if (!recordReady.value || !await formRef.value?.validate().catch(() => false)) return
  busy.value = true
  error.value = ''
  try {
    const optional = (value: string | null) => value?.trim() || null
    const fields = props.kind === 'interview'
      ? { roundName: form.roundName, interviewAt: form.interviewAt?.toISOString() || null, format: optional(form.format),
          questions: optional(form.questions), answers: optional(form.answers), review: optional(form.review), result: optional(form.result) }
      : { title: form.title, kind: form.kind, dueAt: form.dueAt?.toISOString() || null, interviewId: optional(form.interviewId), notes: optional(form.notes) }
    await api('/' + props.kind + 's' + (props.id ? '/' + props.id : ''), props.id ? 'PUT' : 'POST',
      { ...fields, ...(props.id ? { version: form.version } : { applicationId: form.applicationId }) })
    ElMessage.success('已保存')
    emit('saved')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '保存失败'
    conflict.value = cause instanceof ApiError && cause.status === 409 && !!props.id
  } finally { busy.value = false }
}
</script>

<template>
  <el-dialog :model-value="true" :title="title" width="min(700px, calc(100% - 28px))" :close-on-click-modal="false"
    :close-on-press-escape="!busy" :show-close="!busy" @close="emit('close')">
    <div v-loading="loading">
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="form-alert" />
      <el-button v-if="conflict || (id && !recordReady)" :disabled="loading || busy" @click="reload">重新加载最新记录</el-button>
      <p v-if="conflict" class="field-help">当前输入仍保留。请先复制需要保留的笔记；重新加载会替换为最新内容。</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="save">
        <el-form-item label="投递档案" prop="applicationId">
          <el-select v-model="form.applicationId" filterable remote :remote-method="searchApplications" :loading="choiceLoading"
            :disabled="!!id || !!application" placeholder="搜索公司或岗位" @change="applicationChanged">
            <el-option v-for="item in choices" :key="item.id" :value="item.id" :label="item.companyName + ' · ' + item.positionName + (item.recruitmentBatch ? ' · ' + item.recruitmentBatch : '')" />
          </el-select>
        </el-form-item>
        <template v-if="kind === 'interview'">
          <el-form-item label="面试轮次" prop="roundName"><el-input v-model="form.roundName" maxlength="64" placeholder="例如：技术一面、HR 面" /></el-form-item>
          <div class="form-grid">
            <el-form-item label="面试时间"><el-date-picker v-model="form.interviewAt" type="datetime" format="YYYY-MM-DD HH:mm" placeholder="时间未知可留空" /></el-form-item>
            <el-form-item label="面试形式"><el-select v-model="form.format" clearable placeholder="未知可留空"><el-option v-for="(label, key) in interviewFormats" :key="key" :value="key" :label="label" /></el-select></el-form-item>
          </div>
          <el-form-item label="面试问题"><el-input v-model="form.questions" type="textarea" :rows="4" maxlength="40000" /></el-form-item>
          <el-form-item label="我的回答"><el-input v-model="form.answers" type="textarea" :rows="4" maxlength="40000" /></el-form-item>
          <el-form-item label="面试复盘"><el-input v-model="form.review" type="textarea" :rows="4" maxlength="40000" /></el-form-item>
          <el-form-item label="面试结果"><el-input v-model="form.result" type="textarea" :rows="2" maxlength="16000" placeholder="例如：待反馈、通过、需要补充材料" /></el-form-item>
        </template>
        <template v-else>
          <el-form-item label="待办标题" prop="title"><el-input v-model="form.title" maxlength="255" placeholder="例如：完成线上测评" /></el-form-item>
          <div class="form-grid">
            <el-form-item label="事项类型"><el-select v-model="form.kind"><el-option v-for="(label, key) in todoKinds" :key="key" :value="key" :label="label" /></el-select></el-form-item>
            <el-form-item label="截止或开始时间"><el-date-picker v-model="form.dueAt" type="datetime" format="YYYY-MM-DD HH:mm" placeholder="时间未知可留空" /></el-form-item>
          </div>
          <el-form-item label="关联面试（可选）"><el-select v-model="form.interviewId" clearable filterable remote :remote-method="searchInterviews" :loading="interviewLoading"
            :disabled="!form.applicationId" placeholder="仅显示同一档案的面试">
            <el-option v-for="item in interviews" :key="item.id" :value="item.id" :label="item.roundName + ' · ' + eventTime(item.interviewAt)" />
          </el-select><el-button v-if="form.interviewId" text type="primary" @click="form.interviewId = null">解除面试关联</el-button></el-form-item>
          <el-form-item label="待办备注"><el-input v-model="form.notes" type="textarea" :rows="4" maxlength="16000" /></el-form-item>
        </template>
        <p class="field-help">时间按本机时区输入和显示；未知可留空。保存记录不会自动改变招聘阶段。</p>
      </el-form>
    </div>
    <template #footer><el-button :disabled="busy" @click="emit('close')">取消</el-button><el-button type="primary" :loading="busy" :disabled="loading || !recordReady" @click="save">保存</el-button></template>
  </el-dialog>
</template>
