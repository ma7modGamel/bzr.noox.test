import Foundation
import XCTest

@testable import BzrCore

final class CustomerSharedFixtureTests: XCTestCase {
    func testAllCustomerCasesMatch() throws {
        var count = 0
        let fixturePaths = [
            "design/fixtures/SCR-C01/cases.json", "design/fixtures/SCR-C02/cases.json",
            "design/fixtures/SCR-C03/cases.json", "design/fixtures/SCR-C04/cases.json",
            "design/fixtures/SCR-C05/cases.json", "design/fixtures/SCR-C06/cases.json",
            "design/fixtures/SCR-C07/cases.json", "design/fixtures/SCR-C08/cases.json",
            "design/fixtures/SCR-C09/cases.json", "design/fixtures/SCR-C14/cases.json",
            "design/fixtures/SCR-C15/cases.json", "design/fixtures/SCR-C16/cases.json",
            "design/fixtures/SCR-C17/cases.json", "design/fixtures/SCR-C18/cases.json",
            "design/fixtures/SCR-C19/cases.json", "design/fixtures/SCR-C20/cases.json",
            "design/fixtures/SCR-C21/cases.json", "design/fixtures/SCR-C22/cases.json",
            "design/fixtures/SCR-C23/cases.json", "design/fixtures/SCR-C24/cases.json",
            "design/fixtures/SCR-C25/cases.json", "design/fixtures/SCR-C26/cases.json",
            "design/fixtures/SCR-C27/cases.json", "design/fixtures/SCR-C28/cases.json",
            "design/fixtures/SCR-C29/cases.json",
            "design/fixtures/SCR-C30/cases.json", "design/fixtures/SCR-C31/cases.json",
            "design/fixtures/SCR-C32/cases.json", "design/fixtures/SCR-C33/cases.json",
            "design/fixtures/SCR-C34/cases.json", "design/fixtures/SCR-C35/cases.json",
            "design/fixtures/SCR-C36/cases.json",
        ]
        for path in fixturePaths {
            let url = repositoryRoot().appendingPathComponent(path)
            let fixture = try JSONDecoder().decode(CustomerFixture.self, from: Data(contentsOf: url))
            let screen = fixture.screen
            for testCase in fixture.cases {
                let actual = CustomerLogic.reduce(
                    screen: screen, input: testCase.input.mapValues(\.customerValue))
                let expected = testCase.expected
                let label = "\(screen)/\(testCase.id)"
                XCTAssertEqual(actual.phase.rawValue, expected.phase, label)
                XCTAssertEqual(actual.canContinue, expected.canContinue, label)
                optional(expected.messageKey, actual.messageKey, label)
                optional(expected.descriptionError, actual.descriptionError, label)
                optional(expected.termsError, actual.termsError, label)
                optional(expected.showPricing, actual.showPricing, label)
                optional(expected.showMap, actual.showMap, label)
                optional(expected.showEta, actual.showEta, label)
                optional(expected.showStepper, actual.showStepper, label)
                optional(expected.showStatusCard, actual.showStatusCard, label)
                optional(expected.etaApproximate, actual.etaApproximate, label)
                optional(expected.visibleActions, actual.visibleActions, label)
                optional(expected.itemCount, actual.items.count, label)
                optional(expected.selectedIndex, actual.selectedIndex, label)
                optional(expected.selectedOptionIndex, actual.selectedOptionIndex, label)
                optional(expected.fieldErrors, actual.fieldErrors, label)
                optional(expected.isBusy, actual.isBusy, label)
                optional(expected.isEditing, actual.isEditing, label)
                optional(expected.isDefault, actual.isDefault, label)
                optional(expected.showConfirmation, actual.showConfirmation, label)
                optional(expected.pendingIndex, actual.pendingIndex, label)
                optional(expected.countdownSeconds, actual.countdownSeconds, label)
                optional(expected.hasPhoto, actual.hasPhoto, label)
                optional(expected.mapLatitude, actual.mapLatitude, label)
                optional(expected.mapLongitude, actual.mapLongitude, label)
                optional(expected.notificationsDenied, actual.notificationsDenied, label)
                optional(expected.ratingRemindersEnabled, actual.ratingRemindersEnabled, label)
                optional(expected.selectedMaterialIndex, actual.selectedMaterialIndex, label)
                optional(expected.selectedPricingIndex, actual.selectedPricingIndex, label)
                optional(expected.providerVerified, actual.providerVerified, label)
                count += 1
            }
        }
        XCTAssertEqual(count, 227)
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

private struct CustomerFixture: Decodable {
    let screen: String
    let cases: [CustomerCase]
}

private struct CustomerCase: Decodable {
    let id: String
    let input: [String: CustomerFixtureValue]
    let expected: CustomerExpectedState
}

private struct CustomerExpectedState: Decodable {
    let phase: String
    let canContinue: Bool
    let messageKey: String?
    let descriptionError: String?
    let termsError: String?
    let showPricing: Bool?
    let showMap: Bool?
    let showEta: Bool?
    let showStepper: Bool?
    let showStatusCard: Bool?
    let etaApproximate: Bool?
    let visibleActions: [String]?
    let itemCount: Int?
    let selectedIndex: Int?
    let selectedOptionIndex: Int?
    let fieldErrors: [String]?
    let isBusy: Bool?
    let isEditing: Bool?
    let isDefault: Bool?
    let showConfirmation: Bool?
    let pendingIndex: Int?
    let countdownSeconds: Int?
    let hasPhoto: Bool?
    let mapLatitude: Double?
    let mapLongitude: Double?
    let notificationsDenied: Bool?
    let ratingRemindersEnabled: Bool?
    let selectedMaterialIndex: Int?
    let selectedPricingIndex: Int?
    let providerVerified: Bool?

    enum CodingKeys: String, CodingKey {
        case phase
        case canContinue = "can_continue"
        case messageKey = "message_key"
        case descriptionError = "description_error"
        case termsError = "terms_error"
        case showPricing = "show_pricing"
        case showMap = "show_map"
        case showEta = "show_eta"
        case showStepper = "show_stepper"
        case showStatusCard = "show_status_card"
        case etaApproximate = "eta_approximate"
        case visibleActions = "visible_actions"
        case itemCount = "item_count"
        case selectedIndex = "selected_index"
        case selectedOptionIndex = "selected_option_index"
        case fieldErrors = "field_errors"
        case isBusy = "is_busy"
        case isEditing = "is_editing"
        case isDefault = "is_default"
        case showConfirmation = "show_confirmation"
        case pendingIndex = "pending_index"
        case countdownSeconds = "countdown_seconds"
        case hasPhoto = "has_photo"
        case mapLatitude = "map_latitude"
        case mapLongitude = "map_longitude"
        case notificationsDenied = "notifications_denied"
        case ratingRemindersEnabled = "rating_reminders_enabled"
        case selectedMaterialIndex = "selected_material_index"
        case selectedPricingIndex = "selected_pricing_index"
        case providerVerified = "provider_verified"
    }
}

private enum CustomerFixtureValue: Decodable {
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

    var customerValue: CustomerInputValue {
        switch self {
        case let .text(value): .text(value)
        case let .bool(value): .bool(value)
        case let .integer(value): .integer(value)
        case let .strings(value): .strings(value)
        }
    }
}
