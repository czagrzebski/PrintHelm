<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useNotificationsStore } from '@/stores/notifications'
import { useNotificationSocket } from '@/composables/useNotificationSocket'
import type { ApiNotification, NotificationSeverity } from '@/service/NotificationService'

const notificationsStore = useNotificationsStore()
const { connect } = useNotificationSocket()

const panelOpen = ref(false)
const panelRef = ref<HTMLElement | null>(null)

onMounted(async () => {
  await notificationsStore.fetchAll()
  connect()
  document.addEventListener('click', onDocClick)
})

onUnmounted(() => {
  document.removeEventListener('click', onDocClick)
})

function onDocClick(e: MouseEvent) {
  if (panelRef.value && !panelRef.value.contains(e.target as Node)) {
    panelOpen.value = false
  }
}

function togglePanel() {
  panelOpen.value = !panelOpen.value
}

function closePanel() {
  panelOpen.value = false
}

async function acknowledge(n: ApiNotification) {
  await notificationsStore.acknowledge(n.id!)
}

async function acknowledgeAll() {
  await notificationsStore.acknowledgeAll()
}

async function remove(n: ApiNotification) {
  await notificationsStore.remove(n.id!)
}

async function clearAcknowledged() {
  await notificationsStore.clearAcknowledged()
}

function severityIcon(severity: NotificationSeverity | undefined) {
  switch (severity) {
    case 'ERROR':   return 'mdi mdi-alert-circle'
    case 'WARNING': return 'mdi mdi-alert'
    default:        return 'mdi mdi-information'
  }
}

function severityClass(severity: NotificationSeverity | undefined) {
  switch (severity) {
    case 'ERROR':   return 'sev-error'
    case 'WARNING': return 'sev-warning'
    default:        return 'sev-info'
  }
}

function formatDate(dateStr: string | undefined) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}
</script>

<template>
  <div class="notif-wrapper" ref="panelRef">
    <button class="notif-bell" @click="togglePanel" :class="{ active: panelOpen }" :title="'Notifications'">
      <i class="mdi mdi-bell-outline" />
      <span v-if="notificationsStore.unreadCount > 0" class="badge">
        {{ notificationsStore.unreadCount > 99 ? '99+' : notificationsStore.unreadCount }}
      </span>
    </button>

    <Transition name="panel">
      <div v-if="panelOpen" class="notif-panel">
        <div class="panel-header">
          <span class="panel-title">Notifications</span>
          <div class="panel-actions">
            <button class="action-btn" @click="acknowledgeAll" title="Mark all as read">
              <i class="mdi mdi-check-all" />
            </button>
            <button class="action-btn" @click="clearAcknowledged" title="Clear acknowledged">
              <i class="mdi mdi-delete-sweep-outline" />
            </button>
            <button class="action-btn" @click="closePanel" title="Close">
              <i class="mdi mdi-close" />
            </button>
          </div>
        </div>

        <div class="panel-body">
          <div v-if="notificationsStore.loading" class="empty-state">
            Loading…
          </div>
          <div v-else-if="notificationsStore.sortedNotifications.length === 0" class="empty-state">
            <i class="mdi mdi-bell-off-outline" />
            <span>No notifications</span>
          </div>
          <ul v-else class="notif-list">
            <li
              v-for="n in notificationsStore.sortedNotifications"
              :key="n.id"
              class="notif-item"
              :class="{ unread: !n.acknowledged }"
            >
              <i :class="[severityIcon(n.severity), 'notif-icon', severityClass(n.severity)]" />
              <div class="notif-body">
                <div class="notif-title">{{ n.title }}</div>
                <div class="notif-message">{{ n.message }}</div>
                <div class="notif-meta">
                  <span class="notif-printer">{{ n.printerName }}</span>
                  <span class="notif-time">{{ formatDate(n.createdAt) }}</span>
                </div>
              </div>
              <div class="notif-controls">
                <button
                  v-if="!n.acknowledged"
                  class="ctrl-btn"
                  @click="acknowledge(n)"
                  title="Mark as read"
                >
                  <i class="mdi mdi-check" />
                </button>
                <button class="ctrl-btn ctrl-delete" @click="remove(n)" title="Delete">
                  <i class="mdi mdi-close" />
                </button>
              </div>
            </li>
          </ul>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.notif-wrapper {
  position: relative;
}

.notif-bell {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  width: 2.25rem;
  height: 2.25rem;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--ph-text-muted);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  font-size: 1.1rem;
}

.notif-bell:hover,
.notif-bell.active {
  background: rgba(255, 255, 255, 0.06);
  color: var(--ph-text);
}

.badge {
  position: absolute;
  top: 2px;
  right: 2px;
  min-width: 1rem;
  height: 1rem;
  padding: 0 3px;
  background: #f87171;
  color: #fff;
  border-radius: 999px;
  font-size: 0.6rem;
  font-weight: 700;
  line-height: 1rem;
  text-align: center;
  pointer-events: none;
}

.notif-panel {
  position: absolute;
  bottom: calc(100% + 0.5rem);
  left: 0;
  width: 360px;
  max-height: 480px;
  background: var(--ph-glass-heavy);
  backdrop-filter: blur(var(--ph-blur));
  -webkit-backdrop-filter: blur(var(--ph-blur));
  border: 1px solid var(--ph-border-strong);
  border-radius: 14px;
  box-shadow: var(--ph-shadow-pop);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  z-index: 1000;
  animation: ph-scale-in 0.2s cubic-bezier(0.16, 1, 0.3, 1) both;
  transform-origin: bottom left;
}

@media (max-width: 600px) {
  .notif-panel {
    width: min(360px, 90vw);
    left: auto;
    right: 0;
    transform-origin: bottom right;
  }
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--ph-border);
  flex-shrink: 0;
}

.panel-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--ph-text);
}

.panel-actions {
  display: flex;
  gap: 0.25rem;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.75rem;
  height: 1.75rem;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--ph-text-muted);
  cursor: pointer;
  font-size: 0.95rem;
  transition: background 0.15s, color 0.15s;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  color: var(--ph-text);
}

.panel-body {
  flex: 1;
  overflow-y: auto;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 2rem;
  color: var(--ph-text-muted);
  font-size: 0.875rem;
}

.empty-state i {
  font-size: 2rem;
  opacity: 0.4;
}

.notif-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.notif-item {
  display: flex;
  align-items: flex-start;
  gap: 0.625rem;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--ph-border);
  transition: background 0.1s;
}

.notif-item:last-child {
  border-bottom: none;
}

.notif-item.unread {
  background: rgba(34, 211, 238, 0.04);
}

.notif-item:hover {
  background: rgba(255, 255, 255, 0.03);
}

.notif-icon {
  font-size: 1.05rem;
  margin-top: 0.1rem;
  flex-shrink: 0;
}

.sev-error   { color: #f87171; }
.sev-warning { color: #fbbf24; }
.sev-info    { color: #22d3ee; }

.notif-body {
  flex: 1;
  min-width: 0;
}

.notif-title {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--ph-text);
  line-height: 1.3;
}

.notif-message {
  font-size: 0.75rem;
  color: var(--ph-text-muted);
  margin-top: 0.125rem;
  line-height: 1.4;
  word-break: break-word;
}

.notif-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.25rem;
}

.notif-printer {
  font-size: 0.7rem;
  color: var(--ph-accent);
  font-weight: 500;
}

.notif-time {
  font-size: 0.7rem;
  color: var(--ph-text-muted);
  opacity: 0.7;
}

.notif-controls {
  display: flex;
  gap: 0.125rem;
  flex-shrink: 0;
}

.ctrl-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.5rem;
  height: 1.5rem;
  border: none;
  border-radius: 5px;
  background: transparent;
  color: var(--ph-text-muted);
  cursor: pointer;
  font-size: 0.8rem;
  transition: background 0.15s, color 0.15s;
  opacity: 0;
}

.notif-item:hover .ctrl-btn {
  opacity: 1;
}

.ctrl-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  color: var(--ph-text);
}

.ctrl-delete:hover {
  background: rgba(248, 113, 113, 0.15);
  color: #f87171;
}

/* Panel slide-up transition */
.panel-enter-active,
.panel-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.panel-enter-from,
.panel-leave-to {
  opacity: 0;
  transform: translateY(6px);
}
</style>
