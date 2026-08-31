<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import ChatDialog from '@/components/ChatDialog.vue'
import CommandPalette from '@/components/CommandPalette.vue'
import SidebarNav from '@/layouts/SidebarNav.vue'
import Drawer from 'primevue/drawer'
import { useBreakpoint } from '@/composables/useBreakpoint'

const route = useRoute()
const paletteRef = ref<InstanceType<typeof CommandPalette> | null>(null)
const isMobile = useBreakpoint(960)
const drawerOpen = ref(false)

function openPalette() {
  paletteRef.value?.open()
}

watch(
  () => route.path,
  () => {
    drawerOpen.value = false
  },
)
</script>

<template>
  <div class="app-layout">
    <aside v-if="!isMobile" class="sidebar">
      <SidebarNav :on-palette-open="openPalette" />
    </aside>

    <header v-else class="mobile-topbar">
      <button class="icon-btn" title="Open menu" @click="drawerOpen = true">
        <i class="mdi mdi-menu" />
      </button>
      <div class="mobile-topbar-brand">
        <img src="@/assets/logo.svg" class="mobile-topbar-logo" alt="PrintHelm logo" />
        <span class="brand-text">PrintHelm</span>
      </div>
      <button class="icon-btn" title="Command palette" @click="openPalette">
        <i class="mdi mdi-magnify" />
      </button>
    </header>

    <Drawer v-if="isMobile" v-model:visible="drawerOpen" position="left" class="mobile-sidebar-drawer">
      <SidebarNav :on-palette-open="openPalette" />
    </Drawer>

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
  flex-direction: row;
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

.brand-text {
  background: var(--ph-gradient-brand);
  background-size: 200% auto;
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: ph-gradient-pan 8s ease infinite;
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
  flex-shrink: 0;
}

.icon-btn:hover {
  background: rgba(255, 255, 255, 0.06);
  color: var(--ph-text);
}

/* ── Mobile topbar (shown < 960px instead of the docked sidebar) ───── */
.mobile-topbar {
  display: none;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.625rem 0.875rem;
  background: rgba(8, 16, 23, 0.72);
  backdrop-filter: blur(var(--ph-blur));
  -webkit-backdrop-filter: blur(var(--ph-blur));
  border-bottom: 1px solid var(--ph-border);
  position: relative;
  z-index: 10;
  flex-shrink: 0;
}

.mobile-topbar-brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  min-width: 0;
}

.mobile-topbar-logo {
  width: 1.5rem;
  height: 1.5rem;
  flex-shrink: 0;
}

.main-content {
  flex: 1;
  overflow-y: auto;
  background: transparent;
  padding: 2rem;
  min-width: 0;
}

@media (max-width: 960px) {
  .app-layout {
    flex-direction: column;
  }

  .mobile-topbar {
    display: flex;
  }

  .main-content {
    padding: 1rem;
  }
}

@media (max-width: 600px) {
  .main-content {
    padding: 0.75rem;
  }
}
</style>

<!-- Non-scoped: PrimeVue applies the `class` prop directly to the Drawer's root (.p-drawer) element, which is teleported to <body> and outside this component's scoped style tree -->
<style>
.mobile-sidebar-drawer.p-drawer {
  width: 240px;
  background: rgba(8, 16, 23, 0.92);
  backdrop-filter: blur(var(--ph-blur));
  -webkit-backdrop-filter: blur(var(--ph-blur));
  border-right: 1px solid var(--ph-border);
}

.mobile-sidebar-drawer .p-drawer-content {
  display: flex;
  flex-direction: column;
  padding: 1rem 0;
  height: 100%;
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
