<script setup lang="ts">
import { useRouter, RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AuthService from '@/service/AuthService'
import NotificationPanel from '@/components/NotificationPanel.vue'

const router = useRouter()
const authStore = useAuthStore()

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
        <span>PrintHelm</span>
      </div>

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
        <RouterLink to="/settings" class="nav-link">
          <i class="mdi mdi-cog-outline" />
          Settings
        </RouterLink>
      </nav>

      <div class="sidebar-footer">
        <div class="footer-user">
          <i class="mdi mdi-account-circle-outline footer-avatar" />
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
  background: var(--ph-bg-darkest);
  border-right: 1px solid var(--ph-border);
  display: flex;
  flex-direction: column;
  padding: 1rem 0;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.5rem 1.25rem 1.75rem;
  font-size: 1.125rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--ph-accent);
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

.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  padding: 0 0.75rem;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.625rem 0.875rem;
  border-radius: 8px;
  color: var(--ph-text-muted);
  text-decoration: none;
  font-size: 0.875rem;
  font-weight: 500;
  transition: background 0.15s, color 0.15s;
}

.nav-link i {
  font-size: 1.1rem;
  width: 1.1rem;
  text-align: center;
}

.nav-link:hover {
  background: rgba(255, 255, 255, 0.06);
  color: var(--ph-text);
}

.nav-link.router-link-active {
  background: var(--ph-accent-dim);
  color: var(--ph-accent);
}

.sidebar-footer {
  padding: 0.75rem 0.875rem;
  border-top: 1px solid var(--ph-border);
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.footer-user {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
}

.footer-avatar {
  font-size: 1.25rem;
  color: var(--ph-text-muted);
  flex-shrink: 0;
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
  background: var(--ph-bg-mid);
  padding: 2rem;
}
</style>

<!-- Non-scoped so transition classes reach child component roots -->
<style>
.page-enter-active {
  animation: page-fade-in 0.55s ease both;
}

.page-leave-active {
  animation: page-fade-out 0.55s ease both;
}

@keyframes page-fade-in {
  from { opacity: 0; }
  to   { opacity: 1; }
}

@keyframes page-fade-out {
  from { opacity: 1; }
  to   { opacity: 0; }
}
</style>
