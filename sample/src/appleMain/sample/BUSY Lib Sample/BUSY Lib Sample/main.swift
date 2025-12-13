#if os(macOS)
import AppKit
import BridgeConnection

let app = NSApplication.shared
let appDelegate = AppDelegate()
app.delegate = appDelegate
app.setActivationPolicy(.accessory)

MainKt.rootWindow(root: appDelegate.root)
_ = NSApplicationMain(CommandLine.argc, CommandLine.unsafeArgv)
#endif
