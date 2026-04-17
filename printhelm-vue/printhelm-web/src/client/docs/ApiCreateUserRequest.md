# ApiCreateUserRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**username** | **string** |  | [default to undefined]
**password** | **string** |  | [default to undefined]
**firstName** | **string** |  | [optional] [default to undefined]
**lastName** | **string** |  | [optional] [default to undefined]
**roles** | [**Array&lt;ApiRole&gt;**](ApiRole.md) |  | [optional] [default to undefined]

## Example

```typescript
import { ApiCreateUserRequest } from './api';

const instance: ApiCreateUserRequest = {
    username,
    password,
    firstName,
    lastName,
    roles,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
