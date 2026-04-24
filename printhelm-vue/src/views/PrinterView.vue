<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import CameraPlayer from '@/components/CameraPlayer.vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import Dialog from 'primevue/dialog'
import Tag from 'primevue/tag'
import ProgressBar from 'primevue/progressbar'
import { api, BASE_URL } from '@/api/Configuration'
import { usePrinterSocket } from '@/composables/usePrinterSocket'
import { usePrinterCommands } from '@/composables/usePrinterCommands'
import { usePrinterFiles } from '@/composables/usePrinterFiles'
import { useHomingGuard } from '@/composables/useHomingGuard'
import PrintJobInfoDialog from '@/components/PrintJobInfoDialog.vue'
import type { ApiPrinterState, ApiFan, ApiLight, ApiMaterial, ApiIpcam, ApiXcam, ApiUpgradeState } from '@/client/printhelm-web-openapi'
import { useNotificationsStore } from '@/stores/notifications'
import type { ApiNotification, NotificationSeverity } from '@/service/NotificationService'

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
const { files, loading: filesLoading, uploading, uploadProgress, error: filesError, fetchFiles, uploadFile, deleteFile } = usePrinterFiles(printerId)
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
  return new Date(dateStr).toLocaleString(undefined, {
    month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

// ── Data loading ─────────────────────────────────────────────────────────

onMounted(async () => {
  window.addEventListener('keydown', onKeyDown)
  loading.value = true
  error.value = ''
  try {
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
    <div v-if="!state && !error" class="page-loading">
      <span class="page-spinner" />
      <span class="page-loading-text">Waiting for printer data…</span>
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
            <div class="file-name" :title="state?.file">
              <i class="mdi mdi-file-cad-box" />
              {{ state?.file ?? '—' }}
            </div>
            <div v-if="state?.subtaskName" class="subtask-name">
              {{ state.subtaskName }}
            </div>

            <div class="progress-row">
              <ProgressBar
                :value="progressValue"
                :show-value="false"
                class="print-progress"
              />
              <span class="progress-pct">{{ progressValue.toFixed(0) }}%</span>
            </div>

            <div class="print-stats">
              <div class="stat-item">
                <span class="stat-key">Layer</span>
                <span class="stat-val">
                  {{ state?.currentLayer != null ? `${state.currentLayer} / ${state.totalLayers}` : '—' }}
                </span>
              </div>
              <div class="stat-item">
                <span class="stat-key">Remaining</span>
                <span class="stat-val">{{ state?.remainTime ? `${state.remainTime}m` : '—' }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-key">Speed</span>
                <span class="stat-val">{{ state?.spdMag ? `${state.spdMag}%` : '—' }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-key">Type</span>
                <span class="stat-val" style="font-size:0.85rem;text-transform:capitalize">{{ state?.printType || '—' }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-key">GCode</span>
                <span class="stat-val" style="font-size:0.78rem;font-family:monospace">{{ state?.gcodeState || '—' }}</span>
              </div>
              <div v-if="activeMaterial" class="stat-item">
                <span class="stat-key">Material</span>
                <span class="stat-val stat-val--material">
                  <span
                    class="active-material-dot"
                    :style="{ background: materialColor(activeMaterial.color) }"
                  />
                  {{ activeMaterial.name || activeMaterial.type || '—' }}
                </span>
              </div>
            </div>

            <div v-if="(state?.printError != null && state.printError !== 0) || state?.failReason" class="ctrl-error" style="margin-top:0.75rem">
              <i class="mdi mdi-alert-circle-outline" />
              <span v-if="state.failReason">{{ state.failReason }}</span>
              <span v-else>Error 0x{{ state.printError!.toString(16).toUpperCase() }}<span v-if="state.mcPrintErrorCode"> · {{ state.mcPrintErrorCode }}</span></span>
            </div>

            <div class="conn-pills" style="margin-top:0.75rem;margin-bottom:0">
              <span v-if="state?.taskId" class="env-pill mono-pill"><i class="mdi mdi-identifier" /> {{ state.taskId }}</span>
              <span v-if="state?.jobId" class="env-pill mono-pill"><i class="mdi mdi-briefcase-outline" /> {{ state.jobId }}</span>
              <span v-if="state?.projectId" class="env-pill mono-pill"><i class="mdi mdi-folder-outline" /> {{ state.projectId }}</span>
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
          <div v-if="materials.length" class="ams-grid">
            <div
              v-for="(mat, i) in materials"
              :key="i"
              class="ams-slot"
              :class="{ 'ams-slot--loaded': mat.loaded }"
            >
              <div
                class="ams-color"
                :style="{ background: materialColor(mat.color) }"
              >
                <i v-if="mat.loaded" class="mdi mdi-check ams-loaded-icon" />
              </div>
              <div class="ams-info">
                <span class="ams-name">{{ mat.name ?? 'Empty' }}</span>
                <span class="ams-slot-label">Slot {{ i + 1 }}</span>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">
            <i class="mdi mdi-tray-remove" />
            No material data
          </div>

          <div v-if="state?.materialSystem?.temperature || state?.materialSystem?.humidity" class="ams-env">
            <span v-if="state.materialSystem?.temperature" class="env-pill">
              <i class="mdi mdi-thermometer" /> {{ state.materialSystem.temperature }}°C
            </span>
            <span v-if="state.materialSystem?.humidity" class="env-pill">
              <i class="mdi mdi-water-percent" /> {{ state.materialSystem.humidity }}% RH
            </span>
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

        <!-- Connection -->
        <div class="section-card">
          <div class="section-label"><i class="mdi mdi-lan-connect" /> Connection</div>
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

        <!-- AI Monitoring & Firmware -->
        <div class="section-card">
          <div class="section-label"><i class="mdi mdi-eye-outline" /> AI Monitoring</div>
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
              <span class="xcam-state" style="text-transform:capitalize">{{ state.xcam.haltPrintSensitivity }}</span>
            </div>
          </div>
          <div v-else class="empty-state"><i class="mdi mdi-eye-off-outline" /> No AI monitoring data</div>

          <div class="section-sublabel" style="margin-top:1rem"><i class="mdi mdi-update" /> Firmware</div>
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
          <div v-if="state?.upgradeState?.status" class="conn-pills" style="margin-top:0.5rem">
            <span class="env-pill" :class="{ 'env-pill--warn': state.upgradeState.forceUpgrade }">
              <i class="mdi" :class="state.upgradeState.forceUpgrade ? 'mdi-alert-outline' : 'mdi-check-circle-outline'" />
              {{ state.upgradeState.status }}
              <span v-if="state.upgradeState.progress && state.upgradeState.progress !== '0'"> · {{ state.upgradeState.progress }}%</span>
            </span>
          </div>
          <div v-if="!state?.upgradeState" class="empty-state"><i class="mdi mdi-update" /> No firmware data</div>
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
            <div class="temp-card" :class="{ 'temp-card--hot': (state?.nozzleTemp ?? 0) > 100 }">
              <div class="temp-icon-wrap">
                <i class="mdi mdi-printer-3d-nozzle-heat" />
              </div>
              <div class="temp-body">
                <span class="temp-label">Nozzle</span>
                <span class="temp-current">{{ fmtTemp(state?.nozzleTemp) }}</span>
                <span class="temp-target">→ {{ fmtTemp(state?.nozzleTargetTemp) }}</span>
              </div>
              <div v-if="state?.nozzleType || state?.nozzleDiameter" class="temp-meta">
                <span v-if="state?.nozzleType">{{ state.nozzleType }}</span>
                <span v-if="state?.nozzleDiameter">Ø{{ state.nozzleDiameter }}mm</span>
              </div>
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

            <div class="temp-card" :class="{ 'temp-card--warm': (state?.bedTemp ?? 0) > 30 }">
              <div class="temp-icon-wrap bed">
                <i class="mdi mdi-heating-coil" />
              </div>
              <div class="temp-body">
                <span class="temp-label">Bed</span>
                <span class="temp-current">{{ fmtTemp(state?.bedTemp) }}</span>
                <span class="temp-target">→ {{ fmtTemp(state?.bedTargetTemp) }}</span>
              </div>
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
        </div>

        <!-- Fans -->
        <div class="section-card">
          <div class="section-label">
            <i class="mdi mdi-fan" /> Fans
          </div>
          <div v-if="fans.length" class="fans-list">
            <div v-for="fan in fans" :key="fan.name" class="fan-row">
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
  background: rgba(15, 32, 39, 0.6);
  border: 1px solid var(--ph-border);
  border-radius: 12px;
  padding: 1.25rem;
}

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
.page-loading {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
}

.page-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255,255,255,0.1);
  border-top-color: var(--ph-accent, #38bdf8);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

.page-loading-text {
  font-size: 0.875rem;
  color: var(--ph-text-muted);
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
.ams-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 0.75rem;
}

.ams-slot {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 0.5rem;
  border-radius: 10px;
  border: 1px solid var(--ph-border);
  background: rgba(255,255,255,0.02);
  transition: border-color 0.15s;
}

.ams-slot--loaded {
  border-color: var(--ph-accent);
  background: var(--ph-accent-dim);
}

.ams-color {
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 50%;
  border: 2px solid rgba(255,255,255,0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ams-loaded-icon {
  font-size: 0.75rem;
  color: #fff;
  text-shadow: 0 1px 3px rgba(0,0,0,0.7);
}

.ams-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.15rem;
}

.ams-name {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--ph-text);
  text-align: center;
  line-height: 1.2;
}

.ams-slot-label {
  font-size: 0.7rem;
  color: var(--ph-text-muted);
}

.ams-env {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.875rem;
  flex-wrap: wrap;
}

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

.progress-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.print-progress {
  flex: 1;
  height: 8px !important;
}

.progress-pct {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--ph-accent);
  min-width: 2.5rem;
  text-align: right;
}

.print-stats {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 0.5rem;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
  padding: 0.5rem 0.75rem;
  border-radius: 8px;
  background: rgba(255,255,255,0.03);
  border: 1px solid var(--ph-border);
}

.stat-key {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--ph-text-muted);
}

.stat-val {
  font-size: 1rem;
  font-weight: 600;
  color: var(--ph-text);
}

.stat-val--material {
  display: flex;
  align-items: center;
  gap: 0.4rem;
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

.temp-card--hot {
  border-color: rgba(248, 113, 113, 0.4);
}

.temp-card--warm {
  border-color: rgba(251, 146, 60, 0.4);
}

.temp-icon-wrap {
  width: 2rem;
  height: 2rem;
  border-radius: 8px;
  background: rgba(248, 113, 113, 0.12);
  color: #f87171;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
}

.temp-icon-wrap.bed {
  background: rgba(251, 146, 60, 0.12);
  color: #fb923c;
}

.temp-body {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.temp-label {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--ph-text-muted);
}

.temp-current {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--ph-text);
  line-height: 1.1;
}

.temp-target {
  font-size: 0.78rem;
  color: var(--ph-text-muted);
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

.fan-name {
  font-size: 0.8rem;
  color: var(--ph-text-muted);
  min-width: 130px;
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

/* ── Printer details ─────────────────────────────────────────────────── */
.section-sublabel {
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  color: var(--ph-text-muted);
  margin-bottom: 0.625rem;
  display: flex;
  align-items: center;
  gap: 0.35rem;
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
</style>
