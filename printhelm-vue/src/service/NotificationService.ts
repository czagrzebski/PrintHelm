import { api, BASE_URL } from '@/api/Configuration'
import { NotificationsApi } from '@/client/printhelm-web-openapi'

const notificationApi = new NotificationsApi(undefined, BASE_URL, api)

export default notificationApi
export type { ApiNotification, ApiUnreadCountResponse, NotificationType, NotificationSeverity } from '@/client/printhelm-web-openapi'
