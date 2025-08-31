# AuthApi

All URIs are relative to *https://printhelm.czagrzebski.dev/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**login**](#login) | **POST** /auth/login | User login|
|[**refresh**](#refresh) | **POST** /auth/refresh | Refresh access token|

# **login**
> ApiAuthResponse login(apiLoginRequest)


### Example

```typescript
import {
    AuthApi,
    Configuration,
    ApiLoginRequest
} from 'restClient';

const configuration = new Configuration();
const apiInstance = new AuthApi(configuration);

let apiLoginRequest: ApiLoginRequest; //

const { status, data } = await apiInstance.login(
    apiLoginRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiLoginRequest** | **ApiLoginRequest**|  | |


### Return type

**ApiAuthResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Successful login |  -  |
|**401** | Unauthorized |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **refresh**
> ApiAuthResponse refresh()


### Example

```typescript
import {
    AuthApi,
    Configuration
} from 'restClient';

const configuration = new Configuration();
const apiInstance = new AuthApi(configuration);

const { status, data } = await apiInstance.refresh();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**ApiAuthResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Token refreshed |  -  |
|**401** | Invalid refresh token |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

