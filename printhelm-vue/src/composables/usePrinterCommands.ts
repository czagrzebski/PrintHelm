import { ref } from 'vue'
import { api } from '@/api/Configuration'
import type { AxiosError } from 'axios'
import type { ApiJogRequest, ApiLightRequest, ApiPrintFileRequest, ApiSpeedRequest, ApiTempRequest } from '@/client/printhelm-web-openapi'

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
    setSpeed: (speed: number) => send('speed', { speed } satisfies ApiSpeedRequest),
    jog: (axis: string, distance: number) => send('jog', { axis, distance } satisfies ApiJogRequest),
    home: () => send('home'),
    setLight: (node: string, mode: ApiLightRequest['mode']) => send('light', { node, mode } satisfies ApiLightRequest),
    printFile: (filename: string, amsMapping: number[], flowCali: boolean, vibrationCali: boolean, layerInspect: boolean) =>
      send('print', { filename, amsMapping, flowCali, vibrationCali, layerInspect } satisfies ApiPrintFileRequest),
    setNozzleTemp: (temp: number) => send('nozzle-temp', { temp } satisfies ApiTempRequest),
    setBedTemp: (temp: number) => send('bed-temp', { temp } satisfies ApiTempRequest),
  }
}
