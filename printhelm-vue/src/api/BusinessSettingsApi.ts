import { SettingsApi } from '@/client/printhelm-web-openapi'
import type { ApiBusinessSettings } from '@/client/printhelm-web-openapi'
import { api } from './Configuration'

const settingsApi = new SettingsApi(undefined, '', api)

export async function getBusinessSettings(): Promise<ApiBusinessSettings> {
  const res = await settingsApi.getBusinessSettings()
  return res.data
}

export async function updateBusinessSettings(settings: ApiBusinessSettings): Promise<ApiBusinessSettings> {
  const res = await settingsApi.updateBusinessSettings(settings)
  return res.data
}
