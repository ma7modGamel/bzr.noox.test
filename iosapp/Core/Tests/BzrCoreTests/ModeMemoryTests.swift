import Foundation
import XCTest

@testable import BzrCore

/// DEC-067: the shared restore cases, the same ones Android runs.
final class ModeMemoryTests: XCTestCase {
    private struct RestoreInput: Decodable {
        let lastMode: String?
        let providerStatus: String
        let hasPendingLink: Bool
    }

    private struct RestoreExpected: Decodable {
        let target: String
        let forget: Bool
    }

    private struct RestoreCase: Decodable {
        let id: String
        let input: RestoreInput
        let expected: RestoreExpected
    }

    private struct Fixture: Decodable {
        let cases: [RestoreCase]
    }

    func testSharedRestoreCasesMatch() throws {
        let url = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
            .deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("design/fixtures/navigation/mode-restore.json")
        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase
        let fixture = try decoder.decode(Fixture.self, from: Data(contentsOf: url))

        for item in fixture.cases {
            let result = ModeMemory.restoreTarget(
                lastMode: item.input.lastMode, providerStatus: item.input.providerStatus,
                hasPendingLink: item.input.hasPendingLink)
            XCTAssertEqual(
                result, ModeRestore(target: item.expected.target, forget: item.expected.forget), item.id)
        }
        XCTAssertEqual(fixture.cases.count, 7)
    }

    func testMemoryIsKeptPerAccount() throws {
        let defaults = try XCTUnwrap(UserDefaults(suiteName: "ModeMemoryTests"))
        defaults.removePersistentDomain(forName: "ModeMemoryTests")
        let store = ModeMemoryStore(defaults: defaults)

        store.remember(email: "Tech@Bremo.test", mode: ModeMemory.provider)

        XCTAssertEqual(store.lastMode(email: "tech@bremo.test"), ModeMemory.provider)
        XCTAssertNil(store.lastMode(email: "customer@bremo.test"))
        XCTAssertNil(store.lastMode(email: nil))
    }
}
