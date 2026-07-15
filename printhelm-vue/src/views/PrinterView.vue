<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import CameraPlayer from '@/components/CameraPlayer.vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Tag from 'primevue/tag'
import { api } from '@/api/Configuration'
import { printerApi } from '@/api/PrinterApi'
import { diagnosticApi } from '@/api/DiagnosticApi'
import type { ApiDiagnosticReport } from '@/client/printhelm-web-openapi'
import { usePrinterSocket } from '@/composables/usePrinterSocket'
import { usePrinterCommands } from '@/composables/usePrinterCommands'
import { usePrinterFiles } from '@/composables/usePrinterFiles'
import { useHomingGuard } from '@/composables/useHomingGuard'
import PrintJobInfoDialog from '@/components/PrintJobInfoDialog.vue'
import AmsVisual from '@/components/AmsVisual.vue'
import type { ApiPrinterState, ApiFan, ApiLight, ApiMaterial } from '@/client/printhelm-web-openapi'
import { useNotificationsStore } from '@/stores/notifications'
import type { NotificationSeverity } from '@/service/NotificationService'

const route = useRoute()
const router = useRouter()
const printerId = Number(route.params.id)
const { connect } = usePrinterSocket()

interface PrinterInfo {
  printerId: number
  printerName: string
  printerModel: string
  printerType: string
  location: string
  serialNumber: string
  connectionConfig?: {
    connectionType?: string
    brokerUrl?: string
    topic?: string
    username?: string
  }
}

const printer = ref<PrinterInfo | null>(null)
const state = ref<ApiPrinterState | null>(null)
const loading = ref(true)
const error = ref('')
const cameraError = ref(false)
const cameraLoading = ref(true)
const cameraExpanded = ref(false)
const streamKey = ref(0)

// ── Controls ──────────────────────────────────────────────────────────────
const commands = usePrinterCommands(printerId)
const homingGuard = useHomingGuard(printerId, commands)
const moveStep = ref(10)
const nozzleTempInput = ref<number | null>(null)
const bedTempInput = ref<number | null>(null)

async function applyNozzleTemp() {
  if (nozzleTempInput.value == null) return
  await commands.setNozzleTemp(nozzleTempInput.value)
  if (state.value) state.value.nozzleTargetTemp = nozzleTempInput.value
  nozzleTempInput.value = null
}

async function applyBedTemp() {
  if (bedTempInput.value == null) return
  await commands.setBedTemp(bedTempInput.value)
  if (state.value) state.value.bedTargetTemp = bedTempInput.value
  bedTempInput.value = null
}
const MOVE_STEPS = [0.1, 1, 10, 100]

// ── Files ────────────────────────────────────────────────────────────────
const { files, loading: filesLoading, uploading, uploadProgress, error: filesError, fetchFiles, uploadFile, deleteFile, downloadFile } = usePrinterFiles(printerId)
const isDragging = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)
const fileSearch = ref('')
const fileSort = ref<'name' | 'size' | 'date'>('name')
const fileSortAsc = ref(true)
const fileTypeFilter = ref<'all' | '.3mf' | '.gcode'>('all')

const filteredFiles = computed(() => {
  let result = files.value
  if (fileTypeFilter.value !== 'all')
    result = result.filter(f => f.name.toLowerCase().endsWith(fileTypeFilter.value))
  if (fileSearch.value.trim()) {
    const q = fileSearch.value.toLowerCase()
    result = result.filter(f => f.name.toLowerCase().includes(q))
  }
  return [...result].sort((a, b) => {
    let cmp = 0
    if (fileSort.value === 'name') cmp = a.name.localeCompare(b.name)
    else if (fileSort.value === 'size') cmp = a.sizeBytes - b.sizeBytes
    else cmp = new Date(a.lastModified).getTime() - new Date(b.lastModified).getTime()
    return fileSortAsc.value ? cmp : -cmp
  })
})

function setSort(key: 'name' | 'size' | 'date') {
  if (fileSort.value === key) fileSortAsc.value = !fileSortAsc.value
  else { fileSort.value = key; fileSortAsc.value = true }
}

// ── Print job info dialog ─────────────────────────────────────────────────
const showPrintInfo = ref(false)
const infoFile = ref<string | null>(null)
const infoFileSize = ref<number>(0)

// ── Print dialog ──────────────────────────────────────────────────────────
const showPrintDialog = ref(false)
const selectedFile = ref<string | null>(null)
const selectedAmsSlot = ref<number | null>(null)
const printFlowCali = ref(true)
const printVibrationCali = ref(true)
const printLayerInspect = ref(true)

function openPrintDialog(filename: string) {
  selectedFile.value = filename
  selectedAmsSlot.value = null
  showPrintDialog.value = true
}

async function confirmPrint() {
  if (!selectedFile.value) return
  try {
    await commands.printFile(selectedFile.value, selectedAmsSlot.value != null ? [selectedAmsSlot.value] : [], printFlowCali.value, printVibrationCali.value, printLayerInspect.value)
    showPrintDialog.value = false
  } catch { /* error shown via commands.error */ }
}

function fmtFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function onFileDrop(event: DragEvent) {
  isDragging.value = false
  const file = event.dataTransfer?.files?.[0]
  if (file) uploadFile(file).then(fetchFiles).catch(() => {})
}

function onFileInputChange(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (file) uploadFile(file).then(fetchFiles).catch(() => {})
  if (fileInputRef.value) fileInputRef.value.value = ''
}

function onCameraLoad() { cameraLoading.value = false }
function onCameraError() { cameraError.value = true; cameraLoading.value = false }
function retryCamera() { cameraError.value = false; cameraLoading.value = true; streamKey.value++ }

const streamUrl = computed(() => `/hls/printer/${printerId}/stream/index.m3u8`)

// ── Helpers ──────────────────────────────────────────────────────────────

function fmtTemp(val?: number): string {
  return val != null ? `${val.toFixed(0)}°C` : '—'
}

function fmtDuration(mins?: number): string {
  if (mins == null || mins <= 0) return '—'
  const h = Math.floor(mins / 60)
  const m = mins % 60
  return h > 0 ? `${h}h ${m}m` : `${m}m`
}

// ── Current print hero ───────────────────────────────────────────────────

const showPrintDetails = ref(false)

const RING_R = 52
const RING_LEN = 2 * Math.PI * RING_R

const ringOffset = computed(
  () => RING_LEN * (1 - Math.min(Math.max(progressValue.value / 100, 0), 1)),
)

const etaText = computed(() => {
  const mins = state.value?.remainTime
  if (!mins || mins <= 0) return ''
  return new Date(Date.now() + mins * 60000).toLocaleTimeString(undefined, {
    hour: 'numeric',
    minute: '2-digit',
  })
})

// ── Device info accordions ───────────────────────────────────────────────

const devOpen = ref({ connection: false, monitoring: false, firmware: false })

const xcamActiveCount = computed(() => {
  const x = state.value?.xcam
  if (!x) return 0
  return [
    x.firstLayerInspector,
    x.buildplateMarkerDetector,
    x.spaghettiDetector,
    x.printingMonitor,
    x.printHalt,
    x.allowSkipParts,
  ].filter(Boolean).length
})

// ── Temperature gauges ───────────────────────────────────────────────────

const NOZZLE_MAX = 300
const BED_MAX = 110
const GAUGE_R = 50
const GAUGE_LEN = Math.PI * GAUGE_R

function gaugeFrac(temp: number | undefined, max: number): number {
  return Math.min(Math.max((temp ?? 0) / max, 0), 1)
}

function gaugeOffset(temp: number | undefined, max: number): number {
  return GAUGE_LEN * (1 - gaugeFrac(temp, max))
}

function gaugePoint(temp: number | undefined, max: number): { x: number; y: number } {
  const theta = Math.PI * (1 - gaugeFrac(temp, max))
  return { x: 60 + GAUGE_R * Math.cos(theta), y: 60 - GAUGE_R * Math.sin(theta) }
}

function gaugeColor(temp: number | undefined, max: number): string {
  const f = gaugeFrac(temp, max)
  if (f < 0.25) return '#22d3ee'
  if (f < 0.6) return '#fbbf24'
  return '#fb7185'
}

const nozzleHist = ref<number[]>([])
const bedHist = ref<number[]>([])
const HIST_MAX = 120

watch(state, s => {
  if (s?.nozzleTemp != null) {
    nozzleHist.value.push(s.nozzleTemp)
    if (nozzleHist.value.length > HIST_MAX) nozzleHist.value.shift()
  }
  if (s?.bedTemp != null) {
    bedHist.value.push(s.bedTemp)
    if (bedHist.value.length > HIST_MAX) bedHist.value.shift()
  }
})

function sparkPoints(hist: number[]): string {
  if (hist.length < 2) return ''
  const min = Math.min(...hist)
  const max = Math.max(...hist)
  const span = Math.max(max - min, 1)
  return hist
    .map((v, i) => {
      const x = (i / (hist.length - 1)) * 100
      const y = 22 - ((v - min) / span) * 20
      return `${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')
}

const TEMP_PRESETS = [
  { label: 'PLA', nozzle: 220, bed: 55 },
  { label: 'PETG', nozzle: 240, bed: 70 },
  { label: 'ABS', nozzle: 260, bed: 90 },
  { label: 'Off', nozzle: 0, bed: 0 },
]

async function applyPreset(preset: (typeof TEMP_PRESETS)[number]) {
  await commands.setNozzleTemp(preset.nozzle)
  await commands.setBedTemp(preset.bed)
  if (state.value) {
    state.value.nozzleTargetTemp = preset.nozzle
    state.value.bedTargetTemp = preset.bed
  }
}

// ── Fans ─────────────────────────────────────────────────────────────────

function fanSpinDuration(pct: number): string {
  // 100% ≈ 0.3s per revolution, slower speeds spin proportionally slower
  return `${(2.2 - (pct / 100) * 1.9).toFixed(2)}s`
}

function fmtFanSpeed(speed?: string): number {
  if (!speed) return 0
  const n = parseInt(speed)
  // BambuLab reports raw PWM 0-15 or 0-255; normalise to 0-100
  if (n <= 15) return Math.round((n / 15) * 100)
  if (n <= 255) return Math.round((n / 255) * 100)
  return Math.min(n, 100)
}

function materialColor(color?: string): string {
  if (!color) return '#3a3a3a'
  return color.startsWith('#') ? color : `#${color}`
}

function isLightOn(light: ApiLight): boolean {
  return light.state?.toLowerCase() === 'on'
}

function toggleLight(light: ApiLight) {
  const newMode = isLightOn(light) ? 'off' : 'on'
  // Optimistic update
  if (state.value?.lights) {
    const entry = state.value.lights.find(l => l.name === light.name)
    if (entry) entry.state = newMode
  }
  commands.setLight(light.name!, newMode)
}

const statusSeverity = computed(() => {
  const s = state.value?.state?.toLowerCase() ?? ''
  if (s.includes('print')) return 'success'
  if (s === 'idle' || s === 'standby') return 'info'
  if (s === 'offline' || !state.value?.state) return 'secondary'
  return 'warn'
})

const isPrinting = computed(() => state.value?.state?.toLowerCase().includes('print') ?? false)

const isGcodeRunning = computed(() => state.value?.gcodeState?.toUpperCase() === 'RUNNING')

const progressValue = computed(() => state.value?.progress ?? 0)

const materials = computed<ApiMaterial[]>(
  () => state.value?.materialSystem?.materials ?? [],
)

const activeMaterial = computed<ApiMaterial | null>(
  () => materials.value.find(m => m.loaded) ?? null,
)

const fans = computed<ApiFan[]>(() => state.value?.fans ?? [])

const lights = computed<ApiLight[]>(() => state.value?.lights ?? [])

// ── Keyboard ──────────────────────────────────────────────────────────────

function onKeyDown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    cameraExpanded.value = false
  }
}

// ── Notifications ─────────────────────────────────────────────────────────

const notificationsStore = useNotificationsStore()

const printerNotifications = computed(() =>
  notificationsStore.sortedNotifications.filter(n => n.printerId === printerId)
)

function notifSeverityIcon(severity: NotificationSeverity | undefined) {
  switch (severity) {
    case 'ERROR':   return 'mdi mdi-alert-circle'
    case 'WARNING': return 'mdi mdi-alert'
    default:        return 'mdi mdi-information'
  }
}

function notifSeverityClass(severity: NotificationSeverity | undefined) {
  switch (severity) {
    case 'ERROR':   return 'notif-error'
    case 'WARNING': return 'notif-warning'
    default:        return 'notif-info'
  }
}

function notifDate(dateStr: string | undefined) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const mins = Math.floor((Date.now() - d.getTime()) / 60000)
  if (mins < 1) return 'just now'
  if (mins < 60) return `${mins}m ago`
  const hours = Math.floor(mins / 60)
  if (hours < 24) return `${hours}h ago`
  return d.toLocaleString(undefined, {
    month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

// ── Diagnostics ───────────────────────────────────────────────────────────

const showDiagnosticDialog = ref(false)
const diagnosticLoading = ref(false)
const diagnosticReport = ref<ApiDiagnosticReport | null>(null)

async function runDiagnostics() {
  diagnosticLoading.value = true
  diagnosticReport.value = null
  showDiagnosticDialog.value = true
  try {
    const res = await diagnosticApi.diagnosePrinter(printerId)
    diagnosticReport.value = res.data
  } catch {
    diagnosticReport.value = null
  } finally {
    diagnosticLoading.value = false
  }
}

function diagnosticSeverityClass(severity?: string): string {
  switch (severity) {
    case 'CRITICAL': return 'diag-critical'
    case 'HIGH':     return 'diag-high'
    case 'MEDIUM':   return 'diag-medium'
    case 'LOW':      return 'diag-low'
    default:         return 'diag-none'
  }
}

// ── Data loading ─────────────────────────────────────────────────────────

onMounted(async () => {
  window.addEventListener('keydown', onKeyDown)
  loading.value = true
  error.value = ''
  try {
    // Last known state from the backend cache — instant render instead of
    // waiting for the next MQTT push; live socket data overwrites it.
    printerApi
      .getPrinterState(printerId)
      .then(res => {
        if (!state.value) state.value = res.data
      })
      .catch(() => { /* no cached state yet */ })

    const res = await api.get<PrinterInfo>(`/printer/${printerId}`)
    printer.value = res.data

    connect([printerId], (_id, incoming) => {
      state.value = incoming
    })

    fetchFiles()
    notificationsStore.fetchAll()
  } catch {
    error.value = 'Failed to load printer.'
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeyDown)
})
</script>

<template>
  <div class="printer-page">

    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <Button
          icon="pi pi-arrow-left"
          severity="secondary"
          text
          size="small"
          class="back-btn"
          @click="router.back()"
        />
        <div class="header-title">
          <h1 class="printer-name">{{ printer?.printerName ?? '—' }}</h1>
          <div class="printer-meta">
            <span class="meta-chip">{{ printer?.printerModel }}</span>
            <span class="meta-chip">{{ printer?.printerType }}</span>
            <span v-if="printer?.location" class="meta-chip">
              <i class="mdi mdi-map-marker-outline" /> {{ printer.location }}
            </span>
          </div>
        </div>
      </div>
      <div class="header-right">
        <div v-if="state?.wifiSignalStrength" class="info-pill">
          <i class="mdi mdi-wifi" />
          {{ state.wifiSignalStrength }}
        </div>
        <div v-if="printer?.serialNumber" class="info-pill">
          <i class="mdi mdi-barcode" />
          {{ printer.serialNumber }}
        </div>
        <Tag
          :value="state?.state ?? 'Unknown'"
          :severity="statusSeverity"
          class="status-tag"
        />
      </div>
    </div>

    <div v-if="error" class="error-msg">{{ error }}</div>

    <!-- Waiting for first MQTT state -->
    <div v-if="!state && !error" class="skeleton-grid" aria-label="Waiting for printer data">
      <div class="skeleton-col">
        <div class="sk-card" style="height: 180px" />
        <div class="sk-card" style="height: 120px" />
        <div class="sk-card" style="height: 260px" />
      </div>
      <div class="skeleton-col">
        <div class="sk-card" style="height: 240px" />
        <div class="sk-card" style="height: 200px" />
      </div>
    </div>

    <!-- Main grid -->
    <div v-else-if="state" class="main-grid">

      <!-- Left column -->
      <div class="col-left">

        <!-- Print Progress -->
        <div class="section-card">
          <div class="section-label">
            <i class="mdi mdi-printer-3d-nozzle-outline" /> Current Print
          </div>
          <div v-if="state?.file || state?.progress != null" class="print-info">
            <div class="print-hero">
              <div class="ring-wrap">
                <svg viewBox="0 0 120 120" class="ring-svg">
                  <defs>
                    <linearGradient id="pv-ring-grad" x1="0" y1="0" x2="1" y2="1">
                      <stop offset="0%" stop-color="#22d3ee" />
                      <stop offset="100%" stop-color="#818cf8" />
                    </linearGradient>
                  </defs>
                  <circle cx="60" cy="60" :r="RING_R" class="ring-track" />
                  <circle
                    cx="60"
                    cy="60"
                    :r="RING_R"
                    class="ring-fill"
                    :class="{ 'ring-fill--active': isGcodeRunning }"
                    :stroke-dasharray="RING_LEN"
                    :stroke-dashoffset="ringOffset"
                  />
                </svg>
                <div class="ring-center">
                  <span class="ring-pct">{{ progressValue.toFixed(0) }}<span class="ring-pct-sign">%</span></span>
                  <span v-if="state?.currentLayer != null" class="ring-layer">
                    {{ state.currentLayer }} / {{ state.totalLayers }}
                  </span>
                </div>
              </div>

              <div class="print-hero-body">
                <div class="file-name" :title="state?.file">
                  <i class="mdi mdi-file-cad-box" />
                  {{ state?.file ?? '—' }}
                </div>
                <div v-if="state?.subtaskName" class="subtask-name">
                  {{ state.subtaskName }}
                </div>

                <div class="hero-stats">
                  <span class="hero-stat" title="Time remaining">
                    <i class="mdi mdi-clock-outline" />
                    {{ fmtDuration(state?.remainTime) }}
                  </span>
                  <span v-if="etaText" class="hero-stat" title="Estimated finish">
                    <i class="mdi mdi-flag-checkered" />
                    ~{{ etaText }}
                  </span>
                  <span v-if="state?.spdMag" class="hero-stat" title="Print speed">
                    <i class="mdi mdi-speedometer" />
                    {{ state.spdMag }}%
                  </span>
                  <span v-if="activeMaterial" class="hero-stat" title="Loaded material">
                    <span
                      class="active-material-dot"
                      :style="{ background: materialColor(activeMaterial.color) }"
                    />
                    {{ activeMaterial.name || activeMaterial.type || '—' }}
                  </span>
                </div>

                <button class="details-toggle" @click="showPrintDetails = !showPrintDetails">
                  <i class="mdi" :class="showPrintDetails ? 'mdi-chevron-up' : 'mdi-chevron-down'" />
                  {{ showPrintDetails ? 'Hide details' : 'Details' }}
                </button>
              </div>
            </div>

            <div v-if="showPrintDetails" class="print-details">
              <div class="detail-row">
                <span class="detail-key">Type</span>
                <span class="detail-val detail-val--cap">{{ state?.printType || '—' }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-key">GCode state</span>
                <span class="detail-val detail-val--mono">{{ state?.gcodeState || '—' }}</span>
              </div>
              <div class="conn-pills detail-pills">
                <span v-if="state?.taskId" class="env-pill mono-pill"><i class="mdi mdi-identifier" /> {{ state.taskId }}</span>
                <span v-if="state?.jobId" class="env-pill mono-pill"><i class="mdi mdi-briefcase-outline" /> {{ state.jobId }}</span>
                <span v-if="state?.projectId" class="env-pill mono-pill"><i class="mdi mdi-folder-outline" /> {{ state.projectId }}</span>
              </div>
            </div>

            <div v-if="state?.hmsErrors?.length" class="hms-errors">
              <div v-for="(msg, i) in state.hmsErrors" :key="i" class="ctrl-error error-row">
                <i class="mdi mdi-alert-circle-outline" />
                <span class="error-row-msg">{{ msg }}</span>
                <Button
                  v-if="i === 0"
                  icon="pi pi-microchip-ai"
                  label="Diagnose"
                  severity="secondary"
                  size="small"
                  class="error-row-btn"
                  @click="runDiagnostics"
                />
              </div>
            </div>
            <div
              v-else-if="state?.failReason || state?.printErrorDescription || (state?.printError != null && state.printError !== 0)"
              class="ctrl-error error-row error-row--single"
            >
              <i class="mdi mdi-alert-circle-outline" />
              <span class="error-row-msg">
                <span v-if="state.failReason">{{ state.failReason }}</span>
                <span v-else-if="state.printErrorDescription">{{ state.printErrorDescription }}</span>
                <span v-else>Error 0x{{ state.printError!.toString(16).toUpperCase() }}<span v-if="state.mcPrintErrorCode"> · {{ state.mcPrintErrorCode }}</span></span>
              </span>
              <Button
                icon="pi pi-microchip-ai"
                label="Diagnose"
                severity="secondary"
                size="small"
                class="error-row-btn"
                @click="runDiagnostics"
              />
            </div>
          </div>
          <div v-else class="empty-state">
            <i class="mdi mdi-sleep" /> Printer is idle
          </div>
        </div>

        <!-- Print Controls -->
        <div class="section-card">
          <div class="section-label">
            <i class="mdi mdi-gamepad-variant-outline" /> Print Controls
          </div>
          <div v-if="commands.error.value" class="ctrl-error">
            <i class="mdi mdi-alert-circle-outline" /> {{ commands.error.value }}
          </div>
          <div class="ctrl-actions">
            <button class="ctrl-btn ctrl-btn--danger" :disabled="commands.loading.value" @click="commands.stopPrint()">
              <i class="mdi mdi-stop" />
              <span>Stop</span>
            </button>
            <button class="ctrl-btn ctrl-btn--warn" :disabled="commands.loading.value" @click="commands.pausePrint()">
              <i class="mdi mdi-pause" />
              <span>Pause</span>
            </button>
            <button class="ctrl-btn ctrl-btn--success" :disabled="commands.loading.value" @click="commands.resumePrint()">
              <i class="mdi mdi-play" />
              <span>Resume</span>
            </button>
            <button class="ctrl-btn" :disabled="commands.loading.value || isGcodeRunning" @click="homingGuard.home()">
              <i class="mdi mdi-home-outline" />
              <span>Home</span>
            </button>
          </div>
        </div>

        <!-- AMS -->
        <div class="section-card">
          <div class="section-label">
            <i class="mdi mdi-palette-swatch-outline" /> Material System (AMS)
          </div>
          <AmsVisual
            v-if="materials.length"
            :materials="materials"
            :humidity="state?.materialSystem?.humidity"
            :ams-temp="state?.materialSystem?.temperature"
            :nozzle-temp="state?.nozzleTemp"
            :nozzle-target-temp="state?.nozzleTargetTemp"
            :printing="isPrinting"
          />
          <div v-else class="empty-state">
            <i class="mdi mdi-tray-remove" />
            No material data
          </div>
        </div>

        <!-- Files -->
        <div class="section-card">
          <div class="section-label">
            <i class="mdi mdi-folder-outline" /> Files
            <span v-if="files.length" class="file-count-badge">{{ files.length }}</span>
            <button class="ctrl-icon-btn" style="margin-left:auto" title="Refresh" @click="fetchFiles">
              <i class="mdi mdi-refresh" :class="{ 'mdi-spin': filesLoading }" />
            </button>
          </div>

          <div v-if="filesError" class="ctrl-error">
            <i class="mdi mdi-alert-circle-outline" /> {{ filesError }}
          </div>

          <!-- Search + upload toolbar -->
          <div class="file-toolbar">
            <input
              v-model="fileSearch"
              type="text"
              class="file-search-input"
              placeholder="Search files…"
            />
            <button class="ctrl-btn" style="flex-shrink:0" @click="fileInputRef?.click()">
              <i class="mdi mdi-upload" />
              Upload
            </button>
          </div>
          <input ref="fileInputRef" type="file" accept=".gcode,.3mf" style="display:none" @change="onFileInputChange" />

          <!-- Filter + sort row -->
          <div class="file-controls">
            <div class="file-type-chips">
              <button class="file-chip" :class="{ 'file-chip--active': fileTypeFilter === 'all' }" @click="fileTypeFilter = 'all'">All</button>
              <button class="file-chip" :class="{ 'file-chip--active': fileTypeFilter === '.3mf' }" @click="fileTypeFilter = '.3mf'">.3mf</button>
              <button class="file-chip" :class="{ 'file-chip--active': fileTypeFilter === '.gcode' }" @click="fileTypeFilter = '.gcode'">.gcode</button>
            </div>
            <div class="file-sort-chips">
              <button class="file-chip" :class="{ 'file-chip--active': fileSort === 'name' }" @click="setSort('name')">
                Name<i v-if="fileSort === 'name'" class="mdi" :class="fileSortAsc ? 'mdi-arrow-up' : 'mdi-arrow-down'" />
              </button>
              <button class="file-chip" :class="{ 'file-chip--active': fileSort === 'size' }" @click="setSort('size')">
                Size<i v-if="fileSort === 'size'" class="mdi" :class="fileSortAsc ? 'mdi-arrow-up' : 'mdi-arrow-down'" />
              </button>
              <button class="file-chip" :class="{ 'file-chip--active': fileSort === 'date' }" @click="setSort('date')">
                Date<i v-if="fileSort === 'date'" class="mdi" :class="fileSortAsc ? 'mdi-arrow-up' : 'mdi-arrow-down'" />
              </button>
            </div>
          </div>

          <!-- Upload progress -->
          <div v-if="uploading" class="upload-progress">
            <span class="upload-progress-label">Uploading… {{ uploadProgress }}%</span>
            <div class="upload-bar"><div class="upload-bar-fill" :style="{ width: uploadProgress + '%' }" /></div>
          </div>

          <!-- Compact drop zone -->
          <div
            class="file-dropzone file-dropzone--compact"
            :class="{ 'file-dropzone--active': isDragging }"
            @dragover.prevent="isDragging = true"
            @dragleave="isDragging = false"
            @drop.prevent="onFileDrop"
          >
            <i class="mdi mdi-cloud-upload-outline" />
            <span>Drop .3mf or .gcode here</span>
          </div>

          <!-- File list -->
          <div class="file-list file-list--scrollable">
            <div v-if="filesLoading && !files.length" class="empty-state">
              <span class="page-spinner" style="width:18px;height:18px;border-width:2px" /> Loading files…
            </div>
            <div v-else-if="!files.length" class="empty-state">
              <i class="mdi mdi-folder-open-outline" /> No files on printer
            </div>
            <div v-else-if="fileSearch && !filteredFiles.length" class="empty-state">
              <i class="mdi mdi-magnify-close" /> No files matching "{{ fileSearch }}"
            </div>
            <div v-for="file in filteredFiles" :key="file.name" class="file-row">
              <i class="mdi mdi-file-cad-box file-row-icon" />
              <div class="file-row-info">
                <span class="file-row-name">{{ file.name }}</span>
                <span class="file-row-meta">{{ fmtFileSize(file.sizeBytes) }}</span>
              </div>
              <div class="file-row-actions">
                <button
                  v-if="file.name.toLowerCase().endsWith('.3mf')"
                  class="ctrl-icon-btn"
                  title="View job info"
                  @click="infoFile = file.name; infoFileSize = file.sizeBytes; showPrintInfo = true"
                >
                  <i class="mdi mdi-information-outline" />
                </button>
                <button class="ctrl-icon-btn" title="Download" @click="downloadFile(file.name).catch(() => {})">
                  <i class="mdi mdi-download" />
                </button>
                <button class="ctrl-icon-btn" title="Print this file" @click="openPrintDialog(file.name)">
                  <i class="mdi mdi-play-circle-outline" />
                </button>
                <button class="ctrl-icon-btn ctrl-icon-btn--danger" title="Delete" @click="deleteFile(file.name).then(fetchFiles).catch(() => {})">
                  <i class="mdi mdi-trash-can-outline" />
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Device Info -->
        <div class="section-card">
          <div class="section-label"><i class="mdi mdi-information-outline" /> Device Info</div>
          <div class="dev-groups">

            <div class="dev-group">
              <button class="dev-group-head" @click="devOpen.connection = !devOpen.connection">
                <i class="mdi mdi-lan-connect dev-group-icon" />
                <span class="dev-group-title">Connection</span>
                <span class="dev-group-summary">{{ state?.ipAddress ?? printer?.connectionConfig?.connectionType ?? '' }}</span>
                <i class="mdi mdi-chevron-down dev-chevron" :class="{ 'dev-chevron--open': devOpen.connection }" />
              </button>
              <div v-if="devOpen.connection" class="dev-group-body">
                <div class="conn-pills">
                  <span v-if="state?.ipAddress" class="env-pill"><i class="mdi mdi-ip-network-outline" /> {{ state.ipAddress }}</span>
                  <span v-if="state?.wifiSignalStrength" class="env-pill"><i class="mdi mdi-wifi" /> {{ state.wifiSignalStrength }}</span>
                  <span v-if="printer?.connectionConfig?.connectionType" class="env-pill"><i class="mdi mdi-connection" /> {{ printer.connectionConfig.connectionType }}</span>
                </div>
                <div class="conn-mono-rows">
                  <div v-if="printer?.connectionConfig?.brokerUrl" class="conn-mono-row">
                    <span class="conn-mono-label">Broker</span>
                    <span class="conn-mono-val">{{ printer.connectionConfig.brokerUrl }}</span>
                  </div>
                  <div v-if="printer?.connectionConfig?.topic" class="conn-mono-row">
                    <span class="conn-mono-label">Topic</span>
                    <span class="conn-mono-val">{{ printer.connectionConfig.topic }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="dev-group">
              <button class="dev-group-head" @click="devOpen.monitoring = !devOpen.monitoring">
                <i class="mdi mdi-eye-outline dev-group-icon" />
                <span class="dev-group-title">AI Monitoring</span>
                <span class="dev-group-summary">{{ state?.xcam ? `${xcamActiveCount} active` : '' }}</span>
                <i class="mdi mdi-chevron-down dev-chevron" :class="{ 'dev-chevron--open': devOpen.monitoring }" />
              </button>
              <div v-if="devOpen.monitoring" class="dev-group-body">
                <div v-if="state?.xcam" class="xcam-list">
                  <div v-for="feat in [
                    { label: 'First Layer Inspector',    active: state.xcam.firstLayerInspector },
                    { label: 'Build Plate Marker',       active: state.xcam.buildplateMarkerDetector },
                    { label: 'Spaghetti Detector',       active: state.xcam.spaghettiDetector },
                    { label: 'Printing Monitor',         active: state.xcam.printingMonitor },
                    { label: 'Halt on Failure',          active: state.xcam.printHalt },
                    { label: 'Allow Skip Parts',         active: state.xcam.allowSkipParts },
                  ]" :key="feat.label" class="xcam-row">
                    <span class="xcam-dot" :class="feat.active ? 'xcam-dot--on' : ''" />
                    <span class="xcam-label">{{ feat.label }}</span>
                    <span class="xcam-state" :class="feat.active ? 'xcam-state--on' : ''">{{ feat.active ? 'On' : 'Off' }}</span>
                  </div>
                  <div v-if="state.xcam.haltPrintSensitivity" class="xcam-row">
                    <span class="xcam-dot xcam-dot--neutral" />
                    <span class="xcam-label">Halt Sensitivity</span>
                    <span class="xcam-state xcam-state--cap">{{ state.xcam.haltPrintSensitivity }}</span>
                  </div>
                </div>
                <div v-else class="empty-state"><i class="mdi mdi-eye-off-outline" /> No AI monitoring data</div>
              </div>
            </div>

            <div class="dev-group">
              <button class="dev-group-head" @click="devOpen.firmware = !devOpen.firmware">
                <i class="mdi mdi-update dev-group-icon" />
                <span class="dev-group-title">Firmware</span>
                <span class="dev-group-summary">{{ state?.upgradeState?.status ?? '' }}</span>
                <i class="mdi mdi-chevron-down dev-chevron" :class="{ 'dev-chevron--open': devOpen.firmware }" />
              </button>
              <div v-if="devOpen.firmware" class="dev-group-body">
                <div v-if="state?.upgradeState" class="fw-grid">
                  <div v-if="state.upgradeState.otaNewVersionNumber" class="fw-row">
                    <span class="fw-component">OTA</span>
                    <span class="fw-version">{{ state.upgradeState.otaNewVersionNumber }}</span>
                  </div>
                  <div v-if="state.upgradeState.amsNewVersionNumber" class="fw-row">
                    <span class="fw-component">AMS</span>
                    <span class="fw-version">{{ state.upgradeState.amsNewVersionNumber }}</span>
                  </div>
                  <div v-if="state.upgradeState.ahbNewVersionNumber" class="fw-row">
                    <span class="fw-component">AHB</span>
                    <span class="fw-version">{{ state.upgradeState.ahbNewVersionNumber }}</span>
                  </div>
                  <div v-if="state.upgradeState.extNewVersionNumber" class="fw-row">
                    <span class="fw-component">EXT</span>
                    <span class="fw-version">{{ state.upgradeState.extNewVersionNumber }}</span>
                  </div>
                </div>
                <div v-if="state?.upgradeState?.status" class="conn-pills fw-status-pills">
                  <span class="env-pill" :class="{ 'env-pill--warn': state.upgradeState.forceUpgrade }">
                    <i class="mdi" :class="state.upgradeState.forceUpgrade ? 'mdi-alert-outline' : 'mdi-check-circle-outline'" />
                    {{ state.upgradeState.status }}
                    <span v-if="state.upgradeState.progress && state.upgradeState.progress !== '0'"> · {{ state.upgradeState.progress }}%</span>
                  </span>
                </div>
                <div v-if="!state?.upgradeState" class="empty-state"><i class="mdi mdi-update" /> No firmware data</div>
              </div>
            </div>

          </div>
        </div>

      </div>

      <!-- Right column -->
      <div class="col-right">

        <!-- Camera -->
        <div class="section-card camera-card">
          <div class="section-label">
            <i class="mdi mdi-cctv" /> Live Camera
            <button class="camera-expand-btn" @click="cameraExpanded = true" title="Expand">
              <i class="mdi mdi-fullscreen" />
            </button>
          </div>
          <div class="camera-viewport">
            <div v-if="cameraLoading && !cameraError" class="camera-loading">
              <span class="camera-spinner" />
              <span class="camera-loading-text">Connecting…</span>
            </div>
            <CameraPlayer
              v-if="!cameraError"
              v-show="!cameraLoading"
              :key="streamKey"
              :src="streamUrl"
              class="camera-feed"
              @ready="onCameraLoad"
              @error="onCameraError"
            />
            <div v-if="cameraError" class="camera-placeholder">
              <i class="mdi mdi-cctv-off camera-icon" />
              <span class="camera-text">Camera unavailable</span>
              <Button
                label="Retry"
                icon="pi pi-refresh"
                severity="secondary"
                size="small"
                text
                class="camera-retry"
                @click="retryCamera"
              />
            </div>
          </div>
        </div>

        <!-- Movement -->
        <div class="section-card">
          <div class="section-label">
            <i class="mdi mdi-axis-arrow" /> Movement
          </div>
          <div class="move-step-row">
            <span class="ctrl-speed-label">Step</span>
            <div class="ctrl-speed-chips">
              <button
                v-for="s in MOVE_STEPS" :key="s"
                class="speed-chip"
                :class="{ 'speed-chip--active': moveStep === s }"
                @click="moveStep = s"
              >{{ s }}mm</button>
            </div>
          </div>
          <div class="move-grid">
            <div class="move-xy">
              <div class="jog-cross">
                <button class="jog-btn jog-top"    :disabled="commands.loading.value || isPrinting || isGcodeRunning" @click="homingGuard.requestJog('Y', -moveStep)"><i class="mdi mdi-arrow-up" /></button>
                <button class="jog-btn jog-left"   :disabled="commands.loading.value || isPrinting || isGcodeRunning" @click="homingGuard.requestJog('X', -moveStep)"><i class="mdi mdi-arrow-left" /></button>
                <button class="jog-btn jog-home"   :disabled="commands.loading.value || isPrinting || isGcodeRunning" @click="homingGuard.home()"><i class="mdi mdi-home-outline" /></button>
                <button class="jog-btn jog-right"  :disabled="commands.loading.value || isPrinting || isGcodeRunning" @click="homingGuard.requestJog('X',  moveStep)"><i class="mdi mdi-arrow-right" /></button>
                <button class="jog-btn jog-bottom" :disabled="commands.loading.value || isPrinting || isGcodeRunning" @click="homingGuard.requestJog('Y',  moveStep)"><i class="mdi mdi-arrow-down" /></button>
              </div>
              <span class="move-axis-label">X / Y</span>
            </div>
            <div class="move-z">
              <button class="jog-btn" :disabled="commands.loading.value || isPrinting || isGcodeRunning" @click="homingGuard.requestJog('Z', -moveStep)"><i class="mdi mdi-arrow-up" /></button>
              <button class="jog-btn jog-home" :disabled="commands.loading.value || isPrinting || isGcodeRunning" @click="homingGuard.home()"><i class="mdi mdi-home-outline" /></button>
              <button class="jog-btn" :disabled="commands.loading.value || isPrinting || isGcodeRunning" @click="homingGuard.requestJog('Z',  moveStep)"><i class="mdi mdi-arrow-down" /></button>
              <span class="move-axis-label">Z</span>
            </div>
          </div>
        </div>

        <!-- Temperatures -->
        <div class="section-card">
          <div class="section-label">
            <i class="mdi mdi-thermometer-lines" /> Temperatures
          </div>
          <div class="temp-grid">
            <div class="temp-card">
              <div class="temp-head">
                <i class="mdi mdi-printer-3d-nozzle-heat" />
                <span class="temp-label">Nozzle</span>
                <span class="temp-target" title="Target">→ {{ fmtTemp(state?.nozzleTargetTemp) }}</span>
              </div>
              <div class="temp-gauge">
                <svg viewBox="0 0 120 66" class="gauge-svg">
                  <path d="M 10 60 A 50 50 0 0 1 110 60" class="gauge-track" />
                  <path
                    d="M 10 60 A 50 50 0 0 1 110 60"
                    class="gauge-fill"
                    :stroke="gaugeColor(state?.nozzleTemp, NOZZLE_MAX)"
                    :stroke-dasharray="GAUGE_LEN"
                    :stroke-dashoffset="gaugeOffset(state?.nozzleTemp, NOZZLE_MAX)"
                  />
                  <circle
                    v-if="state?.nozzleTargetTemp"
                    :cx="gaugePoint(state.nozzleTargetTemp, NOZZLE_MAX).x"
                    :cy="gaugePoint(state.nozzleTargetTemp, NOZZLE_MAX).y"
                    r="3"
                    class="gauge-target-dot"
                  />
                </svg>
                <span class="gauge-value">{{ fmtTemp(state?.nozzleTemp) }}</span>
              </div>
              <div v-if="state?.nozzleType || state?.nozzleDiameter" class="temp-meta">
                <span v-if="state?.nozzleType">{{ state.nozzleType }}</span>
                <span v-if="state?.nozzleDiameter">Ø{{ state.nozzleDiameter }}mm</span>
              </div>
              <svg v-if="nozzleHist.length > 1" viewBox="0 0 100 24" class="temp-spark" preserveAspectRatio="none">
                <polyline :points="sparkPoints(nozzleHist)" />
              </svg>
              <div class="temp-set-row">
                <input
                  v-model.number="nozzleTempInput"
                  type="number"
                  class="temp-input"
                  placeholder="°C"
                  min="0"
                  max="300"
                  :disabled="commands.loading.value"
                  @keydown.enter="applyNozzleTemp"
                />
                <button
                  class="temp-set-btn"
                  :disabled="commands.loading.value || nozzleTempInput == null"
                  @click="applyNozzleTemp"
                >Set</button>
              </div>
            </div>

            <div class="temp-card">
              <div class="temp-head">
                <i class="mdi mdi-heating-coil" />
                <span class="temp-label">Bed</span>
                <span class="temp-target" title="Target">→ {{ fmtTemp(state?.bedTargetTemp) }}</span>
              </div>
              <div class="temp-gauge">
                <svg viewBox="0 0 120 66" class="gauge-svg">
                  <path d="M 10 60 A 50 50 0 0 1 110 60" class="gauge-track" />
                  <path
                    d="M 10 60 A 50 50 0 0 1 110 60"
                    class="gauge-fill"
                    :stroke="gaugeColor(state?.bedTemp, BED_MAX)"
                    :stroke-dasharray="GAUGE_LEN"
                    :stroke-dashoffset="gaugeOffset(state?.bedTemp, BED_MAX)"
                  />
                  <circle
                    v-if="state?.bedTargetTemp"
                    :cx="gaugePoint(state.bedTargetTemp, BED_MAX).x"
                    :cy="gaugePoint(state.bedTargetTemp, BED_MAX).y"
                    r="3"
                    class="gauge-target-dot"
                  />
                </svg>
                <span class="gauge-value">{{ fmtTemp(state?.bedTemp) }}</span>
              </div>
              <svg v-if="bedHist.length > 1" viewBox="0 0 100 24" class="temp-spark" preserveAspectRatio="none">
                <polyline :points="sparkPoints(bedHist)" />
              </svg>
              <div class="temp-set-row">
                <input
                  v-model.number="bedTempInput"
                  type="number"
                  class="temp-input"
                  placeholder="°C"
                  min="0"
                  max="110"
                  :disabled="commands.loading.value"
                  @keydown.enter="applyBedTemp"
                />
                <button
                  class="temp-set-btn"
                  :disabled="commands.loading.value || bedTempInput == null"
                  @click="applyBedTemp"
                >Set</button>
              </div>
            </div>
          </div>

          <div class="temp-presets">
            <span class="temp-presets-label">Presets</span>
            <button
              v-for="p in TEMP_PRESETS"
              :key="p.label"
              class="temp-preset-chip"
              :disabled="commands.loading.value"
              :title="`Nozzle ${p.nozzle}°C · Bed ${p.bed}°C`"
              @click="applyPreset(p)"
            >{{ p.label }}</button>
          </div>
        </div>

        <!-- Fans -->
        <div class="section-card">
          <div class="section-label">
            <i class="mdi mdi-fan" /> Fans
          </div>
          <div v-if="fans.length" class="fans-list">
            <div v-for="fan in fans" :key="fan.name" class="fan-row">
              <i
                class="mdi mdi-fan fan-icon"
                :class="{ 'fan-icon--on': fmtFanSpeed(fan.speed) > 0 }"
                :style="fmtFanSpeed(fan.speed) > 0 ? { animationDuration: fanSpinDuration(fmtFanSpeed(fan.speed)) } : undefined"
              />
              <span class="fan-name">{{ fan.name }}</span>
              <div class="fan-bar-wrap">
                <div class="fan-bar">
                  <div
                    class="fan-bar-fill"
                    :style="{ width: `${fmtFanSpeed(fan.speed)}%` }"
                  />
                </div>
                <span class="fan-pct">{{ fmtFanSpeed(fan.speed) }}%</span>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">
            <i class="mdi mdi-fan-off" /> No fan data
          </div>
        </div>

        <!-- Lights -->
        <div class="section-card">
          <div class="section-label">
            <i class="mdi mdi-led-strip-variant" /> Lights
          </div>
          <div v-if="lights.length" class="lights-list">
            <div v-for="light in lights" :key="light.name" class="light-row">
              <div class="light-dot" :class="{ 'light-dot--on': isLightOn(light) }" />
              <span class="light-name">{{ light.name }}</span>
              <button
                class="light-toggle"
                :class="{ 'light-toggle--on': isLightOn(light) }"
                :disabled="commands.loading.value"
                @click="toggleLight(light)"
              >
                <i :class="isLightOn(light) ? 'mdi mdi-lightbulb' : 'mdi mdi-lightbulb-off-outline'" />
                {{ isLightOn(light) ? 'On' : 'Off' }}
              </button>
            </div>
          </div>
          <div v-else class="empty-state">
            <i class="mdi mdi-lightbulb-off-outline" /> No light data
          </div>
        </div>

        <!-- Notifications -->
        <div class="section-card pv-notif-card">
          <div class="section-label">
            <i class="mdi mdi-bell-outline" /> Notifications
            <span v-if="notificationsStore.sortedNotifications.filter(n => n.printerId === printerId && !n.acknowledged).length" class="pv-notif-badge">
              {{ notificationsStore.sortedNotifications.filter(n => n.printerId === printerId && !n.acknowledged).length }}
            </span>
            <div class="pv-notif-actions">
              <button
                class="pv-notif-action-btn"
                title="Acknowledge all"
                :disabled="printerNotifications.every(n => n.acknowledged)"
                @click="printerNotifications.filter(n => !n.acknowledged).forEach(n => notificationsStore.acknowledge(n.id!))"
              >
                <i class="mdi mdi-check-all" />
              </button>
              <button
                class="pv-notif-action-btn"
                title="Clear acknowledged"
                :disabled="printerNotifications.every(n => !n.acknowledged)"
                @click="notificationsStore.clearAcknowledged()"
              >
                <i class="mdi mdi-delete-sweep-outline" />
              </button>
            </div>
          </div>

          <div v-if="printerNotifications.length === 0" class="empty-state">
            <i class="mdi mdi-bell-off-outline" /> No notifications
          </div>
          <ul v-else class="pv-notif-list">
            <li
              v-for="n in printerNotifications"
              :key="n.id"
              class="pv-notif-item"
              :class="{ 'pv-notif-item--unread': !n.acknowledged }"
            >
              <i :class="[notifSeverityIcon(n.severity), 'pv-notif-icon', notifSeverityClass(n.severity)]" />
              <div class="pv-notif-body">
                <div class="pv-notif-title">{{ n.title }}</div>
                <div class="pv-notif-message">{{ n.message }}</div>
                <div class="pv-notif-time">{{ notifDate(n.createdAt) }}</div>
              </div>
              <div class="pv-notif-row-actions">
                <button
                  v-if="!n.acknowledged"
                  class="pv-notif-btn"
                  title="Acknowledge"
                  @click="notificationsStore.acknowledge(n.id!)"
                >
                  <i class="mdi mdi-check" />
                </button>
                <button
                  class="pv-notif-btn pv-notif-btn--delete"
                  title="Delete"
                  @click="notificationsStore.remove(n.id!)"
                >
                  <i class="mdi mdi-close" />
                </button>
              </div>
            </li>
          </ul>
        </div>

      </div>
    </div>

    <!-- Print confirmation dialog -->
    <Dialog
      v-model:visible="showPrintDialog"
      modal
      header="Print File"
      :style="{ width: 'min(420px, 90vw)' }"
      :draggable="false"
    >
      <div class="dialog-filename">{{ selectedFile }}</div>

      <div v-if="materials.length" class="dialog-section-label" style="margin-top:1rem">Select AMS Slot</div>
      <div v-if="materials.length" class="ams-selector" style="margin-top:0.5rem">
        <button
          v-for="(mat, i) in materials"
          :key="i"
          class="ams-slot-btn"
          :class="{ 'ams-slot-btn--selected': selectedAmsSlot === i }"
          @click="selectedAmsSlot = selectedAmsSlot === i ? null : i"
        >
          <div class="ams-slot-color" :style="{ background: materialColor(mat.color) }" />
          <span class="ams-slot-name">{{ mat.name ?? `Slot ${i + 1}` }}</span>
        </button>
      </div>

      <div class="dialog-section-label" style="margin-top:1rem">Calibration</div>
      <div class="print-toggles">
        <label class="print-toggle">
          <span class="print-toggle-label">Flow Calibration</span>
          <input type="checkbox" v-model="printFlowCali" class="print-toggle-input" />
          <span class="print-toggle-track" />
        </label>
        <label class="print-toggle">
          <span class="print-toggle-label">Vibration Calibration</span>
          <input type="checkbox" v-model="printVibrationCali" class="print-toggle-input" />
          <span class="print-toggle-track" />
        </label>
        <label class="print-toggle">
          <span class="print-toggle-label">Layer Inspection</span>
          <input type="checkbox" v-model="printLayerInspect" class="print-toggle-input" />
          <span class="print-toggle-track" />
        </label>
      </div>

      <div v-if="commands.error.value" class="ctrl-error" style="margin-top:0.75rem">
        <i class="mdi mdi-alert-circle-outline" /> {{ commands.error.value }}
      </div>

      <template #footer>
        <Button label="Cancel" severity="secondary" text @click="showPrintDialog = false" />
        <Button
          label="Print"
          icon="pi pi-play"
          severity="success"
          :loading="commands.loading.value"
          @click="confirmPrint()"
        />
      </template>
    </Dialog>

    <!-- Homing guard — never homed -->
    <Dialog
      v-model:visible="homingGuard.showNeverHomedDialog.value"
      modal
      header="Printer Not Homed"
      :style="{ width: 'min(400px, 90vw)' }"
      :draggable="false"
      :closable="false"
    >
      <p class="dialog-body-text">
        This printer has not been homed yet. Moving without homing first may cause positioning errors.
      </p>
      <template #footer>
        <Button label="Cancel" severity="secondary" text @click="homingGuard.cancelJog()" />
        <Button label="Continue Anyway" severity="secondary" outlined @click="homingGuard.continueWithoutHoming()" />
        <Button
          label="Home & Move"
          icon="pi pi-home"
          severity="success"
          :loading="commands.loading.value"
          @click="homingGuard.homeAndContinue()"
        />
      </template>
    </Dialog>

    <!-- Homing guard — stale home -->
    <Dialog
      v-model:visible="homingGuard.showStaleDialog.value"
      modal
      header="Re-Home Recommended"
      :style="{ width: 'min(400px, 90vw)' }"
      :draggable="false"
      :closable="false"
    >
      <p class="dialog-body-text">
        The printer was last homed {{ homingGuard.minutesSinceHome.value }} minute{{ homingGuard.minutesSinceHome.value === 1 ? '' : 's' }} ago.
        It is recommended to home before moving.
      </p>
      <template #footer>
        <Button label="Cancel" severity="secondary" text @click="homingGuard.cancelJog()" />
        <Button label="Continue Anyway" severity="secondary" outlined @click="homingGuard.continueWithoutHoming()" />
        <Button
          label="Home & Move"
          icon="pi pi-home"
          severity="success"
          :loading="commands.loading.value"
          @click="homingGuard.homeAndContinue()"
        />
      </template>
    </Dialog>

    <!-- Print job info dialog -->
    <Teleport to="body">
      <Transition name="pji-dialog">
        <PrintJobInfoDialog
          v-if="showPrintInfo && infoFile"
          :printer-id="printerId"
          :filename="infoFile"
          :file-size="infoFileSize"
          @close="showPrintInfo = false; infoFile = null"
        />
      </Transition>
    </Teleport>

    <!-- Camera fullscreen overlay -->
    <Teleport to="body">
      <div v-if="cameraExpanded" class="camera-overlay" @click.self="cameraExpanded = false">
        <div class="camera-overlay-box">
          <div class="camera-overlay-header">
            <span class="camera-overlay-title"><i class="mdi mdi-cctv" /> Live Camera</span>
            <button class="camera-overlay-close" @click="cameraExpanded = false">
              <i class="mdi mdi-close" />
            </button>
          </div>
          <div class="camera-overlay-media">
            <div v-if="cameraLoading && !cameraError" class="camera-loading camera-loading--lg">
              <span class="camera-spinner camera-spinner--lg" />
              <span class="camera-loading-text">Connecting…</span>
            </div>
            <CameraPlayer
              v-if="!cameraError"
              v-show="!cameraLoading"
              :key="streamKey"
              :src="streamUrl"
              class="camera-overlay-feed"
              @ready="onCameraLoad"
              @error="onCameraError"
            />
            <div v-if="cameraError" class="camera-overlay-unavailable">
              <i class="mdi mdi-cctv-off" />
              <span>Camera unavailable</span>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Diagnostic Dialog -->
    <Dialog
      v-model:visible="showDiagnosticDialog"
      header="AI Diagnostic Report"
      :modal="true"
      :dismissable-mask="true"
      :style="{ width: '560px' }"
      class="diagnostic-dialog"
    >
      <div v-if="diagnosticLoading" class="diag-loading">
        <span class="page-spinner" />
        <span>Analyzing printer state…</span>
      </div>

      <div v-else-if="diagnosticReport" class="diag-report">
        <!-- Healthy banner -->
        <div v-if="diagnosticReport.healthy" class="diag-healthy">
          <i class="mdi mdi-check-circle" />
          <span>No issues detected — printer looks healthy.</span>
        </div>

        <!-- Issue summary -->
        <div v-else>
          <div :class="['diag-severity-badge', diagnosticSeverityClass(diagnosticReport.severity)]">
            <i class="mdi mdi-alert-circle-outline" />
            {{ diagnosticReport.severity }}
          </div>

          <div v-if="diagnosticReport.issue" class="diag-section">
            <div class="diag-label">Issue</div>
            <div class="diag-value">{{ diagnosticReport.issue }}</div>
          </div>

          <div v-if="diagnosticReport.likelyCause" class="diag-section">
            <div class="diag-label">Likely Cause</div>
            <div class="diag-value">{{ diagnosticReport.likelyCause }}</div>
          </div>

          <div v-if="diagnosticReport.troubleshootingSteps?.length" class="diag-section">
            <div class="diag-label">Troubleshooting Steps</div>
            <ol class="diag-steps">
              <li v-for="(step, i) in diagnosticReport.troubleshootingSteps" :key="i">
                {{ step }}
              </li>
            </ol>
          </div>

          <div v-if="diagnosticReport.watchFor" class="diag-section">
            <div class="diag-label">Watch For</div>
            <div class="diag-value diag-watch">
              <i class="mdi mdi-eye-outline" /> {{ diagnosticReport.watchFor }}
            </div>
          </div>
        </div>
      </div>

      <div v-else class="diag-error">
        Failed to run diagnostic. Please try again.
      </div>

      <template #footer>
        <Button label="Close" severity="secondary" @click="showDiagnosticDialog = false" />
        <Button label="Re-run" icon="pi pi-refresh" @click="runDiagnostics" :loading="diagnosticLoading" />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
/* ── Page shell ─────────────────────────────────────────────────────── */
.printer-page {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  height: 100%;
}

/* ── Header ─────────────────────────────────────────────────────────── */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.back-btn {
  flex-shrink: 0;
}

.printer-name {
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--ph-text);
  margin: 0 0 0.25rem;
  line-height: 1.2;
}

.printer-meta {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.meta-chip {
  font-size: 0.75rem;
  padding: 0.15rem 0.6rem;
  border-radius: 999px;
  background: rgba(255,255,255,0.07);
  color: var(--ph-text-muted);
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  flex-wrap: wrap;
}

.info-pill {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8rem;
  color: var(--ph-text-muted);
  padding: 0.25rem 0.75rem;
  border-radius: 999px;
  border: 1px solid var(--ph-border);
  background: rgba(255,255,255,0.03);
}

.info-pill i {
  font-size: 0.75rem;
}

.status-tag {
  font-size: 0.8rem;
}

/* ── Main grid ──────────────────────────────────────────────────────── */
.main-grid {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 1.25rem;
  flex: 1;
  min-height: 0;
  align-items: start;
}

@media (max-width: 960px) {
  .main-grid {
    grid-template-columns: 1fr;
  }
}

.col-left,
.col-right {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

/* ── Section card ───────────────────────────────────────────────────── */
.section-card {
  background: var(--ph-glass);
  backdrop-filter: blur(var(--ph-blur));
  -webkit-backdrop-filter: blur(var(--ph-blur));
  border: 1px solid var(--ph-glass-border);
  border-radius: 16px;
  padding: 1.25rem;
  box-shadow: var(--ph-shadow-card), 0 1px 0 rgba(255, 255, 255, 0.04) inset;
  animation: ph-fade-up 0.35s cubic-bezier(0.16, 1, 0.3, 1) both;
  transition: border-color 0.25s, transform 0.25s;
}

.section-card:hover {
  border-color: var(--ph-border-strong);
  transform: translateY(-1px);
}

.section-card:hover {
  border-color: rgba(34, 211, 238, 0.18);
}

/* staggered entrance per column */
.col-left .section-card:nth-child(2),
.col-right .section-card:nth-child(2) { animation-delay: 0.06s; }
.col-left .section-card:nth-child(3),
.col-right .section-card:nth-child(3) { animation-delay: 0.12s; }
.col-left .section-card:nth-child(4),
.col-right .section-card:nth-child(4) { animation-delay: 0.18s; }
.col-left .section-card:nth-child(5),
.col-right .section-card:nth-child(5) { animation-delay: 0.24s; }
.col-left .section-card:nth-child(6),
.col-right .section-card:nth-child(6) { animation-delay: 0.3s; }

.section-label {
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--ph-text-muted);
  margin-bottom: 1rem;
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

/* ── Camera ─────────────────────────────────────────────────────────── */
.camera-expand-btn {
  margin-left: auto;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--ph-text-muted);
  padding: 0.1rem 0.25rem;
  border-radius: 4px;
  display: flex;
  align-items: center;
  font-size: 1rem;
  transition: color 0.15s, background 0.15s;
}

.camera-expand-btn:hover {
  color: var(--ph-text);
  background: rgba(255,255,255,0.07);
}

.camera-viewport {
  aspect-ratio: 16 / 9;
  background: #0a1a21;
  border-radius: 8px;
  border: 1px solid var(--ph-border);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.camera-feed {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.camera-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  text-align: center;
}

.camera-icon {
  font-size: 2rem;
  color: var(--ph-text-muted);
  opacity: 0.4;
}

.camera-text {
  font-size: 0.8rem;
  color: var(--ph-text-muted);
}

.camera-retry {
  margin-top: 0.25rem;
}

/* ── Page loading ────────────────────────────────────────────────────── */
.page-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255,255,255,0.1);
  border-top-color: var(--ph-accent, #38bdf8);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 1.25rem;
  flex: 1;
  align-items: start;
}

@media (max-width: 960px) {
  .skeleton-grid {
    grid-template-columns: 1fr;
  }
}

.skeleton-col {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.sk-card {
  border-radius: 16px;
  border: 1px solid var(--ph-glass-border);
  background: linear-gradient(
    100deg,
    rgba(148, 210, 230, 0.04) 30%,
    rgba(148, 210, 230, 0.09) 50%,
    rgba(148, 210, 230, 0.04) 70%
  );
  background-size: 250% 100%;
  animation: sk-shimmer 1.6s ease-in-out infinite;
}

@keyframes sk-shimmer {
  0% { background-position: 100% 0; }
  100% { background-position: -100% 0; }
}

/* ── Camera loading ──────────────────────────────────────────────────── */
.camera-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  width: 100%;
  height: 100%;
  position: absolute;
  inset: 0;
}

.camera-loading--lg {
  gap: 0.75rem;
}

.camera-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid rgba(255,255,255,0.12);
  border-top-color: var(--ph-accent, #38bdf8);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}

.camera-spinner--lg {
  width: 36px;
  height: 36px;
  border-width: 3px;
}

.camera-loading-text {
  font-size: 0.75rem;
  color: var(--ph-text-muted);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ── Camera overlay ──────────────────────────────────────────────────── */
.camera-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.8);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
}

.camera-overlay-box {
  background: rgba(10, 26, 33, 0.98);
  border: 1px solid var(--ph-border);
  border-radius: 12px;
  overflow: hidden;
  width: 100%;
  max-width: 960px;
  display: flex;
  flex-direction: column;
}

.camera-overlay-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--ph-border);
}

.camera-overlay-title {
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--ph-text-muted);
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.camera-overlay-close {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--ph-text-muted);
  padding: 0.25rem;
  border-radius: 4px;
  font-size: 1.1rem;
  display: flex;
  align-items: center;
  transition: color 0.15s, background 0.15s;
}

.camera-overlay-close:hover {
  color: var(--ph-text);
  background: rgba(255,255,255,0.07);
}

.camera-overlay-media {
  position: relative;
  aspect-ratio: 16 / 9;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.camera-overlay-feed {
  width: 100%;
  aspect-ratio: 16 / 9;
  object-fit: contain;
  display: block;
  background: #000;
}

.camera-overlay-unavailable {
  aspect-ratio: 16 / 9;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  color: var(--ph-text-muted);
  font-size: 0.875rem;
  background: #0a1a21;
}

.camera-overlay-unavailable i {
  font-size: 2rem;
  opacity: 0.4;
}

/* ── AMS ─────────────────────────────────────────────────────────────── */
/* Visual AMS diagram lives in AmsVisual.vue */

.env-pill {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.75rem;
  color: var(--ph-text-muted);
  padding: 0.2rem 0.6rem;
  border-radius: 999px;
  border: 1px solid var(--ph-border);
  background: rgba(255,255,255,0.03);
}

/* ── Print progress ─────────────────────────────────────────────────── */

.file-name {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--ph-text);
  margin-bottom: 0.25rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-name i {
  color: var(--ph-accent);
  flex-shrink: 0;
}

.subtask-name {
  font-size: 0.78rem;
  color: var(--ph-text-muted);
  margin-bottom: 0.75rem;
}

/* ── Print hero (progress ring) ─────────────────────────────────────── */
.print-hero {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}

.ring-wrap {
  position: relative;
  width: 128px;
  height: 128px;
  flex-shrink: 0;
}

.ring-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.ring-track {
  fill: none;
  stroke: rgba(148, 210, 230, 0.1);
  stroke-width: 8;
}

.ring-fill {
  fill: none;
  stroke: url(#pv-ring-grad);
  stroke-width: 8;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.8s cubic-bezier(0.16, 1, 0.3, 1);
}

.ring-fill--active {
  filter: drop-shadow(0 0 6px rgba(34, 211, 238, 0.45));
}

.ring-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.1rem;
}

.ring-pct {
  font-size: 1.6rem;
  font-weight: 700;
  color: var(--ph-text);
  line-height: 1;
}

.ring-pct-sign {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--ph-text-muted);
}

.ring-layer {
  font-size: 0.7rem;
  color: var(--ph-text-muted);
}

.print-hero-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.hero-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem 0.5rem;
  margin-top: 0.25rem;
}

.hero-stat {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--ph-text);
  padding: 0.25rem 0.6rem;
  border-radius: 999px;
  border: 1px solid var(--ph-border);
  background: rgba(255, 255, 255, 0.03);
}

.hero-stat i {
  color: var(--ph-text-muted);
  font-size: 0.85rem;
}

.details-toggle {
  align-self: flex-start;
  display: flex;
  align-items: center;
  gap: 0.2rem;
  margin-top: 0.35rem;
  padding: 0.15rem 0.4rem 0.15rem 0.2rem;
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--ph-text-muted);
  background: none;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: color 0.15s;
}

.details-toggle:hover {
  color: var(--ph-text);
}

.print-details {
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px solid var(--ph-border);
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.detail-row {
  display: flex;
  align-items: baseline;
  gap: 0.75rem;
}

.detail-key {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--ph-text-muted);
  min-width: 90px;
}

.detail-val {
  font-size: 0.82rem;
  color: var(--ph-text);
}

.detail-val--cap {
  text-transform: capitalize;
}

.detail-val--mono {
  font-family: monospace;
  font-size: 0.78rem;
}

.detail-pills {
  margin-top: 0.25rem;
}

.error-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.75rem;
}

.hms-errors .error-row {
  margin-top: 0;
  margin-bottom: 0.4rem;
}

.hms-errors {
  margin-top: 0.75rem;
}

.error-row-msg {
  flex: 1;
}

.error-row-btn {
  margin-left: auto;
  flex-shrink: 0;
}

.active-material-dot {
  width: 0.9rem;
  height: 0.9rem;
  border-radius: 50%;
  border: 1.5px solid rgba(255,255,255,0.2);
  flex-shrink: 0;
}

/* ── Temperatures ───────────────────────────────────────────────────── */
.temp-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.temp-card {
  padding: 0.875rem;
  border-radius: 10px;
  border: 1px solid var(--ph-border);
  background: rgba(255,255,255,0.02);
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  transition: border-color 0.2s;
}

.temp-head {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.temp-head i {
  font-size: 1rem;
  color: var(--ph-text-muted);
}

.temp-label {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--ph-text-muted);
  font-weight: 600;
}

.temp-target {
  font-size: 0.78rem;
  color: var(--ph-text-muted);
  margin-left: auto;
}

.temp-gauge {
  position: relative;
  margin: 0.25rem auto 0;
  width: 120px;
}

.gauge-svg {
  width: 100%;
  display: block;
  overflow: visible;
}

.gauge-track {
  fill: none;
  stroke: rgba(148, 210, 230, 0.1);
  stroke-width: 7;
  stroke-linecap: round;
}

.gauge-fill {
  fill: none;
  stroke-width: 7;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.8s cubic-bezier(0.16, 1, 0.3, 1), stroke 0.4s;
}

.gauge-target-dot {
  fill: var(--ph-text);
  stroke: var(--ph-bg-mid);
  stroke-width: 1.5;
}

.gauge-value {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 2px;
  text-align: center;
  font-size: 1.15rem;
  font-weight: 700;
  color: var(--ph-text);
  line-height: 1;
}

.temp-spark {
  width: 100%;
  height: 24px;
  opacity: 0.75;
}

.temp-spark polyline {
  fill: none;
  stroke: var(--ph-accent);
  stroke-width: 1.5;
  stroke-linejoin: round;
  vector-effect: non-scaling-stroke;
}

.temp-presets {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  margin-top: 0.75rem;
  flex-wrap: wrap;
}

.temp-presets-label {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--ph-text-muted);
  margin-right: 0.25rem;
}

.temp-preset-chip {
  padding: 0.25rem 0.7rem;
  border-radius: 999px;
  border: 1px solid var(--ph-border);
  background: rgba(255, 255, 255, 0.03);
  color: var(--ph-text);
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.15s, background 0.15s;
}

.temp-preset-chip:hover:not(:disabled) {
  border-color: rgba(34, 211, 238, 0.4);
  background: var(--ph-accent-dim);
}

.temp-preset-chip:disabled {
  opacity: 0.5;
  cursor: default;
}

.temp-meta {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
  margin-top: 0.25rem;
}

.temp-meta span {
  font-size: 0.7rem;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
  background: rgba(255,255,255,0.06);
  color: var(--ph-text-muted);
}

.temp-set-row {
  display: flex;
  gap: 0.375rem;
  margin-top: 0.625rem;
}

.temp-input {
  flex: 1;
  min-width: 0;
  padding: 0.25rem 0.5rem;
  border-radius: 6px;
  border: 1px solid var(--ph-border);
  background: rgba(255,255,255,0.04);
  color: var(--ph-text);
  font-size: 0.8rem;
  outline: none;
  transition: border-color 0.15s;
}

.temp-input:focus {
  border-color: var(--ph-accent, #38bdf8);
}

.temp-input:disabled {
  opacity: 0.45;
}

.temp-input::-webkit-inner-spin-button,
.temp-input::-webkit-outer-spin-button {
  opacity: 0.4;
}

.temp-set-btn {
  padding: 0.25rem 0.625rem;
  border-radius: 6px;
  border: 1px solid rgba(56,189,248,0.35);
  background: rgba(56,189,248,0.08);
  color: var(--ph-accent, #38bdf8);
  font-size: 0.75rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s;
  white-space: nowrap;
  flex-shrink: 0;
}

.temp-set-btn:not(:disabled):hover {
  background: rgba(56,189,248,0.16);
}

.temp-set-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* ── Fans ───────────────────────────────────────────────────────────── */
.fans-list {
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
}

.fan-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.fan-icon {
  font-size: 1.15rem;
  color: rgba(255, 255, 255, 0.18);
  flex-shrink: 0;
  transition: color 0.3s;
}

.fan-icon--on {
  color: var(--ph-accent);
  animation: fan-spin linear infinite;
}

@keyframes fan-spin {
  to {
    transform: rotate(360deg);
  }
}

.fan-name {
  font-size: 0.8rem;
  color: var(--ph-text-muted);
  min-width: 110px;
  flex-shrink: 0;
}

.fan-bar-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.fan-bar {
  flex: 1;
  height: 5px;
  border-radius: 3px;
  background: rgba(255,255,255,0.08);
  overflow: hidden;
}

.fan-bar-fill {
  height: 100%;
  border-radius: 3px;
  background: var(--ph-accent);
  transition: width 0.4s ease;
}

.fan-pct {
  font-size: 0.75rem;
  color: var(--ph-text-muted);
  min-width: 2.5rem;
  text-align: right;
}

/* ── Lights ─────────────────────────────────────────────────────────── */
.lights-list {
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
}

.light-row {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}

.light-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255,255,255,0.15);
  flex-shrink: 0;
  transition: background 0.2s;
}

.light-dot--on {
  background: #4ade80;
  box-shadow: 0 0 6px rgba(74, 222, 128, 0.6);
}

.light-name {
  flex: 1;
  font-size: 0.8rem;
  color: var(--ph-text);
  text-transform: capitalize;
}

.light-state {
  font-size: 0.75rem;
  color: var(--ph-text-muted);
  text-transform: capitalize;
}

.light-state.on {
  color: #4ade80;
}

.light-toggle {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  padding: 0.25rem 0.625rem;
  border-radius: 6px;
  border: 1px solid var(--ph-border);
  background: rgba(255,255,255,0.04);
  color: var(--ph-text-muted);
  font-size: 0.75rem;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}

.light-toggle:not(:disabled):hover {
  background: rgba(255,255,255,0.08);
}

.light-toggle--on {
  border-color: rgba(250,204,21,0.4);
  color: #facc15;
  background: rgba(250,204,21,0.08);
}

.light-toggle--on:not(:disabled):hover {
  background: rgba(250,204,21,0.14);
}

.light-toggle:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

/* ── Printer notifications ───────────────────────────────────────────── */
.pv-notif-card .section-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.pv-notif-badge {
  min-width: 1.1rem;
  height: 1.1rem;
  padding: 0 4px;
  background: #f87171;
  color: #fff;
  border-radius: 999px;
  font-size: 0.6rem;
  font-weight: 700;
  line-height: 1.1rem;
  text-align: center;
}

.pv-notif-actions {
  margin-left: auto;
  display: flex;
  gap: 0.25rem;
}

.pv-notif-action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.75rem;
  height: 1.75rem;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--ph-text-muted);
  cursor: pointer;
  font-size: 0.9rem;
  transition: background 0.15s, color 0.15s;
}
.pv-notif-action-btn:hover:not(:disabled) {
  background: rgba(255,255,255,0.08);
  color: var(--ph-text);
}
.pv-notif-action-btn:disabled { opacity: 0.3; cursor: not-allowed; }

.pv-notif-list {
  list-style: none;
  margin: 0.5rem 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.pv-notif-item {
  display: flex;
  align-items: flex-start;
  gap: 0.625rem;
  padding: 0.625rem 0.75rem;
  border-radius: 8px;
  background: rgba(255,255,255,0.02);
  border: 1px solid var(--ph-border);
  transition: background 0.1s;
}

.pv-notif-item--unread {
  background: rgba(34,211,238,0.04);
  border-color: rgba(34,211,238,0.15);
}

.pv-notif-icon {
  font-size: 1rem;
  margin-top: 0.1rem;
  flex-shrink: 0;
}
.notif-error   { color: #f87171; }
.notif-warning { color: #fbbf24; }
.notif-info    { color: #22d3ee; }

.pv-notif-body {
  flex: 1;
  min-width: 0;
}

.pv-notif-title {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ph-text);
  line-height: 1.3;
}

.pv-notif-message {
  font-size: 0.75rem;
  color: var(--ph-text-muted);
  margin-top: 0.1rem;
  line-height: 1.4;
}

.pv-notif-time {
  font-size: 0.68rem;
  color: var(--ph-text-muted);
  opacity: 0.6;
  margin-top: 0.25rem;
}

.pv-notif-row-actions {
  display: flex;
  gap: 0.125rem;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.15s;
}
.pv-notif-item:hover .pv-notif-row-actions { opacity: 1; }

.pv-notif-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.5rem;
  height: 1.5rem;
  border: none;
  border-radius: 5px;
  background: transparent;
  color: var(--ph-text-muted);
  cursor: pointer;
  font-size: 0.8rem;
  transition: background 0.15s, color 0.15s;
}
.pv-notif-btn:hover {
  background: rgba(255,255,255,0.08);
  color: var(--ph-text);
}
.pv-notif-btn--delete:hover {
  background: rgba(248,113,113,0.15);
  color: #f87171;
}

/* ── Shared ─────────────────────────────────────────────────────────── */
.empty-state {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  color: var(--ph-text-muted);
  opacity: 0.7;
}

.error-msg {
  color: #f87171;
  font-size: 0.875rem;
}

/* ── Print controls ──────────────────────────────────────────────────── */
.ctrl-actions {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-bottom: 1rem;
}

.ctrl-error {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.4rem 0.75rem;
  margin-bottom: 0.75rem;
  border-radius: 6px;
  background: rgba(248,113,113,0.1);
  border: 1px solid rgba(248,113,113,0.3);
  color: #f87171;
  font-size: 0.78rem;
}

.ctrl-btn {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.45rem 0.875rem;
  border-radius: 8px;
  border: 1px solid var(--ph-border);
  background: rgba(255,255,255,0.04);
  color: var(--ph-text);
  font-size: 0.8rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}

.ctrl-btn:not(:disabled):hover { background: rgba(255,255,255,0.09); }
.ctrl-btn:disabled { opacity: 0.45; cursor: not-allowed; }

.ctrl-btn--danger { border-color: rgba(248,113,113,0.35); color: #f87171; }
.ctrl-btn--danger:not(:disabled):hover { background: rgba(248,113,113,0.12); }

.ctrl-btn--warn { border-color: rgba(251,191,36,0.35); color: #fbbf24; }
.ctrl-btn--warn:not(:disabled):hover { background: rgba(251,191,36,0.10); }

.ctrl-btn--success { border-color: rgba(74,222,128,0.35); color: #4ade80; }
.ctrl-btn--success:not(:disabled):hover { background: rgba(74,222,128,0.10); }

.ctrl-btn i { font-size: 1rem; }

.ctrl-icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: 1px solid var(--ph-border);
  background: rgba(255,255,255,0.04);
  color: var(--ph-text-muted);
  font-size: 0.95rem;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  flex-shrink: 0;
}

.ctrl-icon-btn:not(:disabled):hover { background: rgba(255,255,255,0.09); color: var(--ph-text); }
.ctrl-icon-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.ctrl-icon-btn--danger:not(:disabled):hover { background: rgba(248,113,113,0.12); color: #f87171; }

.ctrl-speed {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.ctrl-speed-label {
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--ph-text-muted);
  flex-shrink: 0;
}

.ctrl-speed-chips {
  display: flex;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.speed-chip {
  padding: 0.2rem 0.6rem;
  border-radius: 6px;
  border: 1px solid var(--ph-border);
  background: rgba(255,255,255,0.03);
  color: var(--ph-text-muted);
  font-size: 0.75rem;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
}

.speed-chip:not(:disabled):hover { background: rgba(255,255,255,0.08); color: var(--ph-text); }
.speed-chip:disabled { cursor: not-allowed; }
.speed-chip--active {
  border-color: var(--ph-accent, #38bdf8);
  color: var(--ph-accent, #38bdf8);
  background: rgba(56,189,248,0.08);
}

/* ── File manager ────────────────────────────────────────────────────── */
.file-count-badge {
  font-size: 0.65rem;
  font-weight: 600;
  padding: 0.1rem 0.45rem;
  border-radius: 999px;
  background: rgba(56,189,248,0.12);
  color: var(--ph-accent, #38bdf8);
  border: 1px solid rgba(56,189,248,0.2);
  line-height: 1.5;
}

.file-toolbar {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 0.625rem;
}

.file-search-input {
  flex: 1;
  min-width: 0;
  padding: 0.3rem 0.625rem;
  border-radius: 6px;
  border: 1px solid var(--ph-border);
  background: rgba(255,255,255,0.04);
  color: var(--ph-text);
  font-size: 0.8rem;
  outline: none;
  transition: border-color 0.15s;
}

.file-search-input:focus {
  border-color: var(--ph-accent, #38bdf8);
}

.file-search-input::placeholder {
  color: var(--ph-text-muted);
  opacity: 0.6;
}

.file-dropzone {
  border: 1.5px dashed var(--ph-border);
  border-radius: 10px;
  padding: 1.25rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.35rem;
  margin-bottom: 0.75rem;
  transition: border-color 0.15s, background 0.15s;
  cursor: default;
}

.file-dropzone--compact {
  flex-direction: row;
  justify-content: center;
  padding: 0.45rem 0.75rem;
  gap: 0.4rem;
  font-size: 0.78rem;
  color: var(--ph-text-muted);
}

.file-dropzone--compact i {
  font-size: 0.95rem;
  opacity: 0.5;
}

.file-dropzone--active {
  border-color: var(--ph-accent, #38bdf8);
  background: rgba(56,189,248,0.05);
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.file-controls {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.625rem;
  flex-wrap: wrap;
}

.file-type-chips,
.file-sort-chips {
  display: flex;
  gap: 0.3rem;
}

.file-chip {
  display: flex;
  align-items: center;
  gap: 0.2rem;
  padding: 0.15rem 0.5rem;
  border-radius: 5px;
  border: 1px solid var(--ph-border);
  background: rgba(255,255,255,0.03);
  color: var(--ph-text-muted);
  font-size: 0.72rem;
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
  white-space: nowrap;
}

.file-chip:hover {
  background: rgba(255,255,255,0.07);
  color: var(--ph-text);
}

.file-chip--active {
  border-color: rgba(56,189,248,0.4);
  color: var(--ph-accent, #38bdf8);
  background: rgba(56,189,248,0.08);
}

.file-chip i {
  font-size: 0.65rem;
}

.file-list--scrollable {
  max-height: 320px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(255,255,255,0.1) transparent;
  padding-right: 2px;
}

.file-row {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.5rem 0.625rem;
  border-radius: 8px;
  border: 1px solid var(--ph-border);
  background: rgba(255,255,255,0.02);
  transition: background 0.15s;
}

.file-row:hover { background: rgba(255,255,255,0.05); }

.file-row-icon {
  font-size: 1.1rem;
  color: var(--ph-accent, #38bdf8);
  flex-shrink: 0;
  opacity: 0.8;
}

.file-row-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.file-row-name {
  font-size: 0.82rem;
  color: var(--ph-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.file-row-meta {
  font-size: 0.7rem;
  color: var(--ph-text-muted);
}

.file-row-actions {
  display: flex;
  gap: 0.3rem;
  flex-shrink: 0;
}

/* ── Upload progress ─────────────────────────────────────────────────── */
.upload-progress {
  margin-bottom: 0.75rem;
}

.upload-progress-label {
  font-size: 0.75rem;
  color: var(--ph-text-muted);
  display: block;
  margin-bottom: 0.3rem;
}

.upload-bar {
  height: 4px;
  border-radius: 2px;
  background: rgba(255,255,255,0.1);
  overflow: hidden;
}

.upload-bar-fill {
  height: 100%;
  background: var(--ph-accent, #38bdf8);
  border-radius: 2px;
  transition: width 0.2s;
}

/* ── Print dialog ────────────────────────────────────────────────────── */
.dialog-filename {
  font-size: 0.82rem;
  color: var(--ph-text-muted);
  background: rgba(255,255,255,0.04);
  border: 1px solid var(--ph-border);
  border-radius: 6px;
  padding: 0.5rem 0.75rem;
  word-break: break-all;
}

.dialog-section-label {
  font-size: 0.72rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  color: var(--ph-text-muted);
}

.ams-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.ams-slot-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.4rem 0.75rem;
  border-radius: 8px;
  border: 1px solid var(--ph-border);
  background: rgba(255,255,255,0.03);
  color: var(--ph-text);
  font-size: 0.8rem;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}

.ams-slot-btn:hover { background: rgba(255,255,255,0.07); }

.ams-slot-btn--selected {
  border-color: rgba(56,189,248,0.5);
  background: rgba(56,189,248,0.1);
}

.ams-slot-color {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  flex-shrink: 0;
  border: 1px solid rgba(255,255,255,0.15);
}

.ams-slot-name {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ── Print calibration toggles ───────────────────────────────────────── */
.print-toggles {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-top: 0.5rem;
}

.print-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  padding: 0.4rem 0.625rem;
  border-radius: 7px;
  border: 1px solid var(--ph-border);
  background: rgba(255,255,255,0.03);
}

.print-toggle-label {
  font-size: 0.82rem;
  color: var(--ph-text);
}

.print-toggle-input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.print-toggle-track {
  position: relative;
  width: 36px;
  height: 20px;
  border-radius: 999px;
  background: rgba(255,255,255,0.12);
  flex-shrink: 0;
  transition: background 0.2s;
}

.print-toggle-track::after {
  content: '';
  position: absolute;
  top: 3px;
  left: 3px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.2s;
}

.print-toggle-input:checked + .print-toggle-track {
  background: var(--ph-accent, #22d3ee);
}

.print-toggle-input:checked + .print-toggle-track::after {
  transform: translateX(16px);
}

.dialog-body-text {
  font-size: 0.85rem;
  color: var(--ph-text-muted);
  line-height: 1.5;
  margin: 0;
}

.dialog-actions {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
}

/* ── Movement controls ───────────────────────────────────────────────── */
.move-step-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.move-grid {
  display: flex;
  gap: 1.5rem;
  align-items: flex-start;
}

.move-xy, .move-z {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.375rem;
}

.move-axis-label {
  font-size: 0.68rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--ph-text-muted);
  margin-top: 0.25rem;
}

.jog-cross {
  display: grid;
  grid-template-columns: repeat(3, 36px);
  grid-template-rows: repeat(3, 36px);
  gap: 3px;
}

.jog-btn {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid var(--ph-border);
  background: rgba(255,255,255,0.04);
  color: var(--ph-text);
  font-size: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.15s;
}

.jog-btn:not(:disabled):hover { background: rgba(255,255,255,0.1); }
.jog-btn:disabled { opacity: 0.45; cursor: not-allowed; }
.jog-btn.jog-home { color: var(--ph-accent, #38bdf8); border-color: rgba(56,189,248,0.3); }

.jog-top    { grid-column: 2; grid-row: 1; }
.jog-left   { grid-column: 1; grid-row: 2; }
.jog-home   { grid-column: 2; grid-row: 2; }
.jog-right  { grid-column: 3; grid-row: 2; }
.jog-bottom { grid-column: 2; grid-row: 3; }

/* ── Device info accordions ─────────────────────────────────────────── */
.dev-groups {
  display: flex;
  flex-direction: column;
}

.dev-group + .dev-group {
  border-top: 1px solid var(--ph-border);
}

.dev-group-head {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 0.25rem;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--ph-text);
  font-size: 0.82rem;
  font-weight: 600;
  text-align: left;
  transition: color 0.15s;
}

.dev-group-head:hover {
  color: var(--ph-accent);
}

.dev-group-icon {
  font-size: 0.95rem;
  color: var(--ph-text-muted);
}

.dev-group-title {
  flex-shrink: 0;
}

.dev-group-summary {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: right;
  font-size: 0.72rem;
  font-weight: 500;
  color: var(--ph-text-muted);
}

.dev-chevron {
  font-size: 1rem;
  color: var(--ph-text-muted);
  transition: transform 0.2s;
  flex-shrink: 0;
}

.dev-chevron--open {
  transform: rotate(180deg);
}

.dev-group-body {
  padding: 0.25rem 0.25rem 0.85rem;
}

.xcam-state--cap {
  text-transform: capitalize;
}

.fw-status-pills {
  margin-top: 0.5rem;
}

.conn-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
  margin-bottom: 0.75rem;
}

.conn-pills:empty { display: none; }

.mono-pill {
  font-family: monospace;
  font-size: 0.7rem !important;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.env-pill--warn {
  border-color: rgba(251,191,36,0.35);
  color: #fbbf24;
  background: rgba(251,191,36,0.08);
}

.conn-mono-rows {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}

.conn-mono-row {
  display: flex;
  gap: 0.5rem;
  align-items: baseline;
  font-size: 0.75rem;
}

.conn-mono-label {
  color: var(--ph-text-muted);
  flex-shrink: 0;
  width: 3rem;
}

.conn-mono-val {
  font-family: monospace;
  color: var(--ph-text);
  word-break: break-all;
  font-size: 0.7rem;
}

/* xcam feature list */
.xcam-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.xcam-row {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}

.xcam-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: rgba(255,255,255,0.15);
  flex-shrink: 0;
}

.xcam-dot--on {
  background: #4ade80;
  box-shadow: 0 0 5px rgba(74,222,128,0.5);
}

.xcam-dot--neutral {
  background: var(--ph-accent);
}

.xcam-label {
  flex: 1;
  font-size: 0.8rem;
  color: var(--ph-text);
}

.xcam-state {
  font-size: 0.72rem;
  color: var(--ph-text-muted);
}

.xcam-state--on {
  color: #4ade80;
}

/* firmware version grid */
.fw-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.375rem;
}

.fw-row {
  display: flex;
  flex-direction: column;
  padding: 0.4rem 0.625rem;
  border-radius: 7px;
  border: 1px solid var(--ph-border);
  background: rgba(255,255,255,0.02);
}

.fw-component {
  font-size: 0.65rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  color: var(--ph-text-muted);
}

.fw-version {
  font-size: 0.78rem;
  font-family: monospace;
  color: var(--ph-text);
  margin-top: 0.1rem;
}



.mat-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
  border: 1px solid rgba(255, 255, 255, 0.15);
}

/* ── Diagnostic dialog ───────────────────────────────────────────────── */
.diag-loading {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1.5rem 0;
  color: var(--ph-text-muted, #aaa);
}

.diag-report {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.diag-healthy {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem;
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.3);
  border-radius: 8px;
  color: #4ade80;
  font-weight: 500;
}

.diag-severity-badge {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.3rem 0.75rem;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  margin-bottom: 1rem;
}

.diag-critical { background: rgba(239,68,68,0.15); color: #f87171; border: 1px solid rgba(239,68,68,0.3); }
.diag-high     { background: rgba(249,115,22,0.15); color: #fb923c; border: 1px solid rgba(249,115,22,0.3); }
.diag-medium   { background: rgba(234,179,8,0.15);  color: #facc15; border: 1px solid rgba(234,179,8,0.3); }
.diag-low      { background: rgba(59,130,246,0.15); color: #60a5fa; border: 1px solid rgba(59,130,246,0.3); }
.diag-none     { background: rgba(107,114,128,0.15); color: #9ca3af; border: 1px solid rgba(107,114,128,0.3); }

.diag-section {
  margin-bottom: 1rem;
}

.diag-label {
  font-size: 0.72rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--ph-text-muted, #888);
  margin-bottom: 0.3rem;
}

.diag-value {
  font-size: 0.9rem;
  color: var(--ph-text, #e2e8f0);
  line-height: 1.5;
}

.diag-steps {
  margin: 0;
  padding-left: 1.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.diag-steps li {
  font-size: 0.88rem;
  color: var(--ph-text, #e2e8f0);
  line-height: 1.5;
}

.diag-watch {
  display: flex;
  align-items: flex-start;
  gap: 0.4rem;
  color: var(--ph-text-muted, #aaa);
  font-style: italic;
}

.diag-error {
  color: #f87171;
  padding: 1rem 0;
}
</style>
