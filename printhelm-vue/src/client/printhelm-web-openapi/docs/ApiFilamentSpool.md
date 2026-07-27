# ApiFilamentSpool


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**spoolId** | **number** |  | [optional] [default to undefined]
**name** | **string** |  | [optional] [default to undefined]
**brand** | **string** |  | [optional] [default to undefined]
**material** | **string** | Material type (PLA, PETG, ABS, ...) | [optional] [default to undefined]
**colorHex** | **string** | Hex color code (#RRGGBB) | [optional] [default to undefined]
**colorName** | **string** |  | [optional] [default to undefined]
**diameter** | **number** | Filament diameter in mm | [optional] [default to undefined]
**initialWeightGrams** | **number** |  | [optional] [default to undefined]
**remainingWeightGrams** | **number** |  | [optional] [default to undefined]
**spoolCost** | **number** |  | [optional] [default to undefined]
**lowStockThresholdGrams** | **number** |  | [optional] [default to undefined]
**status** | [**FilamentSpoolStatus**](FilamentSpoolStatus.md) |  | [optional] [default to undefined]
**assignedPrinterId** | **number** |  | [optional] [default to undefined]
**assignedPrinterName** | **string** |  | [optional] [default to undefined]
**amsSlot** | **number** | 0-based AMS tray index the spool is loaded in | [optional] [default to undefined]
**notes** | **string** |  | [optional] [default to undefined]
**lowStock** | **boolean** | True when remaining weight is at or below the low-stock threshold | [optional] [default to undefined]
**createdAt** | **string** |  | [optional] [default to undefined]
**updatedAt** | **string** |  | [optional] [default to undefined]

## Example

```typescript
import { ApiFilamentSpool } from './api';

const instance: ApiFilamentSpool = {
    spoolId,
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
    assignedPrinterName,
    amsSlot,
    notes,
    lowStock,
    createdAt,
    updatedAt,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
