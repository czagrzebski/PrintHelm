<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Card from 'primevue/card'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'
import ProgressBar from 'primevue/progressbar'
import { useAuthStore } from '@/stores/auth'
import { api } from '@/api/Configuration'
import { usePrinterSocket } from '@/composables/usePrinterSocket'
import type { ApiPrinterState } from '@/client/printhelm-web-openapi'

const authStore = useAuthStore()
const router = useRouter()
const { connect } = usePrinterSocket()

function onRowClick(event: { data: PrinterRow }) {
  router.push({ name: 'printer', params: { id: event.data.printerId } })
}

interface PrinterRow {
  printerId: number
  printerName: string
  printerModel: string
  printerType: string
  location: string
  serialNumber: string
  // live from WebSocket
  state?: string
  progress?: number
  currentLayer?: number
  totalLayers?: number
  nozzleTemp?: number
  nozzleTargetTemp?: number
  bedTemp?: number
  bedTargetTemp?: number
  file?: string
  subtaskName?: string
  wifiSignalStrength?: string
  lastUpdated?: string
}

const printers = ref<PrinterRow[]>([])
const loading = ref(true)
const error = ref('')

const stats = computed(() => {
  const total = printers.value.length
  const printing = printers.value.filter(
    (p) => p.state?.toLowerCase().includes('print'),
  ).length
  const idle = printers.value.filter(
    (p) => p.state && !p.state.toLowerCase().includes('print') && p.state !== 'OFFLINE',
  ).length
  const offline = printers.value.filter((p) => !p.state || p.state === 'OFFLINE').length
  return [
    { label: 'Total Printers', value: total,    icon: 'mdi mdi-printer-3d',         color: '#22d3ee', bg: 'rgba(34,211,238,0.1)'  },
    { label: 'Printing',       value: printing,  icon: 'mdi mdi-printer-3d-nozzle-heat', color: '#4ade80', bg: 'rgba(74,222,128,0.1)'  },
    { label: 'Idle',           value: idle,      icon: 'mdi mdi-printer-3d-nozzle',      color: '#38bdf8', bg: 'rgba(56,189,248,0.1)'  },
    { label: 'Offline',        value: offline,   icon: 'mdi mdi-printer-3d-off',         color: '#f87171', bg: 'rgba(248,113,113,0.1)' },
  ]
})

function statusSeverity(state?: string) {
  if (!state) return 'secondary'
  const s = state.toLowerCase()
  if (s.includes('print')) return 'success'
  if (s === 'idle' || s === 'standby') return 'info'
  if (s === 'offline') return 'secondary'
  return 'warn'
}

function formatTemp(temp?: number, target?: number): string {
  if (temp == null) return '—'
  if (target != null) return `${temp.toFixed(0)}°/${target.toFixed(0)}°`
  return `${temp.toFixed(0)}°`
}

function formatLayers(cur?: number, total?: number): string {
  if (cur == null || total == null) return '—'
  return `${cur} / ${total}`
}

function truncateFile(name?: string): string {
  if (!name) return '—'
  return name.length > 24 ? name.slice(0, 22) + '…' : name
}

async function fetchPrinters() {
  loading.value = true
  error.value = ''
  try {
    const res = await api.get<PrinterRow[]>('/printer')
    printers.value = res.data

    if (printers.value.length > 0) {
      connect(
        printers.value.map((p) => p.printerId),
        (printerId, state: ApiPrinterState) => {
          const row = printers.value.find((p) => p.printerId === printerId)
          if (!row) return
          row.state = state.state
          row.progress = state.progress
          row.currentLayer = state.currentLayer
          row.totalLayers = state.totalLayers
          row.nozzleTemp = state.nozzleTemp
          row.nozzleTargetTemp = state.nozzleTargetTemp
          row.bedTemp = state.bedTemp
          row.bedTargetTemp = state.bedTargetTemp
          row.file = state.file ?? state.subtaskName
          row.subtaskName = state.subtaskName
          row.wifiSignalStrength = state.wifiSignalStrength
          row.lastUpdated = state.timestamp
            ? new Date(state.timestamp).toLocaleTimeString()
            : new Date().toLocaleTimeString()
        },
      )
    }
  } catch (e) {
    error.value = 'Failed to load printers.'
  } finally {
    loading.value = false
  }
}

onMounted(fetchPrinters)
</script>

<template>
  <div class="dashboard">
    <div class="dashboard-header">
      <div>
        <h2 class="page-title">Dashboard</h2>
        <p class="page-subtitle">Welcome back, {{ authStore.username ?? 'User' }}</p>
      </div>
    </div>

    <div class="stats-grid">
      <Card v-for="stat in stats" :key="stat.label" class="stat-card">
        <template #content>
          <div class="stat-content">
            <div class="stat-icon" :style="{ background: stat.bg, color: stat.color, borderColor: stat.color + '33' }">
              <i :class="stat.icon" />
            </div>
            <div class="stat-info">
              <span class="stat-value">{{ stat.value }}</span>
              <span class="stat-label">{{ stat.label }}</span>
            </div>
          </div>
        </template>
      </Card>
    </div>

    <Card class="printers-card">
      <template #title>Printers</template>
      <template #content>
        <div v-if="error" class="error-msg">{{ error }}</div>
        <DataTable
          :value="printers"
          :loading="loading"
          data-key="printerId"
          empty-message="No printers registered yet."
          size="small"
          scrollable
          scroll-height="flex"
          selection-mode="single"
          class="clickable-rows"
          @row-click="onRowClick"
        >
          <Column field="printerName" header="Name" style="min-width:130px" />
          <Column field="printerModel" header="Model" style="min-width:110px" />
          <Column field="printerType" header="Type" style="min-width:100px" />
          <Column field="location" header="Location" style="min-width:100px">
            <template #body="{ data }">{{ data.location || '—' }}</template>
          </Column>

          <Column header="Status" style="min-width:110px">
            <template #body="{ data }">
              <Tag
                :value="data.state ?? 'Unknown'"
                :severity="statusSeverity(data.state)"
              />
            </template>
          </Column>

          <Column header="Progress" style="min-width:140px">
            <template #body="{ data }">
              <div v-if="data.progress != null" class="progress-cell">
                <ProgressBar :value="data.progress" :show-value="false" style="height:6px" />
                <span class="progress-label">{{ data.progress.toFixed(0) }}%</span>
              </div>
              <span v-else class="muted">—</span>
            </template>
          </Column>

          <Column header="Layers" style="min-width:90px">
            <template #body="{ data }">
              <span :class="{ muted: data.currentLayer == null }">
                {{ formatLayers(data.currentLayer, data.totalLayers) }}
              </span>
            </template>
          </Column>

          <Column header="Nozzle" style="min-width:110px">
            <template #body="{ data }">
              <span :class="{ muted: data.nozzleTemp == null, 'temp-hot': (data.nozzleTemp ?? 0) > 150 }">
                {{ formatTemp(data.nozzleTemp, data.nozzleTargetTemp) }}
              </span>
            </template>
          </Column>

          <Column header="Bed" style="min-width:100px">
            <template #body="{ data }">
              <span :class="{ muted: data.bedTemp == null, 'temp-warm': (data.bedTemp ?? 0) > 40 }">
                {{ formatTemp(data.bedTemp, data.bedTargetTemp) }}
              </span>
            </template>
          </Column>

          <Column header="File" style="min-width:160px">
            <template #body="{ data }">
              <span :class="{ muted: !data.file }" :title="data.file">
                {{ truncateFile(data.file) }}
              </span>
            </template>
          </Column>

          <Column header="WiFi" style="min-width:80px">
            <template #body="{ data }">
              <span :class="{ muted: !data.wifiSignalStrength }">
                {{ data.wifiSignalStrength ?? '—' }}
              </span>
            </template>
          </Column>

          <Column header="Updated" style="min-width:90px">
            <template #body="{ data }">
              <span :class="{ muted: !data.lastUpdated }">
                {{ data.lastUpdated ?? '—' }}
              </span>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  height: 100%;
}

.dashboard-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0 0 0.25rem;
  color: var(--ph-text);
}

.page-subtitle {
  font-size: 0.875rem;
  color: var(--ph-text-muted);
  margin: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1rem;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.stat-icon {
  width: 3rem;
  height: 3rem;
  border-radius: 10px;
  border: 1px solid transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  flex-shrink: 0;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  line-height: 1;
  color: var(--ph-text);
}

.stat-label {
  font-size: 0.78rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--ph-text-muted);
  margin-top: 0.3rem;
}

.printers-card {
  flex: 1;
  min-height: 0;
}

.progress-cell {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.progress-label {
  font-size: 0.75rem;
  color: var(--ph-text-muted);
  white-space: nowrap;
}

.muted {
  color: var(--ph-text-muted);
}

.temp-hot {
  color: #f87171;
}

.temp-warm {
  color: #fb923c;
}

.error-msg {
  color: #f87171;
  font-size: 0.875rem;
  margin-bottom: 1rem;
}
</style>
