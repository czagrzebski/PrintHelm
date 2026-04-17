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
          50: '#f0f6f8',
          100: '#d6e8ed',
          200: '#b0cdd6',
          300: '#80adb9',
          400: '#5a8e9e',
          500: '#417485',
          600: '#2c5364',
          700: '#203a43',
          800: '#182e37',
          900: '#162830',
          950: '#0f2027',
        },
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

app.mount('#app')
