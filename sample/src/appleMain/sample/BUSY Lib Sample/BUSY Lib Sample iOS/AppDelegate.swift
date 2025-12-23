import BridgeConnection
import UIKit
import Foundation

class AppDelegate: NSObject, UIApplicationDelegate {
    let root: ConnectionRootDecomposeComponent = IOSAppComponentKt.getRootDecomposeComponent()


    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [
            UIApplication.LaunchOptionsKey: Any
        ]? = nil
    ) -> Bool {

        IOSAppComponentKt.busyLib.connectionService.onApplicationInit()
        return true
    }
}
