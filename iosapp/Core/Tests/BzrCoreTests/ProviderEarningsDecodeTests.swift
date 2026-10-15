import Foundation
import XCTest

@testable import BzrCore

/// DEC-068: the earnings model decodes the real server answers recorded for both apps
/// (tests/Feature/Api/MobileProviderEarningsResponsesTest.php).
final class ProviderEarningsDecodeTests: XCTestCase {
    private struct Envelope: Decodable { let data: ProviderEarningsPayload }

    private func decode(_ name: String) throws -> ProviderEarningsPayload {
        let url = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
            .deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("androidapp/app/src/test/resources/api/provider/\(name).json")
        return try JSONDecoder().decode(Envelope.self, from: Data(contentsOf: url)).data
    }

    func testBlockedMarketplaceEarningsDecode() throws {
        let earnings = try decode("earnings_marketplace_blocked")
        XCTAssertEqual(earnings.operatingMode, "MARKETPLACE")
        XCTAssertEqual(earnings.dues, ProviderEarningsDues(blocked: true, debt: "650.00", limit: "500.00"))
        XCTAssertEqual(earnings.transactions.map(\.detail), ["عمولة مستحقة للمنصة · 650.00 جنيه"])
    }

    func testEmployeeEarningsDecode() throws {
        let earnings = try decode("earnings_employee")
        XCTAssertEqual(earnings.summary.balance, "-350.00")
        XCTAssertFalse(earnings.dues.blocked)
        XCTAssertNil(earnings.dues.limit)
    }
}
