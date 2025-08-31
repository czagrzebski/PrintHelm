import { api, BASE_URL } from './Configuration'
import { AuthApi } from '@/client/printhelm-web-openapi'

// Create new API instance
const authApi: AuthApi = new AuthApi(undefined, BASE_URL, api)
export default authApi
