import Foundation
import XCTest
#if canImport(FoundationNetworking)
    import FoundationNetworking
#endif
@testable import BzrCore

final class ShareAndWaitingTests: XCTestCase {
    private struct VisitTransport: HTTPTransport {
        let failsSharing: Bool
        let waiting: Bool

        func send(_ request: URLRequest) async throws -> HTTPResult {
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer T")
            if request.url?.path == "/api/v1/orders/7/share-links" {
                XCTAssertEqual(request.httpMethod, "POST")
                XCTAssertEqual(request.httpBody, Data("{}".utf8))
                return HTTPResult(
                    data: Data(#"{"data":{"id":12,"url":"https://example.test/v/test-token","expires_at":"2026-10-14T12:00:00+00:00"}}"#.utf8), statusCode: failsSharing ? 500 : 201
                )
            }
            XCTAssertEqual(request.url?.path, "/api/v1/orders/7")
            let status = waiting ? "OPEN" : "CONFIRMED"
            let waitingJSON =
                waiting ? #", "waiting":{"message":"Waiting for assignment","deadline_label":"Technician assignment deadline","deadline_at":"2026-10-13T12:00:00+00:00"}"# : ""
            return HTTPResult(
                data: Data(
                    """
                    {"data":{"id":7,"number":1044,"status":"\(status)","version":2,"display_status":"order.status.customer.\(status)","available_actions":["share_visit","chat"]\(waitingJSON)}}
                    """.utf8), statusCode: 200)
        }
    }

    private func api(failure: Bool = false, waiting: Bool = false) throws -> LiveCustomerAPI {
        LiveCustomerAPI(
            baseURL: try XCTUnwrap(URL(string: "https://example.test/api/v1/")), token: "T", appMode: "CUSTOMER", transport: VisitTransport(failsSharing: failure, waiting: waiting)
        )
    }

    @MainActor func testSharingUsesTheCurrentOrderAndReturnsATemporaryLink() async throws {
        let model = CustomerViewModel(api: try api(), currencyLabel: "EGP")
        await model.loadOrder(id: 7)
        let link = await model.shareVisit()
        XCTAssertEqual(link?.url, "https://example.test/v/test-token")
        XCTAssertEqual(link?.expiresAt, "2026-10-14T12:00:00+00:00")
        XCTAssertEqual(model.state.screen, "SCR-C09")
        XCTAssertFalse(model.state.isBusy)
    }

    @MainActor func testShareFailureKeepsTrackingAvailableAndAllowsRetry() async throws {
        let model = CustomerViewModel(api: try api(failure: true), currencyLabel: "EGP")
        await model.loadOrder(id: 7)
        let result = await model.shareVisit()
        XCTAssertNil(result)
        XCTAssertEqual(model.state.messageKey, "share.error")
        XCTAssertFalse(model.state.isBusy)
        XCTAssertTrue(model.state.visibleActions.contains("share_visit"))
    }

    @MainActor func testAssignmentWaitUsesTheBackendMessageAndDeadline() async throws {
        let model = CustomerViewModel(api: try api(waiting: true), currencyLabel: "EGP")
        await model.loadOrder(id: 7)
        XCTAssertEqual(model.state.screen, "SCR-C36")
        XCTAssertTrue(model.state.waitingInfo.contains("Waiting for assignment"))
        XCTAssertTrue(model.state.waitingInfo.contains("Technician assignment deadline"))
        XCTAssertTrue(model.state.waitingInfo.contains("2026-10-13"))
    }
    private struct ProposalTransport: HTTPTransport {
        func send(_ request: URLRequest) async throws -> HTTPResult {
            if request.url?.path == "/api/v1/orders/7/proposals" {
                return HTTPResult(
                    data: Data(
                        #"{"data":[{"id":9,"type":"MATERIALS","type_label":"Materials","amount":"80.00","current_total":"620.00","projected_total":"700.00","reason":"Replacement part","status":"PENDING","expires_at":"2100-10-13T12:00:00+00:00"}]}"#
                            .utf8), statusCode: 200)
            }
            XCTAssertEqual(request.url?.path, "/api/v1/orders/7")
            return HTTPResult(
                data: Data(
                    #"{"data":{"id":7,"number":1044,"status":"IN_PROGRESS","version":2,"display_status":"order.status.customer.IN_PROGRESS","available_actions":["approve_proposal","reject_proposal"],"amounts":{"labor_total":"500.00"}}}"#
                        .utf8), statusCode: 200)
        }
    }

    @MainActor func testAdditionalCostShowsBackendCurrentTotalIncludingPreviousApprovedMaterials() async throws {
        let api = LiveCustomerAPI(baseURL: try XCTUnwrap(URL(string: "https://example.test/api/v1/")), token: "T", appMode: "CUSTOMER", transport: ProposalTransport())
        let model = CustomerViewModel(api: api, currencyLabel: "EGP")
        await model.loadOrder(id: 7)
        await model.loadPendingProposal()
        XCTAssertEqual(model.state.screen, "SCR-C31")
        XCTAssertEqual(model.state.fieldValues[3], "620")
        XCTAssertEqual(model.state.items[3], "700")
        XCTAssertTrue(model.state.canContinue)
    }

}
