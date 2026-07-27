# ApiUpdateUserRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**username** | **string** |  | [optional] [default to undefined]
**firstName** | **string** |  | [optional] [default to undefined]
**lastName** | **string** |  | [optional] [default to undefined]
**isActive** | **boolean** |  | [optional] [default to undefined]
**roles** | [**Array&lt;ApiRole&gt;**](ApiRole.md) |  | [optional] [default to undefined]

## Example

```typescript
import { ApiUpdateUserRequest } from './api';

const instance: ApiUpdateUserRequest = {
    username,
    firstName,
    lastName,
    isActive,
    roles,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
