# cordova-line-login-plugin

A Cordova plugin for LINE Login using the official LINE SDKs.

## SDK Versions

- iOS: LineSDKSwift 5.16.1
- Android: LINE SDK for Android 5.12.0

## Supported Cordova Targets

This plugin is updated for modern Cordova projects, including:

- cordova-android 14.x and 15.x
- cordova-ios 7.x and 8.x

Minimum plugin engines:

- cordova >= 10.0.0
- cordova-android >= 10.0.0
- cordova-ios >= 6.0.0

## Platform Requirements

### Android

- Android minSdkVersion >= 24. This is required by LINE SDK for Android 5.12.0.
- AndroidX is required.
- The plugin adds `android.useAndroidX=true` and `android.enableJetifier=true` to `platforms/android/gradle.properties` during `after_prepare` when they are missing.
- Do not set the Cordova activity launch mode to `singleInstance`; LINE SDK needs `onActivityResult`.

Configure your LINE Login channel in LINE Developers Console:

- Android package name
- Android package signature
- Android URL scheme, when used by your app

### iOS

- iOS deployment target >= 13.0. This is required by the current LINE SDK for iOS Swift.
- Swift language version 5 is required. The plugin adds `UseSwiftLanguageVersion=5` to the generated iOS platform config.

Configure your LINE Login channel in LINE Developers Console:

- iOS bundle ID
- iOS scheme

The plugin adds:

- `line3rdp.$(PRODUCT_BUNDLE_IDENTIFIER)` to `CFBundleURLTypes`
- `lineauth2` to `LSApplicationQueriesSchemes`
- `LineSDKSwift/ObjC` through CocoaPods

## Installation

```sh
cordova plugin add cordova-line-login-plugin
```

For a local fork:

```sh
cordova plugin add /path/to/cordova-line-login-plugin
```

## Usage

```js
document.addEventListener('deviceready', onDeviceReady, false);

function onDeviceReady() {
  lineLogin.initialize(
    { channel_id: '1565553788' },
    function () {
      console.log('LINE SDK initialized');
    },
    function (error) {
      console.log(error);
    }
  );
}

function loginWithLine() {
  lineLogin.login(
    function (result) {
      console.log(result);
      // { userID, displayName, pictureURL?, email? }
    },
    function (error) {
      console.log(error);
    }
  );
}

function loginWithLineWeb() {
  lineLogin.loginWeb(
    function (result) {
      console.log(result);
    },
    function (error) {
      console.log(error);
    }
  );
}

function logoutLine() {
  lineLogin.logout(
    function () {
      console.log('Logged out');
    },
    function (error) {
      console.log(error);
    }
  );
}

function getLineAccessToken() {
  lineLogin.getAccessToken(
    function (result) {
      console.log(result);
      // { accessToken, expireTime }
    },
    function (error) {
      console.log(error);
    }
  );
}

function verifyLineAccessToken() {
  lineLogin.verifyAccessToken(
    function () {
      console.log('Access token is valid');
    },
    function (error) {
      console.log(error);
    }
  );
}

function refreshLineAccessToken() {
  lineLogin.refreshAccessToken(
    function (accessTokenOrResult) {
      console.log(accessTokenOrResult);
    },
    function (error) {
      console.log(error);
    }
  );
}
```

## Error Format

Error callbacks return an object:

```js
{
  code: -1, // -1 parameter error, -2 LINE SDK error, -3 unknown error
  sdkErrorCode: 'OPTIONAL_SDK_ERROR_CODE',
  description: 'Error message'
}
```

## LINE SDK Documentation

- [LINE SDK for Android](https://developers.line.biz/en/docs/line-login-sdks/android-sdk/integrate-line-login/)
- [LINE SDK for iOS Swift](https://developers.line.biz/en/docs/line-login-sdks/ios-sdk/swift/integrate-line-login/)
