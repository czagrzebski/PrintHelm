# SettingsApi

All URIs are relative to *https://printhelm.czagrzebski.dev/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**getBusinessSettings**](#getbusinesssettings) | **GET** /settings/business | Get business settings|
|[**updateBusinessSettings**](#updatebusinesssettings) | **PUT** /settings/business | Update business settings|

# **getBusinessSettings**
> ApiBusinessSettings getBusinessSettings()


### Example

```typescript
import {
    SettingsApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new SettingsApi(configuration);

const { status, data } = await apiInstance.getBusinessSettings();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**ApiBusinessSettings**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Business settings |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **updateBusinessSettings**
> ApiBusinessSettings updateBusinessSettings(apiBusinessSettings)


### Example

```typescript
import {
    SettingsApi,
    Configuration,
    ApiBusinessSettings
} from './api';

const configuration = new Configuration();
const apiInstance = new SettingsApi(configuration);

let apiBusinessSettings: ApiBusinessSettings; //

const { status, data } = await apiInstance.updateBusinessSettings(
    apiBusinessSettings
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiBusinessSettings** | **ApiBusinessSettings**|  | |


### Return type

**ApiBusinessSettings**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Updated business settings |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

