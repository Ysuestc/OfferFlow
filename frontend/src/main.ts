import { createApp } from 'vue'
import { ElAlert, ElButton, ElCheckbox, ElDatePicker, ElDialog, ElDrawer, ElForm, ElFormItem,
  ElInput, ElLoading, ElOption, ElPagination, ElSelect, ElTag, ElTimeline, ElTimelineItem } from 'element-plus'
import 'element-plus/dist/index.css'
import './style.css'
import App from './App.vue'

const app = createApp(App)
for (const component of [ElAlert, ElButton, ElCheckbox, ElDatePicker, ElDialog, ElDrawer, ElForm,
  ElFormItem, ElInput, ElOption, ElPagination, ElSelect, ElTag, ElTimeline, ElTimelineItem]) app.use(component)
app.use(ElLoading).mount('#app')
