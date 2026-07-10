import { BASE_URL, api } from './Configuration'
import { JobOrderApi } from '@/client/printhelm-web-openapi'

const jobOrderApi = new JobOrderApi(undefined, BASE_URL, api)
export default jobOrderApi

export async function uploadPartFiles(orderId: number, files: File[], description?: string) {
  return jobOrderApi.uploadJobOrderPartFile(orderId, files, description)
}

export async function uploadGcodeFiles(orderId: number, files: File[], description?: string) {
  return jobOrderApi.uploadJobOrderGcodeFile(orderId, files, description)
}

export async function listPartFileVersions(orderId: number) {
  return jobOrderApi.getJobOrderPartFileVersions(orderId)
}

export async function listGcodeFileVersions(orderId: number) {
  return jobOrderApi.getJobOrderGcodeFileVersions(orderId)
}

export async function selectGcodeFileVersion(orderId: number, versionId: number, fileIndex = 0) {
  return jobOrderApi.selectJobOrderGcodeFileVersion(orderId, versionId, fileIndex)
}

export async function updateGcodeVersionQuantities(
  orderId: number,
  versionId: number,
  quantities: { fileIndex: number; quantity: number }[],
) {
  return jobOrderApi.updateJobOrderGcodeFileVersionQuantities(orderId, versionId, quantities)
}

export async function downloadGcodeFileVersionFile(orderId: number, versionId: number, fileIndex: number, filename: string) {
  const res = await jobOrderApi.downloadJobOrderGcodeFileVersionFile(orderId, versionId, fileIndex, { responseType: 'blob' })
  triggerDownload(res.data as unknown as Blob, filename)
}

export async function downloadPartFileVersionFile(orderId: number, versionId: number, fileIndex: number, filename: string) {
  const res = await jobOrderApi.downloadJobOrderPartFileVersionFile(orderId, versionId, fileIndex, { responseType: 'blob' })
  triggerDownload(res.data as unknown as Blob, filename)
}

export async function fetchPartFileVersionFileBuffer(orderId: number, versionId: number, fileIndex: number): Promise<ArrayBuffer> {
  const res = await jobOrderApi.downloadJobOrderPartFileVersionFile(orderId, versionId, fileIndex, { responseType: 'arraybuffer' })
  return res.data as unknown as ArrayBuffer
}

export async function fetchGcodeFileVersionFileBuffer(orderId: number, versionId: number, fileIndex: number): Promise<ArrayBuffer> {
  const res = await jobOrderApi.downloadJobOrderGcodeFileVersionFile(orderId, versionId, fileIndex, { responseType: 'arraybuffer' })
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
