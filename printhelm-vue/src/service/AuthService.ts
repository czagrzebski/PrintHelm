import { useAuthStore } from '@/stores/auth'
import { api } from '@/api/Configuration'

import type { ApiLoginRequest, ApiAuthResponse } from '@/client/printhelm-web-openapi'

class AuthService {
  async login(username: string, password: string) {
    const data: ApiLoginRequest = { username, password }
    const response = await api.post<ApiAuthResponse>('/auth/login', data)
    this.authStore.setToken(response.data.accessToken ?? null)
    return response
  }

  async logout() {
    const store = this.authStore
    store.setToken(null)
    return await api.post('/auth/logout')
  }

  private get authStore() {
    return useAuthStore()
  }
}

export default new AuthService()
