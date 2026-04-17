import { defineStore } from 'pinia'
import authApi from '@/api/AuthApi'

interface AuthState {
  accessToken: string | null
  username?: string
  userId?: number
  firstName?: string
  lastName?: string
  roles: string[]
  mustChangePassword: boolean
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    accessToken: null,
    username: undefined,
    userId: undefined,
    firstName: undefined,
    lastName: undefined,
    roles: [],
    mustChangePassword: false,
  }),

  getters: {
    isAdmin: (state) => state.roles.some((r) => r === 'ROLE_ADMIN'),
  },

  actions: {
    setToken(token: string | null) {
      this.accessToken = token
    },

    setUsername(name: string | undefined) {
      this.username = name
    },

    setUserInfo(user: { userId?: number | null; username?: string | null; firstName?: string | null; lastName?: string | null; roles?: Array<{ name?: string | null }> | null; mustChangePassword?: boolean | null }) {
      this.userId = user.userId ?? undefined
      this.username = user.username ?? undefined
      this.firstName = user.firstName ?? undefined
      this.lastName = user.lastName ?? undefined
      this.roles = user.roles?.map((r) => r.name ?? '').filter(Boolean) ?? []
      this.mustChangePassword = user.mustChangePassword ?? false
    },

    clearMustChangePassword() {
      this.mustChangePassword = false
    },

    async refreshToken(): Promise<string> {
      try {
        const res = await authApi.refresh()
        this.setToken(res.data.accessToken ?? null)
        if (res.data.user) {
          this.setUserInfo(res.data.user)
        }
        return res.data.accessToken ?? ''
      } catch (err) {
        this.setToken(null)
        throw err
      }
    },
  },
})
