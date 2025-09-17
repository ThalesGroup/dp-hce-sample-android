# Thales NFC Wallet SDK Android Sample Application

Please see our developer portal for further details about the SDK integration:
* [Introduction](https://developer.dbp.thalescloud.io/docs/tsh-hce-android/)
* [Quick start guide](https://developer.dbp.thalescloud.io/docs/tsh-hce-android/4c26a3bda35bf-introduction)


## Changelog

### v1.3
* Project updated to newer Android Studio (Android Studio Narwhal Feature Drop | 2025.1.2 Patch 1), AGP (8.12.0), build tools and dependencies versions
* Project updated to support [NFC Wallet SDK v6.12.0](https://developer.dbp.thalescloud.io/docs/tsh-hce-android/15763107e31ef-release-notes#nfc-wallet-sdk-6120)
* Card enrollment logic modified so it won't be waiting for an incoming push message as per the [documentation](https://developer.dbp.thalescloud.io/docs/tsh-hce-android/uk7rrjfbbo8ru-introduction).
* Added handling of preferred service set/unset so the app could override the default Tap&Pay app while it is on the foreground (needs to be allowed by the end user).
* Added a custom configuration which demonstrates Flexible CDCVM setup.
* Refactored the code related to card payment key replenishment and added a user notification indicating the flow completion.
* Added a network state observer to demonstrate triggering card payment key replenishment when the device becomes online.
* Added a demo code for handling app2app ID&V method.
* Minor updates to improve code quality, readability and app's UI.