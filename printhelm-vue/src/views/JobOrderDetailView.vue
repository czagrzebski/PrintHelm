<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Toast from 'primevue/toast'
import Textarea from 'primevue/textarea'
import Checkbox from 'primevue/checkbox'
import InputNumber from 'primevue/inputnumber'
import InputText from 'primevue/inputtext'
import MultiSelect from 'primevue/multiselect'
import { useToast } from 'primevue/usetoast'
import {
  type ApiJobOrderResponse,
  type ApiUpdateJobOrderRequest,
  ApiJobOrderStatus,
} from '@/client/printhelm-web-openapi'
import jobOrderApi, { uploadPartFile, uploadGcodeFile, downloadPartFile, downloadGcodeFile, fetchGcodeFileBuffer, downloadInvoice, downloadQuote } from '@/api/JobOrderApi'
import { printerApi } from '@/api/PrinterApi'
import PrintJobInfoDialog from '@/components/PrintJobInfoDialog.vue'

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
const uploadingPart = ref(false)
const uploadingGcode = ref(false)
const downloadingPart = ref(false)
const downloadingGcode = ref(false)
const showGcodeViewer = ref(false)

function fetchGcodeForViewer(): Promise<ArrayBuffer> {
  return fetchGcodeFileBuffer(orderId)
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

const quoteMaterials     = ref<string[]>([])
const quotedMaterialCost = ref<number | null>(null)
const quotedCostPerUnit  = ref<number | null>(null)
const quotedQuantity     = ref<number | null>(null)
const quotedLaborCost    = ref<number | null>(null)
const quotedSetupFee     = ref<number | null>(null)
const quotedDiscount     = ref<number | null>(null)
const quoteNotes         = ref('')
const quoteExpiresAt     = ref('')
const savingQuote        = ref(false)
const downloadingQuote   = ref(false)

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
  quoteMaterials.value     = data.quoteMaterials     ?? []
  quotedMaterialCost.value = data.quotedMaterialCost ?? null
  quotedCostPerUnit.value  = data.quotedCostPerUnit  ?? null
  quotedQuantity.value     = data.quotedQuantity     ?? null
  quotedLaborCost.value    = data.quotedLaborCost    ?? null
  quotedSetupFee.value     = data.quotedSetupFee     ?? null
  quotedDiscount.value     = data.quotedDiscount     ?? null
  quoteNotes.value         = data.quoteNotes         ?? ''
  quoteExpiresAt.value     = data.quoteExpiresAt     ?? ''
  quoteLineItems.value     = (data.quoteLineItems ?? []).map(i => ({ label: i.label ?? '', amount: i.amount ?? null }))
}

function addLineItem() {
  quoteLineItems.value.push({ label: '', amount: null })
}

function removeLineItem(index: number) {
  quoteLineItems.value.splice(index, 1)
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

async function handlePartFileUpload(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file || !order.value?.orderId) return
  uploadingPart.value = true
  try {
    const res = await uploadPartFile(order.value.orderId, file)
    order.value = res.data
    toast.add({ severity: 'success', summary: 'Uploaded', detail: '3D file uploaded successfully.', life: 3000 })
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to upload 3D file.', life: 4000 })
  } finally {
    uploadingPart.value = false
    if (partFileInput.value) partFileInput.value.value = ''
  }
}

async function handleGcodeFileUpload(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file || !order.value?.orderId) return
  uploadingGcode.value = true
  try {
    const res = await uploadGcodeFile(order.value.orderId, file)
    order.value = res.data
    toast.add({ severity: 'success', summary: 'Uploaded', detail: 'GCode file uploaded successfully.', life: 3000 })
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to upload GCode file.', life: 4000 })
  } finally {
    uploadingGcode.value = false
    if (gcodeFileInput.value) gcodeFileInput.value.value = ''
  }
}

async function handleDownloadPartFile() {
  if (!order.value?.orderId || !order.value.partFilename) return
  downloadingPart.value = true
  try {
    await downloadPartFile(order.value.orderId, order.value.partFilename)
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to download 3D file.', life: 4000 })
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
      quoteMaterials:     quoteMaterials.value.length > 0 ? quoteMaterials.value : undefined,
      quotedMaterialCost: quotedMaterialCost.value ?? undefined,
      quotedCostPerUnit:  quotedCostPerUnit.value  ?? undefined,
      quotedQuantity:     quotedQuantity.value     ?? undefined,
      quotedLaborCost:    quotedLaborCost.value    ?? undefined,
      quotedSetupFee:     quotedSetupFee.value     ?? undefined,
      quotedDiscount:     quotedDiscount.value     ?? undefined,
      quoteNotes:         quoteNotes.value.trim()  || undefined,
      quoteExpiresAt:     quoteExpiresAt.value     || undefined,
      quoteLineItems:     validLineItems.length > 0 ? validLineItems : undefined,
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

onMounted(() => { fetchOrder(); fetchPrinterNames() })
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
              <div class="invoice-summary-row" v-if="order.quotedMaterialCost">
                <span class="invoice-summary-label">Est. Material Cost</span>
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
            <p class="step-description">Enter the estimated costs for this job to generate a quote for the customer.</p>
            <div class="form-body">
              <div class="field">
                <label class="field-label">Materials</label>
                <MultiSelect
                  v-model="quoteMaterials"
                  :options="MATERIAL_OPTIONS"
                  placeholder="Select material types…"
                  display="chip"
                  class="field-input"
                />
              </div>
              <div class="unit-cost-row">
                <div class="field">
                  <label class="field-label">Cost Per Unit</label>
                  <InputNumber v-model="quotedCostPerUnit" mode="currency" currency="USD" locale="en-US" :min="0" :minFractionDigits="2" placeholder="0.00" class="field-input" />
                </div>
                <div class="field unit-cost-qty">
                  <label class="field-label">Quantity</label>
                  <InputNumber v-model="quotedQuantity" :min="1" :max="99999" placeholder="1" class="field-input" />
                </div>
                <div class="field">
                  <label class="field-label">Est. Material Cost</label>
                  <InputNumber v-model="quotedMaterialCost" mode="currency" currency="USD" locale="en-US" :min="0" :minFractionDigits="2" placeholder="0.00" class="field-input" />
                </div>
              </div>
              <div class="invoice-cost-grid">
                <div class="field">
                  <label class="field-label">Est. Labor / Design Fee</label>
                  <InputNumber v-model="quotedLaborCost" mode="currency" currency="USD" locale="en-US" :min="0" :minFractionDigits="2" placeholder="0.00" class="field-input" />
                </div>
                <div class="field">
                  <label class="field-label">Est. Setup Fee</label>
                  <InputNumber v-model="quotedSetupFee" mode="currency" currency="USD" locale="en-US" :min="0" :minFractionDigits="2" placeholder="0.00" class="field-input" />
                </div>
                <div class="field">
                  <label class="field-label">Discount</label>
                  <InputNumber v-model="quotedDiscount" mode="currency" currency="USD" locale="en-US" :min="0" :minFractionDigits="2" placeholder="0.00" class="field-input" />
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
                  <InputNumber v-model="item.amount" mode="currency" currency="USD" locale="en-US" :min="0" :minFractionDigits="2" placeholder="0.00" class="line-item-amount" />
                  <Button icon="mdi mdi-close" severity="danger" text size="small" @click="removeLineItem(index)" />
                </div>
              </div>
              <div class="invoice-total-preview">
                <span class="invoice-total-label">Estimated Total</span>
                <span class="invoice-total-value">${{ quoteTotal.toFixed(2) }}</span>
              </div>
              <div class="field">
                <label class="field-label">Valid Until (optional)</label>
                <input
                  v-model="quoteExpiresAt"
                  type="date"
                  class="field-input date-input"
                  :min="new Date().toISOString().split('T')[0]"
                />
              </div>
              <div class="field">
                <label class="field-label">Notes (optional)</label>
                <Textarea v-model="quoteNotes" placeholder="Additional notes for the quote…" class="field-input" rows="3" auto-resize />
              </div>
            </div>
          </template>
        </template>

        <!-- Step 4: Design -->
        <template v-else-if="viewingStep === 4">
          <p class="step-description">Create or finalise the 3D design file based on the documented requirements.</p>
          <div class="upload-area" :class="{ 'upload-area--active': isOnCurrentStep }">
            <i class="mdi mdi-cube-scan upload-icon" />
            <p class="upload-label">3D Design File</p>
            <p class="upload-hint">.stl · .obj · .sldprt · .sldasm · .zip</p>
            <div v-if="order.mongoPartFileId" class="upload-status">
              <i class="mdi mdi-check-circle upload-done-icon" />
              <span class="upload-filename-text">{{ order.partFilename }}</span>
            </div>
            <input
              ref="partFileInput"
              type="file"
              accept=".stl,.obj,.sldprt,.sldasm,.zip"
              style="display: none"
              @change="handlePartFileUpload"
            />
            <div class="upload-actions">
              <Button
                v-if="order.mongoPartFileId"
                label="Download"
                icon="pi pi-download"
                severity="secondary"
                :loading="downloadingPart"
                @click="handleDownloadPartFile"
              />
              <Button
                :label="order.mongoPartFileId ? 'Replace 3D File' : 'Upload 3D File'"
                icon="mdi mdi-upload"
                severity="secondary"
                :loading="uploadingPart"
                :disabled="!isOnCurrentStep"
                @click="partFileInput?.click()"
              />
            </div>
          </div>
        </template>

        <!-- Step 5: Setup -->
        <template v-else-if="viewingStep === 5">
          <p class="step-description">Upload the sliced GCode file to prepare the order for printing.</p>
          <div class="upload-area" :class="{ 'upload-area--active': isOnCurrentStep }">
            <i class="mdi mdi-code-braces upload-icon" />
            <p class="upload-label">GCode File</p>
            <p class="upload-hint">.gcode · .3mf</p>
            <div v-if="order.mongoGcodeFileId" class="upload-status">
              <i class="mdi mdi-check-circle upload-done-icon" />
              <span class="upload-filename-text">{{ order.gcodeFilename }}</span>
            </div>
            <input
              ref="gcodeFileInput"
              type="file"
              accept=".gcode,.3mf"
              style="display: none"
              @change="handleGcodeFileUpload"
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
                @click="showGcodeViewer = true"
              />
              <Button
                :label="order.mongoGcodeFileId ? 'Replace GCode File' : 'Upload GCode File'"
                icon="mdi mdi-upload"
                severity="secondary"
                :loading="uploadingGcode"
                :disabled="!isOnCurrentStep"
                @click="gcodeFileInput?.click()"
              />
            </div>
          </div>

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
        </template>

        <!-- Step 7: Printing -->
        <template v-else-if="viewingStep === 7">
          <div class="focal-display">
            <div class="focal-ring">
              <i class="mdi mdi-printer-3d-nozzle focal-icon" />
            </div>
            <p class="focal-label">Print job is in progress</p>
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
                <Button label="Complete Design →" :loading="advancing" @click="advanceStatus(ApiJobOrderStatus.Setup)" />
              </template>
              <template v-else-if="order.status === ApiJobOrderStatus.Setup">
                <Button label="Mark as Ready to Print →" :loading="advancing" @click="advanceStatus(ApiJobOrderStatus.ReadyToPrint)" />
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

    <Teleport to="body">
      <PrintJobInfoDialog
        v-if="showGcodeViewer && order?.gcodeFilename"
        :filename="order.gcodeFilename"
        :fetch-file="fetchGcodeForViewer"
        @close="showGcodeViewer = false"
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

/* ── Date input ──────────────────────────────────────────── */
.date-input {
  background: rgba(255,255,255,0.05);
  border: 1px solid var(--ph-border);
  border-radius: 6px;
  padding: 0.5rem 0.75rem;
  color: var(--ph-text);
  font-size: 0.875rem;
  color-scheme: dark;
}
.date-input:focus {
  outline: none;
  border-color: var(--ph-accent);
}

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
  grid-template-columns: 1fr 100px 1fr;
  gap: 0.75rem;
}

.unit-cost-qty :deep(.p-inputnumber-input) { text-align: center; }
</style>
