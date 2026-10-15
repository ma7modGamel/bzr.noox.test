import Foundation
import XCTest

@testable import BzrCore

#if canImport(FoundationNetworking)
    import FoundationNetworking
#endif

/// DEC-065: only a 401 on a call that carried a token ends the session, and only once.
final class SessionExpiryTests: XCTestCase {
    private struct StatusTransport: HTTPTransport {
        let status: Int
        func send(_ request: URLRequest) async throws -> HTTPResult {
            HTTPResult(data: Data("{}".utf8), statusCode: status)
        }
    }

    private struct Empty: Decodable, Sendable {}

    private let baseURL = URL(string: "https://example.test/api/v1/")!

    private final class Counter: @unchecked Sendable {
        private let lock = NSLock()
        private var value = 0
        func increment() { lock.withLock { value += 1 } }
        var count: Int { lock.withLock { value } }
    }

    private func expiries(during block: () async -> Void) async -> Int {
        let counter = Counter()
        let observer = NotificationCenter.default.addObserver(
            forName: SessionExpiry.notification, object: nil, queue: nil
        ) { _ in counter.increment() }
        defer { NotificationCenter.default.removeObserver(observer) }
        SessionExpiry.shared.reset()
        await block()
        return counter.count
    }

    private func call(_ path: String, token: String?, status: Int) async {
        let client = APIClient(
            baseURL: baseURL, token: token, transport: StatusTransport(status: status))
        _ = try? await client.get(path, as: Empty.self)
    }

    func testRefusedTokenExpiresTheSessionOnce() async {
        let count = await expiries {
            for _ in 0..<3 { await call("orders?scope=current", token: "old", status: 401) }
        }
        XCTAssertEqual(count, 1)
    }

    func testLoginWithoutTokenIsNotAnExpiry() async {
        let count = await expiries { await call("auth/login", token: nil, status: 401) }
        XCTAssertEqual(count, 0)
    }

    func testLogoutWithRefusedTokenIsNotAnExpiry() async {
        let count = await expiries { await call("auth/logout", token: "old", status: 401) }
        XCTAssertEqual(count, 0)
    }

    func testANewSessionCanExpireAgain() async {
        let count = await expiries {
            await call("me", token: "old", status: 401)
            SessionExpiry.shared.reset()
            await call("me", token: "new", status: 401)
        }
        XCTAssertEqual(count, 2)
    }

    func testOtherErrorsKeepTheSession() async {
        let count = await expiries {
            await call("me", token: "t", status: 403)
            await call("me", token: "t", status: 500)
        }
        XCTAssertEqual(count, 0)
    }
}
