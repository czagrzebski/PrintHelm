import './assets/main.css'
import 'primeicons/primeicons.css'
import '@mdi/font/css/materialdesignicons.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'

import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import Aura from '@primeuix/themes/aura'
import { definePreset } from '@primeuix/themes'
import { setupAxiosInterceptors } from './api/Configuration'
import Particles from '@tsparticles/vue3'
import { loadSlim } from '@tsparticles/slim'

const PrintHelmTheme = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#ecfeff',
      100: '#cffafe',
      200: '#a5f3fc',
      300: '#67e8f9',
      400: '#22d3ee',
      500: '#06b6d4',
      600: '#0891b2',
      700: '#0e7490',
      800: '#155e75',
      900: '#164e63',
      950: '#083344',
    },
    colorScheme: {
      dark: {
        primary: {
          color: '{primary.400}',
          inverseColor: '{primary.950}',
          hoverColor: '{primary.300}',
          activeColor: '{primary.200}',
        },
        highlight: {
          background: 'rgba(34, 211, 238, 0.12)',
          focusBackground: 'rgba(34, 211, 238, 0.22)',
          color: '{primary.300}',
          focusColor: '{primary.200}',
        },
        surface: {
          0: '#ffffff',
          50: '#eef5f8',
          100: '#d3e6ec',
          200: '#a9c8d3',
          300: '#7aa5b4',
          400: '#528595',
          500: '#3a6a7c',
          600: '#274d5d',
          700: '#193543',
          800: '#102430',
          900: '#0b1922',
          950: '#060d13',
        },
      },
    },
  },
  components: {
    tabs: {
      tablist: {
        background: 'transparent',
      },
      tabpanel: {
        background: 'transparent',
      },
    },
  },
})

const app = createApp(App)

app.use(createPinia())
setupAxiosInterceptors()

app.use(router)

app.use(PrimeVue, {
  theme: {
    preset: PrintHelmTheme,
    options: {
      darkModeSelector: ':root',
    },
  },
})
app.use(ToastService)
app.use(Particles, {
  init: async (engine: Parameters<typeof loadSlim>[0]) => {
    await loadSlim(engine)
  },
})

app.mount('#app')
