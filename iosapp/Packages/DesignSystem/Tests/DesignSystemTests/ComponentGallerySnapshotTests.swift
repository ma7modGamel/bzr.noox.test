import SnapshotTesting
import SwiftUI
import UIKit
import XCTest
@testable import DesignSystem

/// Same cases and viewport as Android's `ComponentGallerySnapshotTest` (design/fixtures/gallery/components.json).
///
/// 1. Every case is always exported as a 1× PNG (400×800 px, the same pixel size as Paparazzi)
///    to `GALLERY_EXPORT_DIR` (CI: `reports/batch-1/gallery/ios`) for the comparison page.
/// 2. Once iOS references are committed under `__Snapshots__`, each case is also asserted against them.
///    Before that, the test does not fail just because references are missing (43 §8: the reference is
///    the approved Android snapshot; the iOS–Android difference is computed by tools/gen-gallery-report).
final class ComponentGallerySnapshotTests: XCTestCase {
    private let size = CGSize(width: GalleryFixtures.viewportWidth, height: GalleryFixtures.viewportHeight)
    private let traits = UITraitCollection(traitsFrom: [
        UITraitCollection(displayScale: 1),
        UITraitCollection(layoutDirection: .rightToLeft),
        UITraitCollection(userInterfaceStyle: .light),
        UITraitCollection(preferredContentSizeCategory: .large),
    ])

    func test_gallery_pages_match_shared_fixture() throws {
        let strategy = Snapshotting<UIViewController, UIImage>.image(size: size, traits: traits)
        let exportDirectory = Self.exportDirectory()
        try FileManager.default.createDirectory(at: exportDirectory, withIntermediateDirectories: true)

        for fixture in GalleryFixtures.snapshotCases {
            let name = "gallery-\(fixture.id)"
            let controller = UIHostingController(
                rootView: GalleryPage(page: fixture.page, anchorBottom: fixture.anchorBottom, snapshot: true)
                    .frame(width: size.width, height: size.height)
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

    private static func exportDirectory() -> URL {
        if let path = ProcessInfo.processInfo.environment["GALLERY_EXPORT_DIR"], !path.isEmpty {
            return URL(fileURLWithPath: path, isDirectory: true)
        }
        return URL(fileURLWithPath: #filePath).deletingLastPathComponent().appendingPathComponent("__Exports__", isDirectory: true)
    }

    private static func referenceURL(named name: String) -> URL {
        URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .appendingPathComponent("__Snapshots__/ComponentGallerySnapshotTests/test_gallery_pages_match_shared_fixture.\(name).png")
    }
}
