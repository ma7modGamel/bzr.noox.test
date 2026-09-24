import BzrCore
import Foundation
import Security

final class AppAuthSessionStore: AuthSessionStoring {
    private let defaults = UserDefaults.standard

    var token: String? {
        get { KeychainTokenStore.read() }
        set { KeychainTokenStore.write(newValue) }
    }

    var email: String? {
        get { defaults.string(forKey: "auth_email") }
        set { defaults.set(newValue, forKey: "auth_email") }
    }

    func clear() {
        KeychainTokenStore.write(nil)
        defaults.removeObject(forKey: "auth_email")
    }
}

private enum KeychainTokenStore {
    private static let account = "mobile_auth_token"
    private static let service = "noox.bzr.ios"

    static func read() -> String? {
        var result: CFTypeRef?
        let status = SecItemCopyMatching(
            [
                kSecClass: kSecClassGenericPassword,
                kSecAttrAccount: account,
                kSecAttrService: service,
                kSecReturnData: true,
            ] as CFDictionary,
            &result
        )
        guard status == errSecSuccess, let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    static func write(_ token: String?) {
        let query =
            [
                kSecClass: kSecClassGenericPassword,
                kSecAttrAccount: account,
                kSecAttrService: service,
            ] as CFDictionary
        _ = SecItemDelete(query)
        guard let token, let data = token.data(using: .utf8) else { return }
        _ = SecItemAdd(
            [
                kSecClass: kSecClassGenericPassword,
                kSecAttrAccount: account,
                kSecAttrService: service,
                kSecValueData: data,
                kSecAttrAccessible: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            ] as CFDictionary,
            nil
        )
    }
}
