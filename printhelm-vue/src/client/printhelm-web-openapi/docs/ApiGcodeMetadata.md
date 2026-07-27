# ApiGcodeMetadata


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**multiColor** | **boolean** | Whether the print uses more than one filament | [optional] [default to undefined]
**colorCount** | **number** | Number of distinct filament slots used | [optional] [default to undefined]
**estimatedDurationSeconds** | **number** | Predicted print time in seconds (from slice_info.config) | [optional] [default to undefined]
**totalWeightGrams** | **number** | Total filament weight of the print in grams | [optional] [default to undefined]
**filaments** | [**Array&lt;ApiGcodeFilamentInfo&gt;**](ApiGcodeFilamentInfo.md) |  | [optional] [default to undefined]

## Example

```typescript
import { ApiGcodeMetadata } from './api';

const instance: ApiGcodeMetadata = {
    multiColor,
    colorCount,
    estimatedDurationSeconds,
    totalWeightGrams,
    filaments,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
