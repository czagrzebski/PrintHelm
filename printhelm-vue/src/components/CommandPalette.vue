<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { printerApi } from '@/api/PrinterApi'

interface PaletteItem {
  id: string
  label: string
  hint?: string
  icon: string
  action: () => void
}

const router = useRouter()

const visible = ref(false)
const query = ref('')
const activeIndex = ref(0)
const inputRef = ref<HTMLInputElement | null>(null)

const pages: PaletteItem[] = [
  { id: 'page-dashboard', label: 'Dashboard',   hint: 'Page', icon: 'mdi mdi-view-dashboard-outline',  action: () => router.push('/dashboard') },
  { id: 'page-orders',    label: 'Job Orders',  hint: 'Page', icon: 'mdi mdi-clipboard-list-outline',  action: () => router.push('/job-orders') },
  { id: 'page-queue',     label: 'Print Queue', hint: 'Page', icon: 'mdi mdi-format-list-numbered',    action: () => router.push('/queue') },
  { id: 'page-settings',  label: 'Settings',    hint: 'Page', icon: 'mdi mdi-cog-outline',             action: () => router.push('/settings') },
]

const printerItems = ref<PaletteItem[]>([])

async function loadPrinters() {
  try {
    const res = await printerApi.getPrinters()
    printerItems.value = res.data
      .filter((p) => p.printerId != null)
      .map((p) => ({
        id: `printer-${p.printerId}`,
        label: p.printerName ?? `Printer ${p.printerId}`,
        hint: p.printerModel ?? 'Printer',
        icon: 'mdi mdi-printer-3d',
        action: () => router.push({ name: 'printer', params: { id: p.printerId! } }),
      }))
  } catch {
    printerItems.value = []
  }
}

const allItems = computed(() => [...pages, ...printerItems.value])

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase()
  if (!q) return allItems.value
  return allItems.value.filter(
    (item) => item.label.toLowerCase().includes(q) || item.hint?.toLowerCase().includes(q),
  )
})

watch(filtered, () => {
  activeIndex.value = 0
})

function open() {
  visible.value = true
  query.value = ''
  activeIndex.value = 0
  loadPrinters()
  nextTick(() => inputRef.value?.focus())
}

function close() {
  visible.value = false
}

function run(item: PaletteItem) {
  close()
  item.action()
}

function onGlobalKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    visible.value ? close() : open()
  } else if (e.key === 'Escape' && visible.value) {
    close()
  }
}

function onInputKeydown(e: KeyboardEvent) {
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    activeIndex.value = Math.min(activeIndex.value + 1, filtered.value.length - 1)
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    activeIndex.value = Math.max(activeIndex.value - 1, 0)
  } else if (e.key === 'Enter') {
    const item = filtered.value[activeIndex.value]
    if (item) run(item)
  }
}

onMounted(() => window.addEventListener('keydown', onGlobalKeydown))
onUnmounted(() => window.removeEventListener('keydown', onGlobalKeydown))

defineExpose({ open })
</script>

<template>
  <Teleport to="body">
    <Transition name="palette">
      <div v-if="visible" class="palette-backdrop" @click.self="close">
        <div class="palette" role="dialog" aria-label="Command palette">
          <div class="palette-input-row">
            <i class="mdi mdi-magnify palette-search-icon" />
            <input
              ref="inputRef"
              v-model="query"
              type="text"
              class="palette-input"
              placeholder="Jump to a page or printer…"
              @keydown="onInputKeydown"
            />
            <kbd class="palette-kbd">Esc</kbd>
          </div>

          <div class="palette-list">
            <div v-if="!filtered.length" class="palette-empty">
              <i class="mdi mdi-magnify-close" /> No matches
            </div>
            <button
              v-for="(item, i) in filtered"
              :key="item.id"
              class="palette-item"
              :class="{ 'palette-item--active': i === activeIndex }"
              @mouseenter="activeIndex = i"
              @click="run(item)"
            >
              <i :class="item.icon" class="palette-item-icon" />
              <span class="palette-item-label">{{ item.label }}</span>
              <span v-if="item.hint" class="palette-item-hint">{{ item.hint }}</span>
            </button>
          </div>

          <div class="palette-footer">
            <span><kbd class="palette-kbd">↑</kbd><kbd class="palette-kbd">↓</kbd> navigate</span>
            <span><kbd class="palette-kbd">↵</kbd> open</span>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.palette-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 14vh;
  background: rgba(4, 10, 14, 0.6);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
}

.palette {
  width: min(560px, 92vw);
  background: var(--ph-glass-heavy);
  backdrop-filter: blur(var(--ph-blur));
  -webkit-backdrop-filter: blur(var(--ph-blur));
  border: 1px solid var(--ph-border-strong);
  border-radius: 16px;
  box-shadow:
    var(--ph-shadow-pop),
    0 0 60px rgba(34, 211, 238, 0.08),
    0 1px 0 rgba(255, 255, 255, 0.06) inset;
  overflow: hidden;
}

.palette-input-row {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.875rem 1rem;
  border-bottom: 1px solid var(--ph-border);
}

.palette-search-icon {
  font-size: 1.15rem;
  color: var(--ph-accent);
}

.palette-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: var(--ph-text);
  font-size: 0.95rem;
  font-family: inherit;
}

.palette-input::placeholder {
  color: var(--ph-text-muted);
}

.palette-kbd {
  padding: 0.1rem 0.4rem;
  border-radius: 5px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid var(--ph-border-strong);
  color: var(--ph-text-muted);
  font-size: 0.68rem;
  font-family: inherit;
  line-height: 1.4;
}

.palette-list {
  max-height: 320px;
  overflow-y: auto;
  padding: 0.375rem;
}

.palette-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 2rem;
  color: var(--ph-text-muted);
  font-size: 0.875rem;
}

.palette-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
  padding: 0.625rem 0.75rem;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: var(--ph-text);
  font-size: 0.875rem;
  font-family: inherit;
  cursor: pointer;
  text-align: left;
  transition: background 0.1s;
}

.palette-item--active {
  background: rgba(34, 211, 238, 0.12);
}

.palette-item--active .palette-item-icon {
  color: var(--ph-accent);
  text-shadow: 0 0 12px rgba(34, 211, 238, 0.6);
}

.palette-item-icon {
  font-size: 1.1rem;
  color: var(--ph-text-muted);
  width: 1.25rem;
  text-align: center;
  transition: color 0.1s;
}

.palette-item-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.palette-item-hint {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--ph-text-muted);
}

.palette-footer {
  display: flex;
  gap: 1.25rem;
  padding: 0.55rem 1rem;
  border-top: 1px solid var(--ph-border);
  color: var(--ph-text-muted);
  font-size: 0.72rem;
}

.palette-footer span {
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

/* enter/leave */
.palette-enter-active {
  transition: opacity 0.18s ease;
}
.palette-enter-active .palette {
  transition: transform 0.22s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.18s ease;
}
.palette-leave-active {
  transition: opacity 0.14s ease;
}
.palette-enter-from,
.palette-leave-to {
  opacity: 0;
}
.palette-enter-from .palette {
  transform: translateY(-14px) scale(0.97);
}
</style>
