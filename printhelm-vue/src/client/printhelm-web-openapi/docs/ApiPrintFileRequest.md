# ApiPrintFileRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**filename** | **string** | Filename of the gcode to print | [default to undefined]
**amsMapping** | **Array&lt;number&gt;** | AMS tray index mapping for each filament slot | [default to undefined]
**flowCali** | **boolean** | Enable flow rate calibration before print | [default to undefined]
**vibrationCali** | **boolean** | Enable vibration calibration before print | [default to undefined]
**layerInspect** | **boolean** | Enable first-layer inspection | [default to undefined]

## Example

```typescript
import { ApiPrintFileRequest } from './api';

const instance: ApiPrintFileRequest = {
    filename,
    amsMapping,
    flowCali,
    vibrationCali,
    layerInspect,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
