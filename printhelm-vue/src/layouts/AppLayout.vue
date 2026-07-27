<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AuthService from '@/service/AuthService'
import NotificationPanel from '@/components/NotificationPanel.vue'
import ChatDialog from '@/components/ChatDialog.vue'
import CommandPalette from '@/components/CommandPalette.vue'

const router = useRouter()
const authStore = useAuthStore()
const paletteRef = ref<InstanceType<typeof CommandPalette> | null>(null)

async function logout() {
  await AuthService.logout()
  router.push('/login')
}
</script>

<template>
  <div class="app-layout">
    <aside class="sidebar">
      <div class="sidebar-brand">
        <img src="@/assets/logo.svg" class="sidebar-logo" alt="PrintHelm logo" />
        <span class="brand-text">PrintHelm</span>
      </div>

      <button class="palette-trigger" title="Command palette (Ctrl+K)" @click="paletteRef?.open()">
        <i class="mdi mdi-magnify" />
        <span>Quick jump…</span>
        <span class="palette-keys"><kbd>Ctrl</kbd><kbd>K</kbd></span>
      </button>

      <nav class="sidebar-nav">
        <RouterLink to="/dashboard" class="nav-link">
          <i class="mdi mdi-view-dashboard-outline" />
          Dashboard
        </RouterLink>
        <RouterLink to="/job-orders" class="nav-link">
          <i class="mdi mdi-clipboard-list-outline" />
          Job Orders
        </RouterLink>
        <RouterLink to="/queue" class="nav-link">
          <i class="mdi mdi-format-list-numbered" />
          Print Queue
        </RouterLink>
        <RouterLink to="/filament" class="nav-link">
          <i class="mdi mdi-circle-slice-6" />
          Filament
        </RouterLink>
        <RouterLink to="/analytics" class="nav-link">
          <i class="mdi mdi-chart-line" />
          Analytics
        </RouterLink>
        <RouterLink v-if="authStore.isAdmin" to="/audit" class="nav-link">
          <i class="mdi mdi-shield-search" />
          Audit Log
        </RouterLink>
        <RouterLink to="/settings" class="nav-link">
          <i class="mdi mdi-cog-outline" />
          Settings
        </RouterLink>
      </nav>

      <div class="sidebar-footer">
        <div class="footer-user">
          <div class="footer-avatar-ring">
            <i class="mdi mdi-account footer-avatar" />
          </div>
          <span class="footer-username">{{ authStore.username ?? 'User' }}</span>
        </div>
        <div class="footer-actions">
          <NotificationPanel />
          <button class="icon-btn" @click="logout" title="Logout">
            <i class="mdi mdi-logout" />
          </button>
        </div>
      </div>
    </aside>

    <main class="main-content">
      <RouterView v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" :key="$route.path" />
        </Transition>
      </RouterView>
    </main>

    <ChatDialog />
    <CommandPalette ref="paletteRef" />
  </div>
</template>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  width: 240px;
  flex-shrink: 0;
  background: rgba(8, 16, 23, 0.72);
  backdrop-filter: blur(var(--ph-blur));
  -webkit-backdrop-filter: blur(var(--ph-blur));
  border-right: 1px solid var(--ph-border);
  display: flex;
  flex-direction: column;
  padding: 1rem 0;
  position: relative;
  z-index: 10;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.5rem 1.25rem 1.25rem;
  font-size: 1.125rem;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.brand-text {
  background: var(--ph-gradient-brand);
  background-size: 200% auto;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: ph-gradient-pan 8s ease infinite;
}

.sidebar-logo {
  width: 2rem;
  height: 2rem;
  flex-shrink: 0;
  animation: brand-glow 3s ease-in-out infinite;
}

@keyframes brand-glow {
  0%, 100% { filter: drop-shadow(0 0 3px rgba(34, 211, 238, 0.25)); }
  50%       { filter: drop-shadow(0 0 10px rgba(34, 211, 238, 0.65)); }
}

/* ── Command palette trigger ─────────────────────────────────────── */
.palette-trigger {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0 0.75rem 1rem;
  padding: 0.5rem 0.75rem;
  border: 1px solid var(--ph-border-strong);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.03);
  color: var(--ph-text-muted);
  font-size: 0.8rem;
  font-family: inherit;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s, box-shadow 0.2s;
}

.palette-trigger:hover {
  border-color: rgba(34, 211, 238, 0.4);
  background: rgba(34, 211, 238, 0.06);
  box-shadow: 0 0 16px rgba(34, 211, 238, 0.12);
}

.palette-trigger span:first-of-type {
  flex: 1;
  text-align: left;
}

.palette-keys {
  display: flex;
  gap: 0.2rem;
}

.palette-keys kbd {
  padding: 0.05rem 0.3rem;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid var(--ph-border-strong);
  font-size: 0.62rem;
  font-family: inherit;
}

/* ── Nav ─────────────────────────────────────────────────────────── */
.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding: 0 0.75rem;
}

.nav-link {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.625rem 0.875rem;
  border-radius: 10px;
  color: var(--ph-text-muted);
  text-decoration: none;
  font-size: 0.875rem;
  font-weight: 500;
  overflow: hidden;
  transition: background 0.2s, color 0.2s, transform 0.2s;
}

.nav-link i {
  font-size: 1.1rem;
  width: 1.1rem;
  text-align: center;
  transition: transform 0.2s, color 0.2s;
}

.nav-link:hover {
  background: rgba(255, 255, 255, 0.05);
  color: var(--ph-text);
  transform: translateX(3px);
}

.nav-link:hover i {
  transform: scale(1.12);
}

.nav-link.router-link-active {
  background: linear-gradient(90deg, rgba(34, 211, 238, 0.14), rgba(34, 211, 238, 0.04));
  color: var(--ph-accent);
}

/* glowing active indicator bar */
.nav-link.router-link-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 18%;
  bottom: 18%;
  width: 3px;
  border-radius: 99px;
  background: var(--ph-gradient-brand);
  box-shadow: 0 0 10px rgba(34, 211, 238, 0.7);
  animation: nav-pill-in 0.3s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes nav-pill-in {
  from { transform: scaleY(0); }
  to   { transform: scaleY(1); }
}

.nav-link.router-link-active i {
  text-shadow: 0 0 14px rgba(34, 211, 238, 0.7);
}

/* ── Footer ──────────────────────────────────────────────────────── */
.sidebar-footer {
  padding: 0.75rem 0.875rem 0.25rem;
  border-top: 1px solid var(--ph-border);
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.footer-user {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 0.55rem;
  min-width: 0;
}

.footer-avatar-ring {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.8rem;
  height: 1.8rem;
  border-radius: 50%;
  flex-shrink: 0;
  background:
    linear-gradient(var(--ph-bg-darkest), var(--ph-bg-darkest)) padding-box,
    var(--ph-gradient-brand) border-box;
  border: 1.5px solid transparent;
}

.footer-avatar {
  font-size: 1rem;
  color: var(--ph-text-muted);
}

.footer-username {
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--ph-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.footer-actions {
  display: flex;
  align-items: center;
  gap: 0.125rem;
  flex-shrink: 0;
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.25rem;
  height: 2.25rem;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--ph-text-muted);
  font-size: 1.1rem;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.icon-btn:hover {
  background: rgba(255, 255, 255, 0.06);
  color: var(--ph-text);
}

.main-content {
  flex: 1;
  overflow-y: auto;
  background: transparent;
  padding: 2rem;
}
</style>

<!-- Non-scoped so transition classes reach child component roots -->
<style>
.page-enter-active {
  animation: page-slide-in 0.32s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.page-leave-active {
  animation: page-slide-out 0.22s ease both;
}

@keyframes page-slide-in {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}

@keyframes page-slide-out {
  from { opacity: 1; transform: translateY(0); }
  to   { opacity: 0; transform: translateY(-8px); }
}
</style>
