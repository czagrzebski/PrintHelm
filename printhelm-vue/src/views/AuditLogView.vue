<script setup lang="ts">
import { ref, onMounted } from 'vue'
import DataTable, { type DataTablePageEvent } from 'primevue/datatable'
import Column from 'primevue/column'
import Select from 'primevue/select'
import InputText from 'primevue/inputtext'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Toast from 'primevue/toast'
import { useToast } from 'primevue/usetoast'
import auditApi from '@/api/AuditApi'
import type { ApiAuditLogEntry } from '@/client/printhelm-web-openapi'

const toast = useToast()

const entries = ref<ApiAuditLogEntry[]>([])
const loading = ref(true)
const totalRecords = ref(0)
const page = ref(0)
const rows = ref(25)

const entityTypeFilter = ref<string | null>(null)
const usernameFilter = ref('')

const entityTypeOptions = [
  { label: 'Job Orders', value: 'JobOrder' },
  { label: 'Printers', value: 'Printer' },
  { label: 'Filament Spools', value: 'FilamentSpool' },
  { label: 'Users', value: 'User' },
  { label: 'Business Settings', value: 'BusinessSettings' },
]

async function fetchEntries() {
  loading.value = true
  try {
    const res = await auditApi.getAuditLog(
      page.value,
      rows.value,
      entityTypeFilter.value ?? undefined,
      usernameFilter.value.trim() || undefined,
    )
    entries.value = res.data.entries ?? []
    totalRecords.value = res.data.totalElements ?? 0
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to load audit log', life: 4000 })
  } finally {
    loading.value = false
  }
}

function onPage(event: DataTablePageEvent) {
  page.value = event.page
  rows.value = event.rows
  fetchEntries()
}

function applyFilters() {
  page.value = 0
  fetchEntries()
}

function clearFilters() {
  entityTypeFilter.value = null
  usernameFilter.value = ''
  page.value = 0
  fetchEntries()
}

function formatTimestamp(iso?: string): string {
  if (!iso) return ''
  return new Date(iso).toLocaleString(undefined, {
    year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit',
  })
}

function actionLabel(action?: string): string {
  const words = (action ?? '').replace(/_/g, ' ').toLowerCase()
  return words.charAt(0).toUpperCase() + words.slice(1)
}

function actionSeverity(action?: string) {
  if (!action) return 'secondary'
  if (action.endsWith('DELETED') || action.endsWith('UNQUEUED') || action === 'PRINT_STOPPED') return 'danger'
  if (action.endsWith('CREATED') || action.endsWith('UPLOADED') || action === 'PRINT_STARTED' || action === 'PRINT_RESUMED') return 'success'
  if (action === 'PRINT_PAUSED' || action === 'AI_ACTION_PROPOSED') return 'warn'
  if (action.endsWith('UPDATED') || action.endsWith('REORDERED') || action.endsWith('RESET')
    || action.endsWith('_SET') || action === 'AXIS_JOGGED' || action === 'AXES_HOMED') return 'info'
  return 'secondary'
}

onMounted(fetchEntries)
</script>

<template>
  <div class="audit-view">
    <Toast />
    <div class="page-header">
      <div>
        <h2 class="page-title">Audit Log</h2>
        <p class="page-subtitle">Who changed what, and when</p>
      </div>
    </div>

    <div class="filter-row">
      <Select v-model="entityTypeFilter" :options="entityTypeOptions" option-label="label" option-value="value"
        show-clear placeholder="All entity types" class="filter-item" @change="applyFilters" />
      <InputText v-model="usernameFilter" placeholder="Filter by username" class="filter-item"
        @keyup.enter="applyFilters" />
      <Button label="Apply" severity="secondary" icon="mdi mdi-filter-outline" @click="applyFilters" />
      <Button label="Clear" severity="secondary" text @click="clearFilters" />
    </div>

    <div class="table-card">
      <DataTable :value="entries" :loading="loading" data-key="auditId" size="small" lazy paginator
        :rows="rows" :total-records="totalRecords" :first="page * rows"
        :rows-per-page-options="[25, 50, 100]" @page="onPage"
        empty-message="No audit entries match the current filters.">
        <Column header="Time" style="min-width:180px">
          <template #body="{ data }">
            <span class="mono">{{ formatTimestamp(data.timestamp) }}</span>
          </template>
        </Column>
        <Column field="username" header="User" style="min-width:110px">
          <template #body="{ data }">
            <span :class="{ 'system-user': data.username === 'system' }">
              <i v-if="data.username === 'system'" class="mdi mdi-robot-outline" />
              {{ data.username }}
            </span>
          </template>
        </Column>
        <Column header="Action" style="min-width:150px">
          <template #body="{ data }">
            <Tag :value="actionLabel(data.action)" :severity="actionSeverity(data.action)" />
          </template>
        </Column>
        <Column header="Entity" style="min-width:130px">
          <template #body="{ data }">
            {{ data.entityType }}<span v-if="data.entityId" class="muted"> #{{ data.entityId }}</span>
          </template>
        </Column>
        <Column field="details" header="Details" style="min-width:260px">
          <template #body="{ data }">
            <span class="details">{{ data.details ?? '—' }}</span>
          </template>
        </Column>
      </DataTable>
    </div>
  </div>
</template>

<style scoped>
.audit-view {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

@keyframes fade-up {
  from { opacity: 0; transform: translateY(14px); }
  to   { opacity: 1; transform: translateY(0); }
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  animation: fade-up 0.3s ease-out both;
}
.page-title { font-size: 1.5rem; font-weight: 700; margin: 0 0 0.25rem; color: var(--ph-text); }
.page-subtitle { font-size: 0.875rem; color: var(--ph-text-muted); margin: 0; }

.filter-row {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  flex-wrap: wrap;
  animation: fade-up 0.32s ease-out 0.05s both;
}
.filter-item { min-width: 200px; }

.table-card {
  background: var(--ph-glass);
  backdrop-filter: blur(var(--ph-blur));
  -webkit-backdrop-filter: blur(var(--ph-blur));
  border: 1px solid var(--ph-glass-border);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: var(--ph-shadow-card), 0 1px 0 rgba(255, 255, 255, 0.05) inset;
  animation: fade-up 0.35s cubic-bezier(0.16, 1, 0.3, 1) 0.1s both;
}

.mono { font-variant-numeric: tabular-nums; }
.muted { color: var(--ph-text-muted); }
.system-user { color: var(--ph-text-muted); font-style: italic; }
.details {
  display: inline-block;
  max-width: 420px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}
</style>
