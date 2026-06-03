
## 2.0.0

* Updated iOS SDK to LineSDKSwift 5.16.1.
* Updated Android SDK to line-sdk-android 5.12.0.
* Updated Android dependencies for AndroidX-era Cordova Android projects.
* Added Android implementation for `loginWeb` using LINE SDK browser-only login.
* Moved Android LINE API calls off the main thread.
* Added null-safety for optional LINE profile, ID token, and email fields.
* Updated iOS token APIs to `API.Auth`.
* Raised Android minSdkVersion to 24 for LINE SDK 5.12.0 compatibility.

## 1.2.11

* fixed error ionic cordova build

## 1.2.10

* fixed README

## 1.2.9

* fixed Changed the management of sdk from carthage to cocoapods
* fixed README about capacitor  

## 1.2.8

* fixed ios sdk version 5.5.1 for always login failed(error code = 3003)

## 1.2.7

* fix: Android app crash for no avatar user login

## 1.2.6

* Added action to check if the project can be built.
