import { ref } from 'vue'
import { api } from '@/api/Configuration'
import type { AxiosError } from 'axios'
import type { ApiPrinterFile } from '@/client/printhelm-web-openapi'

export type { ApiPrinterFile }

export function usePrinterFiles(printerId: number) {
  const files = ref<ApiPrinterFile[]>([])
  const loading = ref(false)
  const uploading = ref(false)
  const uploadProgress = ref(0)
  const error = ref<string | null>(null)

  async function fetchFiles() {
    loading.value = true
    error.value = null
    try {
      const res = await api.get<ApiPrinterFile[]>(`/printer/${printerId}/files`)
      files.value = res.data
    } catch (e) {
      error.value = extractError(e)
    } finally {
      loading.value = false
    }
  }

  async function uploadFile(file: File) {
    uploading.value = true
    uploadProgress.value = 0
    error.value = null
    const formData = new FormData()
    formData.append('file', file)
    try {
      await api.post(`/printer/${printerId}/files`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        onUploadProgress: (e) => {
          if (e.total) uploadProgress.value = Math.round((e.loaded / e.total) * 100)
        },
      })
    } catch (e) {
      error.value = extractError(e)
      throw e
    } finally {
      uploading.value = false
      uploadProgress.value = 0
    }
  }

  async function deleteFile(name: string) {
    error.value = null
    try {
      await api.delete(`/printer/${printerId}/files/${encodeURIComponent(name)}`)
      files.value = files.value.filter(f => f.name !== name)
    } catch (e) {
      error.value = extractError(e)
      throw e
    }
  }

  function extractError(e: unknown): string {
    const axiosErr = e as AxiosError<string>
    return axiosErr.response?.data ?? axiosErr.message ?? 'Unknown error'
  }

  return { files, loading, uploading, uploadProgress, error, fetchFiles, uploadFile, deleteFile }
}
