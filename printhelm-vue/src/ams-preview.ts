import { createApp, h } from 'vue'
import '@/assets/base.css'
import AmsVisual from '@/components/AmsVisual.vue'

const materials = [
  { name: 'Bambu PLA Basic', type: 'PLA', color: 'FFFFFF', remain: 82, loaded: false },
  { name: 'PolyTerra Green', type: 'PLA', color: '00AE42', remain: 45, loaded: true },
  { name: 'Bambu PETG-CF', type: 'PETG', color: 'E4432F', remain: 100, loaded: false },
  {},
]

createApp({
  render: () =>
    h('div', { class: 'card' }, [
      h(AmsVisual, {
        materials,
        humidity: '32',
        amsTemp: '28.5',
        nozzleTemp: 218,
        nozzleTargetTemp: 220,
        printing: true,
      }),
    ]),
}).mount('#app')
