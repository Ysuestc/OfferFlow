<script setup lang="ts">
import { companyTypes } from '../api'

const selected = defineModel<string>({ required: true })
defineProps<{ disabled?: boolean }>()
</script>

<template>
  <div class="company-type-tags" role="group" aria-label="企业类型">
    <button v-for="(label, value) in companyTypes" :key="value" type="button"
      :class="{ selected: selected === value }" :aria-pressed="selected === value"
      :disabled="disabled" @click="selected = value">
      <span v-if="selected === value" aria-hidden="true">✓ </span>{{ label }}
    </button>
  </div>
</template>

<style scoped>
.company-type-tags { display: flex; flex-wrap: wrap; gap: 8px; width: 100%; }
button {
  border: 1px solid var(--border); border-radius: 999px; padding: 7px 14px;
  background: #fff; color: var(--ink); font-size: 13px; line-height: 1.4;
}
button:hover { border-color: var(--el-color-primary); }
button.selected { color: #fff; background: var(--el-color-primary); border-color: var(--el-color-primary); }
button:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 3px; }
button:disabled { opacity: .65; cursor: not-allowed; }
</style>
