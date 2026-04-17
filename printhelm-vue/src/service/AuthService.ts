import { useAuthStore } from '@/stores/auth'
import { api } from '@/api/Configuration'

import type { ApiLoginRequest, ApiAuthResponse } from '@/client/printhelm-web-openapi'

class AuthService {
  async login(username: string, password: string) {
    const data: ApiLoginRequest = { username, password }
    const response = await api.post<ApiAuthResponse>('/auth/login', data)
    this.authStore.setToken(response.data.accessToken ?? null)
    if (response.data.user) {
      this.authStore.setUserInfo(response.data.user)
    }
    return response
  }

  async logout() {
    try {
      await api.post('/auth/logout')
    } finally {
      const store = this.authStore
      store.setToken(null)
    }
  }

  private get authStore() {
    return useAuthStore()
  }
}

export default new AuthService()
