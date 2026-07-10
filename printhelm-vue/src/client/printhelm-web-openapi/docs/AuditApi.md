# AuditApi

All URIs are relative to *https://printhelm.czagrzebski.dev/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**getAuditLog**](#getauditlog) | **GET** /audit | Search the audit log (admin only)|

# **getAuditLog**
> ApiAuditLogPage getAuditLog()


### Example

```typescript
import {
    AuditApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new AuditApi(configuration);

let page: number; // (optional) (default to 0)
let size: number; // (optional) (default to 25)
let entityType: string; // (optional) (default to undefined)
let username: string; // (optional) (default to undefined)
let action: string; // (optional) (default to undefined)

const { status, data } = await apiInstance.getAuditLog(
    page,
    size,
    entityType,
    username,
    action
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **page** | [**number**] |  | (optional) defaults to 0|
| **size** | [**number**] |  | (optional) defaults to 25|
| **entityType** | [**string**] |  | (optional) defaults to undefined|
| **username** | [**string**] |  | (optional) defaults to undefined|
| **action** | [**string**] |  | (optional) defaults to undefined|


### Return type

**ApiAuditLogPage**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Page of audit log entries |  -  |
|**403** | Forbidden |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

