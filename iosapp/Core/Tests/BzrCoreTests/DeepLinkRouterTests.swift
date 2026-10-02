import Foundation
import XCTest

@testable import BzrCore

final class DeepLinkRouterTests: XCTestCase {
    func testRoutesMatchSharedFixture() throws {
        let url = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
            .deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("design/fixtures/deeplinks/cases.json")
        let fixture = try JSONDecoder().decode(DeepLinkFixture.self, from: Data(contentsOf: url))
        for item in fixture.cases {
            let actual = DeepLinkRouter.route(item.input.link, providerStatus: item.input.providerStatus)
            XCTAssertEqual(actual.mode, item.expected.mode, item.id)
            XCTAssertEqual(actual.target, item.expected.target, item.id)
            XCTAssertEqual(actual.orderID, item.expected.orderID, item.id)
        }
        XCTAssertEqual(fixture.cases.count, 23)
    }
}

private struct DeepLinkFixture: Decodable {
    let cases: [DeepLinkCase]
}

private struct DeepLinkCase: Decodable {
    let id: String
    let input: DeepLinkInput
    let expected: DeepLinkExpected
}

private struct DeepLinkInput: Decodable {
    let link: String
    let providerStatus: String

    enum CodingKeys: String, CodingKey {
        case link
        case providerStatus = "provider_status"
    }
}

private struct DeepLinkExpected: Decodable {
    let mode: String
    let target: String
    let orderID: Int

    enum CodingKeys: String, CodingKey {
        case mode, target
        case orderID = "order_id"
    }
}
