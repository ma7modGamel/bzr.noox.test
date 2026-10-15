import Foundation
import XCTest
@testable import BzrCore

final class MediaDurationTests: XCTestCase {
    func testVideoAtThirtySecondsPassesAndAnExcessFractionIsRejected() {
        XCTAssertNil(CustomerMediaLimits.videoError(durationSeconds: 30))
        XCTAssertEqual(CustomerMediaLimits.videoError(durationSeconds: 30.001), "media.validation.video_duration")
        XCTAssertEqual(CustomerMediaLimits.videoError(durationSeconds: 60), "media.validation.video_duration")
    }

    func testUnreadableOrNonFiniteVideoDurationIsRejected() {
        XCTAssertEqual(CustomerMediaLimits.videoError(durationSeconds: nil), "media.validation.video_unreadable")
        XCTAssertEqual(CustomerMediaLimits.videoError(durationSeconds: .nan), "media.validation.video_unreadable")
        XCTAssertEqual(CustomerMediaLimits.videoError(durationSeconds: .infinity), "media.validation.video_unreadable")
    }

    @MainActor func testRejectedSelectionIsExplainedWithoutAddingMediaOrBlockingTheOptionalStep() throws {
        let model = CustomerViewModel(
            api: LiveCustomerAPI(baseURL: try XCTUnwrap(URL(string: "https://example.test/api/v1/")), token: "T", appMode: "CUSTOMER"), currencyLabel: "EGP")
        model.openMedia()
        model.rejectMedia(reason: "media.validation.video_duration")
        XCTAssertEqual(model.state.fieldErrors, ["media.validation.video_duration"])
        XCTAssertTrue(model.state.items.isEmpty)
        XCTAssertTrue(model.state.canContinue)
        model.completeMediaSelection()
        XCTAssertEqual(model.state.screen, "SCR-C37")
    }
}
