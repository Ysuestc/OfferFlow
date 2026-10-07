<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { api, ApiError, companyTypes, stages, reasons, type Company, type Position, type Application, type Page } from '../api'

type Kind = 'company' | 'position' | 'application' | 'stage'
const props = defineProps<{ kind: Kind; item?: Company | Position | Application; presetPosition?: Position }>()
const emit = defineEmits<{ close: []; saved: [] }>()
const formRef = ref<FormInstance>()
const busy = ref(false)
const error = ref('')
const conflict = ref(false)
const choicesLoading = ref(false)
const companyChoices = ref<Company[]>([])
const positionChoices = ref<Position[]>([])
const current = ref(props.item)
const entryMode = ref<'direct' | 'existing'>(props.presetPosition ? 'existing' : 'direct')
const useExistingCompany = ref(false)
const form = reactive({
  name: '', type: 'INTERNET', website: '', companyId: '', companyName: '', companyType: 'OTHER',
  positionName: '', location: '', direction: '', jd: '',
  recruitmentBatch: '', jobPositionId: '', channel: '官网', appliedOn: null as string | null,
  stage: 'SUBMITTED', stageOn: null as string | null, endReason: '' as string | null, submitted: false,
  notes: '', remark: '', version: 0,
})
function fill(item?: Company | Position | Application) {
  if (!item) return
  Object.assign(form, item)
  if ('currentStage' in item) {
    form.stage = item.currentStage
    form.stageOn = item.currentStageOn
  }
  if ('website' in item) form.website = item.website || ''
  form.notes = 'notes' in item ? item.notes || '' : ''
}
fill(props.item)
if (props.presetPosition) {
  form.jobPositionId = props.presetPosition.id
  positionChoices.value = [props.presetPosition]
}
const title = computed(() => props.kind === 'stage' ? '更新招聘阶段'
  : (props.item ? '编辑' : '新增') + ({ company: '公司', position: '岗位', application: '投递档案' }[props.kind]))
const editingApplication = computed(() => props.kind === 'application' && !!props.item)
const directEntry = computed(() => props.kind === 'application' && !editingApplication.value && entryMode.value === 'direct')
const showReason = computed(() => form.stage === 'ENDED')
const rules = computed<FormRules>(() => ({
  name: [{ required: true, whitespace: true, message: '请填写名称', trigger: 'blur' }],
  companyId: [{ required: true, message: '请选择公司', trigger: 'change' }],
  companyName: [{ required: true, whitespace: true, message: '请填写公司名称', trigger: 'blur' }],
  positionName: [{ required: true, whitespace: true, message: '请填写岗位名称', trigger: 'blur' }],
  jobPositionId: [{ required: true, message: '请选择已有岗位，或切换直接录入', trigger: 'change' }],
  stage: [{ required: true, message: '请选择阶段', trigger: 'change' }],
  endReason: showReason.value ? [{ required: true, message: '请选择结束原因', trigger: 'change' }] : [],
  website: [{
    validator: (_rule, value, callback) => {
      if (!value) return callback()
      try {
        const url = new URL(value)
        if (['https:', 'http:'].includes(url.protocol) && url.hostname && !url.username && !url.password) return callback()
      } catch { /* Form feedback below. */ }
      callback(new Error('请输入有效的 http 或 https 地址'))
    }, trigger: 'blur',
  }],
}))
let choiceRequest = 0
async function searchChoices(query = '') {
  const searchCompanies = props.kind === 'position' || (directEntry.value && useExistingCompany.value)
  const searchPositions = props.kind === 'application' && !editingApplication.value && entryMode.value === 'existing'
  if (!searchCompanies && !searchPositions) return
  const request = ++choiceRequest
  choicesLoading.value = true
  try {
    if (searchCompanies) {
      const page = await api<Page<Company>>('/companies?' + new URLSearchParams({ q: query, size: '30' }))
      if (request === choiceRequest) {
        const existing = companyChoices.value.find(item => item.id === form.companyId)
        companyChoices.value = existing && !page.items.some(item => item.id === existing.id) ? [existing, ...page.items] : page.items
      }
    } else if (searchPositions) {
      const page = await api<Page<Position>>('/positions?' + new URLSearchParams({ q: query, size: '30' }))
      if (request === choiceRequest) {
        const existing = positionChoices.value.find(item => item.id === form.jobPositionId)
        positionChoices.value = existing && !page.items.some(item => item.id === existing.id) ? [existing, ...page.items] : page.items
      }
    }
  } catch (cause) {
    if (request === choiceRequest) error.value = cause instanceof Error ? cause.message : '加载选项失败'
  }
  finally { if (request === choiceRequest) choicesLoading.value = false }
}
async function switchEntry(mode: 'direct' | 'existing') {
  entryMode.value = mode
  await resetChoices()
}
async function switchCompany() {
  useExistingCompany.value = !useExistingCompany.value
  await resetChoices()
}
async function resetChoices() {
  ++choiceRequest
  choicesLoading.value = false
  error.value = ''
  await nextTick()
  formRef.value?.clearValidate()
  await searchChoices()
}
onMounted(async () => {
  if (props.kind === 'position' && props.item && 'companyId' in props.item) {
    companyChoices.value = [await api<Company>('/companies/' + props.item.companyId).catch(() =>
      ({ id: form.companyId, name: 'companyName' in props.item! ? String(props.item.companyName) : '当前公司', type: 'OTHER', website: null, notes: null }))]
  }
  await searchChoices()
})
async function reloadLatest() {
  if (!current.value || !('version' in current.value)) return
  busy.value = true
  try {
    const latest = await api<Application>('/applications/' + current.value.id)
    current.value = latest
    fill(latest)
    error.value = ''
    conflict.value = false
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '加载失败' }
  finally { busy.value = false }
}
async function save() {
  if (busy.value) return
  busy.value = true
  if (!await formRef.value?.validate().catch(() => false)) {
    busy.value = false
    return
  }
  error.value = ''
  conflict.value = false
  try {
    const optional = (value: string | null) => value?.trim() || null
    if (props.kind === 'company') {
      await api('/companies' + (current.value ? '/' + current.value.id : ''), current.value ? 'PUT' : 'POST',
        { name: form.name, type: form.type, website: optional(form.website), notes: optional(form.notes) })
    } else if (props.kind === 'position') {
      await api('/positions' + (current.value ? '/' + current.value.id : ''), current.value ? 'PUT' : 'POST',
        { companyId: form.companyId, name: form.name, location: optional(form.location), direction: optional(form.direction),
          jd: optional(form.jd), recruitmentBatch: optional(form.recruitmentBatch) })
    } else if (props.kind === 'stage') {
      await api('/applications/' + current.value!.id + '/stages', 'POST',
        { stage: form.stage, stageOn: form.stageOn || null, endReason: showReason.value ? form.endReason : null,
          remark: optional(form.remark), version: form.version })
    } else if (editingApplication.value) {
      await api('/applications/' + current.value!.id, 'PUT',
        { channel: optional(form.channel), appliedOn: form.appliedOn || null, notes: optional(form.notes), version: form.version })
    } else {
      const catalog = directEntry.value
        ? { companyId: useExistingCompany.value ? form.companyId : null,
            companyName: useExistingCompany.value ? null : form.companyName, companyType: form.companyType,
            positionName: form.positionName, location: optional(form.location), direction: optional(form.direction),
            recruitmentBatch: optional(form.recruitmentBatch), jd: optional(form.jd) }
        : { jobPositionId: form.jobPositionId }
      await api(directEntry.value ? '/applications/quick' : '/applications', 'POST', {
        ...catalog, channel: optional(form.channel), appliedOn: form.stage === 'TO_APPLY' || (showReason.value && !form.submitted) ? null : form.appliedOn || null,
        stage: form.stage, stageOn: form.stageOn || null, endReason: showReason.value ? form.endReason : null,
        submitted: form.submitted, notes: optional(form.notes),
      })
    }
    ElMessage.success('已保存')
    emit('saved')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '保存失败，请重试'
    conflict.value = cause instanceof ApiError && cause.status === 409 && !!current.value && 'version' in current.value
  } finally { busy.value = false }
}
</script>

<template>
  <el-dialog :model-value="true" :title="title" width="min(620px, calc(100% - 28px))" :close-on-click-modal="false"
    :close-on-press-escape="!busy" :show-close="!busy" @close="emit('close')">
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="form-alert" />
    <el-button v-if="conflict" size="small" :disabled="busy" @click="reloadLatest">重新加载最新档案</el-button>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="save">
      <template v-if="kind === 'company'">
        <el-form-item label="公司名称" prop="name"><el-input v-model="form.name" maxlength="255" placeholder="例如：星河科技" /></el-form-item>
        <div class="form-grid">
          <el-form-item label="公司类型" prop="type"><el-select v-model="form.type"><el-option v-for="(label, value) in companyTypes" :key="value" :label="label" :value="value" /></el-select></el-form-item>
          <el-form-item label="公司官网" prop="website"><el-input v-model="form.website" maxlength="2048" placeholder="https://" /></el-form-item>
        </div>
      </template>
      <template v-if="kind === 'position'">
        <el-form-item label="所属公司" prop="companyId">
          <el-select v-model="form.companyId" filterable remote :remote-method="searchChoices" :loading="choicesLoading" placeholder="输入公司名称搜索">
            <el-option v-for="item in companyChoices" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <span class="field-help">找不到公司？先到公司库新增。</span>
        </el-form-item>
        <el-form-item label="岗位名称" prop="name"><el-input v-model="form.name" maxlength="255" placeholder="例如：Java 后端开发工程师" /></el-form-item>
        <div class="form-grid">
          <el-form-item label="工作地点"><el-input v-model="form.location" maxlength="255" placeholder="例如：上海" /></el-form-item>
          <el-form-item label="岗位方向"><el-input v-model="form.direction" maxlength="128" placeholder="例如：Java 后端 / Agent" /></el-form-item>
        </div>
        <el-form-item label="招聘批次"><el-input v-model="form.recruitmentBatch" maxlength="64" placeholder="例如：2026 秋招（不同批次分别建岗）" /></el-form-item>
        <el-form-item label="JD 描述"><el-input v-model="form.jd" type="textarea" :rows="5" maxlength="40000" placeholder="粘贴岗位职责和要求" /></el-form-item>
      </template>
      <template v-if="kind === 'application'">
        <div v-if="!editingApplication" class="entry-modes" role="group" aria-label="投递录入方式">
          <el-button :type="entryMode === 'direct' ? 'primary' : 'default'" :aria-pressed="entryMode === 'direct'" :disabled="busy" @click="switchEntry('direct')">直接录入</el-button>
          <el-button :type="entryMode === 'existing' ? 'primary' : 'default'" :aria-pressed="entryMode === 'existing'" :disabled="busy" @click="switchEntry('existing')">选择已有岗位</el-button>
        </div>
        <template v-if="directEntry">
          <el-form-item v-if="!useExistingCompany" label="公司名称" prop="companyName">
            <el-input v-model="form.companyName" maxlength="255" placeholder="直接填写公司名称，无需搜索" />
            <el-button link type="primary" :disabled="busy" @click="switchCompany">选择已有公司</el-button>
          </el-form-item>
          <el-form-item v-else label="所属公司" prop="companyId">
            <el-select v-model="form.companyId" filterable remote :remote-method="searchChoices" :loading="choicesLoading" placeholder="选择具体的已有公司">
              <el-option v-for="item in companyChoices" :key="item.id" :label="item.name + ' · ' + companyTypes[item.type] + ' · #' + item.id" :value="item.id" />
            </el-select>
            <el-button link type="primary" :disabled="busy" @click="switchCompany">手动填写公司</el-button>
          </el-form-item>
          <el-form-item label="岗位名称" prop="positionName"><el-input v-model="form.positionName" maxlength="255" placeholder="例如：Java 后端开发工程师" /></el-form-item>
          <details class="entry-details">
            <summary>补充公司类型、地点、方向、批次和 JD（可选）</summary>
            <el-form-item label="公司类型"><el-select v-model="form.companyType"><el-option v-for="(label, value) in companyTypes" :key="value" :label="label" :value="value" /></el-select></el-form-item>
            <div class="form-grid">
              <el-form-item label="工作地点"><el-input v-model="form.location" maxlength="255" placeholder="例如：上海" /></el-form-item>
              <el-form-item label="岗位方向"><el-input v-model="form.direction" maxlength="128" placeholder="例如：Java 后端 / Agent" /></el-form-item>
            </div>
            <el-form-item label="招聘批次"><el-input v-model="form.recruitmentBatch" maxlength="64" placeholder="例如：2026 秋招（不同批次分别建岗）" /></el-form-item>
            <el-form-item label="JD 描述"><el-input v-model="form.jd" type="textarea" :rows="3" maxlength="40000" placeholder="粘贴岗位职责和要求" /></el-form-item>
          </details>
          <p class="field-help">保存时建立公司、岗位和投递。已有的匹配资料会复用；公司类型和 JD 不会覆盖已有资料。</p>
        </template>
        <el-form-item v-if="!editingApplication && !directEntry" label="投递岗位" prop="jobPositionId">
          <el-select v-model="form.jobPositionId" filterable remote :remote-method="searchChoices" :loading="choicesLoading" placeholder="搜索公司或岗位名称">
            <el-option v-for="item in positionChoices" :key="item.id" :label="[item.companyName, item.name, item.location, item.direction, item.recruitmentBatch, '#' + item.id].filter(Boolean).join(' · ')" :value="item.id" />
            <template #empty><div class="position-search-empty">没有匹配岗位<el-button link type="primary" :disabled="busy" @click="switchEntry('direct')">直接录入公司和岗位</el-button></div></template>
          </el-select><span class="field-help">搜索后需要选中一个结果。找不到岗位可切换直接录入；一个岗位保留一份投递档案。</span>
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="主要投递渠道"><el-input v-model="form.channel" maxlength="64" placeholder="官网 / Boss / 内推 / 招聘会" /></el-form-item>
          <el-form-item label="投递日期">
            <el-date-picker v-model="form.appliedOn" type="date" value-format="YYYY-MM-DD" placeholder="未知可留空"
              :disabled="(!editingApplication && (form.stage === 'TO_APPLY' || (showReason && !form.submitted))) || (editingApplication && current && 'submitted' in current && !current.submitted)" />
          </el-form-item>
        </div>
      </template>
      <template v-if="kind === 'stage' || (kind === 'application' && !editingApplication)">
        <div class="form-grid">
          <el-form-item label="招聘阶段" prop="stage"><el-select v-model="form.stage"><el-option v-for="(label, value) in stages" :key="value" :label="label" :value="value" /></el-select></el-form-item>
          <el-form-item label="阶段发生日期"><el-date-picker v-model="form.stageOn" type="date" value-format="YYYY-MM-DD" placeholder="未知可留空" /></el-form-item>
        </div>
        <el-form-item v-if="showReason" label="结束原因" prop="endReason"><el-select v-model="form.endReason"><el-option v-for="(label, value) in reasons" :key="value" :label="label" :value="value" /></el-select></el-form-item>
        <el-form-item v-if="showReason && kind === 'application' && !editingApplication"><el-checkbox v-model="form.submitted">这个岗位曾经实际投递过</el-checkbox></el-form-item>
        <p class="field-help">可直接选择实际阶段。已拒绝表示企业拒绝；主动拒绝 Offer 请选择已结束。</p>
      </template>
      <el-form-item v-if="kind === 'stage'" label="本次变更备注"><el-input v-model="form.remark" type="textarea" :rows="3" maxlength="16000" placeholder="例如：邮件通知二面，时间待定" /><span class="field-help">阶段、发生日期或结束原因变化时，才会新增历史记录。</span></el-form-item>
      <el-form-item v-if="kind === 'company' || kind === 'application'" label="备注"><el-input v-model="form.notes" type="textarea" :rows="3" maxlength="16000" placeholder="记录补充信息" /></el-form-item>
    </el-form>
    <template #footer><el-button :disabled="busy" @click="emit('close')">取消</el-button><el-button type="primary" :loading="busy" @click="save">保存</el-button></template>
  </el-dialog>
</template>
