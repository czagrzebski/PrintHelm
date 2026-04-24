<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import Textarea from 'primevue/textarea'
import Tag from 'primevue/tag'
import Toast from 'primevue/toast'
import { useToast } from 'primevue/usetoast'
import {
  type ApiJobOrderResponse,
  ApiJobOrderStatus,
} from '@/client/printhelm-web-openapi'
import jobOrderApi from '@/api/JobOrderApi'

const router = useRouter()
const toast = useToast()

// ── Status helpers ────────────────────────────────────────────────────────────

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
                title="View workflow" @click="router.push({ name: 'job-order', params: { id: data.orderId } })" />
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
/* ── Page ──────────────────────────────────────────────────────── */
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
.page-title { font-size: 1.5rem; font-weight: 700; margin: 0 0 0.25rem; color: var(--ph-text); }
.page-subtitle { font-size: 0.875rem; color: var(--ph-text-muted); margin: 0; }

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

/* ── Create form ────────────────────────────────────────────────── */
.form-body { display: flex; flex-direction: column; gap: 1rem; padding: 0.25rem 0; }
.field { display: flex; flex-direction: column; gap: 0.375rem; }
.field-label { font-size: 0.8rem; font-weight: 500; color: var(--ph-text-muted); }
.field-input { width: 100%; }
.required { color: #f87171; }

/* ── Delete confirm ─────────────────────────────────────────────── */
.confirm-body { display: flex; align-items: flex-start; gap: 1rem; padding: 0.5rem 0; }
.confirm-icon { font-size: 2rem; color: #f87171; flex-shrink: 0; }
.confirm-body p { margin: 0; font-size: 0.9rem; line-height: 1.5; color: var(--ph-text); }
</style>
