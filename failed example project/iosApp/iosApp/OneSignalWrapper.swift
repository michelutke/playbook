import Foundation
// OneSignalWrapper: bridges OneSignal iOS SDK to KMP
// Requires OneSignal SDK via Swift Package Manager (NT-011)
// Add package: https://github.com/OneSignal/OneSignal-iOS-SDK.git (version 5.x)

@objc public class OneSignalWrapper: NSObject {
    @objc public static func initialize(appId: String) {
        // OneSignal.initialize(appId, withLaunchOptions: nil)
        // Uncomment after adding SPM package
    }

    @objc public static func login(userId: String) {
        // OneSignal.login(userId)
    }

    @objc public static func logout() {
        // OneSignal.logout()
    }

    @objc public static func requestPermission(completion: @escaping (Bool) -> Void) {
        // OneSignal.Notifications.requestPermission({ accepted in completion(accepted) }, fallbackToSettings: true)
        completion(false)
    }
}
