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
**quotedMaterialCost** | **number** | Estimated material cost for the quote | [optional] [default to undefined]
**quotedCostPerUnit** | **number** | Cost per unit of material | [optional] [default to undefined]
**quotedQuantity** | **number** | Number of units of material | [optional] [default to undefined]
**quotedLaborCost** | **number** | Estimated labor or design fee for the quote | [optional] [default to undefined]
**quotedSetupFee** | **number** | Estimated setup fee for the quote | [optional] [default to undefined]
**quotedDiscount** | **number** | Estimated discount for the quote | [optional] [default to undefined]
**quoteNotes** | **string** | Additional notes to include on the quote | [optional] [default to undefined]
**quoteExpiresAt** | **string** | Date until which the quote is valid | [optional] [default to undefined]
**quoteLineItems** | [**Array&lt;ApiQuoteLineItem&gt;**](ApiQuoteLineItem.md) | Optional custom line items for the quote | [optional] [default to undefined]
**quoteMaterials** | **Array&lt;string&gt;** | Material types selected for this quote | [optional] [default to undefined]
**materialCost** | **number** | Cost of materials/filament used | [optional] [default to undefined]
**laborCost** | **number** | Labor or design fee | [optional] [default to undefined]
**setupFee** | **number** | One-time setup fee | [optional] [default to undefined]
**discount** | **number** | Discount amount applied to the total | [optional] [default to undefined]
**invoiceNotes** | **string** | Additional notes to include on the invoice | [optional] [default to undefined]

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
    quotedMaterialCost,
    quotedCostPerUnit,
    quotedQuantity,
    quotedLaborCost,
    quotedSetupFee,
    quotedDiscount,
    quoteNotes,
    quoteExpiresAt,
    quoteLineItems,
    quoteMaterials,
    materialCost,
    laborCost,
    setupFee,
    discount,
    invoiceNotes,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
