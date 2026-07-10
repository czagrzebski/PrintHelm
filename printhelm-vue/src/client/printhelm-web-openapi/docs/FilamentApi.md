# FilamentApi

All URIs are relative to *https://printhelm.czagrzebski.dev/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**createFilamentSpool**](#createfilamentspool) | **POST** /filament/spools | Add a filament spool to the inventory|
|[**deleteFilamentSpool**](#deletefilamentspool) | **DELETE** /filament/spools/{id} | Delete a filament spool|
|[**getFilamentSpools**](#getfilamentspools) | **GET** /filament/spools | List all filament spools|
|[**updateFilamentSpool**](#updatefilamentspool) | **PUT** /filament/spools/{id} | Update a filament spool|

# **createFilamentSpool**
> ApiFilamentSpool createFilamentSpool(apiFilamentSpoolRequest)


### Example

```typescript
import {
    FilamentApi,
    Configuration,
    ApiFilamentSpoolRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new FilamentApi(configuration);

let apiFilamentSpoolRequest: ApiFilamentSpoolRequest; //

const { status, data } = await apiInstance.createFilamentSpool(
    apiFilamentSpoolRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiFilamentSpoolRequest** | **ApiFilamentSpoolRequest**|  | |


### Return type

**ApiFilamentSpool**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**201** | Spool created |  -  |
|**400** | Bad Request |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **deleteFilamentSpool**
> deleteFilamentSpool()


### Example

```typescript
import {
    FilamentApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new FilamentApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.deleteFilamentSpool(
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
|**204** | Spool deleted |  -  |
|**404** | Spool not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getFilamentSpools**
> Array<ApiFilamentSpool> getFilamentSpools()


### Example

```typescript
import {
    FilamentApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new FilamentApi(configuration);

const { status, data } = await apiInstance.getFilamentSpools();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**Array<ApiFilamentSpool>**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | List of filament spools |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **updateFilamentSpool**
> ApiFilamentSpool updateFilamentSpool(apiFilamentSpoolRequest)


### Example

```typescript
import {
    FilamentApi,
    Configuration,
    ApiFilamentSpoolRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new FilamentApi(configuration);

let id: number; // (default to undefined)
let apiFilamentSpoolRequest: ApiFilamentSpoolRequest; //

const { status, data } = await apiInstance.updateFilamentSpool(
    id,
    apiFilamentSpoolRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiFilamentSpoolRequest** | **ApiFilamentSpoolRequest**|  | |
| **id** | [**number**] |  | defaults to undefined|


### Return type

**ApiFilamentSpool**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Spool updated |  -  |
|**404** | Spool not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

