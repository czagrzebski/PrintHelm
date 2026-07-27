import { api, BASE_URL } from './Configuration'
import { PrinterApi, PrinterFilesApi } from '@/client/printhelm-web-openapi'

export const printerApi = new PrinterApi(undefined, BASE_URL, api)
export const printerFilesApi = new PrinterFilesApi(undefined, BASE_URL, api)
