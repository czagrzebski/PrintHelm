# ChatApi

All URIs are relative to *https://printhelm.czagrzebski.dev/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**deleteChatSession**](#deletechatsession) | **DELETE** /chat/sessions/{sessionId} | Delete a chat session|
|[**getChatSession**](#getchatsession) | **GET** /chat/sessions/{sessionId} | Get full message history for a chat session|
|[**getChatSessions**](#getchatsessions) | **GET** /chat/sessions | List all chat sessions for the current user|
|[**sendChatMessage**](#sendchatmessage) | **POST** /chat/message | Send a message to the AI assistant|

# **deleteChatSession**
> deleteChatSession()


### Example

```typescript
import {
    ChatApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new ChatApi(configuration);

let sessionId: string; // (default to undefined)

const { status, data } = await apiInstance.deleteChatSession(
    sessionId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **sessionId** | [**string**] |  | defaults to undefined|


### Return type

void (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**204** | Session deleted |  -  |
|**404** | Session not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getChatSession**
> ApiChatSessionDetail getChatSession()


### Example

```typescript
import {
    ChatApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new ChatApi(configuration);

let sessionId: string; // (default to undefined)

const { status, data } = await apiInstance.getChatSession(
    sessionId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **sessionId** | [**string**] |  | defaults to undefined|


### Return type

**ApiChatSessionDetail**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Chat session with messages |  -  |
|**404** | Session not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getChatSessions**
> Array<ApiChatSessionSummary> getChatSessions()


### Example

```typescript
import {
    ChatApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new ChatApi(configuration);

const { status, data } = await apiInstance.getChatSessions();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**Array<ApiChatSessionSummary>**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | List of chat session summaries |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **sendChatMessage**
> ApiChatResponse sendChatMessage(apiChatRequest)


### Example

```typescript
import {
    ChatApi,
    Configuration,
    ApiChatRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new ChatApi(configuration);

let apiChatRequest: ApiChatRequest; //

const { status, data } = await apiInstance.sendChatMessage(
    apiChatRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiChatRequest** | **ApiChatRequest**|  | |


### Return type

**ApiChatResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | AI response message |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

