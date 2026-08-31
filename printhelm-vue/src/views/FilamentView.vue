<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import Button from 'primevue/button'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import InputNumber from 'primevue/inputnumber'
import Textarea from 'primevue/textarea'
import Select from 'primevue/select'
import Tag from 'primevue/tag'
import Toast from 'primevue/toast'
import { useToast } from 'primevue/usetoast'
import filamentApi from '@/api/FilamentApi'
import { printerApi } from '@/api/PrinterApi'
import type { ApiFilamentSpool, ApiFilamentSpoolRequest } from '@/client/printhelm-web-openapi'
import { FilamentSpoolStatus } from '@/client/printhelm-web-openapi'

const toast = useToast()

const spools = ref<ApiFilamentSpool[]>([])
const loading = ref(true)
const printers = ref<{ label: string; value: number }[]>([])

const showDialog = ref(false)
const editMode = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)

const showDeleteDialog = ref(false)
const spoolToDelete = ref<ApiFilamentSpool | null>(null)
const deleting = ref(false)

const materialOptions = ['PLA', 'PETG', 'ABS', 'ASA', 'TPU', 'PC', 'PA', 'PVA', 'HIPS'].map((m) => ({
  label: m,
  value: m,
}))

const statusOptions = [
  { label: 'Active', value: FilamentSpoolStatus.Active },
  { label: 'Empty', value: FilamentSpoolStatus.Empty },
  { label: 'Archived', value: FilamentSpoolStatus.Archived },
]

interface SpoolForm {
  name: string
  brand: string
  material: string
  colorHex: string
  colorName: string
  diameter: number
  initialWeightGrams: number | null
  remainingWeightGrams: number | null
  spoolCost: number | null
  lowStockThresholdGrams: number
  status: (typeof FilamentSpoolStatus)[keyof typeof FilamentSpoolStatus]
  assignedPrinterId: number | null
  amsSlot: number | null
  notes: string
}

const emptyForm = (): SpoolForm => ({
  name: '',
  brand: '',
  material: 'PLA',
  colorHex: '#22D3EE',
  colorName: '',
  diameter: 1.75,
  initialWeightGrams: 1000,
  remainingWeightGrams: null,
  spoolCost: null,
  lowStockThresholdGrams: 200,
  status: FilamentSpoolStatus.Active,
  assignedPrinterId: null,
  amsSlot: null,
  notes: '',
})

const form = ref<SpoolForm>(emptyForm())

const activeSpools = computed(() => spools.value.filter((s) => s.status === FilamentSpoolStatus.Active))
const lowStockCount = computed(() => activeSpools.value.filter((s) => s.lowStock).length)
const totalRemainingKg = computed(() =>
  (activeSpools.value.reduce((sum, s) => sum + (s.remainingWeightGrams ?? 0), 0) / 1000).toFixed(1),
)

async function fetchSpools() {
  loading.value = true
  try {
    const res = await filamentApi.getFilamentSpools()
    spools.value = res.data
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to load filament spools', life: 4000 })
  } finally {
    loading.value = false
  }
}

async function fetchPrinters() {
  try {
    const res = await printerApi.getPrinters()
    printers.value = (res.data ?? []).map((p) => ({
      label: p.printerName ?? `Printer ${p.printerId}`,
      value: Number(p.printerId),
    }))
  } catch {
    printers.value = []
  }
}

function openCreate() {
  editMode.value = false
  editingId.value = null
  form.value = emptyForm()
  showDialog.value = true
}

function openEdit(spool: ApiFilamentSpool) {
  editMode.value = true
  editingId.value = spool.spoolId ?? null
  form.value = {
    name: spool.name ?? '',
    brand: spool.brand ?? '',
    material: spool.material ?? 'PLA',
    colorHex: spool.colorHex ?? '#FFFFFF',
    colorName: spool.colorName ?? '',
    diameter: spool.diameter ?? 1.75,
    initialWeightGrams: spool.initialWeightGrams ?? null,
    remainingWeightGrams: spool.remainingWeightGrams ?? null,
    spoolCost: spool.spoolCost ?? null,
    lowStockThresholdGrams: spool.lowStockThresholdGrams ?? 200,
    status: spool.status ?? FilamentSpoolStatus.Active,
    assignedPrinterId: spool.assignedPrinterId != null ? Number(spool.assignedPrinterId) : null,
    amsSlot: spool.amsSlot ?? null,
    notes: spool.notes ?? '',
  }
  showDialog.value = true
}

async function submitSpool() {
  if (!form.value.name.trim() || !form.value.material || !form.value.initialWeightGrams) {
    toast.add({ severity: 'warn', summary: 'Missing fields', detail: 'Name, material and initial weight are required', life: 4000 })
    return
  }
  saving.value = true
  const payload: ApiFilamentSpoolRequest = {
    name: form.value.name.trim(),
    brand: form.value.brand || undefined,
    material: form.value.material,
    colorHex: normalizeHex(form.value.colorHex),
    colorName: form.value.colorName || undefined,
    diameter: form.value.diameter,
    initialWeightGrams: form.value.initialWeightGrams,
    remainingWeightGrams: form.value.remainingWeightGrams ?? undefined,
    spoolCost: form.value.spoolCost ?? undefined,
    lowStockThresholdGrams: form.value.lowStockThresholdGrams,
    status: form.value.status,
    assignedPrinterId: form.value.assignedPrinterId ?? undefined,
    amsSlot: form.value.amsSlot ?? undefined,
    notes: form.value.notes || undefined,
  }
  try {
    if (editMode.value && editingId.value != null) {
      await filamentApi.updateFilamentSpool(editingId.value, payload)
      toast.add({ severity: 'success', summary: 'Saved', detail: 'Spool updated', life: 3000 })
    } else {
      await filamentApi.createFilamentSpool(payload)
      toast.add({ severity: 'success', summary: 'Added', detail: 'Spool added to inventory', life: 3000 })
    }
    showDialog.value = false
    await fetchSpools()
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to save spool', life: 4000 })
  } finally {
    saving.value = false
  }
}

function askDelete(spool: ApiFilamentSpool) {
  spoolToDelete.value = spool
  showDeleteDialog.value = true
}

async function confirmDelete() {
  if (spoolToDelete.value?.spoolId == null) return
  deleting.value = true
  try {
    await filamentApi.deleteFilamentSpool(spoolToDelete.value.spoolId)
    toast.add({ severity: 'success', summary: 'Deleted', detail: 'Spool removed', life: 3000 })
    showDeleteDialog.value = false
    await fetchSpools()
  } catch {
    toast.add({ severity: 'error', summary: 'Error', detail: 'Failed to delete spool', life: 4000 })
  } finally {
    deleting.value = false
  }
}

function normalizeHex(hex: string): string | undefined {
  if (!hex) return undefined
  const value = hex.startsWith('#') ? hex : `#${hex}`
  return /^#[0-9a-fA-F]{6}$/.test(value) ? value.toUpperCase() : undefined
}

function remainingPercent(spool: ApiFilamentSpool): number {
  if (!spool.initialWeightGrams || spool.initialWeightGrams <= 0) return 0
  return Math.min(100, Math.max(0, ((spool.remainingWeightGrams ?? 0) / spool.initialWeightGrams) * 100))
}

function remainingBarColor(spool: ApiFilamentSpool): string {
  if (spool.lowStock) return '#e66767'
  return '#0ea5c6'
}

function statusSeverity(status?: string) {
  switch (status) {
    case FilamentSpoolStatus.Active: return 'success'
    case FilamentSpoolStatus.Empty: return 'danger'
    default: return 'secondary'
  }
}

function grams(value?: number | null): string {
  return value != null ? `${Math.round(value)}g` : '—'
}

onMounted(() => {
  fetchSpools()
  fetchPrinters()
})
</script>

<template>
  <div class="filament-view">
    <Toast />
    <div class="page-header">
      <div>
        <h2 class="page-title">Filament Inventory</h2>
        <p class="page-subtitle">Track spools, remaining weight and low-stock alerts</p>
      </div>
      <Button label="Add Spool" icon="mdi mdi-plus" @click="openCreate" />
    </div>

    <div class="summary-row">
      <div class="summary-tile">
        <span class="summary-value">{{ activeSpools.length }}</span>
        <span class="summary-label">Active spools</span>
      </div>
      <div class="summary-tile">
        <span class="summary-value">{{ totalRemainingKg }} kg</span>
        <span class="summary-label">Filament on hand</span>
      </div>
      <div class="summary-tile" :class="{ 'summary-warn': lowStockCount > 0 }">
        <span class="summary-value">
          <i v-if="lowStockCount > 0" class="mdi mdi-alert-outline warn-icon" />
          {{ lowStockCount }}
        </span>
        <span class="summary-label">Low stock</span>
      </div>
    </div>

    <div class="table-card">
      <DataTable :value="spools" :loading="loading" data-key="spoolId" size="small" scrollable
        empty-message="No spools yet — add your first spool to start tracking filament.">
        <Column header="Spool" style="min-width:200px">
          <template #body="{ data }">
            <div class="spool-cell">
              <span class="color-swatch" :style="{ background: data.colorHex ?? '#666' }" />
              <div class="spool-names">
                <span class="spool-name">{{ data.name }}</span>
                <span class="spool-sub muted">
                  {{ [data.brand, data.colorName].filter(Boolean).join(' · ') || '&nbsp;' }}
                </span>
              </div>
            </div>
          </template>
        </Column>
        <Column field="material" header="Material" style="min-width:90px" />
        <Column header="Remaining" style="min-width:220px">
          <template #body="{ data }">
            <div class="remaining-cell">
              <div class="remaining-bar">
                <div class="remaining-fill"
                  :style="{ width: remainingPercent(data) + '%', background: remainingBarColor(data) }" />
              </div>
              <span class="remaining-text">
                {{ grams(data.remainingWeightGrams) }} / {{ grams(data.initialWeightGrams) }}
                <i v-if="data.lowStock" class="mdi mdi-alert-outline low-icon" title="Below low-stock threshold" />
              </span>
            </div>
          </template>
        </Column>
        <Column header="Status" style="min-width:100px">
          <template #body="{ data }">
            <Tag :value="data.status" :severity="statusSeverity(data.status)" />
          </template>
        </Column>
        <Column header="Printer" style="min-width:140px">
          <template #body="{ data }">
            <span v-if="data.assignedPrinterName">
              {{ data.assignedPrinterName }}
              <span v-if="data.amsSlot != null" class="muted"> · Slot {{ data.amsSlot + 1 }}</span>
            </span>
            <span v-else class="muted">Unassigned</span>
          </template>
        </Column>
        <Column header="Actions" style="min-width:110px">
          <template #body="{ data }">
            <div class="row-actions">
              <Button icon="mdi mdi-pencil-outline" severity="secondary" text size="small"
                title="Edit spool" @click="openEdit(data)" />
              <Button icon="mdi mdi-trash-can-outline" severity="danger" text size="small"
                title="Delete spool" @click="askDelete(data)" />
            </div>
          </template>
        </Column>
      </DataTable>
    </div>

    <Dialog v-model:visible="showDialog" :header="editMode ? 'Edit Spool' : 'Add Spool'"
      modal :style="{ width: 'min(520px, 92vw)' }" :draggable="false">
      <div class="form-body">
        <div class="field-grid">
          <div class="field">
            <label class="field-label">Name <span class="required">*</span></label>
            <InputText v-model="form.name" placeholder="e.g. Bambu PLA Basic Black" class="field-input" />
          </div>
          <div class="field">
            <label class="field-label">Brand</label>
            <InputText v-model="form.brand" placeholder="e.g. Bambu Lab" class="field-input" />
          </div>
          <div class="field">
            <label class="field-label">Material <span class="required">*</span></label>
            <Select v-model="form.material" :options="materialOptions" option-label="label" option-value="value"
              editable class="field-input" />
          </div>
          <div class="field">
            <label class="field-label">Diameter (mm)</label>
            <InputNumber v-model="form.diameter" :min-fraction-digits="2" :max-fraction-digits="2" class="field-input" />
          </div>
          <div class="field">
            <label class="field-label">Color</label>
            <div class="color-field">
              <input v-model="form.colorHex" type="color" class="color-input" />
              <InputText v-model="form.colorHex" placeholder="#RRGGBB" class="field-input" />
            </div>
          </div>
          <div class="field">
            <label class="field-label">Color name</label>
            <InputText v-model="form.colorName" placeholder="e.g. Charcoal" class="field-input" />
          </div>
          <div class="field">
            <label class="field-label">Initial weight (g) <span class="required">*</span></label>
            <InputNumber v-model="form.initialWeightGrams" :min="1" class="field-input" />
          </div>
          <div class="field">
            <label class="field-label">Remaining weight (g)</label>
            <InputNumber v-model="form.remainingWeightGrams" :min="0" class="field-input"
              :placeholder="editMode ? '' : 'Defaults to initial weight'" />
          </div>
          <div class="field">
            <label class="field-label">Low-stock threshold (g)</label>
            <InputNumber v-model="form.lowStockThresholdGrams" :min="0" class="field-input" />
          </div>
          <div class="field">
            <label class="field-label">Cost ($)</label>
            <InputNumber v-model="form.spoolCost" mode="decimal" :min-fraction-digits="2" :max-fraction-digits="2"
              class="field-input" />
          </div>
          <div class="field">
            <label class="field-label">Assigned printer</label>
            <Select v-model="form.assignedPrinterId" :options="printers" option-label="label" option-value="value"
              show-clear placeholder="Unassigned" class="field-input" />
          </div>
          <div class="field">
            <label class="field-label">AMS slot (1–4)</label>
            <InputNumber :model-value="form.amsSlot != null ? form.amsSlot + 1 : null"
              @update:model-value="(v: number | null) => (form.amsSlot = v != null ? v - 1 : null)"
              :min="1" :max="4" show-buttons class="field-input" />
          </div>
          <div v-if="editMode" class="field">
            <label class="field-label">Status</label>
            <Select v-model="form.status" :options="statusOptions" option-label="label" option-value="value"
              class="field-input" />
          </div>
        </div>
        <div class="field">
          <label class="field-label">Notes</label>
          <Textarea v-model="form.notes" rows="2" auto-resize class="field-input" />
        </div>
      </div>
      <template #footer>
        <Button label="Cancel" severity="secondary" text :disabled="saving" @click="showDialog = false" />
        <Button :label="editMode ? 'Save Changes' : 'Add Spool'" :loading="saving" @click="submitSpool" />
      </template>
    </Dialog>

    <Dialog v-model:visible="showDeleteDialog" header="Delete Spool" modal :style="{ width: 'min(420px, 92vw)' }" :draggable="false">
      <div class="confirm-body">
        <i class="mdi mdi-alert-circle-outline confirm-icon" />
        <p>
          Delete spool <strong>{{ spoolToDelete?.name }}</strong>?
          Usage history stays intact, but the spool is removed from inventory.
        </p>
      </div>
      <template #footer>
        <Button label="Cancel" severity="secondary" text :disabled="deleting" @click="showDeleteDialog = false" />
        <Button label="Delete" severity="danger" :loading="deleting" @click="confirmDelete" />
      </template>
    </Dialog>
  </div>
</template>

<style scoped>
.filament-view {
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

.summary-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 1rem;
  animation: fade-up 0.32s ease-out 0.05s both;
}

.summary-tile {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding: 1rem 1.25rem;
  background: var(--ph-glass);
  border: 1px solid var(--ph-glass-border);
  border-radius: 14px;
  box-shadow: var(--ph-shadow-card);
}
.summary-value { font-size: 1.5rem; font-weight: 700; color: var(--ph-text); }
.summary-label { font-size: 0.8rem; color: var(--ph-text-muted); }
.summary-warn .summary-value { color: #e66767; }
.warn-icon { font-size: 1.2rem; margin-right: 0.25rem; }

.table-card {
  background: var(--ph-glass);
  backdrop-filter: blur(var(--ph-blur));
  -webkit-backdrop-filter: blur(var(--ph-blur));
  border: 1px solid var(--ph-glass-border);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: var(--ph-shadow-card), 0 1px 0 rgba(255, 255, 255, 0.05) inset;
  animation: fade-up 0.35s cubic-bezier(0.16, 1, 0.3, 1) 0.1s both;
}

.spool-cell { display: flex; align-items: center; gap: 0.625rem; }
.color-swatch {
  width: 1.1rem;
  height: 1.1rem;
  border-radius: 50%;
  flex-shrink: 0;
  border: 2px solid rgba(255, 255, 255, 0.18);
}
.spool-names { display: flex; flex-direction: column; min-width: 0; }
.spool-name { font-weight: 600; color: var(--ph-text); }
.spool-sub { font-size: 0.75rem; }

.remaining-cell { display: flex; flex-direction: column; gap: 0.3rem; }
.remaining-bar {
  height: 6px;
  border-radius: 99px;
  background: rgba(255, 255, 255, 0.08);
  overflow: hidden;
}
.remaining-fill { height: 100%; border-radius: 99px; transition: width 0.3s ease; }
.remaining-text { font-size: 0.75rem; color: var(--ph-text-muted); }
.low-icon { color: #e66767; margin-left: 0.25rem; }

.muted { color: var(--ph-text-muted); }
.row-actions { display: flex; gap: 0.25rem; }

.form-body { display: flex; flex-direction: column; gap: 1rem; padding: 0.25rem 0; }
.field-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
@media (max-width: 480px) {
  .field-grid { grid-template-columns: 1fr; }
}
.field { display: flex; flex-direction: column; gap: 0.375rem; }
.field-label { font-size: 0.8rem; font-weight: 500; color: var(--ph-text-muted); }
.field-input { width: 100%; }
.required { color: #f87171; }

.color-field { display: flex; gap: 0.5rem; align-items: center; }
.color-input {
  width: 2.4rem;
  height: 2.4rem;
  padding: 0;
  border: 1px solid var(--ph-border-strong);
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
}

.confirm-body { display: flex; align-items: flex-start; gap: 1rem; padding: 0.5rem 0; }
.confirm-icon { font-size: 2rem; color: #f87171; flex-shrink: 0; }
.confirm-body p { margin: 0; font-size: 0.9rem; line-height: 1.5; color: var(--ph-text); }
</style>
