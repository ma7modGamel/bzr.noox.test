import BzrCore
import DesignSystem
import SwiftUI
import UIKit
import XCTest

@testable import BzrApp

final class LegalLocalizationTests: XCTestCase {
    @MainActor
    func testLegalHTMLRetainsStructureAndDirection() {
        for (direction, html, heading) in [
            (LayoutDirection.leftToRight, "<h2>Terms</h2><p><strong>Document text</strong></p><ol><li>First</li><li>Second</li></ol>", "Terms"),
            (LayoutDirection.rightToLeft, "<h2>الشروط</h2><p><strong>نص الوثيقة</strong></p><ol><li>الأول</li><li>الثاني</li></ol>", "الشروط"),
        ] {
            let state = CustomerLogic.reduce(
                screen: "SCR-C34",
                input: [
                    "event": .text("loaded"), "body": .text(html),
                    "version": .text("1"), "effective_at": .text("2026-10-11"),
                ])
            let controller = UIHostingController(rootView: C34TermsView(state: state).environment(\.layoutDirection, direction))
            controller.loadViewIfNeeded()
            controller.view.frame = CGRect(origin: .zero, size: CGSize(width: GalleryFixtures.viewportWidth, height: GalleryFixtures.viewportHeight))
            controller.view.layoutIfNeeded()
            let text = descendants(controller.view).compactMap { $0 as? UITextView }.first
            XCTAssertNotNil(text)
            XCTAssertTrue(text?.text.contains(heading) == true)
            XCTAssertFalse(text?.text.contains("<h2>") == true)
            XCTAssertFalse(text?.isEditable ?? true)
            XCTAssertEqual(text?.semanticContentAttribute, direction == .rightToLeft ? .forceRightToLeft : .forceLeftToRight)
        }
    }

    func testNativePluralCatalogForTheApplicationLanguage() {
        let arabic = AppLanguage.current == "ar"
        XCTAssertEqual(bzrPlural("offers.available", count: 1), arabic ? "عرض واحد متاح" : "1 offer available")
        XCTAssertEqual(bzrPlural("offers.available", count: 2), arabic ? "عرضان متاحان" : "2 offers available")
        XCTAssertEqual(bzrPlural("offers.available", count: 3), arabic ? "3 عروض متاحة" : "3 offers available")
    }

    private func descendants(_ view: UIView) -> [UIView] {
        [view] + view.subviews.flatMap(descendants)
    }
}
