import Foundation
import XCTest
#if canImport(FoundationNetworking)
    import FoundationNetworking
#endif
@testable import BzrCore

final class TesterFeedbackTests: XCTestCase {
    private actor FeedbackTransport: HTTPTransport {
        var orderRequests = 0
        var deletionRequests = 0
        let deletionFails: Bool
        let providerEntry: Bool
        init(deletionFails: Bool = false, providerEntry: Bool = false) {
            self.deletionFails = deletionFails
            self.providerEntry = providerEntry
        }
        func send(_ request: URLRequest) async throws -> HTTPResult {
            let json: String
            switch request.url?.path {
            case "/api/v1/me":
                json = """
                    {"user":{"name":"Employee","email":"employee@example.test","phone":"01012345678","available_actions":\(providerEntry ? "[\"open_provider_mode\",\"logout\"]" : "[\"logout\"]")}}
                    """
            case "/api/v1/orders/7":
                orderRequests += 1
                let status = orderRequests == 1 ? "OPEN" : "CONFIRMED"
                json = """
                    {"data":{"id":7,"number":1044,"status":"\(status)","version":\(orderRequests),"display_status":"order.status.customer.\(status)","available_actions":["chat"]}}
                    """
            case "/api/v1/addresses" where request.httpMethod == "GET":
                json = "{\"data\":[\(Self.address(9)),\(Self.address(10))]}"
            case "/api/v1/addresses/9" where request.httpMethod == "DELETE":
                deletionRequests += 1
                if deletionFails {
                    return HTTPResult(data: Data(#"{"error":{"code":"INTERNAL_ERROR","message":"Try again"}}"#.utf8), statusCode: 500)
                }
                json = "{\"data\":{\"deleted_id\":9,\"default_address\":\(Self.address(10))}}"
            default: throw APIClientError.invalidResponse
            }
            return HTTPResult(data: Data(json.utf8), statusCode: 200)
        }
        static func address(_ id: Int) -> String {
            """
            {"id":\(id),"label":"Address \(id)","city":{"id":1,"name":"City"},"area":{"id":2,"name":"Area"},"address_text":"Street","lat":"31.43","lng":"31.66","is_default":true}
            """
        }
    }
    @MainActor private func model(_ transport: FeedbackTransport) throws -> CustomerViewModel {
        CustomerViewModel(
            api: LiveCustomerAPI(baseURL: try XCTUnwrap(URL(string: "https://example.test/api/v1/")), token: "T", appMode: "CUSTOMER", transport: transport), currencyLabel: "EGP")
    }
    @MainActor func testFollowReloadsAssignmentAndOpensAssignedOrder() async throws {
        let transport = FeedbackTransport()
        let model = try model(transport)
        await model.loadOrder(id: 7)
        XCTAssertEqual(model.state.screen, "SCR-C36")
        await model.noOffersAction("follow")
        XCTAssertEqual(model.state.screen, "SCR-C09")
        let count = await transport.orderRequests
        XCTAssertEqual(count, 2)
    }
    @MainActor func testProviderEntryUsesBackendPermission() async throws {
        for enabled in [false, true] {
            let model = try model(FeedbackTransport(providerEntry: enabled))
            await model.loadAccountSummary()
            XCTAssertEqual(model.state.visibleActions.contains("open_provider_mode"), enabled)
        }
    }
    @MainActor func testBothAccountScreensRequireLogoutConfirmationAndCancellationKeepsAccount() async throws {
        let model = try model(FeedbackTransport())
        await model.loadAccountSummary()
        XCTAssertFalse(model.confirmLogout())
        model.requestLogoutConfirmation()
        XCTAssertTrue(model.state.showLogoutConfirmation)
        model.dismissLogoutConfirmation()
        XCTAssertFalse(model.confirmLogout())
        XCTAssertEqual(model.state.screen, "SCR-C02")
        await model.loadAccount()
        await model.accountAction("logout")
        XCTAssertTrue(model.state.showLogoutConfirmation)
        XCTAssertTrue(model.confirmLogout())
        XCTAssertFalse(model.confirmLogout())
    }
    @MainActor func testSuccessfulDeletionConsumesServerDefaultWithoutRequiringAnotherListRequest() async throws {
        let transport = FeedbackTransport()
        let model = try model(transport)
        await model.loadAddresses(selectionMode: true)
        model.requestDeleteAddress(index: 0)
        await model.confirmDeleteAddress()
        XCTAssertEqual(model.state.items, ["Address 10"])
        XCTAssertEqual(model.state.selectedIndex, 0)
        XCTAssertFalse(model.state.showConfirmation)
        await model.confirmDeleteAddress()
        let count = await transport.deletionRequests
        XCTAssertEqual(count, 1)
    }
    @MainActor func testDeletionFailureKeepsListAndAllowsRetry() async throws {
        let transport = FeedbackTransport(deletionFails: true)
        let model = try model(transport)
        await model.loadAddresses(selectionMode: false)
        model.requestDeleteAddress(index: 0)
        await model.confirmDeleteAddress()
        XCTAssertEqual(model.state.items, ["Address 9", "Address 10"])
        XCTAssertEqual(model.state.phase, .content)
        XCTAssertEqual(model.state.messageKey, "address.delete.failed")
        XCTAssertFalse(model.state.isBusy)
        model.requestDeleteAddress(index: 0)
        await model.confirmDeleteAddress()
        let count = await transport.deletionRequests
        XCTAssertEqual(count, 2)
    }
}
