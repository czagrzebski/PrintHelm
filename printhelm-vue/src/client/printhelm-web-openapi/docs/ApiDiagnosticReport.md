# ApiDiagnosticReport


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**healthy** | **boolean** | True if no issues were detected | [optional] [default to undefined]
**issue** | **string** | Short summary of the detected issue, or null if healthy | [optional] [default to undefined]
**likelyCause** | **string** | Most probable root cause of the issue | [optional] [default to undefined]
**severity** | **string** | Severity of the issue (NONE, LOW, MEDIUM, HIGH, CRITICAL) | [optional] [default to undefined]
**troubleshootingSteps** | **Array&lt;string&gt;** | Ordered list of steps to resolve the issue | [optional] [default to undefined]
**watchFor** | **string** | What to monitor after applying the fixes | [optional] [default to undefined]

## Example

```typescript
import { ApiDiagnosticReport } from './api';

const instance: ApiDiagnosticReport = {
    healthy,
    issue,
    likelyCause,
    severity,
    troubleshootingSteps,
    watchFor,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
