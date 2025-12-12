import SwiftUI
import BridgeConnection

#if os(iOS)
@main
struct BUSY_Lib_SampleApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self)
    var appDelegate: AppDelegate

    var body: some Scene {
        WindowGroup {
            RootView(root: appDelegate.root)
        }
    }
}

struct RootView: UIViewControllerRepresentable {
    let root: ConnectionRootDecomposeComponent

    func makeUIViewController(context: Context) -> UIViewController {
        return MainKt.rootViewController(root: root)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}


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
#elseif os(macOS)
class AppDelegate: NSObject, NSApplicationDelegate {
    let root: ConnectionRootDecomposeComponent = MacOSAppComponentKt.getRootDecomposeComponent()

    func applicationDidFinishLaunching(_ notification: Notification) {
        MacOSAppComponentKt.busyLib.connectionService.onApplicationInit()
    }
}
#endif
