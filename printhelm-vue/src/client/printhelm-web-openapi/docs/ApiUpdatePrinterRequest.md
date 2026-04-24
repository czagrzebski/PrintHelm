# ApiUpdatePrinterRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**printerName** | **string** |  | [optional] [default to undefined]
**printerModel** | **string** |  | [optional] [default to undefined]
**location** | **string** |  | [optional] [default to undefined]
**serialNumber** | **string** |  | [optional] [default to undefined]
**connectionConfig** | [**ApiConnectionConfig**](ApiConnectionConfig.md) |  | [optional] [default to undefined]

## Example

```typescript
import { ApiUpdatePrinterRequest } from './api';

const instance: ApiUpdatePrinterRequest = {
    printerName,
    printerModel,
    location,
    serialNumber,
    connectionConfig,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
