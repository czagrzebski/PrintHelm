# PrinterApi

All URIs are relative to *https://printhelm.czagrzebski.dev/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**createPrinter**](#createprinter) | **POST** /printer/createPrinter | Create a new printer|
|[**deletePrinter**](#deleteprinter) | **DELETE** /printer/{id} | Delete a printer|
|[**diagnosePrinter**](#diagnoseprinter) | **POST** /printer/{id}/diagnose | Run an AI-powered diagnostic on a printer|
|[**getPrinterById**](#getprinterbyid) | **GET** /printer/{id} | Get a printer by ID|
|[**getPrinters**](#getprinters) | **GET** /printer | Get all printers|
|[**updatePrinter**](#updateprinter) | **PUT** /printer/{id} | Update a printer|

# **createPrinter**
> CreatePrinter201Response createPrinter(apiCreatePrinterRequest)


### Example

```typescript
import {
    PrinterApi,
    Configuration,
    ApiCreatePrinterRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new PrinterApi(configuration);

let apiCreatePrinterRequest: ApiCreatePrinterRequest; //

const { status, data } = await apiInstance.createPrinter(
    apiCreatePrinterRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiCreatePrinterRequest** | **ApiCreatePrinterRequest**|  | |


### Return type

**CreatePrinter201Response**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**201** | Printer created |  -  |
|**400** | Bad Request |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **deletePrinter**
> deletePrinter()


### Example

```typescript
import {
    PrinterApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new PrinterApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.deletePrinter(
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
|**204** | Printer deleted |  -  |
|**404** | Not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **diagnosePrinter**
> ApiDiagnosticReport diagnosePrinter()


### Example

```typescript
import {
    PrinterApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new PrinterApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.diagnosePrinter(
    id
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|


### Return type

**ApiDiagnosticReport**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Diagnostic report |  -  |
|**404** | Printer not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getPrinterById**
> ApiPrinterResponse getPrinterById()


### Example

```typescript
import {
    PrinterApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new PrinterApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.getPrinterById(
    id
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|


### Return type

**ApiPrinterResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Printer details |  -  |
|**404** | Not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getPrinters**
> Array<ApiPrinterResponse> getPrinters()


### Example

```typescript
import {
    PrinterApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new PrinterApi(configuration);

const { status, data } = await apiInstance.getPrinters();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**Array<ApiPrinterResponse>**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | List of printers |  -  |
|**401** | Unauthorized |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **updatePrinter**
> ApiPrinterResponse updatePrinter(apiUpdatePrinterRequest)


### Example

```typescript
import {
    PrinterApi,
    Configuration,
    ApiUpdatePrinterRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new PrinterApi(configuration);

let id: number; // (default to undefined)
let apiUpdatePrinterRequest: ApiUpdatePrinterRequest; //

const { status, data } = await apiInstance.updatePrinter(
    id,
    apiUpdatePrinterRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiUpdatePrinterRequest** | **ApiUpdatePrinterRequest**|  | |
| **id** | [**number**] |  | defaults to undefined|


### Return type

**ApiPrinterResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Printer updated |  -  |
|**404** | Not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

