# ApiNotification


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **number** |  | [optional] [default to undefined]
**printerId** | **number** |  | [optional] [default to undefined]
**printerName** | **string** |  | [optional] [default to undefined]
**type** | [**NotificationType**](NotificationType.md) |  | [optional] [default to undefined]
**severity** | [**NotificationSeverity**](NotificationSeverity.md) |  | [optional] [default to undefined]
**title** | **string** |  | [optional] [default to undefined]
**message** | **string** |  | [optional] [default to undefined]
**file** | **string** |  | [optional] [default to undefined]
**acknowledged** | **boolean** |  | [optional] [default to undefined]
**createdAt** | **string** |  | [optional] [default to undefined]

## Example

```typescript
import { ApiNotification } from './api';

const instance: ApiNotification = {
    id,
    printerId,
    printerName,
    type,
    severity,
    title,
    message,
    file,
    acknowledged,
    createdAt,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
