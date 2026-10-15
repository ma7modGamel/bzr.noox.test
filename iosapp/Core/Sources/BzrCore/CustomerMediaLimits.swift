import Foundation

public enum CustomerMediaLimits {
    public static let videoSeconds = 30.0
    public static func videoError(durationSeconds: Double?) -> String? {
        guard let durationSeconds, durationSeconds.isFinite, durationSeconds > 0 else { return "media.validation.video_unreadable" }
        return durationSeconds > videoSeconds ? "media.validation.video_duration" : nil
    }
}
