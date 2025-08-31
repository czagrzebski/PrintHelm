import { defineStore } from 'pinia'
import authApi from '@/api/AuthApi'

interface AuthState {
  accessToken: string | null
  username?: string
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    accessToken: null,
    username: undefined,
  }),

  actions: {
    setToken(token: string | null) {
      this.accessToken = token
    },

    setUsername(name: string | undefined) {
      this.username = name
    },

    async refreshToken(): Promise<string> {
      try {
        const res = await authApi.refresh()
        this.setToken(res.data.accessToken ?? null)
        this.setUsername(res.data.user?.username)
        return res.data.accessToken ?? ''
      } catch (err) {
        this.setToken(null)
        throw err
      }
    },
  },
})
