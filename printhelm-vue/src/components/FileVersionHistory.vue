<script setup lang="ts">
import Button from 'primevue/button'
import type { ApiJobOrderFileVersion } from '@/client/printhelm-web-openapi'
import { isViewableModel } from '@/components/ModelViewerDialog.vue'

const props = defineProps<{
  versions: ApiJobOrderFileVersion[]
  mode: 'part' | 'gcode'
  canModify: boolean
  busyVersionId?: number | null
}>()

const emit = defineEmits<{
  download: [version: ApiJobOrderFileVersion]
  view: [version: ApiJobOrderFileVersion]
  preview: [version: ApiJobOrderFileVersion]
  select: [version: ApiJobOrderFileVersion]
}>()

function is3mf(v: ApiJobOrderFileVersion) {
  return v.filename?.toLowerCase().endsWith('.3mf') ?? false
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
          <div class="fvh-name-line">
            <span class="fvh-filename">{{ v.filename }}</span>
            <span v-if="v.active" class="fvh-active-tag">
              <i class="mdi mdi-check-circle-outline" />
              {{ props.mode === 'gcode' ? 'Selected for print' : 'Current' }}
            </span>
          </div>
          <p v-if="v.description" class="fvh-description">{{ v.description }}</p>
          <span class="fvh-date">{{ formatDate(v.createdAt) }}</span>
        </div>
        <div class="fvh-actions">
          <Button
            v-if="props.mode === 'part' && isViewableModel(v.filename)"
            icon="mdi mdi-rotate-3d-variant"
            severity="secondary"
            text
            size="small"
            title="View in 3D"
            @click="emit('view', v)"
          />
          <Button
            v-if="props.mode === 'gcode' && is3mf(v)"
            icon="mdi mdi-rotate-3d-variant"
            severity="secondary"
            text
            size="small"
            title="Preview"
            @click="emit('preview', v)"
          />
          <Button
            icon="pi pi-download"
            severity="secondary"
            text
            size="small"
            title="Download"
            @click="emit('download', v)"
          />
          <Button
            v-if="props.mode === 'gcode' && !v.active"
            label="Use for Print"
            icon="mdi mdi-printer-check"
            severity="secondary"
            outlined
            size="small"
            :disabled="!canModify"
            :loading="busyVersionId === v.versionId"
            @click="emit('select', v)"
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
  gap: 0.2rem;
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
