# ApiCreatePrinterRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**printerName** | **string** |  | [default to undefined]
**printerType** | [**PrinterType**](PrinterType.md) |  | [optional] [default to undefined]
**printerModel** | **string** |  | [optional] [default to undefined]
**location** | **string** |  | [optional] [default to undefined]
**serialNumber** | **string** |  | [optional] [default to undefined]
**connectionConfig** | [**ApiConnectionConfig**](ApiConnectionConfig.md) |  | [default to undefined]

## Example

```typescript
import { ApiCreatePrinterRequest } from './api';

const instance: ApiCreatePrinterRequest = {
    printerName,
    printerType,
    printerModel,
    location,
    serialNumber,
    connectionConfig,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
