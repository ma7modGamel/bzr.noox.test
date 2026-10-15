import Foundation
import XCTest
@testable import BzrCore
#if canImport(FoundationNetworking)
    import FoundationNetworking
#endif

final class StaticDataSourcesTests: XCTestCase {
    private actor Requests {
        var values: [URLRequest] = []
        func append(_ request: URLRequest) { values.append(request) }
        func last() -> URLRequest? { values.last }
    }
    private struct OrderEnvelope: Decodable { let data: ProviderOrder }
    private struct Transport: HTTPTransport {
        let requests: Requests
        let response: Data
        func send(_ request: URLRequest) async throws -> HTTPResult {
            await requests.append(request)
            return HTTPResult(data: response, statusCode: 200)
        }
    }

    func testCustomerGuideDecodesSeparateServerAmountsAndNoDataCreatesNoGuide() throws {
        let type = try JSONDecoder().decode(
            CustomerProblemTypeReference.self,
            from: Data(
                #"{"id":1,"name":"صيانة","is_other":false,"customer_price_guide":{"minimum":"125.00","maximum":"325.00","suggested_amounts":["125.00","225.00","325.00"]}}"#.utf8))
        let state = CustomerLogic.reduce(
            screen: "SCR-C38", input: ["operating_mode": .text("marketplace"), "budget_amounts": .strings(type.customerPriceGuide?.suggestedAmounts ?? [])])
        XCTAssertEqual(state.itemDetails, ["125.00", "225.00", "325.00"])
        XCTAssertTrue(CustomerLogic.reduce(screen: "SCR-C38", input: [:]).itemDetails.isEmpty)
    }

    func testOfferAvailabilityIsOptionalAndExplicitlyDecoded() throws {
        let absent = try JSONDecoder().decode(CustomerOfferProvider.self, from: Data(#"{"id":1,"name":"أحمد","is_verified":true}"#.utf8))
        let available = try JSONDecoder().decode(CustomerOfferProvider.self, from: Data(#"{"id":1,"name":"أحمد","is_verified":true,"available_now":true}"#.utf8))
        XCTAssertNil(absent.availableNow)
        XCTAssertEqual(available.availableNow, true)
    }

    func testStructuredReviewSendsCodesSeparatelyFromCommentAndPhotos() async throws {
        let requests = Requests()
        let api = LiveCustomerAPI(
            baseURL: URL(string: "https://example.test/api/v1/")!, token: "T", appMode: "CUSTOMER",
            transport: Transport(requests: requests, response: Data(#"{"data":{"id":1,"average":4.67}}"#.utf8)))
        try await api.submitStructuredReview(orderId: 7, quality: 5, punctuality: 4, conduct: 5, comment: "شكرا", mediaIDs: [11], tags: ["CAREFUL"])
        let last = await requests.last()
        let request = try XCTUnwrap(last)
        let body = try XCTUnwrap(JSONSerialization.jsonObject(with: try XCTUnwrap(request.httpBody)) as? [String: Any])
        XCTAssertEqual(body["tags"] as? [String], ["CAREFUL"])
        XCTAssertEqual(body["comment"] as? String, "شكرا")
        XCTAssertEqual(body["media_ids"] as? [Int], [11])
    }

    func testExecutionPreservesActualServerStagesAndHasNoDefaultEstimate() {
        let state = CustomerLogic.reduce(
            screen: "SCR-C09",
            input: [
                "status": .text("IN_PROGRESS"), "execution_labels": .strings(["وصل", "إصلاح", "انتهى"]), "execution_states": .strings(["done", "active", "pending"]),
                "execution_times": .strings(["10:00", "10:05", ""]),
            ])
        XCTAssertEqual(state.itemDetails, ["وصل", "إصلاح", "انتهى"])
        XCTAssertEqual(state.itemStates, ["done", "active", "pending"])
        XCTAssertEqual(state.options, ["10:00", "10:05", ""])
        XCTAssertEqual(state.fieldValues[11], "")
        XCTAssertFalse(CustomerLogic.reduce(screen: "SCR-C09", input: ["status": .text("CONFIRMED")]).showMap)
        XCTAssertTrue(CustomerLogic.reduce(screen: "SCR-C09", input: ["status": .text("ON_THE_WAY")]).showMap)
    }

    func testFailedReviewKeepsSelectedCodesAndCommentAndAllowsRetry() {
        let state = CustomerLogic.reduce(
            screen: "SCR-C24",
            input: [
                "event": .text("error"), "ratings": .strings(["5", "4", "5"]), "comment": .text("شكرا"), "tags": .strings(["CAREFUL"]), "available_actions": .strings(["rate"]),
            ])
        XCTAssertEqual(state.messageKey, "error.body")
        XCTAssertEqual(state.items, ["CAREFUL"])
        XCTAssertEqual(state.fieldValues.first, "شكرا")
        XCTAssertTrue(state.canContinue)
    }

    func testProviderEstimateUsesVersionedEndpointAndKeepsMinutesInRequest() async throws {
        let recorded = try LiveAPIRecordedTests.recorded("provider/order_in_progress")
        let list = try XCTUnwrap(JSONSerialization.jsonObject(with: recorded) as? [String: Any])
        let rows = try XCTUnwrap(list["data"] as? [[String: Any]])
        let data = try JSONSerialization.data(withJSONObject: ["data": try XCTUnwrap(rows.first)])
        let requests = Requests()
        let api = LiveProviderAPI(baseURL: URL(string: "https://example.test/api/v1/")!, token: "T", transport: Transport(requests: requests, response: data))
        let order = try JSONDecoder().decode(OrderEnvelope.self, from: data).data
        _ = try await api.updateCompletionEstimate(order: order, minutes: 45)
        let last = await requests.last()
        let request = try XCTUnwrap(last)
        XCTAssertEqual(request.url?.path, "/api/v1/provider/orders/\(order.id)/completion-estimate")
        let body = try XCTUnwrap(JSONSerialization.jsonObject(with: try XCTUnwrap(request.httpBody)) as? [String: Any])
        XCTAssertEqual(body["minutes"] as? Int, 45)
        XCTAssertEqual(body["expected_version"] as? Int, order.version)
    }
}
