# ApiJobOrderVersionFile


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**fileIndex** | **number** | 0-based index of the file within its version | [optional] [default to undefined]
**filename** | **string** | Original filename of the uploaded file | [optional] [default to undefined]
**active** | **boolean** | Whether this file is the currently active one on the job order (e.g. selected for printing) | [optional] [default to undefined]
**printQuantity** | **number** | How many copies of this file must be printed (gcode files; 0 &#x3D; skip, default 1) | [optional] [default to undefined]
**completedPrints** | **number** | How many copies of this file have finished printing | [optional] [default to undefined]

## Example

```typescript
import { ApiJobOrderVersionFile } from './api';

const instance: ApiJobOrderVersionFile = {
    fileIndex,
    filename,
    active,
    printQuantity,
    completedPrints,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
