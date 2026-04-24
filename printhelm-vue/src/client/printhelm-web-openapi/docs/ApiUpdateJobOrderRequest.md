# ApiUpdateJobOrderRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**customerName** | **string** |  | [optional] [default to undefined]
**customerEmail** | **string** |  | [optional] [default to undefined]
**description** | **string** |  | [optional] [default to undefined]
**status** | [**ApiJobOrderStatus**](ApiJobOrderStatus.md) |  | [optional] [default to undefined]
**requirements** | **string** |  | [optional] [default to undefined]
**requiresCustomDesign** | **boolean** |  | [optional] [default to undefined]

## Example

```typescript
import { ApiUpdateJobOrderRequest } from './api';

const instance: ApiUpdateJobOrderRequest = {
    customerName,
    customerEmail,
    description,
    status,
    requirements,
    requiresCustomDesign,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
