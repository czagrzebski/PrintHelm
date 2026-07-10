import { BASE_URL, api } from './Configuration'
import { AuditApi } from '@/client/printhelm-web-openapi'

const auditApi = new AuditApi(undefined, BASE_URL, api)
export default auditApi
