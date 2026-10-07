<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, eventTime, day, stages, stageTone, todoKinds, type Dashboard } from '../api'
const emit = defineEmits<{ application: [id: string]; interview: [id: string]; todos: [timing?: string]; applications: [] }>()
const data = ref<Dashboard>()
const loading = ref(false)
const error = ref('')
const metrics = [
  { key: 'totalSubmitted', label: '总投递数', hint: '实际投递过的档案' },
  { key: 'active', label: '进行中', hint: '已投递且尚未结束或获 Offer' },
  { key: 'interviewing', label: '面试中', hint: '一面至 HR 面，属于进行中' },
  { key: 'offers', label: 'Offer', hint: '当前处于 Offer 阶段' },
  { key: 'rejected', label: '已拒绝', hint: '招聘方拒绝' },
] as const
async function load() {
  loading.value = true
  error.value = ''
  try { data.value = await api<Dashboard>('/dashboard') }
  catch (cause) { error.value = cause instanceof Error ? cause.message : '概览加载失败' }
  finally { loading.value = false }
}
defineExpose({ load })
onMounted(load)
</script>

<template>
  <div v-loading="loading" class="dashboard">
    <div class="dashboard-toolbar"><span>把进展记清楚，把下一步安排好。</span><el-button text :disabled="loading" @click="load">刷新概览</el-button></div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-button v-if="error" @click="load">重新加载概览</el-button>
    <template v-else-if="data">
      <div class="metric-grid">
        <article v-for="metric in metrics" :key="metric.key" class="metric-card" :class="'metric-' + metric.key"><span>{{ metric.label }}</span><strong>{{ data.counts[metric.key] }}</strong><small>{{ metric.hint }}</small></article>
      </div>
      <div class="deadline-summary">
        <button @click="emit('todos', 'UPCOMING')"><span class="small-dot"></span>未来七天 <strong>{{ data.upcomingTodoCount }}</strong></button>
        <button :class="{ overdue: data.overdueTodoCount > 0 }" @click="emit('todos', 'OVERDUE')">已逾期 <strong>{{ data.overdueTodoCount }}</strong></button>
        <button @click="emit('todos', 'UNDATED')">时间未定 <strong>{{ data.undatedTodoCount }}</strong></button>
      </div>
      <div class="dashboard-grid">
        <section class="dashboard-panel"><div class="section-heading"><h2>接下来七天</h2><el-button text @click="emit('todos', 'UPCOMING')">全部待办 ↗</el-button></div>
          <div class="panel-note">未完成事项 · 以服务器当前时刻起算七天</div>
          <button v-for="item in data.upcomingTodos" :key="item.id" class="dashboard-row" @click="emit('todos', 'UPCOMING')"><span class="row-badge">{{ todoKinds[item.kind] }}</span><span><strong>{{ item.title }}</strong><small>{{ item.companyName }} · {{ item.positionName }}</small><time>{{ eventTime(item.dueAt) }}</time></span></button>
          <p v-if="!data.upcomingTodos.length" class="panel-empty">近期没有已定时间的待办。可以去待办页安排下一步。</p>
        </section>
        <section class="dashboard-panel"><div class="section-heading"><h2>需要关注</h2><el-button text @click="emit('todos', 'OVERDUE')">逾期事项 ↗</el-button></div>
          <div class="panel-note">已过时间且尚未完成 · 不自动判定结果</div>
          <button v-for="item in data.overdueTodos" :key="item.id" class="dashboard-row" @click="emit('todos', 'OVERDUE')"><span class="row-badge overdue">逾期</span><span><strong>{{ item.title }}</strong><small>{{ item.companyName }} · {{ item.positionName }}</small><time>{{ eventTime(item.dueAt) }}</time></span></button>
          <p v-if="!data.overdueTodos.length" class="panel-empty">没有逾期事项，按自己的节奏推进。</p>
        </section>
        <section class="dashboard-panel"><div class="section-heading"><h2>最近投递</h2><el-button text @click="emit('applications')">投递台账 ↗</el-button></div>
          <button v-for="item in data.recentApplications" :key="item.id" class="dashboard-row" @click="emit('application', item.id)"><span class="company-avatar">{{ item.companyName.slice(0, 1) }}</span><span><strong>{{ item.positionName }}</strong><small>{{ item.companyName }}</small><time>{{ day(item.appliedOn) }}</time></span><el-tag :type="stageTone(item.currentStage)">{{ stages[item.currentStage] }}</el-tag></button>
          <p v-if="!data.recentApplications.length" class="panel-empty">尚无实际投递，先整理目标公司和岗位。</p>
        </section>
        <section class="dashboard-panel"><div class="section-heading"><h2>最近面试</h2><span class="panel-note">已发生的面试</span></div>
          <button v-for="item in data.recentInterviews" :key="item.id" class="dashboard-row" @click="emit('interview', item.id)"><span class="row-badge">复盘</span><span><strong>{{ item.roundName }} · {{ item.companyName }}</strong><small>{{ item.positionName }}</small><time>{{ eventTime(item.interviewAt) }}</time></span></button>
          <p v-if="!data.recentInterviews.length" class="panel-empty">尚无已定时间的面试记录，未来预约请在面试页查看。</p>
        </section>
      </div>
      <p class="field-help">每栏最多显示 5 条。时间按本机时区显示；更新于 {{ eventTime(data.generatedAt) }}。</p>
    </template>
  </div>
</template>
