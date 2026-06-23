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
**materialCost** | **number** | Cost of materials/filament used | [optional] [default to undefined]
**laborCost** | **number** | Labor or design fee | [optional] [default to undefined]
**setupFee** | **number** | One-time setup fee | [optional] [default to undefined]
**discount** | **number** | Discount amount applied to the total | [optional] [default to undefined]
**invoiceNotes** | **string** | Additional notes included on the invoice | [optional] [default to undefined]
**invoicedAt** | **string** | Timestamp when the invoice was generated | [optional] [default to undefined]
**quotedMaterialCost** | **number** | Estimated material cost from the quote | [optional] [default to undefined]
**quotedCostPerUnit** | **number** | Cost per unit of material | [optional] [default to undefined]
**quotedQuantity** | **number** | Number of units of material | [optional] [default to undefined]
**quotedLaborCost** | **number** | Estimated labor or design fee from the quote | [optional] [default to undefined]
**quotedSetupFee** | **number** | Estimated setup fee from the quote | [optional] [default to undefined]
**quotedDiscount** | **number** | Estimated discount from the quote | [optional] [default to undefined]
**quoteNotes** | **string** | Notes from the quote | [optional] [default to undefined]
**quotedAt** | **string** | Timestamp when the quote was generated | [optional] [default to undefined]
**quoteExpiresAt** | **string** | Date until which the quote is valid | [optional] [default to undefined]
**quoteLineItems** | [**Array&lt;ApiQuoteLineItem&gt;**](ApiQuoteLineItem.md) | Custom line items on the quote | [optional] [default to undefined]
**quoteMaterials** | **Array&lt;string&gt;** | Material types selected for this quote | [optional] [default to undefined]

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
    materialCost,
    laborCost,
    setupFee,
    discount,
    invoiceNotes,
    invoicedAt,
    quotedMaterialCost,
    quotedCostPerUnit,
    quotedQuantity,
    quotedLaborCost,
    quotedSetupFee,
    quotedDiscount,
    quoteNotes,
    quotedAt,
    quoteExpiresAt,
    quoteLineItems,
    quoteMaterials,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
