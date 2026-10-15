import XCTest
@testable import BzrCore

final class RepairAndPaymentTests: XCTestCase {
    func testInstapayRequiresReceiptAndLocksWhileAdministrationVerifies() {
        var input: [String: CustomerInputValue] = [
            "is_instapay": .bool(true), "selected_channel_index": .integer(0), "transfer_reference": .text("IPN-778812"),
            "available_actions": .strings(["submit_instapay_transfer"]),
        ]
        XCTAssertFalse(CustomerLogic.reduce(screen: "SCR-C22", input: input).canContinue)
        input["receipt_uploaded"] = .bool(true)
        XCTAssertTrue(CustomerLogic.reduce(screen: "SCR-C22", input: input).canContinue)
        input["payment_status"] = .text("PENDING_VERIFICATION")
        let pending = CustomerLogic.reduce(screen: "SCR-C22", input: input)
        XCTAssertFalse(pending.canContinue)
        XCTAssertEqual(pending.messageKey, "payment.instapay.pending")
    }

    func testRepairPhotoCountsArePreservedInProviderState() {
        let state = ProviderLogic.reduce(screen: "SCR-P15", input: ["before_photo_count": .integer(2), "after_photo_count": .integer(1)])
        XCTAssertEqual(state.beforePhotoCount, 2)
        XCTAssertEqual(state.afterPhotoCount, 1)
    }

    func testReviewPhotosAreOptionalAndUploadBlocksSubmitting() {
        var input: [String: CustomerInputValue] = ["ratings": .strings(["5", "5", "5"]), "available_actions": .strings(["rate"])]
        XCTAssertTrue(CustomerLogic.reduce(screen: "SCR-C24", input: input).canContinue)
        input["event"] = .text("uploading")
        XCTAssertFalse(CustomerLogic.reduce(screen: "SCR-C24", input: input).canContinue)
    }

    func testWarrantyFormUsesServerActionAndDoesNotRequireADisputeReason() {
        let input: [String: CustomerInputValue] = [
            "warranty": .bool(true), "description": .text("The same fault returned after repair."), "available_actions": .strings(["request_warranty"]),
        ]
        let state = CustomerLogic.reduce(screen: "SCR-C27", input: input)
        XCTAssertTrue(state.isWarranty)
        XCTAssertTrue(state.canContinue)
        XCTAssertFalse(CustomerLogic.reduce(screen: "SCR-C27", input: input.merging(["available_actions": .strings([])]) { $1 }).canContinue)
    }
}
