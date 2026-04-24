# ApiJobOrderResponse


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**orderId** | **number** |  | [optional] [default to undefined]
**customerName** | **string** |  | [optional] [default to undefined]
**customerEmail** | **string** |  | [optional] [default to undefined]
**description** | **string** |  | [optional] [default to undefined]
**status** | [**ApiJobOrderStatus**](ApiJobOrderStatus.md) |  | [optional] [default to undefined]
**requirements** | **string** | Reviewer-written requirements for the print job | [optional] [default to undefined]
**requiresCustomDesign** | **boolean** | Whether a custom design phase is needed | [optional] [default to undefined]
**mongoPartFileId** | **string** | MongoDB ObjectId of the 3D part file (null until MongoDB is integrated) | [optional] [default to undefined]
**mongoGcodeFileId** | **string** | MongoDB ObjectId of the gcode file (null until MongoDB is integrated) | [optional] [default to undefined]
**createdAt** | **string** |  | [optional] [default to undefined]

## Example

```typescript
import { ApiJobOrderResponse } from './api';

const instance: ApiJobOrderResponse = {
    orderId,
    customerName,
    customerEmail,
    description,
    status,
    requirements,
    requiresCustomDesign,
    mongoPartFileId,
    mongoGcodeFileId,
    createdAt,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
