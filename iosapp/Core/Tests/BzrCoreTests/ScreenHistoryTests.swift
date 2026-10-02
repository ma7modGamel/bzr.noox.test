import Foundation
import XCTest

@testable import BzrCore

final class ScreenHistoryTests: XCTestCase {
    func testHistoryMatchesSharedFixture() throws {
        let url = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
            .deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("design/fixtures/navigation/history.json")
        let fixture = try JSONDecoder().decode(HistoryFixture.self, from: Data(contentsOf: url))
        for item in fixture.cases {
            var history = ScreenHistory<String>(root: fixture.root) { $0 }
            for (index, step) in item.steps.enumerated() {
                let label = "\(item.id)#\(index)"
                switch step.op {
                case "show": history.show(step.screen ?? "", restorable: step.restorable ?? true)
                case "commit": history.commit()
                default: XCTAssertEqual(history.back { fixture.root }, step.expect, label)
                }
            }
        }
        XCTAssertEqual(fixture.cases.count, 8)
    }

    func testSlotDayLabelsMatchSharedFixture() throws {
        let url = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
            .deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("design/fixtures/navigation/slot-days.json")
        let fixture = try JSONDecoder().decode(SlotDaysFixture.self, from: Data(contentsOf: url))
        for item in fixture.cases {
            XCTAssertEqual(
                CustomerLogic.slotDayLabels(isoDates: item.dates, names: fixture.names), item.expected, item.id)
        }
    }
}

private struct SlotDaysFixture: Decodable {
    let names: [String]
    let cases: [SlotDaysCase]
}

private struct SlotDaysCase: Decodable {
    let id: String
    let dates: [String]
    let expected: [String]
}

private struct HistoryFixture: Decodable {
    let root: String
    let cases: [HistoryCase]
}

private struct HistoryCase: Decodable {
    let id: String
    let steps: [HistoryStep]
}

private struct HistoryStep: Decodable {
    let op: String
    let screen: String?
    let restorable: Bool?
    let expect: String?
}
