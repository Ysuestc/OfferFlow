<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, eventTime, interviewFormats, type Interview } from '../api'
import ActivityEditor from './ActivityEditor.vue'
const props = defineProps<{ id: string }>()
const emit = defineEmits<{ close: []; updated: []; application: [id: string] }>()
const item = ref<Interview>()
const loading = ref(false)
const error = ref('')
const editing = ref(false)
async function load() {
  loading.value = true
  error.value = ''
  try { item.value = await api<Interview>('/interviews/' + props.id) }
  catch (cause) { error.value = cause instanceof Error ? cause.message : '加载失败' }
  finally { loading.value = false }
}
async function saved() { editing.value = false; await load(); emit('updated') }
onMounted(load)
</script>

<template>
  <el-drawer :model-value="true" title="面试详情" size="min(700px, 100%)" @close="emit('close')">
    <div v-loading="loading" class="detail-body">
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <el-button v-if="error" @click="load">重新加载</el-button>
      <template v-if="item">
        <div class="detail-heading"><span class="eyebrow">{{ item.companyName }}</span><h2>{{ item.roundName }}</h2><p>{{ item.positionName }}</p><el-tag>{{ interviewFormats[item.format || ''] || '形式未定' }}</el-tag><p>{{ eventTime(item.interviewAt) }}</p></div>
        <div class="detail-actions"><el-button type="primary" @click="editing = true">编辑面试记录</el-button><el-button @click="emit('application', item.applicationId)">查看投递档案</el-button></div>
        <section v-for="field in (['questions', 'answers', 'review', 'result'] as const)" :key="field" class="detail-section">
          <h3>{{ { questions: '面试问题', answers: '我的回答', review: '面试复盘', result: '面试结果' }[field] }}</h3><p class="preserve-text">{{ item[field] || '尚未填写' }}</p>
        </section>
      </template>
    </div>
    <ActivityEditor v-if="editing" kind="interview" :id="id" @close="editing = false" @saved="saved" />
  </el-drawer>
</template>
