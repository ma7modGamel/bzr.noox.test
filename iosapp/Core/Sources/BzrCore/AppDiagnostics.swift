import Foundation

/// UI-independent diagnostics; observers receive a fixed error category, never request or response data.
public enum AppDiagnostics {
    public static let failureNotification = Notification.Name("BremoUnexpectedFailure")

    public static func failureKind(_ error: Error) -> String? {
        if error is CancellationError || (error as NSError).domain == NSURLErrorDomain { return nil }
        if let error = error as? APIClientError {
            switch error {
            case let .httpStatus(status): return status >= 500 ? "HTTP_\(status)" : nil
            case .rejected: return nil
            case .invalidResponse: return "InvalidResponse"
            }
        }
        if error is DecodingError { return "DecodingError" }
        return "UnexpectedClientError"
    }

    public static func report(_ error: Error) {
        guard let kind = failureKind(error) else { return }
        NotificationCenter.default.post(name: failureNotification, object: nil, userInfo: ["kind": kind])
    }
}
