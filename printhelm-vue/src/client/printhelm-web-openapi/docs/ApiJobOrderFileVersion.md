# ApiJobOrderFileVersion


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**versionId** | **number** | Unique identifier of this file version | [optional] [default to undefined]
**versionNumber** | **number** | Sequential version number (1-based) | [optional] [default to undefined]
**fileType** | [**ApiJobOrderFileType**](ApiJobOrderFileType.md) |  | [optional] [default to undefined]
**filename** | **string** | Original filename of the uploaded file | [optional] [default to undefined]
**description** | **string** | Description of the changes in this version | [optional] [default to undefined]
**createdAt** | **string** | Timestamp when this version was uploaded | [optional] [default to undefined]
**active** | **boolean** | Whether this version is the currently active one on the job order | [optional] [default to undefined]

## Example

```typescript
import { ApiJobOrderFileVersion } from './api';

const instance: ApiJobOrderFileVersion = {
    versionId,
    versionNumber,
    fileType,
    filename,
    description,
    createdAt,
    active,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
