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
**mongoPartFileId** | **string** | MongoDB ObjectId of the 3D part file | [optional] [default to undefined]
**partFilename** | **string** | Original filename of the uploaded 3D part file | [optional] [default to undefined]
**mongoGcodeFileId** | **string** | MongoDB ObjectId of the gcode file | [optional] [default to undefined]
**gcodeFilename** | **string** | Original filename of the uploaded GCode file | [optional] [default to undefined]
**createdAt** | **string** |  | [optional] [default to undefined]
**assignedPrinterId** | **number** | ID of the printer this job is queued on | [optional] [default to undefined]
**queuePosition** | **number** | Position in the printer\&#39;s queue (1-indexed) | [optional] [default to undefined]
**assignedFilename** | **string** | Gcode filename on the printer\&#39;s SD card | [optional] [default to undefined]
**gcodeMetadata** | [**ApiGcodeMetadata**](ApiGcodeMetadata.md) |  | [optional] [default to undefined]

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
    partFilename,
    mongoGcodeFileId,
    gcodeFilename,
    createdAt,
    assignedPrinterId,
    queuePosition,
    assignedFilename,
    gcodeMetadata,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
