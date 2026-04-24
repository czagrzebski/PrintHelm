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
**subtaskId** | **string** |  | [optional] [default to undefined]
**nozzleType** | **string** |  | [optional] [default to undefined]
**remainTime** | **number** | Estimated remaining print time in minutes | [optional] [default to undefined]
**gcodeState** | **string** | Raw gcode execution state reported by the printer | [optional] [default to undefined]
**printType** | **string** | Type of print job (local, cloud, etc.) | [optional] [default to undefined]
**taskId** | **string** |  | [optional] [default to undefined]
**jobId** | **string** |  | [optional] [default to undefined]
**projectId** | **string** |  | [optional] [default to undefined]
**profileId** | **string** |  | [optional] [default to undefined]
**modelId** | **string** |  | [optional] [default to undefined]
**sdcard** | **boolean** | Whether an SD card is inserted | [optional] [default to undefined]
**homeFlag** | **number** | Bitmask of homed axes reported by the printer | [optional] [default to undefined]
**spdLvl** | **number** | Speed level preset (1&#x3D;silent, 2&#x3D;standard, 3&#x3D;sport, 4&#x3D;ludicrous) | [optional] [default to undefined]
**spdMag** | **number** | Speed magnitude as a percentage | [optional] [default to undefined]
**printError** | **number** | Print error code (0 &#x3D; no error) | [optional] [default to undefined]
**mcPrintErrorCode** | **string** | Motion controller print error code | [optional] [default to undefined]
**failReason** | **string** | Human-readable failure reason if print failed | [optional] [default to undefined]
**materialSystem** | [**ApiMaterialSystem**](ApiMaterialSystem.md) |  | [optional] [default to undefined]
**fans** | [**Array&lt;ApiFan&gt;**](ApiFan.md) |  | [optional] [default to undefined]
**lights** | [**Array&lt;ApiLight&gt;**](ApiLight.md) |  | [optional] [default to undefined]
**ipcam** | [**ApiIpcam**](ApiIpcam.md) |  | [optional] [default to undefined]
**xcam** | [**ApiXcam**](ApiXcam.md) |  | [optional] [default to undefined]
**upgradeState** | [**ApiUpgradeState**](ApiUpgradeState.md) |  | [optional] [default to undefined]

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
    subtaskId,
    nozzleType,
    remainTime,
    gcodeState,
    printType,
    taskId,
    jobId,
    projectId,
    profileId,
    modelId,
    sdcard,
    homeFlag,
    spdLvl,
    spdMag,
    printError,
    mcPrintErrorCode,
    failReason,
    materialSystem,
    fans,
    lights,
    ipcam,
    xcam,
    upgradeState,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
