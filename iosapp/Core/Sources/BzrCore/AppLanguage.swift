import Foundation

/// The same supported-language negotiation is used by UI resources and HTTP requests.
public enum AppLanguage {
    public static func resolve(_ preferences: [String]) -> String {
        for tag in preferences {
            let language = tag.replacingOccurrences(of: "_", with: "-")
                .split(separator: "-").first.map(String.init)?.lowercased()
            if let language, language == "ar" || language == "en" { return language }
        }
        return "ar"
    }

    public static var current: String { resolve(Locale.preferredLanguages) }
}
