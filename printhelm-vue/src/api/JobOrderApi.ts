import { api, BASE_URL } from './Configuration'
import { JobOrderApi } from '@/client/printhelm-web-openapi'

const jobOrderApi: JobOrderApi = new JobOrderApi(undefined, BASE_URL, api)
export default jobOrderApi
