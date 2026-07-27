import { BASE_URL, api } from './Configuration'
import { AnalyticsApi } from '@/client/printhelm-web-openapi'

const analyticsApi = new AnalyticsApi(undefined, BASE_URL, api)
export default analyticsApi
