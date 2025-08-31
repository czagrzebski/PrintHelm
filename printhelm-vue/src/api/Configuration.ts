import axios, {
  AxiosError,
  type AxiosInstance,
  type InternalAxiosRequestConfig,
  type AxiosResponse,
} from 'axios'

import { useAuthStore } from '@/stores/auth'

export const BASE_URL: string = import.meta.env.VITE_SERVICE_URL

export const api: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
})

export function setupAxiosInterceptors() {
  const authStore = useAuthStore()

  api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
    if (authStore.accessToken) {
      if (config.headers && typeof config.headers.set === 'function') {
        config.headers.set('Authorization', `Bearer ${authStore.accessToken}`)
      } else if (config.headers) {
        ;(config.headers as Record<string, string>)['Authorization'] =
          `Bearer ${authStore.accessToken}`
      }
    }
    return config
  })

  api.interceptors.response.use(
    (response: AxiosResponse) => response,
    async (error: AxiosError) => {
      const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

      // Do not retry if the request is to /auth/login
      if (
        error.response?.status === 401 &&
        !originalRequest._retry &&
        !originalRequest.url?.includes('/auth/login')
      ) {
        originalRequest._retry = true

        try {
          const newToken = await authStore.refreshToken()
          if (originalRequest.headers && typeof originalRequest.headers.set === 'function') {
            originalRequest.headers.set('Authorization', `Bearer ${newToken}`)
          } else if (originalRequest.headers) {
            ;(originalRequest.headers as Record<string, string>)['Authorization'] =
              `Bearer ${newToken}`
          }
          return api(originalRequest) // retry with new token
        } catch (refreshError) {
          return Promise.reject(refreshError)
        }
      }

      return Promise.reject(error)
    },
  )
}
