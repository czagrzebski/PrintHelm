<script setup lang="ts">
import { useRouter, RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AuthService from '@/service/AuthService'

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
        <i class="mdi mdi-printer-3d" />
        <span>PrintHelm</span>
      </div>

      <nav class="sidebar-nav">
        <RouterLink to="/dashboard" class="nav-link">
          <i class="mdi mdi-view-dashboard-outline" />
          Dashboard
        </RouterLink>
        <RouterLink to="/settings" class="nav-link">
          <i class="mdi mdi-cog-outline" />
          Settings
        </RouterLink>
      </nav>

      <div class="sidebar-footer">
        <div class="user-info">
          <i class="mdi mdi-account-circle-outline" />
          <span>{{ authStore.username ?? 'User' }}</span>
        </div>
        <button class="logout-link" @click="logout">
          <i class="mdi mdi-logout" />
          Logout
        </button>
      </div>
    </aside>

    <main class="main-content">
      <RouterView />
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

.sidebar-brand i {
  font-size: 1.25rem;
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
  padding: 1rem 1.25rem 0.5rem;
  border-top: 1px solid var(--ph-border);
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  color: var(--ph-text-muted);
  padding: 0.25rem 0;
}

.logout-link {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.625rem 0.875rem;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--ph-text-muted);
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.logout-link i {
  font-size: 0.9rem;
  width: 1rem;
  text-align: center;
}

.logout-link:hover {
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
