# ApiUserResponse


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**userId** | **number** |  | [optional] [default to undefined]
**username** | **string** |  | [optional] [default to undefined]
**firstName** | **string** |  | [optional] [default to undefined]
**lastName** | **string** |  | [optional] [default to undefined]
**isActive** | **boolean** |  | [optional] [default to undefined]
**mustChangePassword** | **boolean** |  | [optional] [default to undefined]
**roles** | [**Array&lt;ApiRole&gt;**](ApiRole.md) |  | [optional] [default to undefined]

## Example

```typescript
import { ApiUserResponse } from './api';

const instance: ApiUserResponse = {
    userId,
    username,
    firstName,
    lastName,
    isActive,
    mustChangePassword,
    roles,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
