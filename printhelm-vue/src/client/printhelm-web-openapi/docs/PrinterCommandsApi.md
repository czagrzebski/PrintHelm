# PrinterCommandsApi

All URIs are relative to *https://printhelm.czagrzebski.dev/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**homeAxes**](#homeaxes) | **POST** /printer/{id}/command/home | Home all axes|
|[**jogAxis**](#jogaxis) | **POST** /printer/{id}/command/jog | Jog an axis|
|[**pausePrint**](#pauseprint) | **POST** /printer/{id}/command/pause | Pause the current print|
|[**resumePrint**](#resumeprint) | **POST** /printer/{id}/command/resume | Resume the current print|
|[**setBedTemp**](#setbedtemp) | **POST** /printer/{id}/command/bed-temp | Set bed target temperature|
|[**setLight**](#setlight) | **POST** /printer/{id}/command/light | Set light state|
|[**setNozzleTemp**](#setnozzletemp) | **POST** /printer/{id}/command/nozzle-temp | Set nozzle target temperature|
|[**setSpeed**](#setspeed) | **POST** /printer/{id}/command/speed | Set print speed level|
|[**startPrint**](#startprint) | **POST** /printer/{id}/command/print | Start a print job|
|[**stopPrint**](#stopprint) | **POST** /printer/{id}/command/stop | Stop the current print|

# **homeAxes**
> homeAxes()


### Example

```typescript
import {
    PrinterCommandsApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new PrinterCommandsApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.homeAxes(
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
|**204** | Command sent |  -  |
|**409** | Printer not connected |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **jogAxis**
> jogAxis(apiJogRequest)


### Example

```typescript
import {
    PrinterCommandsApi,
    Configuration,
    ApiJogRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new PrinterCommandsApi(configuration);

let id: number; // (default to undefined)
let apiJogRequest: ApiJogRequest; //

const { status, data } = await apiInstance.jogAxis(
    id,
    apiJogRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiJogRequest** | **ApiJogRequest**|  | |
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
|**204** | Command sent |  -  |
|**409** | Printer not connected |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **pausePrint**
> pausePrint()


### Example

```typescript
import {
    PrinterCommandsApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new PrinterCommandsApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.pausePrint(
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
|**204** | Command sent |  -  |
|**409** | Printer not connected |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **resumePrint**
> resumePrint()


### Example

```typescript
import {
    PrinterCommandsApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new PrinterCommandsApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.resumePrint(
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
|**204** | Command sent |  -  |
|**409** | Printer not connected |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **setBedTemp**
> setBedTemp(apiTempRequest)


### Example

```typescript
import {
    PrinterCommandsApi,
    Configuration,
    ApiTempRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new PrinterCommandsApi(configuration);

let id: number; // (default to undefined)
let apiTempRequest: ApiTempRequest; //

const { status, data } = await apiInstance.setBedTemp(
    id,
    apiTempRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiTempRequest** | **ApiTempRequest**|  | |
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
|**204** | Command sent |  -  |
|**400** | Temperature out of range (0–110°C) |  -  |
|**409** | Printer not connected |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **setLight**
> setLight(apiLightRequest)


### Example

```typescript
import {
    PrinterCommandsApi,
    Configuration,
    ApiLightRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new PrinterCommandsApi(configuration);

let id: number; // (default to undefined)
let apiLightRequest: ApiLightRequest; //

const { status, data } = await apiInstance.setLight(
    id,
    apiLightRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiLightRequest** | **ApiLightRequest**|  | |
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
|**204** | Command sent |  -  |
|**409** | Printer not connected |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **setNozzleTemp**
> setNozzleTemp(apiTempRequest)


### Example

```typescript
import {
    PrinterCommandsApi,
    Configuration,
    ApiTempRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new PrinterCommandsApi(configuration);

let id: number; // (default to undefined)
let apiTempRequest: ApiTempRequest; //

const { status, data } = await apiInstance.setNozzleTemp(
    id,
    apiTempRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiTempRequest** | **ApiTempRequest**|  | |
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
|**204** | Command sent |  -  |
|**400** | Temperature out of range (0–300°C) |  -  |
|**409** | Printer not connected |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **setSpeed**
> setSpeed(apiSpeedRequest)


### Example

```typescript
import {
    PrinterCommandsApi,
    Configuration,
    ApiSpeedRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new PrinterCommandsApi(configuration);

let id: number; // (default to undefined)
let apiSpeedRequest: ApiSpeedRequest; //

const { status, data } = await apiInstance.setSpeed(
    id,
    apiSpeedRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiSpeedRequest** | **ApiSpeedRequest**|  | |
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
|**204** | Command sent |  -  |
|**409** | Printer not connected |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **startPrint**
> startPrint(apiPrintFileRequest)


### Example

```typescript
import {
    PrinterCommandsApi,
    Configuration,
    ApiPrintFileRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new PrinterCommandsApi(configuration);

let id: number; // (default to undefined)
let apiPrintFileRequest: ApiPrintFileRequest; //

const { status, data } = await apiInstance.startPrint(
    id,
    apiPrintFileRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiPrintFileRequest** | **ApiPrintFileRequest**|  | |
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
|**204** | Command sent |  -  |
|**409** | Printer not connected |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **stopPrint**
> stopPrint()


### Example

```typescript
import {
    PrinterCommandsApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new PrinterCommandsApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.stopPrint(
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
|**204** | Command sent |  -  |
|**409** | Printer not connected |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

