# ApiGcodeFilamentInfo


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**slotIndex** | **number** | 0-based AMS slot index from the .3mf file | [optional] [default to undefined]
**type** | **string** | Material type (PLA, PETG, etc.) | [optional] [default to undefined]
**color** | **string** | Hex color code (#RRGGBB) | [optional] [default to undefined]
**usedGrams** | **number** | Grams of this filament the print consumes (from slice_info.config) | [optional] [default to undefined]

## Example

```typescript
import { ApiGcodeFilamentInfo } from './api';

const instance: ApiGcodeFilamentInfo = {
    slotIndex,
    type,
    color,
    usedGrams,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
