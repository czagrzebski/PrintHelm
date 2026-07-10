<script setup lang="ts">
import Button from 'primevue/button'
import InputNumber from 'primevue/inputnumber'
import type { ApiJobOrderFileVersion, ApiJobOrderVersionFile } from '@/client/printhelm-web-openapi'
import { isViewableModel } from '@/components/ModelViewerDialog.vue'

const props = defineProps<{
  versions: ApiJobOrderFileVersion[]
  mode: 'part' | 'gcode'
  canModify: boolean
  /** `${versionId}:${fileIndex}` of the file whose select request is in flight */
  busyKey?: string | null
}>()

const emit = defineEmits<{
  download: [version: ApiJobOrderFileVersion, file?: ApiJobOrderVersionFile]
  view: [version: ApiJobOrderFileVersion]
  preview: [version: ApiJobOrderFileVersion, file?: ApiJobOrderVersionFile]
  select: [version: ApiJobOrderFileVersion, fileIndex: number]
  quantity: [version: ApiJobOrderFileVersion, fileIndex: number, quantity: number]
}>()

function onQuantityChange(v: ApiJobOrderFileVersion, f: ApiJobOrderVersionFile, value: number | null) {
  const qty = Math.max(0, Math.min(999, Math.round(value ?? 1)))
  if (qty === (f.printQuantity ?? 1)) return
  emit('quantity', v, f.fileIndex ?? 0, qty)
}

/** Files of a version with legacy fallback (pre-multi-file rows only carry `filename`) */
function filesOf(v: ApiJobOrderFileVersion): ApiJobOrderVersionFile[] {
  if (v.files?.length) return v.files
  return v.filename ? [{ fileIndex: 0, filename: v.filename, active: v.active }] : []
}

function is3mf(name?: string) {
  return name?.toLowerCase().endsWith('.3mf') ?? false
}

function isViewable(v: ApiJobOrderFileVersion) {
  return filesOf(v).some((f) => isViewableModel(f.filename))
}

function isMultiFile(v: ApiJobOrderFileVersion) {
  return filesOf(v).length > 1
}

function singleFile(v: ApiJobOrderFileVersion): ApiJobOrderVersionFile | undefined {
  return filesOf(v)[0]
}

function busy(v: ApiJobOrderFileVersion, f: ApiJobOrderVersionFile) {
  return props.busyKey === `${v.versionId}:${f.fileIndex ?? 0}`
}

function formatDate(iso?: string) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString()
}
</script>

<template>
  <div v-if="versions.length > 0" class="fvh">
    <div class="fvh-header">
      <i class="mdi mdi-history" />
      <span>Version History</span>
      <span class="fvh-count">{{ versions.length }} version{{ versions.length === 1 ? '' : 's' }}</span>
    </div>
    <div class="fvh-list">
      <div
        v-for="v in versions"
        :key="v.versionId"
        class="fvh-row"
        :class="{ 'fvh-row--active': v.active }"
      >
        <div class="fvh-badge">v{{ v.versionNumber }}</div>
        <div class="fvh-info">
          <div v-if="props.mode === 'part' || isMultiFile(v)" class="fvh-name-line">
            <span v-if="isMultiFile(v)" class="fvh-filename">
              <i class="mdi mdi-file-multiple-outline" />
              {{ filesOf(v).length }} files {{ props.mode === 'part' ? '(assembly)' : '' }}
            </span>
            <span v-else class="fvh-filename">{{ singleFile(v)?.filename }}</span>
            <span v-if="v.active && props.mode === 'part'" class="fvh-active-tag">
              <i class="mdi mdi-check-circle-outline" /> Current
            </span>
          </div>

          <!-- Part assembly: compact file chips -->
          <div v-if="props.mode === 'part' && isMultiFile(v)" class="fvh-file-chips">
            <span v-for="f in filesOf(v)" :key="f.fileIndex" class="fvh-file-chip">{{ f.filename }}</span>
          </div>

          <!-- GCode: one row per file with quantity, progress, and its own actions -->
          <div v-if="props.mode === 'gcode'" class="fvh-file-list">
            <div
              v-for="f in filesOf(v)"
              :key="f.fileIndex"
              class="fvh-file-row"
              :class="{ 'fvh-file-row--active': f.active }"
            >
              <span class="fvh-file-name">{{ f.filename }}</span>
              <span v-if="f.active" class="fvh-active-tag">
                <i class="mdi mdi-check-circle-outline" /> Selected for print
              </span>
              <span
                class="fvh-progress-chip"
                :class="{ 'fvh-progress-chip--done': (f.completedPrints ?? 0) >= (f.printQuantity ?? 1) && (f.printQuantity ?? 1) > 0 }"
                :title="`${f.completedPrints ?? 0} of ${f.printQuantity ?? 1} prints completed`"
              >
                <i class="mdi mdi-printer-3d-nozzle-outline" />
                {{ f.completedPrints ?? 0 }}/{{ f.printQuantity ?? 1 }}
              </span>
              <span class="fvh-qty" title="Required prints (0 = skip this file)">
                <label class="fvh-qty-label">Qty</label>
                <InputNumber
                  :model-value="f.printQuantity ?? 1"
                  :min="0"
                  :max="999"
                  :disabled="!canModify"
                  show-buttons
                  size="small"
                  button-layout="stacked"
                  class="fvh-qty-input"
                  @update:model-value="(val: number | null) => onQuantityChange(v, f, val)"
                />
              </span>
              <span class="fvh-file-actions">
                <Button
                  v-if="is3mf(f.filename)"
                  icon="mdi mdi-rotate-3d-variant"
                  severity="secondary"
                  text
                  size="small"
                  title="Preview"
                  @click="emit('preview', v, f)"
                />
                <Button
                  icon="pi pi-download"
                  severity="secondary"
                  text
                  size="small"
                  title="Download"
                  @click="emit('download', v, f)"
                />
                <Button
                  v-if="!f.active"
                  label="Use for Print"
                  icon="mdi mdi-printer-check"
                  severity="secondary"
                  outlined
                  size="small"
                  :disabled="!canModify"
                  :loading="busy(v, f)"
                  @click="emit('select', v, f.fileIndex ?? 0)"
                />
              </span>
            </div>
          </div>

          <p v-if="v.description" class="fvh-description">{{ v.description }}</p>
          <span class="fvh-date">{{ formatDate(v.createdAt) }}</span>
        </div>
        <div v-if="props.mode === 'part'" class="fvh-actions">
          <Button
            v-if="isViewable(v)"
            icon="mdi mdi-rotate-3d-variant"
            severity="secondary"
            text
            size="small"
            :title="isMultiFile(v) ? 'View assembly in 3D' : 'View in 3D'"
            @click="emit('view', v)"
          />
          <Button
            icon="pi pi-download"
            severity="secondary"
            text
            size="small"
            :title="isMultiFile(v) ? 'Download all files' : 'Download'"
            @click="emit('download', v)"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.fvh {
  margin-top: 1rem;
  border: 1px solid var(--ph-border);
  border-radius: 10px;
  background: rgba(255,255,255,0.02);
  overflow: hidden;
}

.fvh-header {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.6rem 0.875rem;
  font-size: 0.72rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  color: var(--ph-text-muted);
  border-bottom: 1px solid var(--ph-border);
  background: rgba(255,255,255,0.02);
}

.fvh-count {
  margin-left: auto;
  font-weight: 500;
  text-transform: none;
  letter-spacing: 0;
  font-size: 0.72rem;
  opacity: 0.75;
}

.fvh-list {
  display: flex;
  flex-direction: column;
}

.fvh-row {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.7rem 0.875rem;
  border-bottom: 1px solid var(--ph-border);
  transition: background 0.15s;
}
.fvh-row:last-child { border-bottom: none; }
.fvh-row:hover { background: rgba(255,255,255,0.025); }
.fvh-row--active { background: rgba(34, 211, 238, 0.05); }

.fvh-badge {
  flex-shrink: 0;
  min-width: 2.1rem;
  text-align: center;
  padding: 0.15rem 0.4rem;
  border-radius: 999px;
  font-size: 0.7rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  background: rgba(255,255,255,0.06);
  border: 1px solid var(--ph-border);
  color: var(--ph-text-muted);
  margin-top: 0.1rem;
}
.fvh-row--active .fvh-badge {
  background: rgba(34, 211, 238, 0.12);
  border-color: rgba(34, 211, 238, 0.4);
  color: var(--ph-accent, #22d3ee);
}

.fvh-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.fvh-name-line {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.fvh-filename {
  font-family: monospace;
  font-size: 0.8rem;
  color: var(--ph-text);
  word-break: break-all;
}

.fvh-file-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
}

.fvh-file-chip {
  font-family: monospace;
  font-size: 0.68rem;
  color: var(--ph-text-muted);
  background: rgba(255,255,255,0.05);
  border: 1px solid var(--ph-border);
  border-radius: 4px;
  padding: 0.08rem 0.4rem;
  word-break: break-all;
}

.fvh-file-list {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--ph-border);
  border-radius: 8px;
  overflow: hidden;
}

.fvh-file-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  padding: 0.3rem 0.55rem;
  border-bottom: 1px solid var(--ph-border);
  background: rgba(255,255,255,0.02);
}
.fvh-file-row:last-child { border-bottom: none; }
.fvh-file-row--active { background: rgba(34, 211, 238, 0.06); }

.fvh-file-name {
  font-family: monospace;
  font-size: 0.74rem;
  color: var(--ph-text);
  word-break: break-all;
  flex: 1;
  min-width: 8rem;
}

.fvh-file-actions {
  display: flex;
  align-items: center;
  gap: 0.2rem;
  margin-left: auto;
}

.fvh-progress-chip {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.68rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  padding: 0.1rem 0.45rem;
  border-radius: 999px;
  background: rgba(255,255,255,0.05);
  border: 1px solid var(--ph-border);
  color: var(--ph-text-muted);
  white-space: nowrap;
}
.fvh-progress-chip--done {
  background: rgba(74, 222, 128, 0.1);
  border-color: rgba(74, 222, 128, 0.3);
  color: #4ade80;
}

.fvh-qty {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
}
.fvh-qty-label {
  font-size: 0.62rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--ph-text-muted);
  opacity: 0.7;
}
.fvh-qty-input {
  width: 4.4rem;
}
.fvh-qty-input :deep(.p-inputnumber-input) {
  width: 100%;
  font-size: 0.74rem;
  padding: 0.2rem 0.4rem;
  text-align: center;
}

.fvh-active-tag {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.68rem;
  font-weight: 600;
  padding: 0.1rem 0.45rem;
  border-radius: 999px;
  background: rgba(74, 222, 128, 0.1);
  border: 1px solid rgba(74, 222, 128, 0.3);
  color: #4ade80;
  white-space: nowrap;
}

.fvh-description {
  margin: 0;
  font-size: 0.8rem;
  color: var(--ph-text-muted);
  line-height: 1.45;
  white-space: pre-wrap;
}

.fvh-date {
  font-size: 0.7rem;
  color: var(--ph-text-muted);
  opacity: 0.7;
}

.fvh-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 0.2rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}
</style>
