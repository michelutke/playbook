import UIKit
import ComposeApp

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        print("[Playbook] AppDelegate: didFinishLaunching")
        MainViewControllerKt.doInitKoin()
        return true
    }

    func application(
        _ application: UIApplication,
        configurationForConnecting connectingSceneSession: UISceneSession,
        options: UIScene.ConnectionOptions
    ) -> UISceneConfiguration {
        print("[Playbook] AppDelegate: configurationForConnecting")
        let config = UISceneConfiguration(name: nil, sessionRole: connectingSceneSession.role)
        config.delegateClass = SceneDelegate.self
        return config
    }
}

class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        print("[Playbook] SceneDelegate: willConnectTo")
        guard let windowScene = scene as? UIWindowScene else { return }
        let window = UIWindow(windowScene: windowScene)
        window.rootViewController = MainViewControllerKt.MainViewController()
        self.window = window
        window.makeKeyAndVisible()
        print("[Playbook] SceneDelegate: window made key and visible")
    }

    // CMP's SceneForegroundStateListener subscribes to UISceneWillEnterForegroundNotification.
    // On initial launch the notification fires here — re-post it so CMP definitely receives it
    // after the ComposeUIViewController is fully initialized and subscribed.
    func sceneWillEnterForeground(_ scene: UIScene) {
        print("[Playbook] SceneDelegate: sceneWillEnterForeground")
        DispatchQueue.main.async {
            NotificationCenter.default.post(
                name: UIScene.willEnterForegroundNotification,
                object: scene
            )
        }
    }

    func sceneDidBecomeActive(_ scene: UIScene) {
        print("[Playbook] SceneDelegate: sceneDidBecomeActive")
        window?.rootViewController?.view.setNeedsLayout()
        window?.rootViewController?.view.layoutIfNeeded()
        window?.rootViewController?.view.setNeedsDisplay()
    }
}
