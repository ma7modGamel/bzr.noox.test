import Foundation
import XCTest
@testable import BzrCore

final class AppDiagnosticsTests: XCTestCase {
    func testExpectedFailuresDoNotBecomeCrashIssues() {
        XCTAssertNil(AppDiagnostics.failureKind(CancellationError()))
        XCTAssertNil(AppDiagnostics.failureKind(URLError(.notConnectedToInternet)))
        XCTAssertNil(AppDiagnostics.failureKind(APIClientError.rejected(message: "private customer data")))
        XCTAssertNil(AppDiagnostics.failureKind(APIClientError.httpStatus(401)))
        XCTAssertNil(AppDiagnostics.failureKind(APIClientError.httpStatus(422)))
    }

    func testUnexpectedFailuresHaveFixedCategoriesWithoutPrivateDescriptions() {
        XCTAssertEqual(AppDiagnostics.failureKind(APIClientError.httpStatus(503)), "HTTP_503")
        XCTAssertEqual(AppDiagnostics.failureKind(APIClientError.invalidResponse), "InvalidResponse")
        let error = NSError(domain: "customer@example.test", code: 1, userInfo: [NSLocalizedDescriptionKey: "Bearer secret-token"])
        XCTAssertEqual(AppDiagnostics.failureKind(error), "UnexpectedClientError")
    }
}
