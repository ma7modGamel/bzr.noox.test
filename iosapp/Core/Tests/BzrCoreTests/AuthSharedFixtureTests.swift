import Foundation
import XCTest

@testable import BzrCore

final class AuthSharedFixtureTests: XCTestCase {
    func testAllSharedAuthCasesMatch() throws {
        let fixturePaths = [
            "design/fixtures/SCR-C10/cases.json",
            "design/fixtures/SCR-C11/cases.json",
            "design/fixtures/SCR-C12/cases.json",
            "design/fixtures/SCR-C13/cases.json",
        ]
        var count = 0
        for path in fixturePaths {
            let fixture = try JSONDecoder().decode(
                AuthFixture.self, from: Data(contentsOf: repositoryRoot().appendingPathComponent(path)))
            for testCase in fixture.cases {
                let actual = AuthLogic.reduce(
                    screen: fixture.screen, input: testCase.input.mapValues(\.authValue))
                let label = "\(fixture.screen)/\(testCase.id)"
                XCTAssertEqual(actual.phase.rawValue, testCase.expected.phase, label)
                XCTAssertEqual(actual.canSubmit, testCase.expected.canSubmit, label)
                XCTAssertEqual(actual.nameError, testCase.expected.nameError, label)
                XCTAssertEqual(actual.emailError, testCase.expected.emailError, label)
                XCTAssertEqual(actual.phoneError, testCase.expected.phoneError, label)
                XCTAssertEqual(actual.passwordError, testCase.expected.passwordError, label)
                XCTAssertEqual(actual.messageKey, testCase.expected.messageKey, label)
                XCTAssertEqual(actual.route, testCase.expected.route, label)
                count += 1
            }
        }
        XCTAssertEqual(count, 29)
    }

    private func repositoryRoot() -> URL {
        URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
    }
}

private struct AuthFixture: Decodable {
    let screen: String
    let cases: [AuthCase]
}

private struct AuthCase: Decodable {
    let id: String
    let input: [String: FixtureValue]
    let expected: ExpectedState
}

private struct ExpectedState: Decodable {
    let phase: String
    let canSubmit: Bool
    let nameError: String?
    let emailError: String?
    let phoneError: String?
    let passwordError: String?
    let messageKey: String?
    let route: String?

    enum CodingKeys: String, CodingKey {
        case phase
        case canSubmit = "can_submit"
        case nameError = "name_error"
        case emailError = "email_error"
        case phoneError = "phone_error"
        case passwordError = "password_error"
        case messageKey = "message_key"
        case route
    }
}

private enum FixtureValue: Decodable {
    case text(String)
    case bool(Bool)
    case null

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if container.decodeNil() {
            self = .null
        } else if let value = try? container.decode(Bool.self) {
            self = .bool(value)
        } else {
            self = .text(try container.decode(String.self))
        }
    }

    var authValue: AuthInputValue {
        switch self {
        case let .text(value): .text(value)
        case let .bool(value): .bool(value)
        case .null: .text("")
        }
    }
}
