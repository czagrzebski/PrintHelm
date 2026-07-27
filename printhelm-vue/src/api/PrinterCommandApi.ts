import { api, BASE_URL } from './Configuration'
import { PrinterCommandsApi } from '@/client/printhelm-web-openapi'

export const printerCommandApi = new PrinterCommandsApi(undefined, BASE_URL, api)
