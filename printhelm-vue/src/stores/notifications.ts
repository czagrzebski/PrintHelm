import { defineStore } from 'pinia'
import notificationApi, { type ApiNotification } from '@/service/NotificationService'

interface NotificationsState {
  notifications: ApiNotification[]
  loading: boolean
}

export const useNotificationsStore = defineStore('notifications', {
  state: (): NotificationsState => ({
    notifications: [],
    loading: false,
  }),

  getters: {
    unreadCount: (state) => state.notifications.filter((n) => !n.acknowledged).length,
    sortedNotifications: (state) =>
      [...state.notifications].sort(
        (a, b) => new Date(b.createdAt!).getTime() - new Date(a.createdAt!).getTime(),
      ),
  },

  actions: {
    async fetchAll() {
      this.loading = true
      try {
        const res = await notificationApi.getAllNotifications()
        this.notifications = res.data
      } finally {
        this.loading = false
      }
    },

    addFromWebSocket(notification: ApiNotification) {
      const exists = this.notifications.some((n) => n.id === notification.id)
      if (!exists) {
        this.notifications.unshift(notification)
      }
    },

    async acknowledge(id: number) {
      await notificationApi.acknowledgeNotification(id)
      const n = this.notifications.find((n) => n.id === id)
      if (n) n.acknowledged = true
    },

    async acknowledgeAll() {
      await notificationApi.acknowledgeAllNotifications()
      this.notifications.forEach((n) => (n.acknowledged = true))
    },

    async remove(id: number) {
      await notificationApi.deleteNotification(id)
      this.notifications = this.notifications.filter((n) => n.id !== id)
    },

    async clearAcknowledged() {
      await notificationApi.clearAcknowledgedNotifications()
      this.notifications = this.notifications.filter((n) => !n.acknowledged)
    },
  },
})
