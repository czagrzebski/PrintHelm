import { BASE_URL, api } from './Configuration'
import { FilamentApi } from '@/client/printhelm-web-openapi'

const filamentApi = new FilamentApi(undefined, BASE_URL, api)
export default filamentApi
