import { onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { BASE_URL } from '@/api/Configuration'
import { useAuthStore } from '@/stores/auth'
import type { ApiPrinterState } from '@/client/printhelm-web-openapi'

type StateCallback = (printerId: number, state: ApiPrinterState) => void

export function usePrinterSocket() {
  let client: Client | null = null

  function connect(printerIds: number[], onState: StateCallback) {
    const authStore = useAuthStore()
    const wsUrl = BASE_URL.replace('/api', '/ws')

    client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      connectHeaders: {
        Authorization: `Bearer ${authStore.accessToken}`,
      },
      reconnectDelay: 5000,
      onConnect: () => {
        printerIds.forEach((id) => {
          client!.subscribe(`/topic/printer/${id}/state`, (message) => {
            try {
              const state: ApiPrinterState = JSON.parse(message.body)
              onState(id, state)
            } catch {
              // malformed message — ignore
            }
          })
        })
      },
    })

    client.activate()
  }

  function disconnect() {
    client?.deactivate()
    client = null
  }

  onUnmounted(disconnect)

  return { connect, disconnect }
}
