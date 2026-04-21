import { ref } from 'vue'
import { api } from '@/api/Configuration'
import type { AxiosError } from 'axios'

export function usePrinterCommands(printerId: number) {
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function send(path: string, body?: object) {
    loading.value = true
    error.value = null
    try {
      await api.post(`/printer/${printerId}/command/${path}`, body ?? null)
    } catch (e) {
      const axiosErr = e as AxiosError<string>
      error.value = axiosErr.response?.data ?? axiosErr.message ?? 'Command failed'
      throw e
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    error,
    stopPrint: () => send('stop'),
    pausePrint: () => send('pause'),
    resumePrint: () => send('resume'),
    jog: (axis: string, distance: number) => send('jog', { axis, distance }),
    home: () => send('home'),
    setLight: (node: string, mode: 'on' | 'off') => send('light', { node, mode }),
    printFile: (filename: string, amsMapping: number[], flowCali: boolean, vibrationCali: boolean, layerInspect: boolean) =>
      send('print', { filename, amsMapping, flowCali, vibrationCali, layerInspect }),
    setNozzleTemp: (temp: number) => send('nozzle-temp', { temp }),
    setBedTemp: (temp: number) => send('bed-temp', { temp }),
  }
}
