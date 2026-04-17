# ApiMaterial


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**loaded** | **boolean** |  | [optional] [default to undefined]
**name** | **string** |  | [optional] [default to undefined]
**color** | **string** |  | [optional] [default to undefined]
**type** | **string** |  | [optional] [default to undefined]
**totalWeightGrams** | **number** |  | [optional] [default to undefined]
**costPerGram** | **number** |  | [optional] [default to undefined]
**timestamp** | **string** |  | [optional] [default to undefined]
**remain** | **number** | Estimated filament remaining (%) | [optional] [default to undefined]
**trayDiameter** | **string** | Filament diameter (e.g. \&quot;1.75\&quot;) | [optional] [default to undefined]
**trayWeight** | **string** | Spool weight in grams as reported by AMS tag | [optional] [default to undefined]
**trayUuid** | **string** | RFID UUID of the tray tag | [optional] [default to undefined]
**nozzleTempMin** | **string** | Minimum recommended nozzle temperature for this filament | [optional] [default to undefined]
**nozzleTempMax** | **string** | Maximum recommended nozzle temperature for this filament | [optional] [default to undefined]
**recommendedBedTemp** | **string** | Recommended bed temperature for this filament | [optional] [default to undefined]
**dryingTemp** | **string** | Recommended drying temperature | [optional] [default to undefined]
**dryingTime** | **string** | Recommended drying time in hours | [optional] [default to undefined]

## Example

```typescript
import { ApiMaterial } from './api';

const instance: ApiMaterial = {
    loaded,
    name,
    color,
    type,
    totalWeightGrams,
    costPerGram,
    timestamp,
    remain,
    trayDiameter,
    trayWeight,
    trayUuid,
    nozzleTempMin,
    nozzleTempMax,
    recommendedBedTemp,
    dryingTemp,
    dryingTime,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
