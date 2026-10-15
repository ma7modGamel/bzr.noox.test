import XCTest
import Foundation
#if canImport(FoundationNetworking)
    import FoundationNetworking
#endif
@testable import BzrCore

final class RequestWorkflowTests: XCTestCase {
    private struct AvailabilityTransport: HTTPTransport {
        func send(_ request: URLRequest) async throws -> HTTPResult {
            XCTAssertEqual(request.url?.path, "/api/v1/orders/availability")
            XCTAssertEqual(request.url?.query, "customer_address_id=12&category_id=7")
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer T")
            return HTTPResult(data: Data("{\"now_available\":false}".utf8), statusCode: 200)
        }
    }

    func testAvailabilityUsesTheAuthenticatedAddressAndCategory() async throws {
        let api = LiveCustomerAPI(
            baseURL: try XCTUnwrap(URL(string: "https://example.test/api/v1/")), token: "T",
            appMode: "CUSTOMER", transport: AvailabilityTransport())
        let available = try await api.requestAvailability(addressID: 12, categoryID: 7)
        XCTAssertFalse(available)
    }

    private func timing(mode: String = "staff", material: Int = 2, available: Bool = true) -> [String: CustomerInputValue] {
        [
            "operating_mode": .text(mode), "timing_type": .text("now"), "address_id": .text("12"),
            "selected_material_index": .integer(material), "now_available": .bool(available),
            "material_labels": .strings(["From server"]), "material_codes": .strings(["UNSURE"]),
        ]
    }

    func testStaffIncludesMaterialsAndHasFourSteps() {
        let state = CustomerLogic.reduce(screen: "SCR-C04", input: timing())
        XCTAssertEqual(state.requestSteps, 4)
        XCTAssertTrue(state.showMaterials)
        XCTAssertTrue(state.canContinue)
        XCTAssertFalse(CustomerLogic.reduce(screen: "SCR-C04", input: timing(material: -1)).canContinue)
    }

    func testMarketplaceRetainsItsSeparateMaterialStep() {
        let state = CustomerLogic.reduce(screen: "SCR-C04", input: timing(mode: "marketplace", material: -1))
        XCTAssertEqual(state.requestSteps, 5)
        XCTAssertFalse(state.showMaterials)
        XCTAssertTrue(state.canContinue)
    }

    func testUnavailableNowIsDisabledButAScheduledChoiceRemainsUsable() {
        XCTAssertFalse(CustomerLogic.reduce(screen: "SCR-C04", input: timing(available: false)).canContinue)
        var input = timing(available: false)
        input["timing_type"] = .text("scheduled")
        input["slot_id"] = .text("44")
        input["availability_failed"] = .bool(true)
        let state = CustomerLogic.reduce(screen: "SCR-C04", input: input)
        XCTAssertFalse(state.nowAvailable)
        XCTAssertTrue(state.canContinue)
    }

    func testCachedCallActionsAreNeverPresented() {
        let state = CustomerLogic.reduce(
            screen: "SCR-C09",
            input: [
                "status": .text("ARRIVED"), "available_actions": .strings(["call", "chat"]),
            ])
        XCTAssertFalse(state.visibleActions.contains("call"))
        XCTAssertTrue(state.visibleActions.contains("chat"))
    }
}
