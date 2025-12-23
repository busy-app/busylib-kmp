import SwiftUI
import BridgeConnection

@main
struct App: SwiftUI.App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate: AppDelegate

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
