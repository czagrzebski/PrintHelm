import { onMounted, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useToast } from 'primevue/usetoast'
import { BASE_URL } from '@/api/Configuration'
import { useAuthStore } from '@/stores/auth'
import { useNotificationsStore } from '@/stores/notifications'
import type { ApiNotification } from '@/service/NotificationService'

const SEVERITY_MAP = {
  INFO:    'info',
  WARNING: 'warn',
  ERROR:   'error',
} as const

export function useNotificationSocket() {
  let client: Client | null = null
  const notificationsStore = useNotificationsStore()
  const toast = useToast()

  function connect() {
    const authStore = useAuthStore()
    const wsUrl = BASE_URL.replace('/api', '/ws')

    client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      connectHeaders: {
        Authorization: `Bearer ${authStore.accessToken}`,
      },
      reconnectDelay: 5000,
      onConnect: () => {
        client!.subscribe('/topic/notifications', (message) => {
          try {
            const notification: ApiNotification = JSON.parse(message.body)
            notificationsStore.addFromWebSocket(notification)
            toast.add({
              severity: notification.severity ? SEVERITY_MAP[notification.severity] : 'info',
              summary: notification.title,
              detail: notification.message,
              life: 5000,
            })
          } catch {
            // malformed — ignore
          }
        })
      },
    })

    client.activate()
  }

  function disconnect() {
    client?.deactivate()
    client = null
  }

  function onPageHide(e: PageTransitionEvent) {
    if (e.persisted) disconnect()
  }

  function onPageShow(e: PageTransitionEvent) {
    if (e.persisted) connect()
  }

  onMounted(() => {
    window.addEventListener('pagehide', onPageHide)
    window.addEventListener('pageshow', onPageShow)
  })

  onUnmounted(() => {
    disconnect()
    window.removeEventListener('pagehide', onPageHide)
    window.removeEventListener('pageshow', onPageShow)
  })

  return { connect, disconnect }
}
