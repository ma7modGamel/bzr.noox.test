import Foundation
import XCTest

@testable import BzrCore

final class SharedFixtureTests: XCTestCase {
    func testDecodesEverySharedOrderCaseAndBindsItsActions() throws {
        let fixture = try loadFixture()

        for testCase in fixture.orders {
            let payload = try JSONEncoder().encode(testCase.payload)
            let order = try BzrJSON.decoder().decode(OrderPresentation.self, from: payload)

            XCTAssertEqual(
                ActionBinding.bind(order.availableActions).map(\.textKey.rawValue),
                testCase.expectedActionTextKeys,
                testCase.caseID
            )
            XCTAssertEqual(
                order.stepper?.first(where: { $0.state == .onHold })?.state.rawValue,
                testCase.expectedStepperState,
                testCase.caseID
            )
        }
    }

    func testFormatsSharedNumberAmountCountdownTimeAndDates() throws {
        let formatting = try loadFixture().formatting

        XCTAssertEqual(BzrFormatter.number(formatting.number.input), formatting.number.expected)
        XCTAssertEqual(
            BzrFormatter.amount(formatting.amount.input, currency: formatting.amount.currency),
            formatting.amount.expected
        )
        XCTAssertEqual(BzrFormatter.countdown(formatting.countdown.inputSeconds), formatting.countdown.expected)

        let parser = ISO8601DateFormatter()
        let time = try XCTUnwrap(parser.date(from: formatting.time.iso8601))
        let timeZone = try XCTUnwrap(TimeZone(identifier: formatting.time.timezone))
        XCTAssertEqual(
            BzrFormatter.time(
                time,
                timeZone: timeZone,
                morning: formatting.time.morning,
                evening: formatting.time.evening
            ),
            formatting.time.expected
        )

        let weekdays = ["الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"]
        let months = [
            "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر",
        ]
        let dateText = DateText(today: "اليوم", tomorrow: "غدًا", weekdays: weekdays, months: months)
        for dateCase in formatting.dates {
            let date = try XCTUnwrap(parser.date(from: dateCase.iso8601))
            let reference = try XCTUnwrap(parser.date(from: dateCase.reference))
            let zone = try XCTUnwrap(TimeZone(identifier: dateCase.timezone))
            XCTAssertEqual(
                BzrFormatter.dateLabel(
                    date,
                    relativeTo: reference,
                    timeZone: zone,
                    text: dateText
                ),
                dateCase.expected
            )
        }
    }

    func testCoreHasNoUserInterfaceFrameworkImports() throws {
        let sourceDirectory = packageRoot().appendingPathComponent("Sources/BzrCore")
        let files = try FileManager.default.contentsOfDirectory(
            at: sourceDirectory,
            includingPropertiesForKeys: nil
        ).filter { $0.pathExtension == "swift" }

        for file in files {
            let source = try String(contentsOf: file, encoding: .utf8)
            XCTAssertFalse(source.contains("import SwiftUI"), file.lastPathComponent)
            XCTAssertFalse(source.contains("import UIKit"), file.lastPathComponent)
        }
    }

    private func loadFixture() throws -> MobileCoreFixture {
        let url = repositoryRoot().appendingPathComponent("design/fixtures/contracts/mobile-core.json")
        return try JSONDecoder().decode(MobileCoreFixture.self, from: Data(contentsOf: url))
    }

    private func packageRoot() -> URL {
        URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
    }

    private func repositoryRoot() -> URL {
        packageRoot().deletingLastPathComponent().deletingLastPathComponent()
    }
}

private struct MobileCoreFixture: Decodable {
    let orders: [OrderCase]
    let formatting: FormattingCases
}

private struct OrderCase: Decodable {
    let caseID: String
    let payload: [String: JSONValue]
    let expectedActionTextKeys: [String]
    let expectedStepperState: String?

    enum CodingKeys: String, CodingKey {
        case caseID = "case_id"
        case payload
        case expectedActionTextKeys = "expected_action_text_keys"
        case expectedStepperState = "expected_stepper_state"
    }
}

private struct FormattingCases: Decodable {
    let number: NumberCase
    let amount: AmountCase
    let countdown: CountdownCase
    let time: TimeCase
    let dates: [DateCase]
}

private struct NumberCase: Decodable {
    let input: Double
    let expected: String
}

private struct AmountCase: Decodable {
    let input: Double
    let currency: String
    let expected: String
}

private struct CountdownCase: Decodable {
    let inputSeconds: Int
    let expected: String

    enum CodingKeys: String, CodingKey {
        case inputSeconds = "input_seconds"
        case expected
    }
}

private struct TimeCase: Decodable {
    let iso8601: String
    let timezone: String
    let morning: String
    let evening: String
    let expected: String
}

private struct DateCase: Decodable {
    let iso8601: String
    let reference: String
    let timezone: String
    let expected: String
}

private enum JSONValue: Codable {
    case string(String)
    case number(Double)
    case object([String: JSONValue])
    case array([JSONValue])
    case bool(Bool)
    case null

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if container.decodeNil() {
            self = .null
        } else if let value = try? container.decode(Bool.self) {
            self = .bool(value)
        } else if let value = try? container.decode(Double.self) {
            self = .number(value)
        } else if let value = try? container.decode(String.self) {
            self = .string(value)
        } else if let value = try? container.decode([String: JSONValue].self) {
            self = .object(value)
        } else {
            self = .array(try container.decode([JSONValue].self))
        }
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        switch self {
        case let .string(value): try container.encode(value)
        case let .number(value): try container.encode(value)
        case let .object(value): try container.encode(value)
        case let .array(value): try container.encode(value)
        case let .bool(value): try container.encode(value)
        case .null: try container.encodeNil()
        }
    }
}
