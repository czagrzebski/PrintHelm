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

const AUTH_SKIP_RETRY_PATHS = ['/auth/login', '/auth/refresh']

function setAuthHeader(headers: InternalAxiosRequestConfig['headers'], token: string) {
  headers.set('Authorization', `Bearer ${token}`)
}

let refreshPromise: Promise<string> | null = null
let interceptorsInitialized = false

export function setupAxiosInterceptors() {
  if (interceptorsInitialized) return
  interceptorsInitialized = true

  const authStore = useAuthStore()

  api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
    if (authStore.accessToken) {
      setAuthHeader(config.headers, authStore.accessToken)
    }
    return config
  })

  api.interceptors.response.use(
    (response: AxiosResponse) => response,
    async (error: AxiosError) => {
      const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

      if (
        error.response?.status === 401 &&
        !originalRequest._retry &&
        !AUTH_SKIP_RETRY_PATHS.some((p) => originalRequest.url?.includes(p))
      ) {
        originalRequest._retry = true

        try {
          if (!refreshPromise) {
            refreshPromise = authStore.refreshToken().finally(() => {
              refreshPromise = null
            })
          }
          const newToken = await refreshPromise
          setAuthHeader(originalRequest.headers, newToken)
          return api(originalRequest)
        } catch (refreshError) {
          return Promise.reject(refreshError)
        }
      }

      return Promise.reject(error)
    },
  )
}
