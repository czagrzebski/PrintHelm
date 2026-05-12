import { api, BASE_URL } from './Configuration'
import { ChatApi } from '@/client/printhelm-web-openapi'

export const chatApi = new ChatApi(undefined, BASE_URL, api)
