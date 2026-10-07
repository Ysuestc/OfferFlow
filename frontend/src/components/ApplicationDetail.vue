<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, stages, reasons, stageTone, day, localTime, type Application, type History, type Position } from '../api'
import RecordEditor from './RecordEditor.vue'
const props = defineProps<{ id: string }>()
const emit = defineEmits<{ close: []; updated: [] }>()
const application = ref<Application>()
const history = ref<History[]>([])
const position = ref<Position>()
const loading = ref(true)
const error = ref('')
const editor = ref<'application' | 'stage' | null>(null)
async function load() {
  loading.value = true
  error.value = ''
  try {
    const [app, entries] = await Promise.all([api<Application>('/applications/' + props.id), api<History[]>('/applications/' + props.id + '/history')])
    application.value = app
    history.value = entries
    position.value = await api<Position>('/positions/' + app.jobPositionId)
  } catch (cause) { error.value = cause instanceof Error ? cause.message : '加载失败' }
  finally { loading.value = false }
}
async function saved() { editor.value = null; await load(); emit('updated') }
onMounted(load)
</script>

<template>
  <el-drawer :model-value="true" title="投递档案" size="min(660px, 100%)" @close="emit('close')">
    <div v-loading="loading" class="detail-body">
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />
      <el-button v-if="error" @click="load">重新加载</el-button>
      <template v-if="application">
        <div class="detail-heading">
          <span class="eyebrow">{{ application.companyName }}</span><h2>{{ application.positionName }}</h2>
          <div class="record-meta">{{ application.location || '地点待补充' }} · {{ application.direction || '方向待补充' }} <span v-if="application.recruitmentBatch"> · {{ application.recruitmentBatch }}</span></div>
          <el-tag :type="stageTone(application.currentStage)" effect="light">{{ stages[application.currentStage] }}</el-tag>
          <span v-if="application.endReason" class="reason">{{ reasons[application.endReason] }}</span>
        </div>
        <div class="detail-actions"><el-button type="primary" @click="editor = 'stage'">更新阶段</el-button><el-button @click="editor = 'application'">编辑投递信息</el-button><el-button text @click="load">刷新</el-button></div>
        <dl class="fact-grid">
          <div><dt>主要渠道</dt><dd>{{ application.channel || '未填写' }}</dd></div>
          <div><dt>投递日期</dt><dd>{{ day(application.appliedOn) }}</dd></div>
          <div><dt>是否实际投递</dt><dd>{{ application.submitted ? '已投递过' : '尚未投递' }}</dd></div>
          <div><dt>当前阶段日期</dt><dd>{{ day(application.currentStageOn) }}</dd></div>
        </dl>
        <section v-if="application.notes" class="detail-section"><h3>投递备注</h3><p class="preserve-text">{{ application.notes }}</p></section>
        <section class="detail-section"><h3>阶段时间线 <span>{{ history.length }} 条记录</span></h3><p class="field-help">按记录顺序展示，业务日期未知时留空；纠正阶段会保留原记录。</p>
          <el-timeline>
            <el-timeline-item v-for="entry in [...history].reverse()" :key="entry.id" :timestamp="day(entry.stageOn)" placement="top" :type="stageTone(entry.stage)">
              <div class="timeline-card"><strong>{{ stages[entry.stage] }}</strong><span v-if="entry.endReason"> · {{ reasons[entry.endReason] }}</span>
                <p v-if="entry.remark" class="preserve-text">{{ entry.remark }}</p><small>记录于 {{ localTime(entry.recordedAt) }}</small>
              </div>
            </el-timeline-item>
          </el-timeline>
        </section>
        <section v-if="position?.jd" class="detail-section"><h3>岗位 JD</h3><p class="preserve-text">{{ position.jd }}</p></section>
      </template>
    </div>
    <RecordEditor v-if="editor && application" :kind="editor" :item="application" @close="editor = null" @saved="saved" />
  </el-drawer>
</template>
