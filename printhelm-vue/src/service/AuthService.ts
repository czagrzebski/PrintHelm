import { useAuthStore } from '@/stores/auth'
import authApi from '@/api/AuthApi'

class AuthService {
  async login(username: string, password: string) {
    const response = await authApi.login({ username, password })
    this.authStore.setToken(response.data.accessToken ?? null)
    if (response.data.user) {
      this.authStore.setUserInfo(response.data.user)
    }
    return response
  }

  async logout() {
    try {
      await authApi.logout()
    } finally {
      this.authStore.setToken(null)
    }
  }

  private get authStore() {
    return useAuthStore()
  }
}

export default new AuthService()
