# JobOrderApi

All URIs are relative to *https://printhelm.czagrzebski.dev/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**createJobOrder**](#createjoborder) | **POST** /job-order | Create a new job order|
|[**deleteJobOrder**](#deletejoborder) | **DELETE** /job-order/{id} | Delete a job order|
|[**getJobOrderById**](#getjoborderbyid) | **GET** /job-order/{id} | Get a job order by ID|
|[**getJobOrders**](#getjoborders) | **GET** /job-order | Get all job orders|
|[**updateJobOrder**](#updatejoborder) | **PUT** /job-order/{id} | Update a job order|

# **createJobOrder**
> ApiJobOrderResponse createJobOrder(apiCreateJobOrderRequest)


### Example

```typescript
import {
    JobOrderApi,
    Configuration,
    ApiCreateJobOrderRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

let apiCreateJobOrderRequest: ApiCreateJobOrderRequest; //

const { status, data } = await apiInstance.createJobOrder(
    apiCreateJobOrderRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiCreateJobOrderRequest** | **ApiCreateJobOrderRequest**|  | |


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
|**201** | Job order created |  -  |
|**400** | Bad Request |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **deleteJobOrder**
> deleteJobOrder()


### Example

```typescript
import {
    JobOrderApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.deleteJobOrder(
    id
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|


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
|**204** | Job order deleted |  -  |
|**404** | Not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getJobOrderById**
> ApiJobOrderResponse getJobOrderById()


### Example

```typescript
import {
    JobOrderApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.getJobOrderById(
    id
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|


### Return type

**ApiJobOrderResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Job order details |  -  |
|**404** | Not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getJobOrders**
> Array<ApiJobOrderResponse> getJobOrders()


### Example

```typescript
import {
    JobOrderApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

const { status, data } = await apiInstance.getJobOrders();
```

### Parameters
This endpoint does not have any parameters.


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
|**200** | List of job orders |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **updateJobOrder**
> ApiJobOrderResponse updateJobOrder(apiUpdateJobOrderRequest)


### Example

```typescript
import {
    JobOrderApi,
    Configuration,
    ApiUpdateJobOrderRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

let id: number; // (default to undefined)
let apiUpdateJobOrderRequest: ApiUpdateJobOrderRequest; //

const { status, data } = await apiInstance.updateJobOrder(
    id,
    apiUpdateJobOrderRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiUpdateJobOrderRequest** | **ApiUpdateJobOrderRequest**|  | |
| **id** | [**number**] |  | defaults to undefined|


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
|**200** | Job order updated |  -  |
|**404** | Not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

