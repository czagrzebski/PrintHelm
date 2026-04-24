<script setup lang="ts">
import { ref, onMounted } from 'vue'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Checkbox from 'primevue/checkbox'
import Tag from 'primevue/tag'
import Toast from 'primevue/toast'
import Stepper from 'primevue/stepper'
import StepList from 'primevue/steplist'
import Step from 'primevue/step'
import StepPanels from 'primevue/steppanels'
import StepPanel from 'primevue/steppanel'
import { useToast } from 'primevue/usetoast'
import {
  type ApiJobOrderResponse,
  type ApiUpdateJobOrderRequest,
  ApiJobOrderStatus,
} from '@/client/printhelm-web-openapi'
import jobOrderApi from '@/api/JobOrderApi'

const toast = useToast()

// ── Status helpers ────────────────────────────────────────────────────────────

const STATUS_STEP: Record<string, number> = {
  [ApiJobOrderStatus.Submitted]: 1,
  [ApiJobOrderStatus.Review]: 2,
  [ApiJobOrderStatus.Design]: 3,
  [ApiJobOrderStatus.Setup]: 4,
  [ApiJobOrderStatus.ReadyToPrint]: 5,
  [ApiJobOrderStatus.Printing]: 6,
  [ApiJobOrderStatus.PrintFinished]: 7,
}

const STATUS_META: Record<string, { label: string; severity: string }> = {
  [ApiJobOrderStatus.Submitted]:    { label: 'Submitted',       severity: 'secondary' },
  [ApiJobOrderStatus.Review]:       { label: 'Review',          severity: 'warn' },
  [ApiJobOrderStatus.Design]:       { label: 'Design',          severity: 'info' },
  [ApiJobOrderStatus.Setup]:        { label: 'Setup',           severity: 'info' },
  [ApiJobOrderStatus.ReadyToPrint]: { label: 'Ready to Print',  severity: 'contrast' },
  [ApiJobOrderStatus.Printing]:     { label: 'Printing',        severity: 'success' },
  [ApiJobOrderStatus.PrintFinished]:{ label: 'Print Finished',  severity: 'success' },
}

function statusLabel(status?: ApiJobOrderStatus) {
  return status ? (STATUS_META[status]?.label ?? status) : '—'
}
function statusSeverity(status?: ApiJobOrderStatus) {
  return status ? (STATUS_META[status]?.severity ?? 'secondary') : 'secondary'
}

// ── Orders list ───────────────────────────────────────────────────────────────

const orders = ref<ApiJobOrderResponse[]>([])
const loading = ref(true)

async function fetchOrders() {
  loading.value = true
  try {
    const res = await jobOrderApi.getJobOrders()
    orders.value = res.data
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to load job orders.', life: 4000 })
  } finally {
    loading.value = false
  }
}

// ── Create dialog ─────────────────────────────────────────────────────────────

const showCreateDialog = ref(false)
const creating = ref(false)
const createForm = ref({ customerName: '', customerEmail: '', description: '' })

function openCreate() {
  createForm.value = { customerName: '', customerEmail: '', description: '' }
  showCreateDialog.value = true
}

async function submitCreate() {
  if (!createForm.value.customerName.trim()) {
    toast.add({ severity: 'warn', summary: 'Validation', detail: 'Customer name is required.', life: 4000 })
    return
  }
  creating.value = true
  try {
    await jobOrderApi.createJobOrder({
      customerName: createForm.value.customerName.trim(),
      customerEmail: createForm.value.customerEmail.trim() || undefined,
      description: createForm.value.description.trim() || undefined,
    })
    toast.add({ severity: 'success', summary: 'Created', detail: 'Job order created.', life: 3000 })
    showCreateDialog.value = false
    await fetchOrders()
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to create job order.', life: 4000 })
  } finally {
    creating.value = false
  }
}

// ── Workflow (stepper) dialog ─────────────────────────────────────────────────

const showWorkflowDialog = ref(false)
const selectedOrder = ref<ApiJobOrderResponse | null>(null)
const activeViewStep = ref<number>(1)
const advancing = ref(false)

// Review step local state
const reviewRequirements = ref('')
const reviewRequiresCustomDesign = ref(false)

function openWorkflow(order: ApiJobOrderResponse) {
  selectedOrder.value = order
  activeViewStep.value = STATUS_STEP[order.status ?? ApiJobOrderStatus.Submitted] ?? 1
  reviewRequirements.value = order.requirements ?? ''
  reviewRequiresCustomDesign.value = order.requiresCustomDesign ?? false
  showWorkflowDialog.value = true
}

function isCurrentStep(step: number): boolean {
  if (!selectedOrder.value?.status) return false
  return STATUS_STEP[selectedOrder.value.status] === step
}

async function advanceStatus(nextStatus: ApiJobOrderStatus, extra?: Partial<ApiUpdateJobOrderRequest>) {
  if (!selectedOrder.value?.orderId) return
  advancing.value = true
  try {
    const res = await jobOrderApi.updateJobOrder(selectedOrder.value.orderId, {
      status: nextStatus,
      ...extra,
    })
    selectedOrder.value = res.data
    activeViewStep.value = STATUS_STEP[nextStatus] ?? activeViewStep.value
    await fetchOrders()
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to update job order.', life: 4000 })
  } finally {
    advancing.value = false
  }
}

function completeReview() {
  const next = reviewRequiresCustomDesign.value
    ? ApiJobOrderStatus.Design
    : ApiJobOrderStatus.Setup
  advanceStatus(next, {
    requirements: reviewRequirements.value.trim() || undefined,
    requiresCustomDesign: reviewRequiresCustomDesign.value,
  })
}

// ── Delete dialog ─────────────────────────────────────────────────────────────

const showDeleteDialog = ref(false)
const orderToDelete = ref<ApiJobOrderResponse | null>(null)
const deleting = ref(false)

async function confirmDelete() {
  if (!orderToDelete.value?.orderId) return
  deleting.value = true
  try {
    await jobOrderApi.deleteJobOrder(orderToDelete.value.orderId)
    toast.add({ severity: 'success', summary: 'Deleted', detail: 'Job order removed.', life: 3000 })
    showDeleteDialog.value = false
    orderToDelete.value = null
    await fetchOrders()
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to delete job order.', life: 4000 })
  } finally {
    deleting.value = false
  }
}

function formatDate(iso?: string) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString()
}

onMounted(fetchOrders)
</script>

<template>
  <div class="job-order-view">
    <Toast />
    <div class="page-header">
      <div>
        <h2 class="page-title">Job Orders</h2>
        <p class="page-subtitle">Manage customer print job orders</p>
      </div>
      <Button label="New Order" icon="mdi mdi-plus" @click="openCreate" />
    </div>

    <div class="table-card">
      <DataTable :value="orders" :loading="loading" data-key="orderId"
        empty-message="No job orders yet." size="small">
        <Column field="orderId" header="ID" style="min-width:60px;max-width:80px" />
        <Column field="customerName" header="Customer" style="min-width:150px" />
        <Column header="Email" style="min-width:180px">
          <template #body="{ data }">
            <span :class="{ muted: !data.customerEmail }">{{ data.customerEmail || '—' }}</span>
          </template>
        </Column>
        <Column header="Status" style="min-width:140px">
          <template #body="{ data }">
            <Tag :value="statusLabel(data.status)" :severity="statusSeverity(data.status)" />
          </template>
        </Column>
        <Column header="Created" style="min-width:160px">
          <template #body="{ data }">{{ formatDate(data.createdAt) }}</template>
        </Column>
        <Column header="Actions" style="min-width:110px">
          <template #body="{ data }">
            <div class="row-actions">
              <Button icon="mdi mdi-eye-outline" severity="secondary" text size="small"
                title="View workflow" @click="openWorkflow(data)" />
              <Button icon="mdi mdi-trash-can-outline" severity="danger" text size="small"
                title="Delete" @click="orderToDelete = data; showDeleteDialog = true" />
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <!-- Create Dialog -->
    <Dialog v-model:visible="showCreateDialog" header="New Job Order"
      modal :style="{ width: '520px' }" :closable="!creating">
      <div class="form-body">
        <div class="field">
          <label class="field-label">Customer Name <span class="required">*</span></label>
          <InputText v-model="createForm.customerName" placeholder="e.g. Jane Smith" class="field-input" />
        </div>
        <div class="field">
          <label class="field-label">Customer Email</label>
          <InputText v-model="createForm.customerEmail" placeholder="e.g. jane@example.com" class="field-input" />
        </div>
        <div class="field">
          <label class="field-label">Description</label>
          <Textarea v-model="createForm.description" placeholder="Describe the print job…"
            class="field-input" rows="3" auto-resize />
        </div>
      </div>
      <template #footer>
        <Button label="Cancel" severity="secondary" text :disabled="creating" @click="showCreateDialog = false" />
        <Button label="Create Order" :loading="creating" @click="submitCreate" />
      </template>
    </Dialog>

    <!-- Workflow Dialog -->
    <Dialog v-model:visible="showWorkflowDialog"
      :header="`Order #${selectedOrder?.orderId} — ${selectedOrder?.customerName}`"
      modal :style="{ width: '780px' }" :closable="!advancing">
      <Stepper v-model:value="activeViewStep" class="workflow-stepper">
        <StepList>
          <Step :value="1">Submitted</Step>
          <Step :value="2">Review</Step>
          <Step :value="3">Design</Step>
          <Step :value="4">Setup</Step>
          <Step :value="5">Ready to Print</Step>
          <Step :value="6">Printing</Step>
          <Step :value="7">Print Finished</Step>
        </StepList>

        <StepPanels>
          <!-- Step 1: Submitted -->
          <StepPanel :value="1">
            <div class="step-content">
              <h3 class="step-title">Submission Details</h3>
              <div class="info-grid">
                <div class="info-item">
                  <span class="info-label">Customer</span>
                  <span class="info-value">{{ selectedOrder?.customerName || '—' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">Email</span>
                  <span class="info-value">{{ selectedOrder?.customerEmail || '—' }}</span>
                </div>
                <div class="info-item info-item--full">
                  <span class="info-label">Description</span>
                  <span class="info-value">{{ selectedOrder?.description || '—' }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">Submitted</span>
                  <span class="info-value">{{ formatDate(selectedOrder?.createdAt) }}</span>
                </div>
              </div>
            </div>
            <div v-if="isCurrentStep(1)" class="step-actions">
              <Button label="Move to Review" icon="pi pi-arrow-right" icon-pos="right"
                :loading="advancing" @click="advanceStatus(ApiJobOrderStatus.Review)" />
            </div>
          </StepPanel>

          <!-- Step 2: Review -->
          <StepPanel :value="2">
            <div class="step-content">
              <h3 class="step-title">Review</h3>
              <p class="step-description">Evaluate the submission and document requirements. Determine whether a custom design or a 3D file was provided.</p>
              <div class="form-body">
                <div class="field">
                  <label class="field-label">Requirements</label>
                  <Textarea
                    v-model="reviewRequirements"
                    :disabled="!isCurrentStep(2)"
                    placeholder="Document the requirements for this print job…"
                    class="field-input"
                    rows="4"
                    auto-resize />
                </div>
                <div class="field checkbox-field">
                  <Checkbox
                    v-model="reviewRequiresCustomDesign"
                    :disabled="!isCurrentStep(2)"
                    input-id="requiresCustomDesign"
                    :binary="true" />
                  <label for="requiresCustomDesign" class="field-label checkbox-label">
                    Requires custom design
                  </label>
                </div>
                <div v-if="!isCurrentStep(2) && selectedOrder?.requirements" class="info-grid">
                  <div class="info-item info-item--full">
                    <span class="info-label">Recorded Requirements</span>
                    <span class="info-value">{{ selectedOrder.requirements }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="isCurrentStep(2)" class="step-actions">
              <Button label="Complete Review" icon="pi pi-arrow-right" icon-pos="right"
                :loading="advancing" @click="completeReview" />
            </div>
          </StepPanel>

          <!-- Step 3: Design -->
          <StepPanel :value="3">
            <div class="step-content">
              <h3 class="step-title">Design</h3>
              <p class="step-description">Create or finalise the 3D design file based on the documented requirements.</p>
              <div class="upload-area">
                <i class="mdi mdi-cube-outline upload-icon" />
                <p class="upload-label">Upload 3D Design File</p>
                <p class="upload-hint">Accepted formats: .stl, .obj, .sldprt, .sldasm, .zip</p>
                <Button label="Upload 3D File" icon="mdi mdi-upload" severity="secondary"
                  disabled title="File upload coming soon" />
              </div>
            </div>
            <div v-if="isCurrentStep(3)" class="step-actions">
              <Button label="Complete Design" icon="pi pi-arrow-right" icon-pos="right"
                :loading="advancing" @click="advanceStatus(ApiJobOrderStatus.Setup)" />
            </div>
          </StepPanel>

          <!-- Step 4: Setup -->
          <StepPanel :value="4">
            <div class="step-content">
              <h3 class="step-title">Setup</h3>
              <p class="step-description">Upload the sliced GCode file to prepare the order for printing.</p>
              <div class="upload-area">
                <i class="mdi mdi-file-cog-outline upload-icon" />
                <p class="upload-label">Upload GCode File</p>
                <p class="upload-hint">Accepted formats: .gcode.3mf</p>
                <Button label="Upload GCode File" icon="mdi mdi-upload" severity="secondary"
                  disabled title="File upload coming soon" />
              </div>
            </div>
            <div v-if="isCurrentStep(4)" class="step-actions">
              <Button label="Mark as Ready to Print" icon="pi pi-arrow-right" icon-pos="right"
                :loading="advancing" @click="advanceStatus(ApiJobOrderStatus.ReadyToPrint)" />
            </div>
          </StepPanel>

          <!-- Step 5: Ready to Print -->
          <StepPanel :value="5">
            <div class="step-content">
              <h3 class="step-title">Ready to Print</h3>
              <p class="step-description">The order is prepared and waiting to be sent to a printer.</p>
              <div class="info-grid">
                <div class="info-item">
                  <span class="info-label">Customer</span>
                  <span class="info-value">{{ selectedOrder?.customerName }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">Custom Design</span>
                  <span class="info-value">{{ selectedOrder?.requiresCustomDesign ? 'Yes' : 'No' }}</span>
                </div>
                <div v-if="selectedOrder?.requirements" class="info-item info-item--full">
                  <span class="info-label">Requirements</span>
                  <span class="info-value">{{ selectedOrder.requirements }}</span>
                </div>
              </div>
            </div>
            <div v-if="isCurrentStep(5)" class="step-actions">
              <Button label="Start Printing" icon="pi pi-arrow-right" icon-pos="right"
                :loading="advancing" @click="advanceStatus(ApiJobOrderStatus.Printing)" />
            </div>
          </StepPanel>

          <!-- Step 6: Printing -->
          <StepPanel :value="6">
            <div class="step-content">
              <h3 class="step-title">Printing</h3>
              <div class="status-display">
                <i class="mdi mdi-printer-3d-nozzle status-icon status-icon--active" />
                <p class="status-text">Print job is in progress</p>
              </div>
            </div>
            <div v-if="isCurrentStep(6)" class="step-actions">
              <Button label="Mark as Finished" icon="pi pi-check" icon-pos="right"
                :loading="advancing" @click="advanceStatus(ApiJobOrderStatus.PrintFinished)" />
            </div>
          </StepPanel>

          <!-- Step 7: Print Finished -->
          <StepPanel :value="7">
            <div class="step-content">
              <h3 class="step-title">Print Finished</h3>
              <div class="status-display">
                <i class="mdi mdi-check-circle-outline status-icon status-icon--done" />
                <p class="status-text">The print job has been completed successfully.</p>
              </div>
              <div class="info-grid">
                <div class="info-item">
                  <span class="info-label">Customer</span>
                  <span class="info-value">{{ selectedOrder?.customerName }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">Email</span>
                  <span class="info-value">{{ selectedOrder?.customerEmail || '—' }}</span>
                </div>
              </div>
            </div>
          </StepPanel>
        </StepPanels>
      </Stepper>
    </Dialog>

    <!-- Delete Dialog -->
    <Dialog v-model:visible="showDeleteDialog" header="Delete Job Order"
      modal :style="{ width: '420px' }" :closable="!deleting">
      <div class="confirm-body">
        <i class="mdi mdi-alert-circle-outline confirm-icon" />
        <p>Delete order for <strong>{{ orderToDelete?.customerName }}</strong>? This cannot be undone.</p>
      </div>
      <template #footer>
        <Button label="Cancel" severity="secondary" text :disabled="deleting"
          @click="showDeleteDialog = false" />
        <Button label="Delete" severity="danger" :loading="deleting" @click="confirmDelete" />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.job-order-view {
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

.page-title {
  font-size: 1.5rem; font-weight: 700; margin: 0 0 0.25rem; color: var(--ph-text);
}

.page-subtitle {
  font-size: 0.875rem; color: var(--ph-text-muted); margin: 0;
}

.table-card {
  background: #162830;
  border: 1px solid var(--ph-border);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.25);
  animation: fade-up 0.35s cubic-bezier(0.16, 1, 0.3, 1) 0.1s both;
}

.muted { color: var(--ph-text-muted); }

.row-actions { display: flex; gap: 0.25rem; }

/* Create / workflow form */
.form-body {
  display: flex; flex-direction: column; gap: 1rem; padding: 0.25rem 0;
}

.field { display: flex; flex-direction: column; gap: 0.375rem; }
.field-label { font-size: 0.8rem; font-weight: 500; color: var(--ph-text-muted); }
.field-input { width: 100%; }
.required { color: #f87171; }

.checkbox-field { flex-direction: row; align-items: center; gap: 0.625rem; }
.checkbox-label { margin: 0; cursor: pointer; font-size: 0.875rem; color: var(--ph-text); }

/* Stepper dialog */
.workflow-stepper {
  width: 100%;
}

.step-content {
  padding: 1.25rem 0 0.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
  min-height: 200px;
}

.step-title {
  font-size: 1rem;
  font-weight: 600;
  margin: 0;
  color: var(--ph-text);
}

.step-description {
  font-size: 0.875rem;
  color: var(--ph-text-muted);
  margin: 0;
  line-height: 1.5;
}

.step-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 1.25rem;
}

/* Info grid */
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.info-item--full {
  grid-column: 1 / -1;
}

.info-label {
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--ph-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.info-value {
  font-size: 0.875rem;
  color: var(--ph-text);
}

/* Upload area */
.upload-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 2rem;
  border: 2px dashed var(--ph-border);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.02);
}

.upload-icon {
  font-size: 2.5rem;
  color: var(--ph-text-muted);
}

.upload-label {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--ph-text);
  margin: 0;
}

.upload-hint {
  font-size: 0.8rem;
  color: var(--ph-text-muted);
  margin: 0 0 0.5rem;
}

/* Status display */
.status-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  padding: 1.5rem 0;
}

.status-icon {
  font-size: 3rem;
}

.status-icon--active { color: #22d3ee; }
.status-icon--done   { color: #4ade80; }

.status-text {
  font-size: 0.9rem;
  color: var(--ph-text-muted);
  margin: 0;
}

/* Delete confirm */
.confirm-body {
  display: flex; align-items: flex-start; gap: 1rem; padding: 0.5rem 0;
}
.confirm-icon { font-size: 2rem; color: #f87171; flex-shrink: 0; }
.confirm-body p { margin: 0; font-size: 0.9rem; line-height: 1.5; color: var(--ph-text); }
</style>
