import BzrCore
import FirebaseCore
import FirebaseCrashlytics
import Foundation

enum BremoDiagnostics {
    private static let observer = NotificationCenter.default.addObserver(
        forName: AppDiagnostics.failureNotification, object: nil, queue: nil
    ) { notification in
        guard FirebaseApp.app() != nil, let kind = notification.userInfo?["kind"] as? String else { return }
        let error = NSError(domain: "BremoClient.\(kind)", code: 1, userInfo: [NSLocalizedDescriptionKey: kind])
        Crashlytics.crashlytics().record(error: error)
    }

    static func start() {
        guard FirebaseApp.app() != nil else { return }
        _ = observer
        let crashlytics = Crashlytics.crashlytics()
        crashlytics.setCustomValue("iOS", forKey: "platform")
        crashlytics.setCustomValue(Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "unknown", forKey: "app_version")
        crashlytics.setCustomValue(Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "unknown", forKey: "app_build")
    }

    static func screen(_ screen: String, mode: String) {
        guard FirebaseApp.app() != nil else { return }
        let crashlytics = Crashlytics.crashlytics()
        crashlytics.setCustomValue(screen, forKey: "screen")
        crashlytics.setCustomValue(mode, forKey: "mode")
        crashlytics.log("screen=\(screen) mode=\(mode)")
    }
}
