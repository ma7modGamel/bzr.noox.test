import BzrCore
import DesignSystem
import SnapshotTesting
import SwiftUI
import UIKit
import XCTest

@testable import BzrApp

final class CustomerAuthSnapshotTests: XCTestCase {
    private let size = CGSize(width: GalleryFixtures.viewportWidth, height: GalleryFixtures.viewportHeight)
    private let traits = UITraitCollection(traitsFrom: [
        UITraitCollection(displayScale: 1),
        UITraitCollection(layoutDirection: .rightToLeft),
        UITraitCollection(userInterfaceStyle: .light),
        UITraitCollection(preferredContentSizeCategory: .large),
    ])

    func testSharedSnapshotCases() throws {
        let strategy = Snapshotting<UIViewController, UIImage>.image(size: size, traits: traits)
        let exportDirectory = Self.exportDirectory()
        try FileManager.default.createDirectory(at: exportDirectory, withIntermediateDirectories: true)

        for fixture in try Self.fixtures() {
            for testCase in fixture.cases where testCase.snapshot {
                let state = AuthLogic.reduce(screen: fixture.screen, input: testCase.authInput)
                let name = "\(fixture.screen)-\(testCase.id)"
                let controller = UIHostingController(
                    rootView: screen(fixture.screen, state: state).frame(width: size.width, height: size.height)
                )
                controller.overrideUserInterfaceStyle = .light

                let rendered = expectation(description: name)
                strategy.snapshot(controller).run { image in
                    XCTAssertEqual(image.size.width * image.scale, self.size.width, "\(name) must be exported at 1×")
                    try? image.pngData()?.write(to: exportDirectory.appendingPathComponent("\(name).png"))
                    rendered.fulfill()
                }
                wait(for: [rendered], timeout: 30)

                if FileManager.default.fileExists(atPath: Self.referenceURL(named: name).path) {
                    assertSnapshot(of: controller, as: strategy, named: name)
                }
            }
        }
    }

    private func screen(_ screen: String, state: AuthUIState) -> AnyView {
        switch screen {
        case "SCR-C10": AnyView(CustomerLoginScreen(state: state))
        case "SCR-C11": AnyView(CustomerRegisterScreen(state: state))
        case "SCR-C12": AnyView(CustomerEmailVerificationScreen(state: state))
        default: AnyView(CustomerPasswordRecoveryScreen(state: state))
        }
    }

    private static func fixtures() throws -> [AuthSnapshotFixture] {
        try ["SCR-C10", "SCR-C11", "SCR-C12", "SCR-C13"].map { screen in
            let url = repositoryRoot().appendingPathComponent("design/fixtures/\(screen)/cases.json")
            return try JSONDecoder().decode(AuthSnapshotFixture.self, from: Data(contentsOf: url))
        }
    }

    private static func exportDirectory() -> URL {
        if let path = ProcessInfo.processInfo.environment["AUTH_EXPORT_DIR"], !path.isEmpty {
            return URL(fileURLWithPath: path, isDirectory: true)
        }
        return URL(fileURLWithPath: #filePath).deletingLastPathComponent().appendingPathComponent("__Exports__", isDirectory: true)
    }

    private static func referenceURL(named name: String) -> URL {
        URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .appendingPathComponent("__Snapshots__/CustomerAuthSnapshotTests/testSharedSnapshotCases.\(name).png")
    }

    private static func repositoryRoot() -> URL {
        URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
    }
}

private struct AuthSnapshotFixture: Decodable {
    let screen: String
    let cases: [AuthSnapshotCase]
}

private struct AuthSnapshotCase: Decodable {
    let id: String
    let snapshot: Bool
    let rawInput: [String: SnapshotInput]

    var authInput: [String: AuthInputValue] { rawInput.mapValues(\.authValue) }

    enum CodingKeys: String, CodingKey {
        case id
        case snapshot
        case rawInput = "input"
    }
}

private enum SnapshotInput: Decodable {
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
