import UIKit
import composeApp

@main
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {

        window = UIWindow(frame: UIScreen.main.bounds)
        let mainVC = MainViewControllerKt.MainViewController()
        window?.rootViewController = mainVC
        window?.makeKeyAndVisible()
        return true
    }
}
