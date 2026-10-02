import BzrCore
import FirebaseCore
import FirebaseMessaging
import SwiftUI
import UIKit
import UserNotifications

/// Push state shared by the app delegate and the SwiftUI root (DEC-058): the FCM token, and a link that
/// arrived from a notification tap before the root could route it.
@MainActor
@Observable
final class PushCenter {
    static let shared = PushCenter()

    private(set) var token: String?
    var pendingLink: String?
    var onToken: (String) -> Void = { _ in }

    private let defaults = UserDefaults.standard

    private init() { token = defaults.string(forKey: "push.fcm_token") }

    func store(_ token: String) {
        self.token = token
        defaults.set(token, forKey: "push.fcm_token")
        onToken(token)
    }

    /// 17 §إذن الإشعارات — the system prompt once, after the first C01 or P08.
    func requestPermissionOnce() {
        guard !defaults.bool(forKey: "push.permission_asked") else {
            UIApplication.shared.registerForRemoteNotifications()
            return
        }
        defaults.set(true, forKey: "push.permission_asked")
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, _ in
            guard granted else { return }
            Task { @MainActor in UIApplication.shared.registerForRemoteNotifications() }
        }
    }

    /// C32 shows the settings banner when this is false; refreshed whenever the app becomes active.
    private(set) var notificationsAllowed = true

    func refreshAuthorization() {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            let allowed = settings.authorizationStatus != .denied
            Task { @MainActor in PushCenter.shared.notificationsAllowed = allowed }
        }
    }
}

/// APNs through Firebase Messaging (DEC-058). Firebase starts only when `GoogleService-Info.plist` is bundled
/// (DEP-PUSH-02); without it the app runs, and the in-app list (C32) still works (EC-22).
final class BzrAppDelegate: NSObject, UIApplicationDelegate, MessagingDelegate,
    UNUserNotificationCenterDelegate
{
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        if Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil {
            FirebaseApp.configure()
            Messaging.messaging().delegate = self
        }
        return true
    }

    func application(
        _ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        guard FirebaseApp.app() != nil else { return }
        Messaging.messaging().apnsToken = deviceToken
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken else { return }
        Task { @MainActor in PushCenter.shared.store(fcmToken) }
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter, willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .list, .sound]
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse
    ) async {
        guard let link = response.notification.request.content.userInfo["deep_link"] as? String,
            !link.isEmpty
        else { return }
        await MainActor.run { PushCenter.shared.pendingLink = link }
    }
}

func openNotificationSettings() {
    guard let url = URL(string: UIApplication.openNotificationSettingsURLString) else { return }
    UIApplication.shared.open(url)
}
