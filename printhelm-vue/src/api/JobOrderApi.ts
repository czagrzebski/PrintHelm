import { BASE_URL, api } from './Configuration'
import { JobOrderApi } from '@/client/printhelm-web-openapi'

const jobOrderApi = new JobOrderApi(undefined, BASE_URL, api)
export default jobOrderApi

export async function uploadPartFile(orderId: number, file: File, description?: string) {
  return jobOrderApi.uploadJobOrderPartFile(orderId, file, description)
}

export async function uploadGcodeFile(orderId: number, file: File, description?: string) {
  return jobOrderApi.uploadJobOrderGcodeFile(orderId, file, description)
}

export async function listPartFileVersions(orderId: number) {
  return jobOrderApi.getJobOrderPartFileVersions(orderId)
}

export async function listGcodeFileVersions(orderId: number) {
  return jobOrderApi.getJobOrderGcodeFileVersions(orderId)
}

export async function selectGcodeFileVersion(orderId: number, versionId: number) {
  return jobOrderApi.selectJobOrderGcodeFileVersion(orderId, versionId)
}

export async function downloadPartFileVersion(orderId: number, versionId: number, filename: string) {
  const res = await jobOrderApi.downloadJobOrderPartFileVersion(orderId, versionId, { responseType: 'blob' })
  triggerDownload(res.data as unknown as Blob, filename)
}

export async function downloadGcodeFileVersion(orderId: number, versionId: number, filename: string) {
  const res = await jobOrderApi.downloadJobOrderGcodeFileVersion(orderId, versionId, { responseType: 'blob' })
  triggerDownload(res.data as unknown as Blob, filename)
}

export async function fetchPartFileVersionBuffer(orderId: number, versionId: number): Promise<ArrayBuffer> {
  const res = await jobOrderApi.downloadJobOrderPartFileVersion(orderId, versionId, { responseType: 'arraybuffer' })
  return res.data as unknown as ArrayBuffer
}

export async function fetchGcodeFileVersionBuffer(orderId: number, versionId: number): Promise<ArrayBuffer> {
  const res = await jobOrderApi.downloadJobOrderGcodeFileVersion(orderId, versionId, { responseType: 'arraybuffer' })
  return res.data as unknown as ArrayBuffer
}

export async function fetchPartFileBuffer(orderId: number): Promise<ArrayBuffer> {
  const res = await jobOrderApi.downloadJobOrderPartFile(orderId, { responseType: 'arraybuffer' })
  return res.data as unknown as ArrayBuffer
}

export async function downloadPartFile(orderId: number, filename: string) {
  const res = await jobOrderApi.downloadJobOrderPartFile(orderId, { responseType: 'blob' })
  triggerDownload(res.data as unknown as Blob, filename)
}

export async function downloadGcodeFile(orderId: number, filename: string) {
  const res = await jobOrderApi.downloadJobOrderGcodeFile(orderId, { responseType: 'blob' })
  triggerDownload(res.data as unknown as Blob, filename)
}

export async function fetchGcodeFileBuffer(orderId: number): Promise<ArrayBuffer> {
  const res = await jobOrderApi.downloadJobOrderGcodeFile(orderId, { responseType: 'arraybuffer' })
  return res.data as unknown as ArrayBuffer
}

export async function downloadInvoice(orderId: number) {
  const res = await jobOrderApi.downloadJobOrderInvoice(orderId, { responseType: 'blob' })
  triggerDownload(res.data as unknown as Blob, `invoice-${orderId}.pdf`)
}

export async function downloadQuote(orderId: number) {
  const res = await jobOrderApi.downloadJobOrderQuote(orderId, { responseType: 'blob' })
  triggerDownload(res.data as unknown as Blob, `quote-${orderId}.pdf`)
}


function triggerDownload(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}
