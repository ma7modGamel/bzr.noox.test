import Foundation

public struct DateText: Equatable, Sendable {
    public let today: String
    public let tomorrow: String
    public let weekdays: [String]
    public let months: [String]

    public init(today: String, tomorrow: String, weekdays: [String], months: [String]) {
        self.today = today
        self.tomorrow = tomorrow
        self.weekdays = weekdays
        self.months = months
    }
}

public enum BzrFormatter {
    public static func ltrValue(_ value: String) -> String { "\u{2066}\(value)\u{2069}" }

    public static func legalDate(_ value: String) -> String {
        guard let date = ISO8601DateFormatter().date(from: value) else { return value }
        let formatter = DateFormatter()
        formatter.locale = posix
        formatter.timeZone = TimeZone(identifier: "Africa/Cairo")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }

    private static let posix = Locale(identifier: "en_US_POSIX")

    public static func number(_ value: Double) -> String {
        value.truncatingRemainder(dividingBy: 1) == 0
            ? String(Int64(value))
            : String(format: "%.1f", locale: posix, value)
    }

    public static func number(_ value: Int) -> String {
        String(value)
    }

    public static func amount(_ value: Double, currency: String) -> String {
        "\(number(value)) \(currency)"
    }

    public static func money(_ value: String, currency: String) -> String {
        let number = Decimal(string: value, locale: posix).map { NSDecimalNumber(decimal: $0).stringValue } ?? value
        return "\(number) \(currency)"
    }

    public static func fill(_ template: String, values: [(String, String)]) -> String {
        values.reduce(template) { text, pair in
            text.replacingOccurrences(of: "{\(pair.0)}", with: pair.1)
        }
    }

    public static func countdown(_ seconds: Int) -> String {
        String(format: "%02d:%02d", locale: posix, seconds / 60, seconds % 60)
    }

    public static func time(
        _ date: Date,
        timeZone: TimeZone,
        morning: String,
        evening: String
    ) -> String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        let components = calendar.dateComponents([.hour, .minute], from: date)
        let hour = components.hour ?? 0
        let minute = components.minute ?? 0
        let twelveHour = hour % 12 == 0 ? 12 : hour % 12
        let period = hour < 12 ? morning : evening
        return String(format: "%d:%02d %@", locale: posix, twelveHour, minute, period)
    }

    public static func dateLabel(
        _ date: Date,
        relativeTo reference: Date,
        timeZone: TimeZone,
        text: DateText
    ) -> String {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        let start = calendar.startOfDay(for: reference)
        let target = calendar.startOfDay(for: date)
        let distance = calendar.dateComponents([.day], from: start, to: target).day
        if distance == 0 { return text.today }
        if distance == 1 { return text.tomorrow }

        let components = calendar.dateComponents([.weekday, .day, .month], from: target)
        let weekday = text.weekdays[(components.weekday ?? 1) - 1]
        let month = text.months[(components.month ?? 1) - 1]
        return "\(weekday) \(components.day ?? 1) \(month)"
    }
}
