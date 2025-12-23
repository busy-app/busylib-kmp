import BridgeConnection
import AppKit
import Foundation

class AppDelegate: NSObject, NSApplicationDelegate {
    let root: ConnectionRootDecomposeComponent = MacOSAppComponentKt.getRootDecomposeComponent()

    func applicationDidFinishLaunching(_ notification: Notification) {
        MacOSAppComponentKt.busyLib.connectionService.onApplicationInit()
    }
}
