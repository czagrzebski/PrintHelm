# RoleApi

All URIs are relative to *https://printhelm.czagrzebski.dev/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**getRoles**](#getroles) | **GET** /role | Get all roles|

# **getRoles**
> Array<ApiRole> getRoles()


### Example

```typescript
import {
    RoleApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new RoleApi(configuration);

const { status, data } = await apiInstance.getRoles();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**Array<ApiRole>**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | List of roles |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

