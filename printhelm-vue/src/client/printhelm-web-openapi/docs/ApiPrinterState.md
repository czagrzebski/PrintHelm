# ApiPrinterState


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**printerId** | **number** |  | [optional] [default to undefined]
**currentLayer** | **number** |  | [optional] [default to undefined]
**totalLayers** | **number** |  | [optional] [default to undefined]
**state** | **string** |  | [optional] [default to undefined]
**bedTemp** | **number** |  | [optional] [default to undefined]
**bedTargetTemp** | **number** |  | [optional] [default to undefined]
**nozzleTargetTemp** | **number** |  | [optional] [default to undefined]
**nozzleDiameter** | **number** |  | [optional] [default to undefined]
**subtask** | **string** |  | [optional] [default to undefined]
**nozzleTemp** | **number** |  | [optional] [default to undefined]
**ipAddress** | **string** |  | [optional] [default to undefined]
**file** | **string** |  | [optional] [default to undefined]
**timestamp** | **string** |  | [optional] [default to undefined]
**progress** | **number** |  | [optional] [default to undefined]
**wifiSignalStrength** | **string** |  | [optional] [default to undefined]
**subtaskName** | **string** |  | [optional] [default to undefined]
**nozzleType** | **string** |  | [optional] [default to undefined]
**materialSystem** | [**ApiMaterialSystem**](ApiMaterialSystem.md) |  | [optional] [default to undefined]
**fans** | [**Array&lt;ApiFan&gt;**](ApiFan.md) |  | [optional] [default to undefined]
**lights** | [**Array&lt;ApiLight&gt;**](ApiLight.md) |  | [optional] [default to undefined]

## Example

```typescript
import { ApiPrinterState } from './api';

const instance: ApiPrinterState = {
    printerId,
    currentLayer,
    totalLayers,
    state,
    bedTemp,
    bedTargetTemp,
    nozzleTargetTemp,
    nozzleDiameter,
    subtask,
    nozzleTemp,
    ipAddress,
    file,
    timestamp,
    progress,
    wifiSignalStrength,
    subtaskName,
    nozzleType,
    materialSystem,
    fans,
    lights,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
