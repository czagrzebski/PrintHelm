<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Button from 'primevue/button'
import Tag from 'primevue/tag'
import Toast from 'primevue/toast'
import Textarea from 'primevue/textarea'
import Checkbox from 'primevue/checkbox'
import { useToast } from 'primevue/usetoast'
import {
  type ApiJobOrderResponse,
  type ApiUpdateJobOrderRequest,
  ApiJobOrderStatus,
} from '@/client/printhelm-web-openapi'
import jobOrderApi, { uploadPartFile, uploadGcodeFile, downloadPartFile, downloadGcodeFile, fetchGcodeFileBuffer } from '@/api/JobOrderApi'
import { printerApi } from '@/api/PrinterApi'
import PrintJobInfoDialog from '@/components/PrintJobInfoDialog.vue'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const orderId = Number(route.params.id)

const STEPS = [
  { value: 1, label: 'Submitted',      icon: 'mdi mdi-inbox-arrow-down' },
  { value: 2, label: 'Review',         icon: 'mdi mdi-clipboard-text-outline' },
  { value: 3, label: 'Design',         icon: 'mdi mdi-cube-outline' },
  { value: 4, label: 'Setup',          icon: 'mdi mdi-file-cog-outline' },
  { value: 5, label: 'Ready to Print', icon: 'mdi mdi-printer-check' },
  { value: 6, label: 'Printing',       icon: 'mdi mdi-printer-3d-nozzle' },
  { value: 7, label: 'Finished',       icon: 'mdi mdi-check-decagram' },
]

const STATUS_STEP: Record<string, number> = {
  [ApiJobOrderStatus.Submitted]:    1,
  [ApiJobOrderStatus.Review]:       2,
  [ApiJobOrderStatus.Design]:       3,
  [ApiJobOrderStatus.Setup]:        4,
  [ApiJobOrderStatus.ReadyToPrint]: 5,
  [ApiJobOrderStatus.Printing]:     6,
  [ApiJobOrderStatus.PrintFinished]:7,
}

const STATUS_META: Record<string, { label: string; severity: string }> = {
  [ApiJobOrderStatus.Submitted]:    { label: 'Submitted',      severity: 'secondary' },
  [ApiJobOrderStatus.Review]:       { label: 'Review',         severity: 'warn' },
  [ApiJobOrderStatus.Design]:       { label: 'Design',         severity: 'info' },
  [ApiJobOrderStatus.Setup]:        { label: 'Setup',          severity: 'info' },
  [ApiJobOrderStatus.ReadyToPrint]: { label: 'Ready to Print', severity: 'contrast' },
  [ApiJobOrderStatus.Printing]:     { label: 'Printing',       severity: 'success' },
  [ApiJobOrderStatus.PrintFinished]:{ label: 'Print Finished', severity: 'success' },
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

async function fetchOrder() {
  loading.value = true
  try {
    const res = await jobOrderApi.getJobOrderById(orderId)
    order.value = res.data
    viewingStep.value = STATUS_STEP[res.data.status ?? ApiJobOrderStatus.Submitted] ?? 1
    reviewRequirements.value = res.data.requirements ?? ''
    reviewRequiresCustomDesign.value = res.data.requiresCustomDesign ?? false
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
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to update order.', life: 4000 })
  } finally {
    advancing.value = false
  }
}

function prevStatusFor(current?: ApiJobOrderStatus): ApiJobOrderStatus | null {
  switch (current) {
    case ApiJobOrderStatus.Review:       return ApiJobOrderStatus.Submitted
    case ApiJobOrderStatus.Design:       return ApiJobOrderStatus.Review
    case ApiJobOrderStatus.Setup:        return order.value?.requiresCustomDesign
                                           ? ApiJobOrderStatus.Design : ApiJobOrderStatus.Review
    case ApiJobOrderStatus.ReadyToPrint: return ApiJobOrderStatus.Setup
    default: return null
  }
}

async function regressStatus() {
  const prev = prevStatusFor(order.value?.status)
  if (prev) await advanceStatus(prev)
}

function completeReview() {
  const next = reviewRequiresCustomDesign.value
    ? ApiJobOrderStatus.Design : ApiJobOrderStatus.Setup
  advanceStatus(next, {
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

        <!-- Step 3: Design -->
        <template v-else-if="viewingStep === 3">
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

        <!-- Step 4: Setup -->
        <template v-else-if="viewingStep === 4">
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

        <!-- Step 5: Ready to Print -->
        <template v-else-if="viewingStep === 5">
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

        <!-- Step 6: Printing -->
        <template v-else-if="viewingStep === 6">
          <div class="focal-display">
            <div class="focal-ring">
              <i class="mdi mdi-printer-3d-nozzle focal-icon" />
            </div>
            <p class="focal-label">Print job is in progress</p>
          </div>
        </template>

        <!-- Step 7: Finished -->
        <template v-else-if="viewingStep === 7">
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
</style>
