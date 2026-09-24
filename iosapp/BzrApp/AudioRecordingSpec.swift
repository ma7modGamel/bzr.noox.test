import AVFoundation

/// BR-014 — contract shared with Android and enforced again by the server.
enum AudioRecordingSpec {
    static let fileExtension = "m4a"
    static let mimeType = "audio/mp4"
    static let formatID = kAudioFormatMPEG4AAC
    static let channelCount = 1
    static let bitRate = 64_000
    static let maxDurationSeconds: TimeInterval = 120
    static let maxFileSizeBytes = 5 * 1024 * 1024

    static let settings: [String: Any] = [
        AVFormatIDKey: formatID,
        AVNumberOfChannelsKey: channelCount,
        AVEncoderBitRateKey: bitRate,
    ]
}
