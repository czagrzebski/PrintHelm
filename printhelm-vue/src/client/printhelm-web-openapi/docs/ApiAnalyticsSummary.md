# ApiAnalyticsSummary


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**days** | **number** | Window size the summary covers | [optional] [default to undefined]
**printsCompleted** | **number** |  | [optional] [default to undefined]
**printsFailed** | **number** |  | [optional] [default to undefined]
**printsCanceled** | **number** |  | [optional] [default to undefined]
**successRate** | **number** | Completed / all finished attempts (null when no attempts) | [optional] [default to undefined]
**totalPrintMinutes** | **number** |  | [optional] [default to undefined]
**filamentGramsUsed** | **number** |  | [optional] [default to undefined]
**revenue** | **number** | Sum of invoiced totals within the window | [optional] [default to undefined]
**invoicedOrders** | **number** |  | [optional] [default to undefined]
**openOrders** | **number** | Orders not yet finished or invoiced (all time) | [optional] [default to undefined]
**ordersByStatus** | **{ [key: string]: number; }** |  | [optional] [default to undefined]

## Example

```typescript
import { ApiAnalyticsSummary } from './api';

const instance: ApiAnalyticsSummary = {
    days,
    printsCompleted,
    printsFailed,
    printsCanceled,
    successRate,
    totalPrintMinutes,
    filamentGramsUsed,
    revenue,
    invoicedOrders,
    openOrders,
    ordersByStatus,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
