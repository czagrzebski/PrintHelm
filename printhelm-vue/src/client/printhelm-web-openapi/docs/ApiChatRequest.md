# ApiChatRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**content** | **string** | The user\&#39;s new message text | [default to undefined]
**sessionId** | **string** | Existing session ID to continue; omit to start a new session | [optional] [default to undefined]
**model** | [**ApiChatModel**](ApiChatModel.md) |  | [optional] [default to undefined]

## Example

```typescript
import { ApiChatRequest } from './api';

const instance: ApiChatRequest = {
    content,
    sessionId,
    model,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
