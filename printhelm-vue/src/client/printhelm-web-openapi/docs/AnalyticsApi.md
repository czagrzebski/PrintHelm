# AnalyticsApi

All URIs are relative to *https://printhelm.czagrzebski.dev/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**getAnalyticsSummary**](#getanalyticssummary) | **GET** /analytics/summary | Aggregate print/order stats for the last N days|
|[**getAnalyticsTrends**](#getanalyticstrends) | **GET** /analytics/trends | Daily print activity series for the last N days|
|[**getPrinterAnalytics**](#getprinteranalytics) | **GET** /analytics/printers | Per-printer utilization stats for the last N days|

# **getAnalyticsSummary**
> ApiAnalyticsSummary getAnalyticsSummary()


### Example

```typescript
import {
    AnalyticsApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new AnalyticsApi(configuration);

let days: number; // (optional) (default to 30)

const { status, data } = await apiInstance.getAnalyticsSummary(
    days
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **days** | [**number**] |  | (optional) defaults to 30|


### Return type

**ApiAnalyticsSummary**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Analytics summary |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getAnalyticsTrends**
> Array<ApiAnalyticsTrendPoint> getAnalyticsTrends()


### Example

```typescript
import {
    AnalyticsApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new AnalyticsApi(configuration);

let days: number; // (optional) (default to 30)

const { status, data } = await apiInstance.getAnalyticsTrends(
    days
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **days** | [**number**] |  | (optional) defaults to 30|


### Return type

**Array<ApiAnalyticsTrendPoint>**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Daily trend points |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getPrinterAnalytics**
> Array<ApiPrinterAnalytics> getPrinterAnalytics()


### Example

```typescript
import {
    AnalyticsApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new AnalyticsApi(configuration);

let days: number; // (optional) (default to 30)

const { status, data } = await apiInstance.getPrinterAnalytics(
    days
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **days** | [**number**] |  | (optional) defaults to 30|


### Return type

**Array<ApiPrinterAnalytics>**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | Per-printer analytics |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

