import { api, BASE_URL } from './Configuration'
import { PrinterApi } from '@/client/printhelm-web-openapi'

export const diagnosticApi = new PrinterApi(undefined, BASE_URL, api)
