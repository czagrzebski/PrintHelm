# UserApi

All URIs are relative to *https://printhelm.czagrzebski.dev/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**createUser**](#createuser) | **POST** /user/createUser | Create a new user|

# **createUser**
> ApiUserResponse createUser(apiCreateUserRequest)


### Example

```typescript
import {
    UserApi,
    Configuration,
    ApiCreateUserRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new UserApi(configuration);

let apiCreateUserRequest: ApiCreateUserRequest; //

const { status, data } = await apiInstance.createUser(
    apiCreateUserRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiCreateUserRequest** | **ApiCreateUserRequest**|  | |


### Return type

**ApiUserResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**201** | User created |  -  |
|**400** | Bad Request |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

