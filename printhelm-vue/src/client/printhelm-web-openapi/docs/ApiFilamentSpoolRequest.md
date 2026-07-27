# ApiFilamentSpoolRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**name** | **string** |  | [default to undefined]
**brand** | **string** |  | [optional] [default to undefined]
**material** | **string** |  | [default to undefined]
**colorHex** | **string** |  | [optional] [default to undefined]
**colorName** | **string** |  | [optional] [default to undefined]
**diameter** | **number** |  | [optional] [default to undefined]
**initialWeightGrams** | **number** |  | [default to undefined]
**remainingWeightGrams** | **number** |  | [optional] [default to undefined]
**spoolCost** | **number** |  | [optional] [default to undefined]
**lowStockThresholdGrams** | **number** |  | [optional] [default to undefined]
**status** | [**FilamentSpoolStatus**](FilamentSpoolStatus.md) |  | [optional] [default to undefined]
**assignedPrinterId** | **number** |  | [optional] [default to undefined]
**amsSlot** | **number** |  | [optional] [default to undefined]
**notes** | **string** |  | [optional] [default to undefined]

## Example

```typescript
import { ApiFilamentSpoolRequest } from './api';

const instance: ApiFilamentSpoolRequest = {
    name,
    brand,
    material,
    colorHex,
    colorName,
    diameter,
    initialWeightGrams,
    remainingWeightGrams,
    spoolCost,
    lowStockThresholdGrams,
    status,
    assignedPrinterId,
    amsSlot,
    notes,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
