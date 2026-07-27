# PrinterFilesApi

All URIs are relative to *https://printhelm.czagrzebski.dev/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**deletePrinterFile**](#deleteprinterfile) | **DELETE** /printer/{id}/files/{filename} | Delete a file from printer storage|
|[**downloadPrinterFile**](#downloadprinterfile) | **GET** /printer/{id}/files/{filename} | Download a file from printer storage|
|[**listPrinterFiles**](#listprinterfiles) | **GET** /printer/{id}/files | List files on printer storage|
|[**uploadPrinterFile**](#uploadprinterfile) | **POST** /printer/{id}/files | Upload a file to printer storage|

# **deletePrinterFile**
> deletePrinterFile()


### Example

```typescript
import {
    PrinterFilesApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new PrinterFilesApi(configuration);

let id: number; // (default to undefined)
let filename: string; // (default to undefined)

const { status, data } = await apiInstance.deletePrinterFile(
    id,
    filename
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|
| **filename** | [**string**] |  | defaults to undefined|


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
|**204** | File deleted |  -  |
|**502** | Error communicating with printer |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **downloadPrinterFile**
> File downloadPrinterFile()


### Example

```typescript
import {
    PrinterFilesApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new PrinterFilesApi(configuration);

let id: number; // (default to undefined)
let filename: string; // (default to undefined)

const { status, data } = await apiInstance.downloadPrinterFile(
    id,
    filename
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|
| **filename** | [**string**] |  | defaults to undefined|


### Return type

**File**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/octet-stream


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | File content |  -  |
|**502** | Error communicating with printer |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **listPrinterFiles**
> Array<ApiPrinterFile> listPrinterFiles()


### Example

```typescript
import {
    PrinterFilesApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new PrinterFilesApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.listPrinterFiles(
    id
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|


### Return type

**Array<ApiPrinterFile>**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | List of files |  -  |
|**502** | Error communicating with printer |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **uploadPrinterFile**
> uploadPrinterFile()


### Example

```typescript
import {
    PrinterFilesApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new PrinterFilesApi(configuration);

let id: number; // (default to undefined)
let file: File; // (default to undefined)

const { status, data } = await apiInstance.uploadPrinterFile(
    id,
    file
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|
| **file** | [**File**] |  | defaults to undefined|


### Return type

void (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**204** | File uploaded |  -  |
|**400** | Bad request |  -  |
|**502** | Error communicating with printer |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

