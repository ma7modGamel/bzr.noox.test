import BzrCore

/// The single number formatter shared by spec with Android (`BzrFormat.kt`), 43 §2:
/// Western Arabic digits everywhere, whatever the device locale.
public enum BzrFormat {
    /// 350 → "350", 4.9 → "4.9".
    public static func number(_ value: Double) -> String {
        BzrFormatter.number(value)
    }

    public static func number(_ value: Int) -> String { BzrFormatter.number(value) }

    /// Fills `{name}` placeholders of a template from strings.ar.json.
    public static func fill(_ template: String, _ values: [(String, String)]) -> String {
        BzrFormatter.fill(template, values: values)
    }

    public static func fill(_ template: String, n: Int) -> String { fill(template, [("n", number(n))]) }

    public static func fill(_ template: String, n: Double) -> String { fill(template, [("n", number(n))]) }

    /// 760 → "12:40".
    public static func countdown(_ seconds: Int) -> String {
        BzrFormatter.countdown(seconds)
    }
}
