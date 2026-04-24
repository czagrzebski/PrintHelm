<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Dialog from 'primevue/dialog'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import Toast from 'primevue/toast'
import ProgressBar from 'primevue/progressbar'
import Checkbox from 'primevue/checkbox'
import { useToast } from 'primevue/usetoast'
import { usePrinterSocket } from '@/composables/usePrinterSocket'
import type { ApiPrinterState, ApiJobOrderResponse } from '@/client/printhelm-web-openapi'
import { ApiJobOrderStatus } from '@/client/printhelm-web-openapi'
import printQueueApi from '@/api/PrintQueueApi'
import { printerApi } from '@/api/PrinterApi'

const toast = useToast()
const router = useRouter()
const { connect } = usePrinterSocket()

// ── Printer data ──────────────────────────────────────────────────────────────

interface PrinterRow {
  printerId: number
  printerName: string
  printerModel: string
  location: string
}

const printers = ref<PrinterRow[]>([])
const printerStates = ref<Record<number, ApiPrinterState>>({})
const queues = ref<Record<number, ApiJobOrderResponse[]>>({})
const loadingPrinters = ref(true)
const loadingQueues = ref<Record<number, boolean>>({})
let mounted = true

async function fetchPrinters() {
  loadingPrinters.value = true
  try {
    const res = await printerApi.getPrinters()
    const rows: PrinterRow[] = res.data
      .filter((p) => p.printerId != null)
      .map((p) => ({ printerId: p.printerId!, printerName: p.printerName!, printerModel: p.printerModel!, location: p.location ?? '' }))
    printers.value = rows
    await Promise.all(rows.map((p) => fetchQueue(p.printerId)))
    connect(
      rows.map((p) => p.printerId),
      (id, state) => { printerStates.value[id] = state }
    )
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to load printers.', life: 4000 })
  } finally {
    loadingPrinters.value = false
  }
}

async function fetchQueue(printerId: number, showSpinner = false) {
  if (showSpinner) loadingQueues.value[printerId] = true
  try {
    const res = await printQueueApi.getPrinterQueue(printerId)
    if (mounted) queues.value[printerId] = res.data
  } catch {
    if (mounted) queues.value[printerId] = []
  } finally {
    if (mounted && showSpinner) loadingQueues.value[printerId] = false
  }
}

async function refreshQueues() {
  await Promise.all(printers.value.map((p) => fetchQueue(p.printerId, true)))
}

function isPrinting(printerId: number): boolean {
  const state = printerStates.value[printerId]
  if (!state) return false
  const s = state.gcodeState?.toUpperCase() ?? ''
  return s === 'RUNNING' || s === 'PREPARE'
}

function gcodeStateLabel(printerId: number): string {
  return printerStates.value[printerId]?.gcodeState ?? 'OFFLINE'
}

function gcodeStateSeverity(printerId: number): string {
  const s = gcodeStateLabel(printerId).toUpperCase()
  if (s === 'RUNNING' || s === 'PREPARE') return 'success'
  if (s === 'PAUSE') return 'warn'
  if (s === 'FAILED') return 'danger'
  if (s === 'FINISH') return 'info'
  return 'secondary'
}

// ── Start dialog ──────────────────────────────────────────────────────────────

const showStartDialog = ref(false)
const startTargetPrinterId = ref<number | null>(null)
const startTargetJob = ref<ApiJobOrderResponse | null>(null)
const startOptions = ref({ flowCali: false, vibrationCali: false, layerInspect: false })
const starting = ref(false)
const amsSlotSelections = reactive<Record<number, number>>({})

const startDialogAmsMaterials = computed(() => {
  if (!startTargetPrinterId.value) return []
  return printerStates.value[startTargetPrinterId.value]?.materialSystem?.materials ?? []
})

const startDialogFilaments = computed(() => startTargetJob.value?.gcodeMetadata?.filaments ?? [])

// Always show AMS mapping when the printer has trays loaded.
// When the job has no filament metadata (plain .gcode), synthesize a single unnamed slot
// so the user can still pick which tray to print with.
const showAmsMapping = computed(() => startDialogAmsMaterials.value.length > 0)

const effectiveFilaments = computed(() =>
  startDialogFilaments.value.length > 0
    ? startDialogFilaments.value
    : [{ slotIndex: 0, type: undefined, color: undefined }],
)

function compatibleTrays(filamentType: string | undefined): { label: string; value: number }[] {
  const normalizedType = (filamentType ?? '').toLowerCase().trim()
  const materials = startDialogAmsMaterials.value

  const matching = materials
    .map((m, i) => ({ m, i }))
    .filter(({ m }) => {
      if (!m.type) return false // empty tray
      if (!normalizedType) return true
      const amsType = m.type.toLowerCase()
      return amsType === normalizedType
    })

  const candidates = matching.length > 0 ? matching : materials.map((m, i) => ({ m, i })).filter(({ m }) => !!m.type)
  return candidates.map(({ m, i }) => ({
    label: `Tray ${i}: ${m.type ?? '?'}${m.color ? ' (#' + m.color + ')' : ''}`,
    value: i,
  }))
}

const amsSelectionComplete = computed(() => {
  if (!showAmsMapping.value) return true
  return effectiveFilaments.value.every((f) => amsSlotSelections[f.slotIndex!] !== undefined)
})

function openStartDialog(printerId: number, job: ApiJobOrderResponse) {
  startTargetPrinterId.value = printerId
  startTargetJob.value = job
  startOptions.value = { flowCali: false, vibrationCali: false, layerInspect: false }
  Object.keys(amsSlotSelections).forEach((k) => delete amsSlotSelections[Number(k)])
  showStartDialog.value = true
}

async function submitStart() {
  if (!startTargetPrinterId.value || !startTargetJob.value?.orderId) return
  if (!amsSelectionComplete.value) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Select an AMS tray for every filament slot before starting.', life: 4000 })
    return
  }
  starting.value = true

  let amsMapping: number[] = []
  if (showAmsMapping.value) {
    amsMapping = effectiveFilaments.value.map((f) => amsSlotSelections[f.slotIndex!])
  }

  try {
    await printQueueApi.startQueuedJob(startTargetPrinterId.value, startTargetJob.value.orderId, {
      amsMapping,
      flowCali: startOptions.value.flowCali,
      vibrationCali: startOptions.value.vibrationCali,
      layerInspect: startOptions.value.layerInspect,
    })
    toast.add({ severity: 'success', summary: 'Started', detail: 'Print job started.', life: 3000 })
    showStartDialog.value = false
    await fetchQueue(startTargetPrinterId.value)
  } catch (e: any) {
    const msg = e?.response?.data ?? 'Failed to start print.'
    toast.add({ severity: 'error', summary: 'Error', detail: msg, life: 4000 })
  } finally {
    starting.value = false
  }
}

// ── Remove ────────────────────────────────────────────────────────────────────

const showRemoveDialog = ref(false)
const removeTargetPrinterId = ref<number | null>(null)
const removeTargetJob = ref<ApiJobOrderResponse | null>(null)
const removing = ref(false)

function openRemoveDialog(printerId: number, job: ApiJobOrderResponse) {
  removeTargetPrinterId.value = printerId
  removeTargetJob.value = job
  showRemoveDialog.value = true
}

async function confirmRemove() {
  if (!removeTargetPrinterId.value || !removeTargetJob.value?.orderId) return
  removing.value = true
  try {
    await printQueueApi.removeJobFromQueue(removeTargetPrinterId.value, removeTargetJob.value.orderId)
    toast.add({ severity: 'success', summary: 'Removed', detail: 'Job removed from queue.', life: 3000 })
    showRemoveDialog.value = false
    await fetchQueue(removeTargetPrinterId.value)
  } catch (e: any) {
    const msg = e?.response?.data ?? 'Failed to remove job.'
    toast.add({ severity: 'error', summary: 'Error', detail: msg, life: 4000 })
  } finally {
    removing.value = false
  }
}

// ── Reassign ──────────────────────────────────────────────────────────────────

const showReassignDialog = ref(false)
const reassignSourcePrinterId = ref<number | null>(null)
const reassignTargetPrinterId = ref<number | null>(null)
const reassignTargetJob = ref<ApiJobOrderResponse | null>(null)
const reassigning = ref(false)

const reassignablePrinters = computed(() =>
  printers.value
    .filter((p) => p.printerId !== reassignSourcePrinterId.value)
    .map((p) => ({ label: p.printerName, value: p.printerId }))
)

function openReassignDialog(printerId: number, job: ApiJobOrderResponse) {
  reassignSourcePrinterId.value = printerId
  reassignTargetPrinterId.value = null
  reassignTargetJob.value = job
  showReassignDialog.value = true
}

async function confirmReassign() {
  if (!reassignSourcePrinterId.value || !reassignTargetPrinterId.value || !reassignTargetJob.value?.orderId) return
  reassigning.value = true
  try {
    await printQueueApi.removeJobFromQueue(reassignSourcePrinterId.value, reassignTargetJob.value.orderId)
    await printQueueApi.addJobToQueue(reassignTargetPrinterId.value, { jobOrderId: reassignTargetJob.value.orderId })
    toast.add({ severity: 'success', summary: 'Reassigned', detail: 'Job reassigned successfully.', life: 3000 })
    showReassignDialog.value = false
    await Promise.all([
      fetchQueue(reassignSourcePrinterId.value),
      fetchQueue(reassignTargetPrinterId.value),
    ])
  } catch (e: any) {
    const msg = e?.response?.data ?? 'Failed to reassign job.'
    toast.add({ severity: 'error', summary: 'Error', detail: msg, life: 4000 })
  } finally {
    reassigning.value = false
  }
}

// ── Reorder ───────────────────────────────────────────────────────────────────

async function onRowReorder(printerId: number, event: { value: ApiJobOrderResponse[] }) {
  queues.value[printerId] = event.value
  const entries = event.value.map((job, index) => ({
    jobOrderId: job.orderId!,
    position: index + 1,
  }))
  try {
    await printQueueApi.reorderQueue(printerId, { entries })
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to save new order.', life: 3000 })
    await fetchQueue(printerId)
  }
}

let pollInterval: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  fetchPrinters()
  pollInterval = setInterval(refreshQueues, 15_000)
})

onUnmounted(() => {
  mounted = false
  if (pollInterval !== null) clearInterval(pollInterval)
})
</script>

<template>
  <div class="queue-view">
    <Toast />

    <div class="page-header">
      <div>
        <h2 class="page-title">Print Queue</h2>
        <p class="page-subtitle">Assign and manage print jobs across all printers</p>
      </div>
    </div>

    <div v-if="loadingPrinters" class="loading-state">
      <i class="mdi mdi-loading mdi-spin loading-icon" />
      <span>Loading printers…</span>
    </div>

    <div v-else-if="printers.length === 0" class="empty-state">
      <i class="mdi mdi-printer-off empty-icon" />
      <p>No printers configured yet.</p>
    </div>

    <div v-else class="printer-grid">
      <div v-for="printer in printers" :key="printer.printerId" class="printer-card">

        <!-- Card header -->
        <div class="card-header">
          <div class="printer-info">
            <span class="printer-name">{{ printer.printerName }}</span>
            <span class="printer-model">{{ printer.printerModel }}</span>
          </div>
          <div class="header-right">
            <Tag :value="gcodeStateLabel(printer.printerId)"
                 :severity="gcodeStateSeverity(printer.printerId)"
                 class="state-tag" />
          </div>
        </div>

        <!-- Active print progress -->
        <div v-if="isPrinting(printer.printerId) && printerStates[printer.printerId]"
             class="active-print">
          <div class="active-print-info">
            <i class="mdi mdi-printer active-icon" />
            <span class="active-filename">{{ printerStates[printer.printerId].subtaskName || printerStates[printer.printerId].file || 'Printing…' }}</span>
            <span class="active-progress">{{ printerStates[printer.printerId].progress?.toFixed(0) }}%</span>
          </div>
          <ProgressBar :value="Number(printerStates[printer.printerId].progress ?? 0)"
                       class="print-progress" :show-value="false" />
        </div>

        <!-- Queue table -->
        <DataTable :value="queues[printer.printerId] ?? []"
                   data-key="orderId"
                   empty-message="No jobs queued."
                   size="small"
                   :loading="loadingQueues[printer.printerId] ?? false"
                   @row-reorder="onRowReorder(printer.printerId, $event)">
          <Column row-reorder style="width: 2.5rem" />
          <Column header="#" style="width:3rem">
            <template #body="{ data }">
              <span class="pos-badge">{{ data.queuePosition }}</span>
            </template>
          </Column>
          <Column header="Job" style="min-width:100px">
            <template #body="{ data }">
              <button class="job-link" @click="router.push({ name: 'job-order', params: { id: data.orderId } })">
                #{{ data.orderId }}
              </button>
            </template>
          </Column>
          <Column field="customerName" header="Customer" style="min-width:120px" />
          <Column header="File" style="min-width:150px">
            <template #body="{ data }">
              <span class="filename-cell" :title="data.assignedFilename">{{ data.assignedFilename }}</span>
            </template>
          </Column>
          <Column header="" style="width:100px">
            <template #body="{ data }">
              <div class="row-actions">
                <Button icon="mdi mdi-play" severity="success" text size="small"
                        title="Start print"
                        :disabled="isPrinting(printer.printerId)"
                        @click="openStartDialog(printer.printerId, data)" />
                <Button v-if="printers.length > 1"
                        icon="mdi mdi-transfer" severity="secondary" text size="small"
                        title="Reassign to different printer"
                        :disabled="data.status === ApiJobOrderStatus.Printing"
                        @click="openReassignDialog(printer.printerId, data)" />
                <Button icon="mdi mdi-close" severity="danger" text size="small"
                        title="Remove from queue"
                        @click="openRemoveDialog(printer.printerId, data)" />
              </div>
            </template>
          </Column>
        </DataTable>
      </div>
    </div>

    <!-- Start dialog -->
    <Dialog v-model:visible="showStartDialog" header="Start Print Job"
            modal :style="{ width: '460px' }" :closable="!starting">
      <div class="form-body">
        <p class="start-info">
          Starting <strong>{{ startTargetJob?.assignedFilename }}</strong>
          for <strong>{{ startTargetJob?.customerName }}</strong>.
        </p>

        <!-- AMS filament mapping (shown whenever filament metadata + AMS trays are available) -->
        <div v-if="showAmsMapping" class="ams-mapping-section">
          <div class="ams-mapping-header">
            <i class="mdi mdi-palette-outline" />
            AMS Filament Mapping
          </div>
          <div class="ams-mapping-rows">
            <div v-for="f in effectiveFilaments" :key="f.slotIndex" class="ams-mapping-row">
              <span class="ams-swatch" :style="{ background: f.color ?? 'transparent' }" />
              <span class="ams-slot-label">{{ f.type ?? 'Filament' }} (Slot {{ f.slotIndex }})</span>
              <Select
                v-model="amsSlotSelections[f.slotIndex!]"
                :options="compatibleTrays(f.type)"
                option-label="label"
                option-value="value"
                placeholder="Select AMS tray"
                class="ams-tray-select"
              >
                <template #option="{ option }">
                  <div class="ams-option">
                    <span
                      v-if="startDialogAmsMaterials[option.value]?.color"
                      class="ams-option-swatch"
                      :style="{ background: '#' + startDialogAmsMaterials[option.value].color }"
                    />
                    <span>{{ option.label }}</span>
                  </div>
                </template>
              </Select>
            </div>
          </div>
        </div>

        <div class="checkbox-group">
          <label class="checkbox-row">
            <Checkbox v-model="startOptions.flowCali" :binary="true" />
            Flow calibration
          </label>
          <label class="checkbox-row">
            <Checkbox v-model="startOptions.vibrationCali" :binary="true" />
            Vibration calibration
          </label>
          <label class="checkbox-row">
            <Checkbox v-model="startOptions.layerInspect" :binary="true" />
            Layer inspection
          </label>
        </div>
      </div>
      <template #footer>
        <Button label="Cancel" severity="secondary" text :disabled="starting"
                @click="showStartDialog = false" />
        <Button label="Start Print" severity="success" :loading="starting"
                :disabled="!amsSelectionComplete" @click="submitStart" />
      </template>
    </Dialog>

    <!-- Remove dialog -->
    <Dialog v-model:visible="showRemoveDialog" header="Remove from Queue"
            modal :style="{ width: '400px' }" :closable="!removing">
      <div class="confirm-body">
        <i class="mdi mdi-alert-circle-outline confirm-icon" />
        <p>Remove job <strong>#{{ removeTargetJob?.orderId }}</strong> ({{ removeTargetJob?.customerName }}) from the queue? The job will return to READY_TO_PRINT.</p>
      </div>
      <template #footer>
        <Button label="Cancel" severity="secondary" text :disabled="removing"
                @click="showRemoveDialog = false" />
        <Button label="Remove" severity="danger" :loading="removing" @click="confirmRemove" />
      </template>
    </Dialog>
    <!-- Reassign dialog -->
    <Dialog v-model:visible="showReassignDialog" header="Reassign Job"
            modal :style="{ width: '400px' }" :closable="!reassigning">
      <div class="form-body">
        <p class="start-info">
          Reassign job <strong>#{{ reassignTargetJob?.orderId }}</strong>
          ({{ reassignTargetJob?.customerName }}) to a different printer.
        </p>
        <div class="field">
          <label class="field-label">Target Printer</label>
          <Select
            v-model="reassignTargetPrinterId"
            :options="reassignablePrinters"
            option-label="label"
            option-value="value"
            placeholder="Select printer…"
            class="field-input"
          />
        </div>
      </div>
      <template #footer>
        <Button label="Cancel" severity="secondary" text :disabled="reassigning"
                @click="showReassignDialog = false" />
        <Button label="Reassign" :loading="reassigning"
                :disabled="!reassignTargetPrinterId" @click="confirmReassign" />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
/* ── Page ───────────────────────────────────────────────────────── */
.queue-view {
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

/* ── States ─────────────────────────────────────────────────────── */
.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  padding: 4rem 0;
  color: var(--ph-text-muted);
}
.loading-icon, .empty-icon { font-size: 2.5rem; }

/* ── Printer grid ───────────────────────────────────────────────── */
.printer-grid {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  animation: fade-up 0.35s cubic-bezier(0.16, 1, 0.3, 1) 0.1s both;
}

.printer-card {
  background: #162830;
  border: 1px solid var(--ph-border);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.25);
}

/* ── Card header ────────────────────────────────────────────────── */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--ph-border);
}
.printer-info {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}
.printer-name { font-weight: 600; font-size: 1rem; color: var(--ph-text); }
.printer-model { font-size: 0.8rem; color: var(--ph-text-muted); }
.header-right { display: flex; align-items: center; gap: 0.75rem; }
.state-tag { font-size: 0.75rem; }

/* ── Active print ───────────────────────────────────────────────── */
.active-print {
  padding: 0.75rem 1.25rem;
  background: rgba(6, 182, 212, 0.06);
  border-bottom: 1px solid var(--ph-border);
}
.active-print-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
  font-size: 0.85rem;
}
.active-icon { color: #06b6d4; font-size: 1.1rem; }
.active-filename { flex: 1; color: var(--ph-text); font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.active-progress { color: #06b6d4; font-weight: 600; flex-shrink: 0; }
.print-progress { height: 4px !important; }

/* ── Queue table ────────────────────────────────────────────────── */
.pos-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.5rem;
  height: 1.5rem;
  border-radius: 50%;
  background: rgba(6, 182, 212, 0.15);
  color: #06b6d4;
  font-size: 0.75rem;
  font-weight: 600;
}

.job-link {
  background: none;
  border: none;
  color: #06b6d4;
  cursor: pointer;
  font-size: 0.875rem;
  padding: 0;
  text-decoration: underline;
}
.job-link:hover { color: #22d3ee; }

.filename-cell {
  font-size: 0.8rem;
  color: var(--ph-text-muted);
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 180px;
}

.row-actions { display: flex; gap: 0.25rem; }

/* ── Dialogs ────────────────────────────────────────────────────── */
.form-body { display: flex; flex-direction: column; gap: 1rem; padding: 0.25rem 0; }
.field { display: flex; flex-direction: column; gap: 0.375rem; }
.field-label { font-size: 0.8rem; font-weight: 500; color: var(--ph-text-muted); }
.field-input { width: 100%; }
.start-info { margin: 0 0 0.5rem; font-size: 0.9rem; color: var(--ph-text); line-height: 1.5; }

.checkbox-group { display: flex; flex-direction: column; gap: 0.625rem; }
.checkbox-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: var(--ph-text);
  cursor: pointer;
}

.confirm-body { display: flex; align-items: flex-start; gap: 1rem; padding: 0.5rem 0; }
.confirm-icon { font-size: 2rem; color: #f87171; flex-shrink: 0; }
.confirm-body p { margin: 0; font-size: 0.9rem; line-height: 1.5; color: var(--ph-text); }

/* ── AMS mapping ──────────────────────────────────────────────── */
.ams-mapping-section {
  padding: 0.75rem;
  background: rgba(251, 191, 36, 0.05);
  border: 1px solid rgba(251, 191, 36, 0.2);
  border-radius: 8px;
}
.ams-mapping-header {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.78rem;
  font-weight: 600;
  color: #fbbf24;
  margin-bottom: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.ams-mapping-rows { display: flex; flex-direction: column; gap: 0.5rem; }
.ams-mapping-row {
  display: flex;
  align-items: center;
  gap: 0.625rem;
}
.ams-swatch {
  width: 0.875rem;
  height: 0.875rem;
  border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.2);
  flex-shrink: 0;
}
.ams-slot-label {
  font-size: 0.8rem;
  color: var(--ph-text);
  min-width: 110px;
  white-space: nowrap;
}
.ams-tray-select { flex: 1; }
.ams-option { display: flex; align-items: center; gap: 0.5rem; font-size: 0.825rem; }
.ams-option-swatch {
  width: 0.75rem;
  height: 0.75rem;
  border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.2);
  flex-shrink: 0;
}


</style>
