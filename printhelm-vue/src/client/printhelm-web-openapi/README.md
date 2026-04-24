## restClient@6.9.0

This generator creates TypeScript/JavaScript client that utilizes [axios](https://github.com/axios/axios). The generated Node module can be used in the following environments:

Environment
* Node.js
* Webpack
* Browserify

Language level
* ES5 - you must have a Promises/A+ library installed
* ES6

Module system
* CommonJS
* ES6 module system

It can be used in both TypeScript and JavaScript. In TypeScript, the definition will be automatically resolved via `package.json`. ([Reference](https://www.typescriptlang.org/docs/handbook/declaration-files/consumption.html))

### Building

To build and compile the typescript sources to javascript use:
```
npm install
npm run build
```

### Publishing

First build the package then run `npm publish`

### Consuming

navigate to the folder of your consuming project and run one of the following commands.

_published:_

```
npm install restClient@6.9.0 --save
```

_unPublished (not recommended):_

```
npm install PATH_TO_GENERATED_PACKAGE --save
```

### Documentation for API Endpoints

All URIs are relative to *https://printhelm.czagrzebski.dev/api/v1*

Class | Method | HTTP request | Description
------------ | ------------- | ------------- | -------------
*AuthApi* | [**login**](docs/AuthApi.md#login) | **POST** /auth/login | User login
*AuthApi* | [**logout**](docs/AuthApi.md#logout) | **POST** /auth/logout | User logout
*AuthApi* | [**refresh**](docs/AuthApi.md#refresh) | **POST** /auth/refresh | Refresh access token
*JobOrderApi* | [**createJobOrder**](docs/JobOrderApi.md#createjoborder) | **POST** /job-order | Create a new job order
*JobOrderApi* | [**deleteJobOrder**](docs/JobOrderApi.md#deletejoborder) | **DELETE** /job-order/{id} | Delete a job order
*JobOrderApi* | [**getJobOrderById**](docs/JobOrderApi.md#getjoborderbyid) | **GET** /job-order/{id} | Get a job order by ID
*JobOrderApi* | [**getJobOrders**](docs/JobOrderApi.md#getjoborders) | **GET** /job-order | Get all job orders
*JobOrderApi* | [**updateJobOrder**](docs/JobOrderApi.md#updatejoborder) | **PUT** /job-order/{id} | Update a job order
*NotificationsApi* | [**acknowledgeAllNotifications**](docs/NotificationsApi.md#acknowledgeallnotifications) | **PUT** /notifications/acknowledge-all | Acknowledge all notifications
*NotificationsApi* | [**acknowledgeNotification**](docs/NotificationsApi.md#acknowledgenotification) | **PUT** /notifications/{id}/acknowledge | Acknowledge a single notification
*NotificationsApi* | [**clearAcknowledgedNotifications**](docs/NotificationsApi.md#clearacknowledgednotifications) | **DELETE** /notifications/clear-acknowledged | Delete all acknowledged notifications
*NotificationsApi* | [**deleteNotification**](docs/NotificationsApi.md#deletenotification) | **DELETE** /notifications/{id} | Delete a notification
*NotificationsApi* | [**getAllNotifications**](docs/NotificationsApi.md#getallnotifications) | **GET** /notifications | Get all notifications
*NotificationsApi* | [**getNotificationsByPrinter**](docs/NotificationsApi.md#getnotificationsbyprinter) | **GET** /notifications/printer/{printerId} | Get notifications for a specific printer
*NotificationsApi* | [**getUnreadNotificationCount**](docs/NotificationsApi.md#getunreadnotificationcount) | **GET** /notifications/unread-count | Get count of unacknowledged notifications
*PrinterApi* | [**createPrinter**](docs/PrinterApi.md#createprinter) | **POST** /printer/createPrinter | Create a new printer
*PrinterApi* | [**deletePrinter**](docs/PrinterApi.md#deleteprinter) | **DELETE** /printer/{id} | Delete a printer
*PrinterApi* | [**getPrinterById**](docs/PrinterApi.md#getprinterbyid) | **GET** /printer/{id} | Get a printer by ID
*PrinterApi* | [**getPrinters**](docs/PrinterApi.md#getprinters) | **GET** /printer | Get all printers
*PrinterApi* | [**updatePrinter**](docs/PrinterApi.md#updateprinter) | **PUT** /printer/{id} | Update a printer
*PrinterCommandsApi* | [**homeAxes**](docs/PrinterCommandsApi.md#homeaxes) | **POST** /printer/{id}/command/home | Home all axes
*PrinterCommandsApi* | [**jogAxis**](docs/PrinterCommandsApi.md#jogaxis) | **POST** /printer/{id}/command/jog | Jog an axis
*PrinterCommandsApi* | [**pausePrint**](docs/PrinterCommandsApi.md#pauseprint) | **POST** /printer/{id}/command/pause | Pause the current print
*PrinterCommandsApi* | [**resumePrint**](docs/PrinterCommandsApi.md#resumeprint) | **POST** /printer/{id}/command/resume | Resume the current print
*PrinterCommandsApi* | [**setBedTemp**](docs/PrinterCommandsApi.md#setbedtemp) | **POST** /printer/{id}/command/bed-temp | Set bed target temperature
*PrinterCommandsApi* | [**setLight**](docs/PrinterCommandsApi.md#setlight) | **POST** /printer/{id}/command/light | Set light state
*PrinterCommandsApi* | [**setNozzleTemp**](docs/PrinterCommandsApi.md#setnozzletemp) | **POST** /printer/{id}/command/nozzle-temp | Set nozzle target temperature
*PrinterCommandsApi* | [**setSpeed**](docs/PrinterCommandsApi.md#setspeed) | **POST** /printer/{id}/command/speed | Set print speed level
*PrinterCommandsApi* | [**startPrint**](docs/PrinterCommandsApi.md#startprint) | **POST** /printer/{id}/command/print | Start a print job
*PrinterCommandsApi* | [**stopPrint**](docs/PrinterCommandsApi.md#stopprint) | **POST** /printer/{id}/command/stop | Stop the current print
*PrinterFilesApi* | [**deletePrinterFile**](docs/PrinterFilesApi.md#deleteprinterfile) | **DELETE** /printer/{id}/files/{filename} | Delete a file from printer storage
*PrinterFilesApi* | [**downloadPrinterFile**](docs/PrinterFilesApi.md#downloadprinterfile) | **GET** /printer/{id}/files/{filename} | Download a file from printer storage
*PrinterFilesApi* | [**listPrinterFiles**](docs/PrinterFilesApi.md#listprinterfiles) | **GET** /printer/{id}/files | List files on printer storage
*PrinterFilesApi* | [**uploadPrinterFile**](docs/PrinterFilesApi.md#uploadprinterfile) | **POST** /printer/{id}/files | Upload a file to printer storage
*RoleApi* | [**getRoles**](docs/RoleApi.md#getroles) | **GET** /role | Get all roles
*UserApi* | [**adminResetPassword**](docs/UserApi.md#adminresetpassword) | **PUT** /user/{id}/password | Admin reset user password
*UserApi* | [**changeMyPassword**](docs/UserApi.md#changemypassword) | **PUT** /user/me/password | Change own password
*UserApi* | [**createUser**](docs/UserApi.md#createuser) | **POST** /user/createUser | Create a new user
*UserApi* | [**deleteUser**](docs/UserApi.md#deleteuser) | **DELETE** /user/{id} | Delete a user
*UserApi* | [**getCurrentUser**](docs/UserApi.md#getcurrentuser) | **GET** /user/me | Get current authenticated user
*UserApi* | [**getUserById**](docs/UserApi.md#getuserbyid) | **GET** /user/{id} | Get user by ID
*UserApi* | [**getUsers**](docs/UserApi.md#getusers) | **GET** /user | Get all users
*UserApi* | [**updateUser**](docs/UserApi.md#updateuser) | **PUT** /user/{id} | Update a user


### Documentation For Models

 - [ApiAddPrinterConnectionRequest](docs/ApiAddPrinterConnectionRequest.md)
 - [ApiAdminResetPasswordRequest](docs/ApiAdminResetPasswordRequest.md)
 - [ApiAuthResponse](docs/ApiAuthResponse.md)
 - [ApiChangePasswordRequest](docs/ApiChangePasswordRequest.md)
 - [ApiConnectionConfig](docs/ApiConnectionConfig.md)
 - [ApiCreateJobOrderRequest](docs/ApiCreateJobOrderRequest.md)
 - [ApiCreatePrinterRequest](docs/ApiCreatePrinterRequest.md)
 - [ApiCreateUserRequest](docs/ApiCreateUserRequest.md)
 - [ApiErrorResponse](docs/ApiErrorResponse.md)
 - [ApiFan](docs/ApiFan.md)
 - [ApiIpcam](docs/ApiIpcam.md)
 - [ApiJobOrderResponse](docs/ApiJobOrderResponse.md)
 - [ApiJobOrderStatus](docs/ApiJobOrderStatus.md)
 - [ApiJogRequest](docs/ApiJogRequest.md)
 - [ApiLight](docs/ApiLight.md)
 - [ApiLightRequest](docs/ApiLightRequest.md)
 - [ApiLoginRequest](docs/ApiLoginRequest.md)
 - [ApiMaterial](docs/ApiMaterial.md)
 - [ApiMaterialSystem](docs/ApiMaterialSystem.md)
 - [ApiMqttConnectionConfig](docs/ApiMqttConnectionConfig.md)
 - [ApiNotification](docs/ApiNotification.md)
 - [ApiPrintFileRequest](docs/ApiPrintFileRequest.md)
 - [ApiPrinterFile](docs/ApiPrinterFile.md)
 - [ApiPrinterResponse](docs/ApiPrinterResponse.md)
 - [ApiPrinterState](docs/ApiPrinterState.md)
 - [ApiRefreshRequest](docs/ApiRefreshRequest.md)
 - [ApiRole](docs/ApiRole.md)
 - [ApiSpeedRequest](docs/ApiSpeedRequest.md)
 - [ApiTempRequest](docs/ApiTempRequest.md)
 - [ApiUnreadCountResponse](docs/ApiUnreadCountResponse.md)
 - [ApiUpdateJobOrderRequest](docs/ApiUpdateJobOrderRequest.md)
 - [ApiUpdatePrinterRequest](docs/ApiUpdatePrinterRequest.md)
 - [ApiUpdateUserRequest](docs/ApiUpdateUserRequest.md)
 - [ApiUpgradeState](docs/ApiUpgradeState.md)
 - [ApiUserResponse](docs/ApiUserResponse.md)
 - [ApiXcam](docs/ApiXcam.md)
 - [ConnectionType](docs/ConnectionType.md)
 - [CreatePrinter201Response](docs/CreatePrinter201Response.md)
 - [NotificationSeverity](docs/NotificationSeverity.md)
 - [NotificationType](docs/NotificationType.md)
 - [PrinterType](docs/PrinterType.md)


<a id="documentation-for-authorization"></a>
## Documentation For Authorization

Endpoints do not require authorization.

