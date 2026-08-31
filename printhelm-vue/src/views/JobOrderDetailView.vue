<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Toast from 'primevue/toast'
import Textarea from 'primevue/textarea'
import Checkbox from 'primevue/checkbox'
import DatePicker from 'primevue/datepicker'
import Dialog from 'primevue/dialog'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import MultiSelect from 'primevue/multiselect'
import { useToast } from 'primevue/usetoast'
import {
  type ApiJobOrderFileVersion,
  type ApiJobOrderVersionFile,
  type ApiJobOrderResponse,
  type ApiUpdateJobOrderRequest,
  ApiJobOrderStatus,
} from '@/client/printhelm-web-openapi'
import jobOrderApi, {
  uploadPartFiles,
  uploadGcodeFiles,
  downloadPartFile,
  downloadGcodeFile,
  fetchGcodeFileBuffer,
  fetchPartFileBuffer,
  listPartFileVersions,
  listGcodeFileVersions,
  selectGcodeFileVersion,
  updateGcodeVersionQuantities,
  downloadPartFileVersionFile,
  downloadGcodeFileVersionFile,
  fetchPartFileVersionFileBuffer,
  fetchGcodeFileVersionFileBuffer,
  downloadInvoice,
  downloadQuote,
} from '@/api/JobOrderApi'
import { printerApi } from '@/api/PrinterApi'
import PrintJobInfoDialog from '@/components/PrintJobInfoDialog.vue'
import ModelViewerDialog, { isViewableModel, type ViewerFile } from '@/components/ModelViewerDialog.vue'
import FileVersionHistory from '@/components/FileVersionHistory.vue'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const orderId = Number(route.params.id)

const STEPS = [
  { value: 1, label: 'Submitted',      icon: 'mdi mdi-inbox-arrow-down' },
  { value: 2, label: 'Review',         icon: 'mdi mdi-clipboard-text-outline' },
  { value: 3, label: 'Quote',          icon: 'mdi mdi-file-document-outline' },
  { value: 4, label: 'Design',         icon: 'mdi mdi-cube-outline' },
  { value: 5, label: 'Setup',          icon: 'mdi mdi-file-cog-outline' },
  { value: 6, label: 'Ready to Print', icon: 'mdi mdi-printer-check' },
  { value: 7, label: 'Printing',       icon: 'mdi mdi-printer-3d-nozzle' },
  { value: 8, label: 'Finished',       icon: 'mdi mdi-check-decagram' },
  { value: 9, label: 'Invoice',        icon: 'mdi mdi-receipt-text-outline' },
]

const STATUS_STEP: Record<string, number> = {
  [ApiJobOrderStatus.Submitted]:    1,
  [ApiJobOrderStatus.Review]:       2,
  [ApiJobOrderStatus.Quoted]:       3,
  [ApiJobOrderStatus.Design]:       4,
  [ApiJobOrderStatus.Setup]:        5,
  [ApiJobOrderStatus.ReadyToPrint]: 6,
  [ApiJobOrderStatus.Printing]:     7,
  [ApiJobOrderStatus.PrintFinished]:8,
  [ApiJobOrderStatus.Invoiced]:     9,
}

const STATUS_META: Record<string, { label: string; severity: string }> = {
  [ApiJobOrderStatus.Submitted]:    { label: 'Submitted',      severity: 'secondary' },
  [ApiJobOrderStatus.Review]:       { label: 'Review',         severity: 'warn' },
  [ApiJobOrderStatus.Quoted]:       { label: 'Quoted',         severity: 'warn' },
  [ApiJobOrderStatus.Design]:       { label: 'Design',         severity: 'info' },
  [ApiJobOrderStatus.Setup]:        { label: 'Setup',          severity: 'info' },
  [ApiJobOrderStatus.ReadyToPrint]: { label: 'Ready to Print', severity: 'contrast' },
  [ApiJobOrderStatus.Printing]:     { label: 'Printing',       severity: 'success' },
  [ApiJobOrderStatus.PrintFinished]:{ label: 'Print Finished', severity: 'success' },
  [ApiJobOrderStatus.Invoiced]:     { label: 'Invoiced',       severity: 'success' },
}

function statusLabel(s?: ApiJobOrderStatus) {
  return s ? (STATUS_META[s]?.label ?? s) : '—'
}
function statusSeverity(s?: ApiJobOrderStatus) {
  return s ? (STATUS_META[s]?.severity ?? 'secondary') : 'secondary'
}
function formatDate(iso?: string) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString()
}

const order = ref<ApiJobOrderResponse | null>(null)
const loading = ref(true)
const advancing = ref(false)
const viewingStep = ref(1)

const partFileInput = ref<HTMLInputElement | null>(null)
const gcodeFileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const downloadingPart = ref(false)
const downloadingGcode = ref(false)

// ── File versioning ───────────────────────────────────────────────────────
const partVersions = ref<ApiJobOrderFileVersion[]>([])
const gcodeVersions = ref<ApiJobOrderFileVersion[]>([])
const selectingFileKey = ref<string | null>(null)

// Upload-with-description dialog
const showUploadDialog = ref(false)
const uploadTarget = ref<'part' | 'gcode'>('part')
const pendingUploadFiles = ref<File[]>([])
const uploadDescription = ref('')

// Viewer dialogs
interface ViewerState { filename: string; fetchFile: () => Promise<ArrayBuffer> }
const modelViewer = ref<ViewerFile[] | null>(null)
const gcodeViewer = ref<ViewerState | null>(null)

// Latest design version drives the design-step display and 3D view
const latestPartVersion = computed(() => partVersions.value[0] ?? null)

/**
 * Print plan progress for the active gcode version (the one containing the file
 * selected for print): total required prints across files × quantity vs completed.
 */
const printPlan = computed(() => {
  const active =
    gcodeVersions.value.find((v) => v.files?.some((f) => f.active)) ??
    gcodeVersions.value.find((v) => v.active)
  if (!active?.files?.length) return null
  let total = 0
  let completed = 0
  for (const f of active.files) {
    const qty = f.printQuantity ?? 1
    total += qty
    completed += Math.min(f.completedPrints ?? 0, qty)
  }
  return { total, completed, versionNumber: active.versionNumber }
})

function fetchGcodeForViewer(): Promise<ArrayBuffer> {
  return fetchGcodeFileBuffer(orderId)
}

async function fetchVersions() {
  try {
    const [partRes, gcodeRes] = await Promise.all([
      listPartFileVersions(orderId),
      listGcodeFileVersions(orderId),
    ])
    partVersions.value = partRes.data
    gcodeVersions.value = gcodeRes.data
  } catch {
    // non-critical — version history simply stays empty
  }
}

const statusStep = computed(() =>
  STATUS_STEP[order.value?.status ?? ApiJobOrderStatus.Submitted] ?? 1
)
const isOnCurrentStep = computed(() => viewingStep.value === statusStep.value)

const reviewRequirements = ref('')
const reviewRequiresCustomDesign = ref(false)

const MATERIAL_OPTIONS = [
  'PLA', 'PLA+', 'PLA-HF', 'PLA-CF',
  'PETG', 'PETG-HF', 'PETG-CF',
  'ABS', 'ASA',
  'TPU', 'TPE',
  'Nylon (PA)', 'PA-CF', 'PA-GF', 'PA12-CF', 'PAHT-CF',
  'PC', 'PEI (ULTEM)',
  'HIPS', 'PVA',
  'PPS', 'PPS-CF',
]

const quoteMaterials       = ref<string[]>([])
const quotedMaterialCost   = ref<number | null>(null)
const quotedCostPerUnit    = ref<number | null>(null)
const quotedQuantity       = ref<number | null>(null)
const quotedLaborCost      = ref<number | null>(null)
const quotedSetupFee       = ref<number | null>(null)
const quotedDiscount       = ref<number | null>(null)
const quotedPrintTimeHours = ref<number | null>(null)
const quotedFilamentGrams  = ref<number | null>(null)
const quotedLeadTimeDays   = ref<number | null>(null)
const quoteNotes           = ref('')
const quoteExpiresAt       = ref<Date | null>(null)
const savingQuote          = ref(false)
const downloadingQuote     = ref(false)

/** Serialize a Date to the API's `date` format (local YYYY-MM-DD, no TZ shift) */
function toIsoDate(d: Date): string {
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

function formatDateOnly(iso?: string) {
  if (!iso) return '—'
  return new Date(`${iso}T00:00:00`).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' })
}

watch([quotedCostPerUnit, quotedQuantity], ([perUnit, qty]) => {
  if (perUnit != null && qty != null && qty > 0) {
    quotedMaterialCost.value = Math.round(perUnit * qty * 100) / 100
  }
})

interface LineItem { label: string; amount: number | null }
const quoteLineItems = ref<LineItem[]>([])

const quoteTotal = computed(() => {
  const fixed = (quotedMaterialCost.value ?? 0) + (quotedLaborCost.value ?? 0) + (quotedSetupFee.value ?? 0)
  const extra = quoteLineItems.value.reduce((sum, item) => sum + (item.amount ?? 0), 0)
  return Math.max(0, fixed + extra - (quotedDiscount.value ?? 0))
})

function syncQuoteFields(data: ApiJobOrderResponse) {
  quoteMaterials.value       = data.quoteMaterials       ?? []
  quotedMaterialCost.value   = data.quotedMaterialCost   ?? null
  quotedCostPerUnit.value    = data.quotedCostPerUnit    ?? null
  quotedQuantity.value       = data.quotedQuantity       ?? null
  quotedLaborCost.value      = data.quotedLaborCost      ?? null
  quotedSetupFee.value       = data.quotedSetupFee       ?? null
  quotedDiscount.value       = data.quotedDiscount       ?? null
  quotedPrintTimeHours.value = data.quotedPrintTimeHours ?? null
  quotedFilamentGrams.value  = data.quotedFilamentGrams  ?? null
  quotedLeadTimeDays.value   = data.quotedLeadTimeDays   ?? null
  quoteNotes.value           = data.quoteNotes           ?? ''
  quoteExpiresAt.value       = data.quoteExpiresAt ? new Date(`${data.quoteExpiresAt}T00:00:00`) : null
  quoteLineItems.value       = (data.quoteLineItems ?? []).map(i => ({ label: i.label ?? '', amount: i.amount ?? null }))
  designNotes.value          = data.designNotes ?? ''
}

function addLineItem() {
  quoteLineItems.value.push({ label: '', amount: null })
}

function removeLineItem(index: number) {
  quoteLineItems.value.splice(index, 1)
}

// ── Design step ───────────────────────────────────────────────────────────
const designNotes = ref('')
const savingDesignNotes = ref(false)

async function saveDesignNotes() {
  if (!order.value?.orderId) return
  savingDesignNotes.value = true
  try {
    const res = await jobOrderApi.updateJobOrder(order.value.orderId, {
      designNotes: designNotes.value.trim() || undefined,
    })
    order.value = res.data
    toast.add({ severity: 'success', summary: 'Saved', detail: 'Design notes saved.', life: 2500 })
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to save design notes.', life: 4000 })
  } finally {
    savingDesignNotes.value = false
  }
}

function completeDesign() {
  advanceStatus(ApiJobOrderStatus.Setup, {
    designNotes: designNotes.value.trim() || undefined,
  })
}

const invoiceMaterialCost = ref<number | null>(null)
const invoiceLaborCost    = ref<number | null>(null)
const invoiceSetupFee     = ref<number | null>(null)
const invoiceDiscount     = ref<number | null>(null)
const invoiceNotes        = ref('')
const downloadingInvoice  = ref(false)

const invoiceTotal = computed(() => {
  const sub = (invoiceMaterialCost.value ?? 0) + (invoiceLaborCost.value ?? 0) + (invoiceSetupFee.value ?? 0)
  return Math.max(0, sub - (invoiceDiscount.value ?? 0))
})

function syncInvoiceFields(data: ApiJobOrderResponse) {
  invoiceMaterialCost.value = data.materialCost ?? null
  invoiceLaborCost.value    = data.laborCost    ?? null
  invoiceSetupFee.value     = data.setupFee     ?? null
  invoiceDiscount.value     = data.discount     ?? null
  invoiceNotes.value        = data.invoiceNotes ?? ''
}

async function fetchOrder() {
  loading.value = true
  try {
    const res = await jobOrderApi.getJobOrderById(orderId)
    order.value = res.data
    viewingStep.value = STATUS_STEP[res.data.status ?? ApiJobOrderStatus.Submitted] ?? 1
    reviewRequirements.value = res.data.requirements ?? ''
    reviewRequiresCustomDesign.value = res.data.requiresCustomDesign ?? false
    syncQuoteFields(res.data)
    syncInvoiceFields(res.data)
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to load order.', life: 4000 })
  } finally {
    loading.value = false
  }
}

async function advanceStatus(nextStatus: ApiJobOrderStatus, extra?: Partial<ApiUpdateJobOrderRequest>) {
  if (!order.value?.orderId) return
  advancing.value = true
  try {
    const res = await jobOrderApi.updateJobOrder(order.value.orderId, { status: nextStatus, ...extra })
    order.value = res.data
    viewingStep.value = STATUS_STEP[nextStatus] ?? viewingStep.value
    reviewRequirements.value = res.data.requirements ?? ''
    reviewRequiresCustomDesign.value = res.data.requiresCustomDesign ?? false
    syncQuoteFields(res.data)
    syncInvoiceFields(res.data)
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to update order.', life: 4000 })
  } finally {
    advancing.value = false
  }
}

function prevStatusFor(current?: ApiJobOrderStatus): ApiJobOrderStatus | null {
  switch (current) {
    case ApiJobOrderStatus.Review:       return ApiJobOrderStatus.Submitted
    case ApiJobOrderStatus.Quoted:       return ApiJobOrderStatus.Review
    case ApiJobOrderStatus.Design:       return ApiJobOrderStatus.Quoted
    case ApiJobOrderStatus.Setup:        return order.value?.requiresCustomDesign
                                           ? ApiJobOrderStatus.Design : ApiJobOrderStatus.Quoted
    case ApiJobOrderStatus.ReadyToPrint: return ApiJobOrderStatus.Setup
    default: return null
  }
}

async function regressStatus() {
  const prev = prevStatusFor(order.value?.status)
  if (prev) await advanceStatus(prev)
}

function completeReview() {
  advanceStatus(ApiJobOrderStatus.Quoted, {
    requirements: reviewRequirements.value.trim() || undefined,
    requiresCustomDesign: reviewRequiresCustomDesign.value,
  })
}

const printerNames = ref<Record<number, string>>({})

async function fetchPrinterNames() {
  try {
    const res = await printerApi.getPrinters()
    printerNames.value = Object.fromEntries(res.data.map((p) => [p.printerId!, p.printerName!]))
  } catch {
    // non-critical
  }
}

function handlePartFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  input.value = ''
  if (files.length === 0) return
  const invalid = files.filter((f) => !/\.(glb|gltf)$/i.test(f.name))
  if (invalid.length > 0) {
    toast.add({
      severity: 'warn',
      summary: 'Unsupported file type',
      detail: `Only .glb and .gltf files are supported: ${invalid.map((f) => f.name).join(', ')}`,
      life: 5000,
    })
    return
  }
  pendingUploadFiles.value = files
  uploadTarget.value = 'part'
  uploadDescription.value = ''
  showUploadDialog.value = true
}

function handleGcodeFileSelected(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  input.value = ''
  if (files.length === 0) return
  const invalid = files.filter((f) => !/\.(gcode|3mf)$/i.test(f.name))
  if (invalid.length > 0) {
    toast.add({
      severity: 'warn',
      summary: 'Unsupported file type',
      detail: `Only .gcode and .3mf files are supported: ${invalid.map((f) => f.name).join(', ')}`,
      life: 5000,
    })
    return
  }
  pendingUploadFiles.value = files
  uploadTarget.value = 'gcode'
  uploadDescription.value = ''
  showUploadDialog.value = true
}

async function confirmUpload() {
  const files = pendingUploadFiles.value
  if (files.length === 0 || !order.value?.orderId) return
  uploading.value = true
  const isPart = uploadTarget.value === 'part'
  try {
    const description = uploadDescription.value.trim() || undefined
    const res = isPart
      ? await uploadPartFiles(order.value.orderId, files, description)
      : await uploadGcodeFiles(order.value.orderId, files, description)
    order.value = res.data
    await fetchVersions()
    showUploadDialog.value = false
    pendingUploadFiles.value = []
    toast.add({
      severity: 'success',
      summary: 'Uploaded',
      detail: `New ${isPart ? 'design' : 'GCode'} version uploaded (${files.length} file${files.length === 1 ? '' : 's'}).`,
      life: 3000,
    })
  } catch {
    toast.add({
      severity: 'error',
      summary: 'Error',
      detail: `Failed to upload ${isPart ? '3D design' : 'GCode'} file${files.length === 1 ? '' : 's'}.`,
      life: 4000,
    })
  } finally {
    uploading.value = false
  }
}

function cancelUpload() {
  showUploadDialog.value = false
  pendingUploadFiles.value = []
  uploadDescription.value = ''
}

function handleDownloadVersion(
  mode: 'part' | 'gcode',
  version: ApiJobOrderFileVersion,
  file?: ApiJobOrderVersionFile,
) {
  if (version.versionId == null) return
  const versionId = version.versionId
  const download = mode === 'part' ? downloadPartFileVersionFile : downloadGcodeFileVersionFile
  const targets = file
    ? [{ fileIndex: file.fileIndex ?? 0, filename: file.filename ?? 'file' }]
    : versionFiles(version)
  Promise.all(targets.map((f) => download(orderId, versionId, f.fileIndex, f.filename))).catch(() => {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to download file version.', life: 4000 })
  })
}

/** Files of a version with legacy fallback (pre-multi-file rows only carry `filename`) */
function versionFiles(version: ApiJobOrderFileVersion): { fileIndex: number; filename: string }[] {
  if (version.files?.length) {
    return version.files.map((f, i) => ({ fileIndex: f.fileIndex ?? i, filename: f.filename ?? 'file' }))
  }
  return version.filename ? [{ fileIndex: 0, filename: version.filename }] : []
}

function handleViewPartVersion(version: ApiJobOrderFileVersion) {
  if (version.versionId == null) return
  const versionId = version.versionId
  const viewable = versionFiles(version).filter((f) => isViewableModel(f.filename))
  if (viewable.length === 0) return
  modelViewer.value = viewable.map((f) => ({
    filename: f.filename,
    fetchFile: () => fetchPartFileVersionFileBuffer(orderId, versionId, f.fileIndex),
  }))
}

const currentPartViewable = computed(() => {
  if (latestPartVersion.value) {
    return versionFiles(latestPartVersion.value).some((f) => isViewableModel(f.filename))
  }
  return !!order.value?.mongoPartFileId && isViewableModel(order.value.partFilename)
})

function handleViewCurrentPart() {
  if (latestPartVersion.value) {
    handleViewPartVersion(latestPartVersion.value)
    return
  }
  // Legacy orders that predate version records only have the order-level file
  if (!order.value?.partFilename) return
  modelViewer.value = [
    {
      filename: order.value.partFilename,
      fetchFile: () => fetchPartFileBuffer(orderId),
    },
  ]
}

function handlePreviewGcodeVersion(version: ApiJobOrderFileVersion, file?: ApiJobOrderVersionFile) {
  if (version.versionId == null) return
  const versionId = version.versionId
  const target = file ?? versionFiles(version)[0]
  if (!target?.filename) return
  const fileIndex = target.fileIndex ?? 0
  gcodeViewer.value = {
    filename: target.filename,
    fetchFile: () => fetchGcodeFileVersionFileBuffer(orderId, versionId, fileIndex),
  }
}

function handlePreviewCurrentGcode() {
  if (!order.value?.gcodeFilename) return
  gcodeViewer.value = { filename: order.value.gcodeFilename, fetchFile: fetchGcodeForViewer }
}

async function handleSelectGcodeVersion(version: ApiJobOrderFileVersion, fileIndex: number) {
  if (version.versionId == null || !order.value?.orderId) return
  selectingFileKey.value = `${version.versionId}:${fileIndex}`
  try {
    const res = await selectGcodeFileVersion(order.value.orderId, version.versionId, fileIndex)
    order.value = res.data
    await fetchVersions()
    const filename = versionFiles(version)[fileIndex]?.filename ?? version.filename
    toast.add({
      severity: 'success',
      summary: 'File Selected',
      detail: `v${version.versionNumber} (${filename}) will be used for printing.`,
      life: 3500,
    })
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to select gcode file.', life: 4000 })
  } finally {
    selectingFileKey.value = null
  }
}

async function handleUpdateQuantity(version: ApiJobOrderFileVersion, fileIndex: number, quantity: number) {
  if (version.versionId == null || !order.value?.orderId) return
  try {
    await updateGcodeVersionQuantities(order.value.orderId, version.versionId, [{ fileIndex, quantity }])
    await fetchVersions()
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to update print quantity.', life: 4000 })
    await fetchVersions()
  }
}

async function handleDownloadPartFile() {
  if (!order.value?.orderId) return
  downloadingPart.value = true
  try {
    const latest = latestPartVersion.value
    if (latest?.versionId != null) {
      await Promise.all(
        versionFiles(latest).map((f) =>
          downloadPartFileVersionFile(orderId, latest.versionId!, f.fileIndex, f.filename),
        ),
      )
    } else if (order.value.partFilename) {
      await downloadPartFile(order.value.orderId, order.value.partFilename)
    }
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to download 3D design files.', life: 4000 })
  } finally {
    downloadingPart.value = false
  }
}

async function handleDownloadGcodeFile() {
  if (!order.value?.orderId || !order.value.gcodeFilename) return
  downloadingGcode.value = true
  try {
    await downloadGcodeFile(order.value.orderId, order.value.gcodeFilename)
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to download GCode file.', life: 4000 })
  } finally {
    downloadingGcode.value = false
  }
}


async function handleSaveAndDownloadQuote() {
  if (!order.value?.orderId) return
  savingQuote.value = true
  try {
    const validLineItems = quoteLineItems.value
      .filter(i => i.label.trim() && i.amount != null && i.amount > 0)
      .map(i => ({ label: i.label.trim(), amount: i.amount! }))
    const res = await jobOrderApi.updateJobOrder(order.value.orderId, {
      quoteMaterials:       quoteMaterials.value.length > 0 ? quoteMaterials.value : undefined,
      quotedMaterialCost:   quotedMaterialCost.value   ?? undefined,
      quotedCostPerUnit:    quotedCostPerUnit.value    ?? undefined,
      quotedQuantity:       quotedQuantity.value       ?? undefined,
      quotedLaborCost:      quotedLaborCost.value      ?? undefined,
      quotedSetupFee:       quotedSetupFee.value       ?? undefined,
      quotedDiscount:       quotedDiscount.value       ?? undefined,
      quotedPrintTimeHours: quotedPrintTimeHours.value ?? undefined,
      quotedFilamentGrams:  quotedFilamentGrams.value  ?? undefined,
      quotedLeadTimeDays:   quotedLeadTimeDays.value   ?? undefined,
      quoteNotes:           quoteNotes.value.trim()    || undefined,
      quoteExpiresAt:       quoteExpiresAt.value ? toIsoDate(quoteExpiresAt.value) : undefined,
      quoteLineItems:       validLineItems.length > 0 ? validLineItems : undefined,
    })
    order.value = res.data
    syncQuoteFields(res.data)
    await downloadQuote(order.value.orderId!)
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to save or download quote.', life: 4000 })
  } finally {
    savingQuote.value = false
  }
}

async function handleAcceptQuote() {
  const next = order.value?.requiresCustomDesign ? ApiJobOrderStatus.Design : ApiJobOrderStatus.Setup
  await advanceStatus(next)
}

async function handleDownloadQuote() {
  if (!order.value?.orderId) return
  downloadingQuote.value = true
  try {
    await downloadQuote(order.value.orderId)
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to download quote.', life: 4000 })
  } finally {
    downloadingQuote.value = false
  }
}

async function handleGenerateInvoice() {
  await advanceStatus(ApiJobOrderStatus.Invoiced, {
    materialCost: invoiceMaterialCost.value ?? undefined,
    laborCost:    invoiceLaborCost.value    ?? undefined,
    setupFee:     invoiceSetupFee.value     ?? undefined,
    discount:     invoiceDiscount.value     ?? undefined,
    invoiceNotes: invoiceNotes.value.trim() || undefined,
  })
  toast.add({ severity: 'success', summary: 'Invoiced', detail: 'Invoice generated. You can now download the PDF.', life: 4000 })
}

async function handleDownloadInvoice() {
  if (!order.value?.orderId) return
  downloadingInvoice.value = true
  try {
    await downloadInvoice(order.value.orderId)
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to download invoice.', life: 4000 })
  } finally {
    downloadingInvoice.value = false
  }
}

onMounted(() => { fetchOrder(); fetchPrinterNames(); fetchVersions() })
</script>

<template>
  <div class="job-order-detail">
    <Toast />

    <div v-if="loading" class="page-loading">
      <span class="page-spinner" />
      <span class="page-loading-text">Loading order…</span>
    </div>

    <template v-else-if="order">
      <!-- Header — mirrors PrinterView layout -->
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
            <h1 class="order-name">Order #{{ order.orderId }}</h1>
            <div class="order-meta">
              <span class="meta-chip"><i class="mdi mdi-account-outline" /> {{ order.customerName }}</span>
              <span v-if="order.customerEmail" class="meta-chip"><i class="mdi mdi-email-outline" /> {{ order.customerEmail }}</span>
              <span class="meta-chip"><i class="mdi mdi-calendar-outline" /> {{ formatDate(order.createdAt) }}</span>
            </div>
          </div>
        </div>
        <div class="header-right">
          <Tag :value="statusLabel(order.status)" :severity="statusSeverity(order.status)" class="status-tag" />
        </div>
      </div>

      <!-- Timeline — click to browse; does not transition status -->
      <div class="section-card">
        <div class="timeline">
          <template v-for="(step, i) in STEPS" :key="step.value">
            <button
              class="timeline-step"
              :class="{
                'timeline-step--done':    statusStep > step.value,
                'timeline-step--current': statusStep === step.value,
                'timeline-step--future':  statusStep < step.value,
                'timeline-step--viewing': viewingStep === step.value,
              }"
              @click="viewingStep = step.value"
            >
              <div class="timeline-node">
                <i v-if="statusStep > step.value" class="pi pi-check" />
                <span v-else>{{ step.value }}</span>
              </div>
              <span class="timeline-label">{{ step.label }}</span>
            </button>
            <div
              v-if="i < STEPS.length - 1"
              class="timeline-connector"
              :class="{ 'timeline-connector--done': statusStep > step.value }"
            />
          </template>
        </div>
      </div>

      <!-- Browsing notice — only when not on the current step -->
      <div v-if="!isOnCurrentStep" class="browse-notice">
        <i class="mdi mdi-eye-outline" />
        <span>Browsing <strong>{{ STEPS[viewingStep - 1].label }}</strong> — read only</span>
        <button class="browse-jump-btn" @click="viewingStep = statusStep">
          <i class="mdi mdi-map-marker-outline" /> Jump to current step
        </button>
      </div>

      <!-- Step content card -->
      <div class="section-card">
        <div class="section-label">
          <i :class="STEPS[viewingStep - 1].icon" />
          {{ STEPS[viewingStep - 1].label }}
          <span v-if="!isOnCurrentStep" class="view-only-badge">viewing only</span>
        </div>

        <!-- Step 1: Submitted -->
        <template v-if="viewingStep === 1">
          <div class="info-grid">
            <div class="info-card">
              <span class="info-label">Customer</span>
              <span class="info-value">{{ order.customerName }}</span>
            </div>
            <div class="info-card">
              <span class="info-label">Email</span>
              <span class="info-value">{{ order.customerEmail || '—' }}</span>
            </div>
            <div class="info-card info-card--full">
              <span class="info-label">Description</span>
              <span class="info-value">{{ order.description || '—' }}</span>
            </div>
            <div class="info-card">
              <span class="info-label">Submitted</span>
              <span class="info-value">{{ formatDate(order.createdAt) }}</span>
            </div>
          </div>
        </template>

        <!-- Step 2: Review -->
        <template v-else-if="viewingStep === 2">
          <p class="step-description">Evaluate the submission, document requirements, and determine whether a custom design is needed.</p>
          <template v-if="isOnCurrentStep">
            <div class="form-body">
              <div class="field">
                <label class="field-label">Requirements</label>
                <Textarea v-model="reviewRequirements" placeholder="Document the requirements…" class="field-input" rows="4" auto-resize />
              </div>
              <div class="field checkbox-field">
                <Checkbox v-model="reviewRequiresCustomDesign" input-id="requiresCustomDesign" :binary="true" />
                <label for="requiresCustomDesign" class="checkbox-label">Requires custom design</label>
              </div>
            </div>
          </template>
          <template v-else>
            <div class="info-grid">
              <div class="info-card info-card--full">
                <span class="info-label">Requirements</span>
                <span class="info-value">{{ order.requirements || '—' }}</span>
              </div>
              <div class="info-card">
                <span class="info-label">Custom Design</span>
                <span class="info-value">{{ order.requiresCustomDesign ? 'Yes' : order.requiresCustomDesign === false ? 'No' : '—' }}</span>
              </div>
            </div>
          </template>
        </template>

        <!-- Step 3: Quote -->
        <template v-else-if="viewingStep === 3">
          <template v-if="order.status !== ApiJobOrderStatus.Quoted && statusStep > 3">
            <!-- Read-only quote summary when browsing past this step -->
            <div class="invoice-summary-card">
              <div v-if="order.quoteMaterials && order.quoteMaterials.length > 0" class="invoice-summary-row">
                <span class="invoice-summary-label">Materials</span>
                <span class="invoice-summary-value">{{ order.quoteMaterials.join(', ') }}</span>
              </div>
              <div class="invoice-summary-row" v-if="order.quotedPrintTimeHours">
                <span class="invoice-summary-label">Est. Print Time</span>
                <span class="invoice-summary-value">{{ order.quotedPrintTimeHours }} h</span>
              </div>
              <div class="invoice-summary-row" v-if="order.quotedFilamentGrams">
                <span class="invoice-summary-label">Est. Filament Usage</span>
                <span class="invoice-summary-value">{{ order.quotedFilamentGrams }} g</span>
              </div>
              <div class="invoice-summary-row" v-if="order.quotedLeadTimeDays">
                <span class="invoice-summary-label">Est. Lead Time</span>
                <span class="invoice-summary-value">{{ order.quotedLeadTimeDays }} business day{{ order.quotedLeadTimeDays === 1 ? '' : 's' }}</span>
              </div>
              <div class="invoice-summary-row" v-if="order.quoteExpiresAt">
                <span class="invoice-summary-label">Valid Until</span>
                <span class="invoice-summary-value">{{ formatDateOnly(order.quoteExpiresAt) }}</span>
              </div>
              <div class="invoice-summary-divider" v-if="order.quoteMaterials?.length || order.quotedPrintTimeHours || order.quotedFilamentGrams || order.quotedLeadTimeDays || order.quoteExpiresAt" />
              <div class="invoice-summary-row" v-if="order.quotedMaterialCost">
                <span class="invoice-summary-label">
                  Est. Material Cost<template v-if="order.quotedQuantity && order.quotedCostPerUnit"> ({{ order.quotedQuantity }} × ${{ order.quotedCostPerUnit.toFixed(2) }})</template>
                </span>
                <span class="invoice-summary-value">${{ order.quotedMaterialCost.toFixed(2) }}</span>
              </div>
              <div class="invoice-summary-row" v-if="order.quotedLaborCost">
                <span class="invoice-summary-label">Est. Labor / Design Fee</span>
                <span class="invoice-summary-value">${{ order.quotedLaborCost.toFixed(2) }}</span>
              </div>
              <div class="invoice-summary-row" v-if="order.quotedSetupFee">
                <span class="invoice-summary-label">Est. Setup Fee</span>
                <span class="invoice-summary-value">${{ order.quotedSetupFee.toFixed(2) }}</span>
              </div>
              <template v-if="order.quoteLineItems && order.quoteLineItems.length > 0">
                <div class="invoice-summary-row" v-for="(item, i) in order.quoteLineItems" :key="i">
                  <span class="invoice-summary-label">{{ item.label }}</span>
                  <span class="invoice-summary-value">${{ (item.amount ?? 0).toFixed(2) }}</span>
                </div>
              </template>
              <div class="invoice-summary-divider" />
              <div class="invoice-summary-row" v-if="order.quotedDiscount && order.quotedDiscount > 0">
                <span class="invoice-summary-label">Discount</span>
                <span class="invoice-summary-value invoice-summary-value--discount">-${{ order.quotedDiscount.toFixed(2) }}</span>
              </div>
              <div class="invoice-summary-row invoice-summary-row--total">
                <span class="invoice-summary-label">Est. Total</span>
                <span class="invoice-summary-value invoice-summary-value--total">
                  ${{ Math.max(0, ((order.quotedMaterialCost ?? 0) + (order.quotedLaborCost ?? 0) + (order.quotedSetupFee ?? 0) + (order.quoteLineItems ?? []).reduce((s, i) => s + (i.amount ?? 0), 0)) - (order.quotedDiscount ?? 0)).toFixed(2) }}
                </span>
              </div>
              <div v-if="order.quoteNotes" class="invoice-notes-section">
                <span class="invoice-summary-label">Notes</span>
                <p class="invoice-notes-text">{{ order.quoteNotes }}</p>
              </div>
            </div>
            <div class="invoice-download-row">
              <Button label="Download Quote PDF" icon="pi pi-download" severity="secondary" :loading="downloadingQuote" @click="handleDownloadQuote" />
              <span v-if="order.quotedAt" class="invoice-date-hint">Quoted {{ formatDate(order.quotedAt) }}</span>
            </div>
          </template>
          <template v-else>
            <!-- Active quote step: cost entry form -->
            <p class="step-description">Enter the estimated costs and print details for this job to generate a quote for the customer.</p>
            <div class="form-body">
              <div class="form-subsection">
                <span class="form-subsection-label"><i class="mdi mdi-currency-usd" /> Costs</span>
                <div class="field">
                  <label class="field-label">Materials</label>
                  <MultiSelect
                    v-model="quoteMaterials"
                    :options="MATERIAL_OPTIONS"
                    placeholder="Select material types…"
                    display="chip"
                    filter
                    class="field-input"
                  />
                </div>
                <div class="unit-cost-row">
                  <div class="field">
                    <label class="field-label">Cost Per Unit</label>
                    <InputNumber v-model="quotedCostPerUnit" mode="currency" currency="USD" locale="en-US" :min="0" :minFractionDigits="2" placeholder="$0.00" fluid />
                  </div>
                  <span class="unit-cost-op" aria-hidden="true">×</span>
                  <div class="field unit-cost-qty">
                    <label class="field-label">Quantity</label>
                    <InputNumber v-model="quotedQuantity" :min="1" :max="99999" placeholder="1" fluid />
                  </div>
                  <span class="unit-cost-op" aria-hidden="true">=</span>
                  <div class="field">
                    <label class="field-label">Est. Material Cost</label>
                    <InputNumber v-model="quotedMaterialCost" mode="currency" currency="USD" locale="en-US" :min="0" :minFractionDigits="2" placeholder="$0.00" fluid />
                  </div>
                </div>
                <div class="quote-field-grid">
                  <div class="field">
                    <label class="field-label">Est. Labor / Design Fee</label>
                    <InputNumber v-model="quotedLaborCost" mode="currency" currency="USD" locale="en-US" :min="0" :minFractionDigits="2" placeholder="$0.00" fluid />
                  </div>
                  <div class="field">
                    <label class="field-label">Est. Setup Fee</label>
                    <InputNumber v-model="quotedSetupFee" mode="currency" currency="USD" locale="en-US" :min="0" :minFractionDigits="2" placeholder="$0.00" fluid />
                  </div>
                  <div class="field">
                    <label class="field-label">Discount</label>
                    <InputNumber v-model="quotedDiscount" mode="currency" currency="USD" locale="en-US" :min="0" :minFractionDigits="2" placeholder="$0.00" fluid />
                  </div>
                </div>
                <!-- Line items -->
                <div class="line-items-section">
                  <div class="line-items-header">
                    <span class="field-label">Additional Line Items</span>
                    <Button label="Add Item" icon="mdi mdi-plus" size="small" severity="secondary" text @click="addLineItem" />
                  </div>
                  <div v-for="(item, index) in quoteLineItems" :key="index" class="line-item-row">
                    <InputText v-model="item.label" placeholder="Description" class="line-item-label" />
                    <InputNumber v-model="item.amount" mode="currency" currency="USD" locale="en-US" :min="0" :minFractionDigits="2" placeholder="$0.00" class="line-item-amount" />
                    <Button icon="mdi mdi-close" severity="danger" text size="small" @click="removeLineItem(index)" />
                  </div>
                </div>
                <div class="invoice-total-preview">
                  <span class="invoice-total-label">Estimated Total</span>
                  <span class="invoice-total-value">${{ quoteTotal.toFixed(2) }}</span>
                </div>
              </div>

              <div class="form-subsection">
                <span class="form-subsection-label"><i class="mdi mdi-printer-3d" /> Print Estimates</span>
                <div class="quote-field-grid">
                  <div class="field">
                    <label class="field-label">Est. Print Time</label>
                    <InputNumber v-model="quotedPrintTimeHours" :min="0" :maxFractionDigits="1" suffix=" h" placeholder="0 h" fluid />
                  </div>
                  <div class="field">
                    <label class="field-label">Est. Filament Usage</label>
                    <InputNumber v-model="quotedFilamentGrams" :min="0" :maxFractionDigits="1" suffix=" g" placeholder="0 g" fluid />
                  </div>
                  <div class="field">
                    <label class="field-label">Est. Lead Time</label>
                    <InputNumber v-model="quotedLeadTimeDays" :min="0" :max="365" suffix=" days" placeholder="0 days" fluid />
                  </div>
                </div>
              </div>

              <div class="form-subsection">
                <span class="form-subsection-label"><i class="mdi mdi-text-box-outline" /> Terms</span>
                <div class="quote-field-grid quote-field-grid--wide">
                  <div class="field">
                    <label class="field-label">Valid Until (optional)</label>
                    <DatePicker
                      v-model="quoteExpiresAt"
                      :min-date="new Date()"
                      date-format="M d, yy"
                      show-icon
                      icon-display="input"
                      show-button-bar
                      placeholder="No expiration"
                      fluid
                    />
                  </div>
                </div>
                <div class="field">
                  <label class="field-label">Notes (optional)</label>
                  <Textarea v-model="quoteNotes" placeholder="Additional notes for the quote…" class="field-input" rows="3" auto-resize />
                </div>
              </div>
            </div>
          </template>
        </template>

        <!-- Step 4: Design -->
        <template v-else-if="viewingStep === 4">
          <p class="step-description">Create or finalise the 3D design based on the documented requirements. Upload one or more glTF files — multiple files form an assembly, and each upload is kept as a new version.</p>

          <div v-if="order.requirements || order.description" class="design-brief-panel">
            <div class="design-brief-header">
              <i class="mdi mdi-clipboard-text-outline design-brief-icon" />
              <span>Design Brief</span>
            </div>
            <div class="design-brief-body">
              <div v-if="order.description" class="metadata-row">
                <span class="metadata-label">Request</span>
                <span class="metadata-value">{{ order.description }}</span>
              </div>
              <div v-if="order.requirements" class="metadata-row">
                <span class="metadata-label">Requirements</span>
                <span class="metadata-value">{{ order.requirements }}</span>
              </div>
            </div>
          </div>

          <div class="upload-area" :class="{ 'upload-area--active': isOnCurrentStep }">
            <i class="mdi mdi-cube-scan upload-icon" />
            <p class="upload-label">3D Design Files</p>
            <p class="upload-hint">.glb · .gltf (meters) — select multiple files to upload an assembly</p>
            <div v-if="latestPartVersion || order.mongoPartFileId" class="upload-status">
              <i class="mdi mdi-check-circle upload-done-icon" />
              <span v-if="latestPartVersion" class="upload-filename-text">
                {{ versionFiles(latestPartVersion).map((f) => f.filename).join(' · ') }}
              </span>
              <span v-else class="upload-filename-text">{{ order.partFilename }}</span>
            </div>
            <input
              ref="partFileInput"
              type="file"
              accept=".glb,.gltf"
              multiple
              style="display: none"
              @change="handlePartFileSelected"
            />
            <div class="upload-actions">
              <Button
                v-if="currentPartViewable"
                label="View 3D"
                icon="mdi mdi-rotate-3d-variant"
                severity="secondary"
                @click="handleViewCurrentPart"
              />
              <Button
                v-if="latestPartVersion || order.mongoPartFileId"
                label="Download"
                icon="pi pi-download"
                severity="secondary"
                :loading="downloadingPart"
                @click="handleDownloadPartFile"
              />
              <Button
                :label="latestPartVersion || order.mongoPartFileId ? 'Upload New Version' : 'Upload 3D Files'"
                icon="mdi mdi-upload"
                severity="secondary"
                :loading="uploading && uploadTarget === 'part'"
                :disabled="!isOnCurrentStep"
                @click="partFileInput?.click()"
              />
            </div>
          </div>

          <div v-if="isOnCurrentStep || order.designNotes" class="design-notes-section">
            <template v-if="isOnCurrentStep">
              <div class="line-items-header">
                <span class="field-label">Design Notes</span>
                <Button
                  label="Save Notes"
                  icon="mdi mdi-content-save-outline"
                  size="small"
                  severity="secondary"
                  text
                  :loading="savingDesignNotes"
                  @click="saveDesignNotes"
                />
              </div>
              <Textarea
                v-model="designNotes"
                placeholder="Dimensions, tolerances, design decisions, customer feedback…"
                class="field-input"
                rows="3"
                auto-resize
              />
            </template>
            <template v-else>
              <span class="field-label">Design Notes</span>
              <p class="design-notes-text">{{ order.designNotes }}</p>
            </template>
          </div>

          <FileVersionHistory
            :versions="partVersions"
            mode="part"
            :can-modify="isOnCurrentStep"
            @download="(v, f) => handleDownloadVersion('part', v, f)"
            @view="handleViewPartVersion"
          />
        </template>

        <!-- Step 5: Setup -->
        <template v-else-if="viewingStep === 5">
          <p class="step-description">Upload the sliced GCode files to prepare the order for printing. Each upload is kept as a new version — slice an assembly into multiple files and upload them together, then choose which file is sent to the printer.</p>
          <div class="upload-area" :class="{ 'upload-area--active': isOnCurrentStep }">
            <i class="mdi mdi-code-braces upload-icon" />
            <p class="upload-label">GCode Files</p>
            <p class="upload-hint">.gcode · .3mf — select multiple files for a sliced assembly</p>
            <div v-if="order.mongoGcodeFileId" class="upload-status">
              <i class="mdi mdi-check-circle upload-done-icon" />
              <span class="upload-filename-text">{{ order.gcodeFilename }}</span>
            </div>
            <input
              ref="gcodeFileInput"
              type="file"
              accept=".gcode,.3mf"
              multiple
              style="display: none"
              @change="handleGcodeFileSelected"
            />
            <div class="upload-actions">
              <Button
                v-if="order.mongoGcodeFileId"
                label="Download"
                icon="pi pi-download"
                severity="secondary"
                :loading="downloadingGcode"
                @click="handleDownloadGcodeFile"
              />
              <Button
                v-if="order.mongoGcodeFileId && order.gcodeFilename?.toLowerCase().endsWith('.3mf')"
                label="Preview"
                icon="mdi mdi-rotate-3d-variant"
                severity="secondary"
                @click="handlePreviewCurrentGcode"
              />
              <Button
                :label="order.mongoGcodeFileId ? 'Upload New Version' : 'Upload GCode Files'"
                icon="mdi mdi-upload"
                severity="secondary"
                :loading="uploading && uploadTarget === 'gcode'"
                :disabled="!isOnCurrentStep"
                @click="gcodeFileInput?.click()"
              />
            </div>
          </div>

          <FileVersionHistory
            :versions="gcodeVersions"
            mode="gcode"
            :can-modify="isOnCurrentStep"
            :busy-key="selectingFileKey"
            @download="(v, f) => handleDownloadVersion('gcode', v, f)"
            @preview="handlePreviewGcodeVersion"
            @select="handleSelectGcodeVersion"
            @quantity="handleUpdateQuantity"
          />

          <div v-if="order.gcodeMetadata" class="print-metadata-card">
            <div class="print-metadata-header">
              <i class="mdi mdi-information-outline print-metadata-icon" />
              <span>Print Metadata</span>
              <span v-if="order.gcodeMetadata.multiColor" class="multicolor-badge">Multi-Color</span>
              <span v-else class="singlecolor-badge">Single Color</span>
            </div>
            <div class="print-metadata-body">
              <div class="metadata-row">
                <span class="metadata-label">Material</span>
                <span class="metadata-value">{{ order.gcodeMetadata.filaments?.[0]?.type ?? '—' }}</span>
              </div>
              <div class="metadata-row">
                <span class="metadata-label">Colors</span>
                <span class="metadata-value">{{ order.gcodeMetadata.colorCount }}</span>
              </div>
              <div v-if="order.gcodeMetadata.filaments?.length" class="metadata-row">
                <span class="metadata-label">Filaments</span>
                <div class="filament-swatches">
                  <div
                    v-for="f in order.gcodeMetadata.filaments"
                    :key="f.slotIndex"
                    class="filament-swatch-item"
                    :title="`Slot ${f.slotIndex}: ${f.type} ${f.color}`"
                  >
                    <span class="color-swatch" :style="{ background: f.color }" />
                    <span class="swatch-label">{{ f.type }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- Step 6: Ready to Print -->
        <template v-else-if="viewingStep === 6">
          <p class="step-description">The order is fully prepared and awaiting a printer.</p>

          <div v-if="printPlan && printPlan.total > 1" class="print-plan-banner">
            <i class="mdi mdi-progress-check print-plan-icon" />
            <div class="print-plan-info">
              <span class="print-plan-title">
                Print plan — <strong>{{ printPlan.completed }} of {{ printPlan.total }}</strong> prints completed
                <span class="print-plan-version">v{{ printPlan.versionNumber }}</span>
              </span>
              <div class="print-plan-bar">
                <div class="print-plan-bar-fill" :style="{ width: `${(printPlan.completed / printPlan.total) * 100}%` }" />
              </div>
              <span class="print-plan-hint">Each finished print requeues the order automatically until every file and copy is done.</span>
            </div>
          </div>
          <div class="info-grid">
            <div class="info-card">
              <span class="info-label">Customer</span>
              <span class="info-value">{{ order.customerName }}</span>
            </div>
            <div class="info-card">
              <span class="info-label">Custom Design</span>
              <span class="info-value">{{ order.requiresCustomDesign ? 'Yes' : 'No' }}</span>
            </div>
            <div v-if="order.requirements" class="info-card info-card--full">
              <span class="info-label">Requirements</span>
              <span class="info-value">{{ order.requirements }}</span>
            </div>
          </div>

          <div class="queue-assignment-panel">
            <div class="queue-assignment-header">
              <i class="mdi mdi-format-list-numbered queue-assignment-icon" />
              <span>Print Queue Assignment</span>
            </div>
            <template v-if="order.assignedPrinterId">
              <div class="info-grid" style="margin-top:0.75rem">
                <div class="info-card">
                  <span class="info-label">Assigned Printer</span>
                  <span class="info-value">{{ printerNames[order.assignedPrinterId] ?? `Printer #${order.assignedPrinterId}` }}</span>
                </div>
                <div class="info-card">
                  <span class="info-label">Queue Position</span>
                  <span class="info-value">#{{ order.queuePosition }}</span>
                </div>
                <div v-if="order.assignedFilename" class="info-card info-card--full">
                  <span class="info-label">File</span>
                  <span class="info-value info-value--mono">{{ order.assignedFilename }}</span>
                </div>
              </div>
            </template>
            <template v-else>
              <p class="queue-not-assigned">
                Not yet assigned to a printer.
                <button class="queue-link" @click="router.push({ name: 'queue' })">Manage in Print Queue →</button>
              </p>
            </template>
          </div>

          <FileVersionHistory
            :versions="gcodeVersions"
            mode="gcode"
            :can-modify="isOnCurrentStep && !order.assignedPrinterId"
            :busy-key="selectingFileKey"
            @download="(v, f) => handleDownloadVersion('gcode', v, f)"
            @preview="handlePreviewGcodeVersion"
            @select="handleSelectGcodeVersion"
            @quantity="handleUpdateQuantity"
          />
        </template>

        <!-- Step 7: Printing -->
        <template v-else-if="viewingStep === 7">
          <div class="focal-display">
            <div class="focal-ring">
              <i class="mdi mdi-printer-3d-nozzle focal-icon" />
            </div>
            <p class="focal-label">
              Print job is in progress
              <template v-if="printPlan && printPlan.total > 1">
                — printing <strong>{{ order.gcodeFilename }}</strong> ({{ printPlan.completed + 1 }} of {{ printPlan.total }})
              </template>
            </p>
          </div>

          <div v-if="printPlan && printPlan.total > 1" class="print-plan-banner">
            <i class="mdi mdi-progress-check print-plan-icon" />
            <div class="print-plan-info">
              <span class="print-plan-title">
                Print plan — <strong>{{ printPlan.completed }} of {{ printPlan.total }}</strong> prints completed
                <span class="print-plan-version">v{{ printPlan.versionNumber }}</span>
              </span>
              <div class="print-plan-bar">
                <div class="print-plan-bar-fill" :style="{ width: `${(printPlan.completed / printPlan.total) * 100}%` }" />
              </div>
              <span class="print-plan-hint">When this print finishes, the order requeues automatically for the next file or copy.</span>
            </div>
          </div>
        </template>

        <!-- Step 8: Finished -->
        <template v-else-if="viewingStep === 8">
          <div class="focal-display">
            <div class="focal-ring focal-ring--done">
              <i class="mdi mdi-check focal-icon focal-icon--done" />
            </div>
            <p class="focal-label">The print job has been completed successfully.</p>
          </div>
          <div class="info-grid" style="margin-top:1rem">
            <div class="info-card">
              <span class="info-label">Customer</span>
              <span class="info-value">{{ order.customerName }}</span>
            </div>
            <div class="info-card">
              <span class="info-label">Email</span>
              <span class="info-value">{{ order.customerEmail || '—' }}</span>
            </div>
          </div>
        </template>

        <!-- Step 9: Invoice -->
        <template v-else-if="viewingStep === 9">
          <template v-if="order.status === ApiJobOrderStatus.Invoiced">
            <!-- Read-only cost summary once invoiced -->
            <div class="focal-display" style="margin-bottom:1.25rem">
              <div class="focal-ring focal-ring--done">
                <i class="mdi mdi-receipt-check focal-icon focal-icon--done" />
              </div>
              <p class="focal-label">Invoice has been generated.</p>
            </div>
            <div class="invoice-summary-card">
              <div class="invoice-summary-row" v-if="order.materialCost">
                <span class="invoice-summary-label">Material Cost</span>
                <span class="invoice-summary-value">${{ order.materialCost.toFixed(2) }}</span>
              </div>
              <div class="invoice-summary-row" v-if="order.laborCost">
                <span class="invoice-summary-label">Labor / Design Fee</span>
                <span class="invoice-summary-value">${{ order.laborCost.toFixed(2) }}</span>
              </div>
              <div class="invoice-summary-row" v-if="order.setupFee">
                <span class="invoice-summary-label">Setup Fee</span>
                <span class="invoice-summary-value">${{ order.setupFee.toFixed(2) }}</span>
              </div>
              <div class="invoice-summary-divider" />
              <div class="invoice-summary-row" v-if="order.discount && order.discount > 0">
                <span class="invoice-summary-label">Discount</span>
                <span class="invoice-summary-value invoice-summary-value--discount">-${{ order.discount.toFixed(2) }}</span>
              </div>
              <div class="invoice-summary-row invoice-summary-row--total">
                <span class="invoice-summary-label">Total</span>
                <span class="invoice-summary-value invoice-summary-value--total">
                  ${{ Math.max(0, ((order.materialCost ?? 0) + (order.laborCost ?? 0) + (order.setupFee ?? 0)) - (order.discount ?? 0)).toFixed(2) }}
                </span>
              </div>
              <div v-if="order.invoiceNotes" class="invoice-notes-section">
                <span class="invoice-summary-label">Notes</span>
                <p class="invoice-notes-text">{{ order.invoiceNotes }}</p>
              </div>
            </div>
            <div class="invoice-download-row">
              <Button
                label="Download PDF"
                icon="pi pi-download"
                :loading="downloadingInvoice"
                @click="handleDownloadInvoice"
              />
              <span v-if="order.invoicedAt" class="invoice-date-hint">
                Invoiced {{ formatDate(order.invoicedAt) }}
              </span>
            </div>
          </template>
          <template v-else>
            <!-- Cost entry form — shown when at PRINT_FINISHED viewing step 8 -->
            <p class="step-description">Enter the cost breakdown to generate the customer invoice.</p>
            <div class="form-body">
              <div class="invoice-cost-grid">
                <div class="field">
                  <label class="field-label">Material Cost</label>
                  <InputNumber v-model="invoiceMaterialCost" mode="currency" currency="USD" locale="en-US" :min="0" :minFractionDigits="2" placeholder="0.00" class="field-input" />
                </div>
                <div class="field">
                  <label class="field-label">Labor / Design Fee</label>
                  <InputNumber v-model="invoiceLaborCost" mode="currency" currency="USD" locale="en-US" :min="0" :minFractionDigits="2" placeholder="0.00" class="field-input" />
                </div>
                <div class="field">
                  <label class="field-label">Setup Fee</label>
                  <InputNumber v-model="invoiceSetupFee" mode="currency" currency="USD" locale="en-US" :min="0" :minFractionDigits="2" placeholder="0.00" class="field-input" />
                </div>
                <div class="field">
                  <label class="field-label">Discount</label>
                  <InputNumber v-model="invoiceDiscount" mode="currency" currency="USD" locale="en-US" :min="0" :minFractionDigits="2" placeholder="0.00" class="field-input" />
                </div>
              </div>
              <div class="invoice-total-preview">
                <span class="invoice-total-label">Estimated Total</span>
                <span class="invoice-total-value">${{ invoiceTotal.toFixed(2) }}</span>
              </div>
              <div class="field">
                <label class="field-label">Notes (optional)</label>
                <Textarea v-model="invoiceNotes" placeholder="Additional notes for the invoice…" class="field-input" rows="3" auto-resize />
              </div>
            </div>
          </template>
        </template>

        <!-- Workflow Transition — only visible when viewing the current step -->
        <template v-if="isOnCurrentStep">
          <div class="transition-divider" />
          <div class="transition-section">
            <span class="transition-label"><i class="mdi mdi-swap-horizontal" /> Workflow Transition</span>
            <div class="transition-btns">
              <Button
                v-if="prevStatusFor(order.status)"
                :label="`← ${statusLabel(prevStatusFor(order.status) ?? undefined)}`"
                severity="secondary"
                outlined
                size="small"
                :loading="advancing"
                @click="regressStatus"
              />
              <template v-if="order.status === ApiJobOrderStatus.Submitted">
                <Button label="Move to Review →" :loading="advancing" @click="advanceStatus(ApiJobOrderStatus.Review)" />
              </template>
              <template v-else-if="order.status === ApiJobOrderStatus.Review">
                <Button label="Complete Review →" :loading="advancing" @click="completeReview" />
              </template>
              <template v-else-if="order.status === ApiJobOrderStatus.Quoted">
                <Button
                  label="Save & Download Quote PDF"
                  icon="mdi mdi-file-download-outline"
                  severity="secondary"
                  :loading="savingQuote"
                  @click="handleSaveAndDownloadQuote"
                />
                <Button label="Accept Quote →" :loading="advancing" @click="handleAcceptQuote" />
              </template>
              <template v-else-if="order.status === ApiJobOrderStatus.Design">
                <Button label="Complete Design →" :loading="advancing" @click="completeDesign" />
              </template>
              <template v-else-if="order.status === ApiJobOrderStatus.Setup">
                <span v-if="!order.mongoGcodeFileId" class="transition-info">
                  <i class="mdi mdi-alert-circle-outline" /> Upload a GCode or 3MF file to continue
                </span>
                <Button
                  label="Mark as Ready to Print →"
                  :loading="advancing"
                  :disabled="!order.mongoGcodeFileId"
                  @click="advanceStatus(ApiJobOrderStatus.ReadyToPrint)"
                />
              </template>
              <template v-else-if="order.status === ApiJobOrderStatus.ReadyToPrint">
                <span class="transition-info">Waiting for printer assignment…</span>
              </template>
              <template v-else-if="order.status === ApiJobOrderStatus.Printing">
                <span class="transition-info">Print in progress — managed by printer</span>
              </template>
              <template v-else-if="order.status === ApiJobOrderStatus.PrintFinished">
                <Button
                  label="Create Invoice →"
                  icon="mdi mdi-receipt-text-outline"
                  :loading="advancing"
                  @click="viewingStep = 9"
                />
              </template>
              <template v-else-if="order.status === ApiJobOrderStatus.Invoiced">
                <Button
                  label="Download PDF"
                  icon="pi pi-download"
                  severity="secondary"
                  :loading="downloadingInvoice"
                  @click="handleDownloadInvoice"
                />
              </template>
            </div>
          </div>
        </template>

        <!-- Invoice Generate button — shown in step 9 only when not yet invoiced -->
        <template v-if="viewingStep === 9 && order.status !== ApiJobOrderStatus.Invoiced && isOnCurrentStep">
          <div class="transition-divider" />
          <div class="transition-section">
            <span class="transition-label"><i class="mdi mdi-receipt-text-outline" /> Generate Invoice</span>
            <div class="transition-btns">
              <Button
                label="Generate Invoice & Mark as Invoiced"
                icon="mdi mdi-receipt-send-outline"
                :loading="advancing"
                @click="handleGenerateInvoice"
              />
            </div>
          </div>
        </template>
      </div>
    </template>

    <!-- Upload new version dialog -->
    <Dialog
      v-model:visible="showUploadDialog"
      modal
      :header="uploadTarget === 'part' ? 'Upload 3D Design Version' : 'Upload GCode Version'"
      :style="{ width: 'min(440px, 92vw)' }"
      :closable="!uploading"
      @hide="cancelUpload"
    >
      <div class="upload-dialog-body">
        <div v-for="file in pendingUploadFiles" :key="file.name" class="upload-dialog-file">
          <i class="mdi mdi-file-outline" />
          <span class="upload-dialog-filename">{{ file.name }}</span>
        </div>
        <p v-if="uploadTarget === 'part' && pendingUploadFiles.length > 1" class="upload-dialog-note">
          These {{ pendingUploadFiles.length }} files will be stored together as one assembly version.
        </p>
        <div class="field">
          <label class="field-label">Description of changes</label>
          <Textarea
            v-model="uploadDescription"
            placeholder="What changed in this version?"
            class="field-input"
            rows="3"
            auto-resize
          />
        </div>
      </div>
      <template #footer>
        <Button label="Cancel" severity="secondary" text :disabled="uploading" @click="cancelUpload" />
        <Button label="Upload" icon="mdi mdi-upload" :loading="uploading" @click="confirmUpload" />
      </template>
    </Dialog>

    <Teleport to="body">
      <PrintJobInfoDialog
        v-if="gcodeViewer"
        :filename="gcodeViewer.filename"
        :fetch-file="gcodeViewer.fetchFile"
        @close="gcodeViewer = null"
      />
      <ModelViewerDialog
        v-if="modelViewer"
        :files="modelViewer"
        @close="modelViewer = null"
      />
    </Teleport>
  </div>
</template>

<style scoped>
@keyframes fade-up {
  from { opacity: 0; transform: translateY(14px); }
  to   { opacity: 1; transform: translateY(0); }
}
@keyframes spin { to { transform: rotate(360deg); } }

.job-order-detail {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  max-width: 1100px;
  margin: 0 auto;
  animation: fade-up 0.3s ease-out both;
}

/* ── Loading ─────────────────────────────────────────────── */
.page-loading {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 3rem 0;
  color: var(--ph-text-muted);
  font-size: 0.9rem;
}
.page-spinner {
  width: 1.25rem;
  height: 1.25rem;
  border: 2px solid var(--ph-border);
  border-top-color: var(--ph-accent);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

/* ── Header ──────────────────────────────────────────────── */
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
.back-btn { flex-shrink: 0; }
.order-name {
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--ph-text);
  margin: 0 0 0.25rem;
  line-height: 1.2;
}
.order-meta { display: flex; gap: 0.5rem; flex-wrap: wrap; }
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
.status-tag { font-size: 0.8rem; }

/* ── Section card ────────────────────────────────────────── */
.section-card {
  background: var(--ph-glass);
  backdrop-filter: blur(var(--ph-blur));
  -webkit-backdrop-filter: blur(var(--ph-blur));
  border: 1px solid var(--ph-glass-border);
  border-radius: 16px;
  padding: 1.25rem;
  box-shadow: var(--ph-shadow-card), 0 1px 0 rgba(255, 255, 255, 0.04) inset;
  animation: ph-fade-up 0.35s cubic-bezier(0.16, 1, 0.3, 1) both;
  transition: border-color 0.25s;
}

.section-card:hover {
  border-color: rgba(34, 211, 238, 0.18);
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
.view-only-badge {
  margin-left: 0.25rem;
  font-size: 0.65rem;
  font-weight: 500;
  padding: 0.1rem 0.4rem;
  border-radius: 4px;
  background: rgba(96, 165, 250, 0.1);
  border: 1px solid rgba(96, 165, 250, 0.25);
  color: #93c5fd;
  text-transform: lowercase;
  letter-spacing: 0;
}

/* ── Timeline ────────────────────────────────────────────── */
.timeline {
  display: flex;
  align-items: flex-start;
  overflow-x: auto;
}
.timeline-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.4rem;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  flex-shrink: 0;
  transition: opacity 0.15s;
}
.timeline-step:hover { opacity: 0.75; }
.timeline-node {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 600;
  border: 2px solid var(--ph-border);
  color: var(--ph-text-muted);
  background: transparent;
  transition: all 0.2s;
}
.timeline-step--done .timeline-node {
  background: rgba(74, 222, 128, 0.12);
  border-color: rgba(74, 222, 128, 0.5);
  color: #4ade80;
}
.timeline-step--current .timeline-node {
  background: rgba(34, 211, 238, 0.12);
  border-color: var(--ph-accent);
  color: var(--ph-accent);
}
.timeline-step--future .timeline-node {
  opacity: 0.45;
}
.timeline-step--viewing .timeline-node {
  outline: 2px solid rgba(255,255,255,0.35);
  outline-offset: 2px;
}
.timeline-step--viewing.timeline-step--current .timeline-node {
  outline-color: var(--ph-accent);
}
.timeline-step--viewing.timeline-step--done .timeline-node {
  outline-color: rgba(74, 222, 128, 0.55);
}
.timeline-label {
  font-size: 0.68rem;
  color: var(--ph-text-muted);
  text-align: center;
  white-space: nowrap;
}
.timeline-step--current .timeline-label,
.timeline-step--viewing .timeline-label {
  color: var(--ph-text);
  font-weight: 600;
}
.timeline-connector {
  flex: 1;
  min-width: 0.75rem;
  height: 2px;
  background: var(--ph-border);
  margin-top: 1.25rem;
  align-self: flex-start;
}
.timeline-connector--done { background: rgba(74, 222, 128, 0.4); }

/* ── Browse notice ───────────────────────────────────────── */
.browse-notice {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.6rem 1rem;
  background: rgba(96, 165, 250, 0.07);
  border: 1px solid rgba(96, 165, 250, 0.18);
  border-radius: 8px;
  font-size: 0.825rem;
  color: #93c5fd;
}
.browse-notice > i { font-size: 0.9rem; flex-shrink: 0; }
.browse-jump-btn {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-size: 0.775rem;
  background: rgba(96, 165, 250, 0.1);
  border: 1px solid rgba(96, 165, 250, 0.28);
  border-radius: 6px;
  color: #93c5fd;
  padding: 0.25rem 0.625rem;
  cursor: pointer;
  transition: background 0.15s;
  white-space: nowrap;
}
.browse-jump-btn:hover { background: rgba(96, 165, 250, 0.18); }

/* ── Step content ────────────────────────────────────────── */
.step-description {
  font-size: 0.875rem;
  color: var(--ph-text-muted);
  margin: 0 0 1rem;
  line-height: 1.6;
}

/* ── Info grid ───────────────────────────────────────────── */
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.625rem; }
@media (max-width: 600px) {
  .info-grid { grid-template-columns: 1fr; }
}
.info-card {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  padding: 0.75rem 0.875rem;
  background: rgba(255,255,255,0.03);
  border: 1px solid var(--ph-border);
  border-radius: 8px;
}
.info-card--full { grid-column: 1 / -1; }
.info-label {
  font-size: 0.68rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--ph-text-muted);
}
.info-value { font-size: 0.875rem; color: var(--ph-text); line-height: 1.5; }
.info-value--mono { font-family: monospace; font-size: 0.8rem; }

/* ── Queue assignment panel ──────────────────────────────── */
.queue-assignment-panel {
  margin-top: 1rem;
  padding: 0.875rem 1rem;
  background: rgba(6, 182, 212, 0.06);
  border: 1px solid rgba(6, 182, 212, 0.2);
  border-radius: 8px;
}
.queue-assignment-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  font-weight: 600;
  color: #06b6d4;
  margin-bottom: 0.25rem;
}
.queue-assignment-icon { font-size: 1rem; }
.queue-not-assigned {
  margin: 0.5rem 0 0;
  font-size: 0.875rem;
  color: var(--ph-text-muted);
}
.queue-link {
  background: none;
  border: none;
  color: #06b6d4;
  cursor: pointer;
  font-size: 0.875rem;
  padding: 0;
  text-decoration: underline;
}
.queue-link:hover { color: #22d3ee; }

/* ── Print metadata card ─────────────────────────────────── */
.print-metadata-card {
  margin-top: 1rem;
  padding: 0.875rem 1rem;
  background: rgba(6, 182, 212, 0.05);
  border: 1px solid rgba(6, 182, 212, 0.18);
  border-radius: 8px;
}
.print-metadata-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  font-weight: 600;
  color: #06b6d4;
  margin-bottom: 0.75rem;
}
.print-metadata-icon { font-size: 1rem; }
.multicolor-badge, .singlecolor-badge {
  margin-left: auto;
  font-size: 0.65rem;
  font-weight: 500;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}
.multicolor-badge {
  background: rgba(251, 191, 36, 0.12);
  border: 1px solid rgba(251, 191, 36, 0.3);
  color: #fbbf24;
}
.singlecolor-badge {
  background: rgba(74, 222, 128, 0.1);
  border: 1px solid rgba(74, 222, 128, 0.25);
  color: #4ade80;
}
.print-metadata-body { display: flex; flex-direction: column; gap: 0.5rem; }
.metadata-row { display: flex; align-items: flex-start; gap: 0.75rem; font-size: 0.825rem; }
.metadata-label {
  min-width: 70px;
  font-size: 0.68rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--ph-text-muted);
  padding-top: 0.1rem;
}
.metadata-value { color: var(--ph-text); }
.filament-swatches { display: flex; flex-wrap: wrap; gap: 0.5rem; }
.filament-swatch-item {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.25rem 0.5rem;
  background: rgba(255,255,255,0.04);
  border: 1px solid var(--ph-border);
  border-radius: 6px;
  font-size: 0.775rem;
  color: var(--ph-text);
}
.color-swatch {
  width: 0.875rem;
  height: 0.875rem;
  border-radius: 50%;
  border: 1px solid rgba(255,255,255,0.2);
  flex-shrink: 0;
}
.swatch-label { font-size: 0.75rem; }

/* ── Form ────────────────────────────────────────────────── */
.form-body { display: flex; flex-direction: column; gap: 1rem; }
.field { display: flex; flex-direction: column; gap: 0.375rem; }
.field-label { font-size: 0.8rem; font-weight: 500; color: var(--ph-text-muted); }
.field-input { width: 100%; }
.checkbox-field { flex-direction: row; align-items: center; gap: 0.625rem; }
.checkbox-label { margin: 0; cursor: pointer; font-size: 0.875rem; color: var(--ph-text); }

/* ── Upload area ─────────────────────────────────────────── */
.upload-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 2rem 1.5rem;
  border: 2px dashed var(--ph-border);
  border-radius: 12px;
  background: rgba(255,255,255,0.015);
  transition: border-color 0.2s, background 0.2s;
}
.upload-area--active {
  border-color: rgba(34, 211, 238, 0.3);
  background: rgba(34, 211, 238, 0.04);
}
.upload-icon { font-size: 2.25rem; color: var(--ph-text-muted); }
.upload-area--active .upload-icon { color: var(--ph-accent); }
.upload-label { font-size: 0.9rem; font-weight: 600; color: var(--ph-text); margin: 0; }
.upload-hint { font-size: 0.775rem; color: var(--ph-text-muted); margin: 0 0 0.375rem; }
.upload-status { display: flex; align-items: center; gap: 0.375rem; font-size: 0.825rem; color: #4ade80; margin-bottom: 0.25rem; }
.upload-done-icon { font-size: 1rem; }
.upload-filename-text { font-family: monospace; font-size: 0.8rem; word-break: break-all; }
.upload-actions { display: flex; gap: 0.5rem; flex-wrap: wrap; justify-content: center; }

/* ── Focal display ───────────────────────────────────────── */
@keyframes pulse-ring {
  0%   { box-shadow: 0 0 0 0   rgba(34, 211, 238, 0.35); }
  70%  { box-shadow: 0 0 0 14px rgba(34, 211, 238, 0); }
  100% { box-shadow: 0 0 0 0   rgba(34, 211, 238, 0); }
}
@keyframes pop-in {
  0%   { transform: scale(0.5); opacity: 0; }
  70%  { transform: scale(1.12); }
  100% { transform: scale(1); opacity: 1; }
}
.focal-display { display: flex; flex-direction: column; align-items: center; gap: 1rem; padding: 1.5rem 0 0.75rem; }
.focal-ring {
  width: 5rem; height: 5rem;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  background: rgba(34, 211, 238, 0.1);
  border: 2px solid rgba(34, 211, 238, 0.3);
  animation: pulse-ring 2s ease-out infinite;
}
.focal-ring--done {
  background: rgba(74, 222, 128, 0.1);
  border-color: rgba(74, 222, 128, 0.35);
  animation: none;
}
.focal-ring--done .focal-icon--done { animation: pop-in 0.5s cubic-bezier(0.16, 1, 0.3, 1) both; }
.focal-icon { font-size: 2rem; color: var(--ph-accent); }
.focal-icon--done { color: #4ade80; }
.focal-label { font-size: 0.875rem; color: var(--ph-text-muted); margin: 0; text-align: center; }

/* ── Workflow Transition ─────────────────────────────────── */
.transition-divider {
  height: 1px;
  background: var(--ph-border);
  margin: 1.25rem 0 1rem;
}
.transition-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}
.transition-label {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--ph-text-muted);
}
.transition-btns { display: flex; align-items: center; gap: 0.625rem; flex-wrap: wrap; }
.transition-info { font-size: 0.8125rem; color: var(--ph-text-muted); font-style: italic; }

/* ── Invoice cost grid ───────────────────────────────────── */
.invoice-cost-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}
@media (max-width: 600px) {
  .invoice-cost-grid { grid-template-columns: 1fr; }
}
.invoice-total-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  background: rgba(6, 182, 212, 0.06);
  border: 1px solid rgba(6, 182, 212, 0.2);
  border-radius: 10px;
}
.invoice-total-label {
  font-size: 0.8rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--ph-text-muted);
}
.invoice-total-value {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--ph-accent);
}

/* ── Invoice summary (read-only) ─────────────────────────── */
.invoice-summary-card {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  background: rgba(255,255,255,0.03);
  border: 1px solid var(--ph-border);
  border-radius: 12px;
  padding: 1rem 1.25rem;
  max-width: 480px;
  margin-top: 0.75rem;
}
.invoice-summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.875rem;
}
.invoice-summary-row--total {
  margin-top: 0.25rem;
}
.invoice-summary-label {
  color: var(--ph-text-muted);
  font-size: 0.825rem;
}
.invoice-summary-value { color: var(--ph-text); font-weight: 500; }
.invoice-summary-value--discount { color: #f87171; }
.invoice-summary-value--total {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--ph-accent);
}
.invoice-summary-divider {
  height: 1px;
  background: var(--ph-border);
  margin: 0.25rem 0;
}
.invoice-notes-section {
  margin-top: 0.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.invoice-notes-text {
  margin: 0;
  font-size: 0.8rem;
  color: var(--ph-text);
  white-space: pre-wrap;
}
.invoice-download-row {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 1.25rem;
}
.invoice-date-hint {
  font-size: 0.775rem;
  color: var(--ph-text-muted);
  font-style: italic;
}

.line-items-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.line-items-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.line-item-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.line-item-label { flex: 1; min-width: 0; }
.line-item-amount { width: 160px; flex-shrink: 0; }

.unit-cost-row {
  display: grid;
  grid-template-columns: 1fr auto 110px auto 1fr;
  gap: 0.625rem;
  align-items: end;
}
.unit-cost-op {
  padding-bottom: 0.55rem;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--ph-text-muted);
  user-select: none;
}
@media (max-width: 640px) {
  .unit-cost-row { grid-template-columns: 1fr; }
  .unit-cost-op { display: none; }
}

/* ── Quote form subsections ──────────────────────────────── */
.form-subsection {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
  padding: 1rem;
  background: rgba(255,255,255,0.02);
  border: 1px solid var(--ph-border);
  border-radius: 10px;
}
.form-subsection-label {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.7rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--ph-text-muted);
}
.form-subsection-label > i { font-size: 0.85rem; color: var(--ph-accent); }
.quote-field-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.75rem;
}
.quote-field-grid--wide { grid-template-columns: repeat(2, 1fr); }
@media (max-width: 640px) {
  .quote-field-grid, .quote-field-grid--wide { grid-template-columns: 1fr; }
}

/* ── Design step ─────────────────────────────────────────── */
.design-brief-panel {
  margin-bottom: 1rem;
  padding: 0.875rem 1rem;
  background: rgba(6, 182, 212, 0.05);
  border: 1px solid rgba(6, 182, 212, 0.18);
  border-radius: 8px;
}
.design-brief-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  font-weight: 600;
  color: #06b6d4;
  margin-bottom: 0.75rem;
}
.design-brief-icon { font-size: 1rem; }
.design-brief-body { display: flex; flex-direction: column; gap: 0.5rem; }
.design-brief-body .metadata-label { min-width: 100px; }
.design-notes-section {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  margin-top: 1rem;
}
.design-notes-text {
  margin: 0;
  padding: 0.75rem 0.875rem;
  background: rgba(255,255,255,0.03);
  border: 1px solid var(--ph-border);
  border-radius: 8px;
  font-size: 0.875rem;
  color: var(--ph-text);
  line-height: 1.5;
  white-space: pre-wrap;
}

/* ── Upload version dialog ───────────────────────────────── */
/* ── Print plan banner ───────────────────────────────────── */
.print-plan-banner {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.85rem 1rem;
  margin-bottom: 1rem;
  border: 1px solid rgba(34, 211, 238, 0.25);
  border-radius: 10px;
  background: rgba(34, 211, 238, 0.05);
}
.print-plan-icon {
  color: var(--ph-accent);
  font-size: 1.2rem;
  margin-top: 0.1rem;
}
.print-plan-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  min-width: 0;
}
.print-plan-title {
  font-size: 0.85rem;
  color: var(--ph-text);
}
.print-plan-title strong { color: var(--ph-accent); }
.print-plan-version {
  margin-left: 0.4rem;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.08rem 0.4rem;
  border-radius: 999px;
  background: rgba(255,255,255,0.06);
  border: 1px solid var(--ph-border);
  color: var(--ph-text-muted);
}
.print-plan-bar {
  height: 6px;
  border-radius: 999px;
  background: rgba(255,255,255,0.08);
  overflow: hidden;
}
.print-plan-bar-fill {
  height: 100%;
  border-radius: 999px;
  background: var(--ph-accent);
  transition: width 0.4s ease;
}
.print-plan-hint {
  font-size: 0.72rem;
  color: var(--ph-text-muted);
}

.upload-dialog-body {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}
.upload-dialog-body > .field { margin-top: 0.4rem; }
.upload-dialog-note {
  margin: 0;
  font-size: 0.75rem;
  color: var(--ph-text-muted);
}
.upload-dialog-file {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 0.75rem;
  background: rgba(255,255,255,0.04);
  border: 1px solid var(--ph-border);
  border-radius: 8px;
  color: var(--ph-text);
  font-size: 0.85rem;
}
.upload-dialog-file > i { color: var(--ph-accent); flex-shrink: 0; }
.upload-dialog-filename {
  font-family: monospace;
  font-size: 0.8rem;
  word-break: break-all;
}

.unit-cost-qty :deep(.p-inputnumber-input) { text-align: center; }
</style>
