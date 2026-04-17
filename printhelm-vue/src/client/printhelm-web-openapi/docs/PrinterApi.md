# PrinterApi

All URIs are relative to *https://printhelm.czagrzebski.dev/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**createPrinter**](#createprinter) | **POST** /printer/createPrinter | Create a new printer|

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

