import Foundation
import XCTest

@testable import BzrCore

final class ProviderSharedFixtureTests: XCTestCase {
    func testAllProviderCasesMatch() throws {
        var count = 0
        let paths = [
            "design/fixtures/SCR-P01/cases.json", "design/fixtures/SCR-P02/cases.json",
            "design/fixtures/SCR-P03/cases.json", "design/fixtures/SCR-P04/cases.json",
            "design/fixtures/SCR-P05/cases.json", "design/fixtures/SCR-P06/cases.json",
            "design/fixtures/SCR-P07/cases.json", "design/fixtures/SCR-P08/cases.json",
            "design/fixtures/SCR-P09/cases.json", "design/fixtures/SCR-P10/cases.json",
            "design/fixtures/SCR-P11/cases.json",
            "design/fixtures/SCR-P12/cases.json", "design/fixtures/SCR-P13/cases.json",
            "design/fixtures/SCR-P14/cases.json", "design/fixtures/SCR-P15/cases.json",
            "design/fixtures/SCR-P16/cases.json", "design/fixtures/SCR-P20/cases.json",
            "design/fixtures/SCR-P21/cases.json",
            "design/fixtures/SCR-P17/cases.json", "design/fixtures/SCR-P18/cases.json",
            "design/fixtures/SCR-P19/cases.json", "design/fixtures/SCR-C19/cases.json",
        ]
        for path in paths {
            let data = try Data(contentsOf: repositoryRoot().appendingPathComponent(path))
            let fixture = try JSONDecoder().decode(ProviderFixture.self, from: data)
            for testCase in fixture.cases {
                let actual = ProviderLogic.reduce(
                    screen: fixture.screen, input: testCase.input.mapValues(\.providerValue))
                let expected = testCase.expected
                let label = "\(fixture.screen)/\(testCase.id)"
                XCTAssertEqual(actual.phase.rawValue, expected.phase, label)
                XCTAssertEqual(actual.canContinue, expected.canContinue, label)
                optional(expected.messageKey, actual.messageKey, label)
                optional(expected.fieldErrors, actual.fieldErrors, label)
                optional(expected.visibleActions, actual.visibleActions, label)
                optional(expected.itemCount, actual.items.count, label)
                optional(expected.selectedCount, actual.selectedCount, label)
                optional(expected.isBusy, actual.isBusy, label)
                optional(expected.hasProfilePhoto, actual.hasProfilePhoto, label)
                optional(expected.hasIDFront, actual.hasIDFront, label)
                optional(expected.hasIDBack, actual.hasIDBack, label)
                optional(expected.availableNow, actual.availableNow, label)
                optional(expected.activeOrderID, actual.activeOrderID, label)
                optional(expected.displayStatus, actual.displayStatus, label)
                optional(expected.unreadCount, actual.unreadCount, label)
                optional(expected.selectedOptionIndex, actual.selectedOptionIndex, label)
                optional(expected.showPriceGuide, actual.showPriceGuide, label)
                optional(expected.showOutsideReason, actual.showOutsideReason, label)
                optional(expected.hasPhoto, actual.hasPhoto, label)
                optional(expected.currentAction, actual.currentAction, label)
                optional(expected.stepCount, actual.stepStates.count, label)
                optional(expected.secondaryItemCount, actual.secondaryOptions.count, label)
                count += 1
            }
        }
        XCTAssertEqual(count, 148)
    }

    private func optional<T: Equatable>(_ expected: T?, _ actual: T, _ label: String) {
        if let expected { XCTAssertEqual(actual, expected, label) }
    }

    private func optional<T: Equatable>(_ expected: T?, _ actual: T?, _ label: String) {
        if let expected { XCTAssertEqual(actual, expected, label) }
    }

    private func repositoryRoot() -> URL {
        URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
            .deletingLastPathComponent().deletingLastPathComponent()
    }
}

private struct ProviderFixture: Decodable {
    let screen: String
    let cases: [ProviderCase]
}

private struct ProviderCase: Decodable {
    let id: String
    let input: [String: ProviderFixtureValue]
    let expected: ProviderExpectedState
}

private struct ProviderExpectedState: Decodable {
    let phase: String
    let canContinue: Bool
    let messageKey: String?
    let fieldErrors: [String]?
    let visibleActions: [String]?
    let itemCount: Int?
    let selectedCount: Int?
    let isBusy: Bool?
    let hasProfilePhoto: Bool?
    let hasIDFront: Bool?
    let hasIDBack: Bool?
    let availableNow: Bool?
    let activeOrderID: Int?
    let displayStatus: String?
    let unreadCount: Int?
    let selectedOptionIndex: Int?
    let showPriceGuide: Bool?
    let showOutsideReason: Bool?
    let hasPhoto: Bool?
    let currentAction: String?
    let stepCount: Int?
    let secondaryItemCount: Int?

    enum CodingKeys: String, CodingKey {
        case phase
        case canContinue = "can_continue"
        case messageKey = "message_key"
        case fieldErrors = "field_errors"
        case visibleActions = "visible_actions"
        case itemCount = "item_count"
        case selectedCount = "selected_count"
        case isBusy = "is_busy"
        case hasProfilePhoto = "has_profile_photo"
        case hasIDFront = "has_id_front"
        case hasIDBack = "has_id_back"
        case availableNow = "available_now"
        case activeOrderID = "active_order_id"
        case displayStatus = "display_status"
        case unreadCount = "unread_count"
        case selectedOptionIndex = "selected_option_index"
        case showPriceGuide = "show_price_guide"
        case showOutsideReason = "show_outside_reason"
        case hasPhoto = "has_photo"
        case currentAction = "current_action"
        case stepCount = "step_count"
        case secondaryItemCount = "secondary_item_count"
    }
}

private enum ProviderFixtureValue: Decodable {
    case text(String)
    case bool(Bool)
    case integer(Int)
    case strings([String])

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let value = try? container.decode(Bool.self) {
            self = .bool(value)
        } else if let value = try? container.decode(Int.self) {
            self = .integer(value)
        } else if let value = try? container.decode([String].self) {
            self = .strings(value)
        } else {
            self = .text(try container.decode(String.self))
        }
    }

    var providerValue: ProviderInputValue {
        switch self {
        case let .text(value): .text(value)
        case let .bool(value): .bool(value)
        case let .integer(value): .integer(value)
        case let .strings(value): .strings(value)
        }
    }
}
