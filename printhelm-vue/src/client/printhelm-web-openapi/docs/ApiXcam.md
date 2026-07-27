# ApiXcam

AI-based visual monitoring settings (X1 series)

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**firstLayerInspector** | **boolean** | First layer inspection enabled | [optional] [default to undefined]
**buildplateMarkerDetector** | **boolean** | Build plate marker detection enabled | [optional] [default to undefined]
**spaghettiDetector** | **boolean** | Spaghetti failure detection enabled | [optional] [default to undefined]
**printingMonitor** | **boolean** | General printing monitor enabled | [optional] [default to undefined]
**printHalt** | **boolean** | Whether the printer will halt on a detected failure | [optional] [default to undefined]
**haltPrintSensitivity** | **string** | Sensitivity level for halt-on-failure (low / medium / high) | [optional] [default to undefined]
**allowSkipParts** | **boolean** | Allow skipping failed objects | [optional] [default to undefined]

## Example

```typescript
import { ApiXcam } from './api';

const instance: ApiXcam = {
    firstLayerInspector,
    buildplateMarkerDetector,
    spaghettiDetector,
    printingMonitor,
    printHalt,
    haltPrintSensitivity,
    allowSkipParts,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
