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
import { printerApi } from '@/api/PrinterApi'
import { usePrinterSocket } from '@/composables/usePrinterSocket'
import { useCountUp } from '@/composables/useCountUp'
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

const totalCount = computed(() => printers.value.length)
const printingCount = computed(
  () => printers.value.filter((p) => p.state?.toLowerCase().includes('print')).length,
)
const idleCount = computed(
  () =>
    printers.value.filter(
      (p) => p.state && !p.state.toLowerCase().includes('print') && p.state !== 'OFFLINE',
    ).length,
)
const offlineCount = computed(
  () => printers.value.filter((p) => !p.state || p.state === 'OFFLINE').length,
)

const totalDisplay = useCountUp(totalCount)
const printingDisplay = useCountUp(printingCount)
const idleDisplay = useCountUp(idleCount)
const offlineDisplay = useCountUp(offlineCount)

const stats = computed(() => [
  { label: 'Total Printers', value: totalDisplay.value,    live: false,                     icon: 'mdi mdi-printer-3d',             color: '#22d3ee', bg: 'rgba(34,211,238,0.1)'  },
  { label: 'Printing',       value: printingDisplay.value, live: printingCount.value > 0,   icon: 'mdi mdi-printer-3d-nozzle-heat', color: '#4ade80', bg: 'rgba(74,222,128,0.1)'  },
  { label: 'Idle',           value: idleDisplay.value,     live: false,                     icon: 'mdi mdi-printer-3d-nozzle',      color: '#38bdf8', bg: 'rgba(56,189,248,0.1)'  },
  { label: 'Offline',        value: offlineDisplay.value,  live: false,                     icon: 'mdi mdi-printer-3d-off',         color: '#f87171', bg: 'rgba(248,113,113,0.1)' },
])

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

function applyState(row: PrinterRow, state: ApiPrinterState) {
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
}

async function fetchPrinters() {
  loading.value = true
  error.value = ''
  try {
    const res = await api.get<PrinterRow[]>('/printer')
    printers.value = res.data

    if (printers.value.length > 0) {
      // Last known states from the backend cache — fills the table instantly;
      // live socket data overwrites as it arrives.
      printerApi
        .getPrinterStates()
        .then(({ data }) => {
          for (const [id, cached] of Object.entries(data)) {
            const row = printers.value.find((p) => p.printerId === Number(id))
            if (row) applyState(row, cached)
          }
        })
        .catch(() => { /* no cached states yet */ })

      connect(
        printers.value.map((p) => p.printerId),
        (printerId, state: ApiPrinterState) => {
          const row = printers.value.find((p) => p.printerId === printerId)
          if (row) applyState(row, state)
        },
      )
    }
  } catch {
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
      <div class="live-badge" :class="{ 'live-badge--active': printingCount > 0 }">
        <span class="live-dot" />
        {{ printingCount > 0 ? `${printingCount} printing now` : 'Fleet idle' }}
      </div>
    </div>

    <div class="stats-grid">
      <Card
        v-for="(stat, i) in stats"
        :key="stat.label"
        class="stat-card"
        :style="{ animationDelay: `${i * 0.07}s` }"
      >
        <template #content>
          <div class="stat-content">
            <div class="stat-icon" :style="{ background: stat.bg, color: stat.color, borderColor: stat.color + '33', boxShadow: `0 0 18px ${stat.color}22` }">
              <i :class="stat.icon" />
              <span v-if="stat.live" class="stat-live-dot" />
            </div>
            <div class="stat-info">
              <span class="stat-value" :style="stat.live ? { color: stat.color } : undefined">{{ stat.value }}</span>
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
  animation: fade-up 0.3s ease-out both;
}

.live-badge {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.4rem 0.875rem;
  border-radius: 99px;
  border: 1px solid var(--ph-border-strong);
  background: var(--ph-glass);
  backdrop-filter: blur(var(--ph-blur));
  color: var(--ph-text-muted);
  font-size: 0.78rem;
  font-weight: 500;
}

.live-badge--active {
  border-color: rgba(74, 222, 128, 0.35);
  color: #4ade80;
  box-shadow: 0 0 18px rgba(74, 222, 128, 0.12);
}

.live-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--ph-text-muted);
  flex-shrink: 0;
}

.live-badge--active .live-dot {
  background: #4ade80;
  animation: ph-pulse-dot 1.8s ease-out infinite;
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

/* Stat card: staggered entry + hover lift */
.stat-card {
  animation: fade-up 0.35s cubic-bezier(0.16, 1, 0.3, 1) both;
  transition: transform 0.2s ease, box-shadow 0.2s ease !important;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow:
    0 16px 40px rgba(0, 0, 0, 0.45),
    0 0 24px rgba(34, 211, 238, 0.08) !important;
  border-color: rgba(34, 211, 238, 0.25) !important;
}

.printers-card {
  animation: fade-up 0.35s cubic-bezier(0.16, 1, 0.3, 1) 0.28s both;
}

@keyframes fade-up {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.stat-icon {
  position: relative;
  width: 3rem;
  height: 3rem;
  border-radius: 12px;
  border: 1px solid transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  flex-shrink: 0;
  transition: transform 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.stat-card:hover .stat-icon {
  transform: scale(1.08) rotate(-3deg);
}

.stat-live-dot {
  position: absolute;
  top: -3px;
  right: -3px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #4ade80;
  border: 2px solid var(--ph-bg-darkest);
  animation: ph-pulse-dot 1.8s ease-out infinite;
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
  font-variant-numeric: tabular-nums;
  transition: color 0.3s;
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
