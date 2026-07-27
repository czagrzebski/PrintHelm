# ApiAuditLogEntry


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**auditId** | **number** |  | [optional] [default to undefined]
**timestamp** | **string** |  | [optional] [default to undefined]
**username** | **string** |  | [optional] [default to undefined]
**action** | **string** | Machine-readable action code (e.g. JOB_ORDER_CREATED) | [optional] [default to undefined]
**entityType** | **string** |  | [optional] [default to undefined]
**entityId** | **string** |  | [optional] [default to undefined]
**details** | **string** |  | [optional] [default to undefined]

## Example

```typescript
import { ApiAuditLogEntry } from './api';

const instance: ApiAuditLogEntry = {
    auditId,
    timestamp,
    username,
    action,
    entityType,
    entityId,
    details,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
