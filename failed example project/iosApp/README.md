# iOS App

The iOS app is a Compose Multiplatform project. Generate the Xcode project using:
https://kmp.jetbrains.com/

Select: Android + iOS, include iosApp module, use shared module from :shared.

After generation, copy the iosApp/ Xcode project here.

## Push Notifications (OneSignal) — Manual Xcode Steps

### NT-011: Add OneSignal SDK
In Xcode → File → Add Package Dependencies → enter:
`https://github.com/OneSignal/OneSignal-XCFramework`
Select the `OneSignalFramework` product and add it to the iosApp target.

### NT-016: Enable Background Modes
In Xcode → select iosApp target → Signing & Capabilities → + Capability → Background Modes → enable **Remote notifications**.
