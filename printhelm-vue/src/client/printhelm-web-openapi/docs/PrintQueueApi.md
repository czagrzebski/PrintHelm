# PrintQueueApi

All URIs are relative to *https://printhelm.czagrzebski.dev/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**addJobToQueue**](#addjobtoqueue) | **POST** /printer/{printerId}/queue | Assign a job order to a printer\&#39;s queue|
|[**getPrinterQueue**](#getprinterqueue) | **GET** /printer/{printerId}/queue | Get the print queue for a printer|
|[**removeJobFromQueue**](#removejobfromqueue) | **DELETE** /printer/{printerId}/queue/{jobOrderId} | Remove a job order from the print queue|
|[**reorderQueue**](#reorderqueue) | **PATCH** /printer/{printerId}/queue | Reorder jobs in the print queue|
|[**startQueuedJob**](#startqueuedjob) | **POST** /printer/{printerId}/queue/{jobOrderId}/start | Start printing a queued job|

# **addJobToQueue**
> ApiJobOrderResponse addJobToQueue(apiQueueJobRequest)


### Example

```typescript
import {
    PrintQueueApi,
    Configuration,
    ApiQueueJobRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new PrintQueueApi(configuration);

let printerId: number; // (default to undefined)
let apiQueueJobRequest: ApiQueueJobRequest; //

const { status, data } = await apiInstance.addJobToQueue(
    printerId,
    apiQueueJobRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiQueueJobRequest** | **ApiQueueJobRequest**|  | |
| **printerId** | [**number**] |  | defaults to undefined|


### Return type

**ApiJobOrderResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**201** | Job assigned to queue |  -  |
|**400** | Bad request (e.g. job not in READY_TO_PRINT status) |  -  |
|**409** | Job already assigned to a printer |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getPrinterQueue**
> Array<ApiJobOrderResponse> getPrinterQueue()


### Example

```typescript
import {
    PrintQueueApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new PrintQueueApi(configuration);

let printerId: number; // (default to undefined)

const { status, data } = await apiInstance.getPrinterQueue(
    printerId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **printerId** | [**number**] |  | defaults to undefined|


### Return type

**Array<ApiJobOrderResponse>**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Ordered list of queued job orders |  -  |
|**404** | Printer not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **removeJobFromQueue**
> removeJobFromQueue()


### Example

```typescript
import {
    PrintQueueApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new PrintQueueApi(configuration);

let printerId: number; // (default to undefined)
let jobOrderId: number; // (default to undefined)

const { status, data } = await apiInstance.removeJobFromQueue(
    printerId,
    jobOrderId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **printerId** | [**number**] |  | defaults to undefined|
| **jobOrderId** | [**number**] |  | defaults to undefined|


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
|**204** | Job removed from queue |  -  |
|**404** | Not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **reorderQueue**
> reorderQueue(apiQueueReorderRequest)


### Example

```typescript
import {
    PrintQueueApi,
    Configuration,
    ApiQueueReorderRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new PrintQueueApi(configuration);

let printerId: number; // (default to undefined)
let apiQueueReorderRequest: ApiQueueReorderRequest; //

const { status, data } = await apiInstance.reorderQueue(
    printerId,
    apiQueueReorderRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiQueueReorderRequest** | **ApiQueueReorderRequest**|  | |
| **printerId** | [**number**] |  | defaults to undefined|


### Return type

void (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**204** | Queue reordered |  -  |
|**400** | Bad request |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **startQueuedJob**
> startQueuedJob(apiQueueStartRequest)


### Example

```typescript
import {
    PrintQueueApi,
    Configuration,
    ApiQueueStartRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new PrintQueueApi(configuration);

let printerId: number; // (default to undefined)
let jobOrderId: number; // (default to undefined)
let apiQueueStartRequest: ApiQueueStartRequest; //

const { status, data } = await apiInstance.startQueuedJob(
    printerId,
    jobOrderId,
    apiQueueStartRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiQueueStartRequest** | **ApiQueueStartRequest**|  | |
| **printerId** | [**number**] |  | defaults to undefined|
| **jobOrderId** | [**number**] |  | defaults to undefined|


### Return type

void (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**204** | Print started |  -  |
|**400** | Bad request |  -  |
|**409** | Printer already printing or printer not connected |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

