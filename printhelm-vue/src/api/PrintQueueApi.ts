import { api, BASE_URL } from './Configuration'
import { PrintQueueApi } from '@/client/printhelm-web-openapi'

const printQueueApi: PrintQueueApi = new PrintQueueApi(undefined, BASE_URL, api)
export default printQueueApi
