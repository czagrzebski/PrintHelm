# ApiUpgradeState


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**status** | **string** | Current upgrade status (e.g. IDLE, DOWNLOADING, INSTALLING) | [optional] [default to undefined]
**progress** | **string** | Upgrade progress percentage as a string | [optional] [default to undefined]
**otaNewVersionNumber** | **string** | Available OTA firmware version | [optional] [default to undefined]
**amsNewVersionNumber** | **string** | Available AMS firmware version | [optional] [default to undefined]
**ahbNewVersionNumber** | **string** | Available AHB firmware version | [optional] [default to undefined]
**extNewVersionNumber** | **string** | Available extruder firmware version | [optional] [default to undefined]
**message** | **string** | Human-readable status message | [optional] [default to undefined]
**forceUpgrade** | **boolean** | Whether this upgrade is mandatory | [optional] [default to undefined]

## Example

```typescript
import { ApiUpgradeState } from './api';

const instance: ApiUpgradeState = {
    status,
    progress,
    otaNewVersionNumber,
    amsNewVersionNumber,
    ahbNewVersionNumber,
    extNewVersionNumber,
    message,
    forceUpgrade,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
