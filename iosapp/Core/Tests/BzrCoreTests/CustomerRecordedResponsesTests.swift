import Foundation
import XCTest

@testable import BzrCore

/// DEC-061 / 9أ stage 3: iOS decodes the same real server answers Android reads
/// (androidapp/app/src/test/resources/api/customer, recorded by tests/Feature/Api/Mobile*ResponsesTest.php).
final class CustomerRecordedResponsesTests: XCTestCase {
    private func recorded(_ name: String) throws -> Data {
        let url = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
            .deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("androidapp/app/src/test/resources/api/customer/\(name).json")
        return try Data(contentsOf: url)
    }

    private func decode<Value: Decodable>(_ type: Value.Type, _ name: String) throws -> Value {
        try BzrJSON.decoder().decode(type, from: recorded(name))
    }

    func testOrderDecodes() throws {
        let order = try decode(CustomerOrderEnvelope.self, "order_open").data
        XCTAssertEqual(order.number, "1")
    }

    func testOrderListDecodes() throws {
        XCTAssertFalse(try decode(CustomerOrderListEnvelope.self, "orders_current").data.isEmpty)
    }

    func testNumberAcceptsTextAndIsOptional() throws {
        struct Row: Decodable { @FlexibleString var number: String? }
        let decoder = JSONDecoder()
        XCTAssertEqual(try decoder.decode(Row.self, from: Data(#"{"number":"42"}"#.utf8)).number, "42")
        XCTAssertNil(try decoder.decode(Row.self, from: Data(#"{"number":null}"#.utf8)).number)
        XCTAssertNil(try decoder.decode(Row.self, from: Data("{}".utf8)).number)
    }
}
