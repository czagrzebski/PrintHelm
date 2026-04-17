# UserApi

All URIs are relative to *https://printhelm.czagrzebski.dev/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**adminResetPassword**](#adminresetpassword) | **PUT** /user/{id}/password | Admin reset user password|
|[**changeMyPassword**](#changemypassword) | **PUT** /user/me/password | Change own password|
|[**createUser**](#createuser) | **POST** /user/createUser | Create a new user|
|[**deleteUser**](#deleteuser) | **DELETE** /user/{id} | Delete a user|
|[**getCurrentUser**](#getcurrentuser) | **GET** /user/me | Get current authenticated user|
|[**getUserById**](#getuserbyid) | **GET** /user/{id} | Get user by ID|
|[**getUsers**](#getusers) | **GET** /user | Get all users|
|[**updateUser**](#updateuser) | **PUT** /user/{id} | Update a user|

# **adminResetPassword**
> adminResetPassword(apiAdminResetPasswordRequest)


### Example

```typescript
import {
    UserApi,
    Configuration,
    ApiAdminResetPasswordRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new UserApi(configuration);

let id: number; // (default to undefined)
let apiAdminResetPasswordRequest: ApiAdminResetPasswordRequest; //

const { status, data } = await apiInstance.adminResetPassword(
    id,
    apiAdminResetPasswordRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiAdminResetPasswordRequest** | **ApiAdminResetPasswordRequest**|  | |
| **id** | [**number**] |  | defaults to undefined|


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
|**204** | Password reset |  -  |
|**404** | Not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **changeMyPassword**
> changeMyPassword(apiChangePasswordRequest)


### Example

```typescript
import {
    UserApi,
    Configuration,
    ApiChangePasswordRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new UserApi(configuration);

let apiChangePasswordRequest: ApiChangePasswordRequest; //

const { status, data } = await apiInstance.changeMyPassword(
    apiChangePasswordRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiChangePasswordRequest** | **ApiChangePasswordRequest**|  | |


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
|**204** | Password changed |  -  |
|**400** | Bad Request |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

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

# **deleteUser**
> deleteUser()


### Example

```typescript
import {
    UserApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new UserApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.deleteUser(
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
|**204** | User deleted |  -  |
|**404** | Not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getCurrentUser**
> ApiUserResponse getCurrentUser()


### Example

```typescript
import {
    UserApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new UserApi(configuration);

const { status, data } = await apiInstance.getCurrentUser();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**ApiUserResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Current user info |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getUserById**
> ApiUserResponse getUserById()


### Example

```typescript
import {
    UserApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new UserApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.getUserById(
    id
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|


### Return type

**ApiUserResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | User info |  -  |
|**404** | Not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getUsers**
> Array<ApiUserResponse> getUsers()


### Example

```typescript
import {
    UserApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new UserApi(configuration);

const { status, data } = await apiInstance.getUsers();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**Array<ApiUserResponse>**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | List of users |  -  |
|**403** | Forbidden |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **updateUser**
> ApiUserResponse updateUser(apiUpdateUserRequest)


### Example

```typescript
import {
    UserApi,
    Configuration,
    ApiUpdateUserRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new UserApi(configuration);

let id: number; // (default to undefined)
let apiUpdateUserRequest: ApiUpdateUserRequest; //

const { status, data } = await apiInstance.updateUser(
    id,
    apiUpdateUserRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiUpdateUserRequest** | **ApiUpdateUserRequest**|  | |
| **id** | [**number**] |  | defaults to undefined|


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
|**200** | User updated |  -  |
|**404** | Not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

