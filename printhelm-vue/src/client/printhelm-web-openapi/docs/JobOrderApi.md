# JobOrderApi

All URIs are relative to *https://printhelm.czagrzebski.dev/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**createJobOrder**](#createjoborder) | **POST** /job-order | Create a new job order|
|[**deleteJobOrder**](#deletejoborder) | **DELETE** /job-order/{id} | Delete a job order|
|[**downloadJobOrderGcodeFile**](#downloadjobordergcodefile) | **GET** /job-order/{id}/gcode-file | Download GCode file for a job order|
|[**downloadJobOrderGcodeFileVersion**](#downloadjobordergcodefileversion) | **GET** /job-order/{id}/gcode-file/versions/{versionId} | Download a specific version of the GCode file|
|[**downloadJobOrderGcodeFileVersionFile**](#downloadjobordergcodefileversionfile) | **GET** /job-order/{id}/gcode-file/versions/{versionId}/files/{fileIndex} | Download a single file from a specific version of the GCode files|
|[**downloadJobOrderInvoice**](#downloadjoborderinvoice) | **GET** /job-order/{id}/invoice | Download invoice PDF for a job order|
|[**downloadJobOrderPartFile**](#downloadjoborderpartfile) | **GET** /job-order/{id}/part-file | Download 3D part file for a job order|
|[**downloadJobOrderPartFileVersion**](#downloadjoborderpartfileversion) | **GET** /job-order/{id}/part-file/versions/{versionId} | Download a specific version of the 3D part file|
|[**downloadJobOrderPartFileVersionFile**](#downloadjoborderpartfileversionfile) | **GET** /job-order/{id}/part-file/versions/{versionId}/files/{fileIndex} | Download a single file from a specific version of the 3D design files|
|[**downloadJobOrderQuote**](#downloadjoborderquote) | **GET** /job-order/{id}/quote | Download quote PDF for a job order|
|[**getJobOrderById**](#getjoborderbyid) | **GET** /job-order/{id} | Get a job order by ID|
|[**getJobOrderGcodeFileVersions**](#getjobordergcodefileversions) | **GET** /job-order/{id}/gcode-file/versions | List all uploaded versions of the GCode file|
|[**getJobOrderPartFileVersions**](#getjoborderpartfileversions) | **GET** /job-order/{id}/part-file/versions | List all uploaded versions of the 3D part file|
|[**getJobOrders**](#getjoborders) | **GET** /job-order | Get all job orders|
|[**selectJobOrderGcodeFileVersion**](#selectjobordergcodefileversion) | **POST** /job-order/{id}/gcode-file/versions/{versionId}/select | Select a GCode file version (and file within it) as the active file used for printing|
|[**updateJobOrder**](#updatejoborder) | **PUT** /job-order/{id} | Update a job order|
|[**updateJobOrderGcodeFileVersionQuantities**](#updatejobordergcodefileversionquantities) | **PUT** /job-order/{id}/gcode-file/versions/{versionId}/quantities | Set the required print quantity per file of a GCode version (the print plan)|
|[**uploadJobOrderGcodeFile**](#uploadjobordergcodefile) | **POST** /job-order/{id}/gcode-file | Upload a new version of the GCode files for a job order (multiple files when an assembly is sliced into one file per part)|
|[**uploadJobOrderPartFile**](#uploadjoborderpartfile) | **POST** /job-order/{id}/part-file | Upload a new version of the 3D design files for a job order (one or more .glb/.gltf files forming an assembly)|

# **createJobOrder**
> ApiJobOrderResponse createJobOrder(apiCreateJobOrderRequest)


### Example

```typescript
import {
    JobOrderApi,
    Configuration,
    ApiCreateJobOrderRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

let apiCreateJobOrderRequest: ApiCreateJobOrderRequest; //

const { status, data } = await apiInstance.createJobOrder(
    apiCreateJobOrderRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiCreateJobOrderRequest** | **ApiCreateJobOrderRequest**|  | |


### Return type

**ApiJobOrderResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**201** | Job order created |  -  |
|**400** | Bad Request |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **deleteJobOrder**
> deleteJobOrder()


### Example

```typescript
import {
    JobOrderApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.deleteJobOrder(
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
|**204** | Job order deleted |  -  |
|**404** | Not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **downloadJobOrderGcodeFile**
> File downloadJobOrderGcodeFile()


### Example

```typescript
import {
    JobOrderApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.downloadJobOrderGcodeFile(
    id
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|


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
|**404** | Job order or file not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **downloadJobOrderGcodeFileVersion**
> File downloadJobOrderGcodeFileVersion()


### Example

```typescript
import {
    JobOrderApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

let id: number; // (default to undefined)
let versionId: number; // (default to undefined)

const { status, data } = await apiInstance.downloadJobOrderGcodeFileVersion(
    id,
    versionId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|
| **versionId** | [**number**] |  | defaults to undefined|


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
|**404** | Job order or version not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **downloadJobOrderGcodeFileVersionFile**
> File downloadJobOrderGcodeFileVersionFile()


### Example

```typescript
import {
    JobOrderApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

let id: number; // (default to undefined)
let versionId: number; // (default to undefined)
let fileIndex: number; // (default to undefined)

const { status, data } = await apiInstance.downloadJobOrderGcodeFileVersionFile(
    id,
    versionId,
    fileIndex
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|
| **versionId** | [**number**] |  | defaults to undefined|
| **fileIndex** | [**number**] |  | defaults to undefined|


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
|**404** | Job order, version, or file not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **downloadJobOrderInvoice**
> File downloadJobOrderInvoice()


### Example

```typescript
import {
    JobOrderApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.downloadJobOrderInvoice(
    id
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|


### Return type

**File**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/pdf


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Invoice PDF |  -  |
|**404** | Job order not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **downloadJobOrderPartFile**
> File downloadJobOrderPartFile()


### Example

```typescript
import {
    JobOrderApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.downloadJobOrderPartFile(
    id
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|


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
|**404** | Job order or file not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **downloadJobOrderPartFileVersion**
> File downloadJobOrderPartFileVersion()


### Example

```typescript
import {
    JobOrderApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

let id: number; // (default to undefined)
let versionId: number; // (default to undefined)

const { status, data } = await apiInstance.downloadJobOrderPartFileVersion(
    id,
    versionId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|
| **versionId** | [**number**] |  | defaults to undefined|


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
|**404** | Job order or version not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **downloadJobOrderPartFileVersionFile**
> File downloadJobOrderPartFileVersionFile()


### Example

```typescript
import {
    JobOrderApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

let id: number; // (default to undefined)
let versionId: number; // (default to undefined)
let fileIndex: number; // (default to undefined)

const { status, data } = await apiInstance.downloadJobOrderPartFileVersionFile(
    id,
    versionId,
    fileIndex
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|
| **versionId** | [**number**] |  | defaults to undefined|
| **fileIndex** | [**number**] |  | defaults to undefined|


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
|**404** | Job order, version, or file not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **downloadJobOrderQuote**
> File downloadJobOrderQuote()


### Example

```typescript
import {
    JobOrderApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.downloadJobOrderQuote(
    id
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|


### Return type

**File**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/pdf


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Quote PDF |  -  |
|**404** | Job order not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getJobOrderById**
> ApiJobOrderResponse getJobOrderById()


### Example

```typescript
import {
    JobOrderApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.getJobOrderById(
    id
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|


### Return type

**ApiJobOrderResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Job order details |  -  |
|**404** | Not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getJobOrderGcodeFileVersions**
> Array<ApiJobOrderFileVersion> getJobOrderGcodeFileVersions()


### Example

```typescript
import {
    JobOrderApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.getJobOrderGcodeFileVersions(
    id
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|


### Return type

**Array<ApiJobOrderFileVersion>**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | GCode file versions, newest first |  -  |
|**404** | Job order not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getJobOrderPartFileVersions**
> Array<ApiJobOrderFileVersion> getJobOrderPartFileVersions()


### Example

```typescript
import {
    JobOrderApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.getJobOrderPartFileVersions(
    id
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|


### Return type

**Array<ApiJobOrderFileVersion>**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Part file versions, newest first |  -  |
|**404** | Job order not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getJobOrders**
> Array<ApiJobOrderResponse> getJobOrders()


### Example

```typescript
import {
    JobOrderApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

const { status, data } = await apiInstance.getJobOrders();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**Array<ApiJobOrderResponse>**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | List of job orders |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **selectJobOrderGcodeFileVersion**
> ApiJobOrderResponse selectJobOrderGcodeFileVersion()


### Example

```typescript
import {
    JobOrderApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

let id: number; // (default to undefined)
let versionId: number; // (default to undefined)
let fileIndex: number; //0-based index of the file within the version to print (for multi-file versions) (optional) (default to 0)

const { status, data } = await apiInstance.selectJobOrderGcodeFileVersion(
    id,
    versionId,
    fileIndex
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|
| **versionId** | [**number**] |  | defaults to undefined|
| **fileIndex** | [**number**] | 0-based index of the file within the version to print (for multi-file versions) | (optional) defaults to 0|


### Return type

**ApiJobOrderResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Version selected; returns updated job order |  -  |
|**404** | Job order or version not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **updateJobOrder**
> ApiJobOrderResponse updateJobOrder(apiUpdateJobOrderRequest)


### Example

```typescript
import {
    JobOrderApi,
    Configuration,
    ApiUpdateJobOrderRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

let id: number; // (default to undefined)
let apiUpdateJobOrderRequest: ApiUpdateJobOrderRequest; //

const { status, data } = await apiInstance.updateJobOrder(
    id,
    apiUpdateJobOrderRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiUpdateJobOrderRequest** | **ApiUpdateJobOrderRequest**|  | |
| **id** | [**number**] |  | defaults to undefined|


### Return type

**ApiJobOrderResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Job order updated |  -  |
|**404** | Not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **updateJobOrderGcodeFileVersionQuantities**
> ApiJobOrderFileVersion updateJobOrderGcodeFileVersionQuantities(apiVersionFileQuantity)


### Example

```typescript
import {
    JobOrderApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

let id: number; // (default to undefined)
let versionId: number; // (default to undefined)
let apiVersionFileQuantity: Array<ApiVersionFileQuantity>; //

const { status, data } = await apiInstance.updateJobOrderGcodeFileVersionQuantities(
    id,
    versionId,
    apiVersionFileQuantity
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **apiVersionFileQuantity** | **Array<ApiVersionFileQuantity>**|  | |
| **id** | [**number**] |  | defaults to undefined|
| **versionId** | [**number**] |  | defaults to undefined|


### Return type

**ApiJobOrderFileVersion**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Quantities updated; returns the updated version |  -  |
|**400** | Invalid file index or quantity |  -  |
|**404** | Job order or version not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **uploadJobOrderGcodeFile**
> ApiJobOrderResponse uploadJobOrderGcodeFile()


### Example

```typescript
import {
    JobOrderApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

let id: number; // (default to undefined)
let files: Array<File>; //One or more .gcode/.3mf files uploaded together as a single version (default to undefined)
let description: string; //Description of the changes in this version (optional) (default to undefined)

const { status, data } = await apiInstance.uploadJobOrderGcodeFile(
    id,
    files,
    description
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|
| **files** | **Array&lt;File&gt;** | One or more .gcode/.3mf files uploaded together as a single version | defaults to undefined|
| **description** | [**string**] | Description of the changes in this version | (optional) defaults to undefined|


### Return type

**ApiJobOrderResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Files uploaded; returns updated job order |  -  |
|**400** | Unsupported file type (only .gcode/.3mf are accepted) |  -  |
|**404** | Job order not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **uploadJobOrderPartFile**
> ApiJobOrderResponse uploadJobOrderPartFile()


### Example

```typescript
import {
    JobOrderApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new JobOrderApi(configuration);

let id: number; // (default to undefined)
let files: Array<File>; //One or more .glb/.gltf files uploaded together as a single version (default to undefined)
let description: string; //Description of the changes in this version (optional) (default to undefined)

const { status, data } = await apiInstance.uploadJobOrderPartFile(
    id,
    files,
    description
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|
| **files** | **Array&lt;File&gt;** | One or more .glb/.gltf files uploaded together as a single version | defaults to undefined|
| **description** | [**string**] | Description of the changes in this version | (optional) defaults to undefined|


### Return type

**ApiJobOrderResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: multipart/form-data
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Files uploaded; returns updated job order |  -  |
|**400** | Unsupported file type (only .glb/.gltf are accepted) |  -  |
|**404** | Job order not found |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

