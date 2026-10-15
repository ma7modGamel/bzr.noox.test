import Foundation
import XCTest
@testable import BzrCore
#if canImport(FoundationNetworking)
    import FoundationNetworking
#endif

final class StaffMediaAndArrivalTests: XCTestCase {
    private actor Requests {
        var request: URLRequest?
        func append(_ value: URLRequest) { request = value }
        func last() -> URLRequest? { request }
    }
    private struct Transport: HTTPTransport {
        let requests: Requests
        let response: Data
        func send(_ request: URLRequest) async throws -> HTTPResult {
            await requests.append(request)
            return HTTPResult(data: response, statusCode: 200)
        }
    }

    func testMessagePhotosDecodePrivateURLsAndStayAssociatedWithTheCorrectMessage() throws {
        let value = try JSONDecoder().decode(CustomerMessageMedia.self, from: Data(#"{"id":7,"type":"IMAGE","content_url":"https://example.test/private/7"}"#.utf8))
        XCTAssertEqual(value.contentURL, "https://example.test/private/7")
        let state = CustomerLogic.reduce(
            screen: "SCR-C19", input: ["message_bodies": .strings(["الأول", "الثاني"]), "image_groups": .strings(["https://example.test/a\nhttps://example.test/b", ""])])
        XCTAssertEqual(state.imageGroups, [["https://example.test/a", "https://example.test/b"], []])
        let provider = ProviderLogic.reduce(screen: "SCR-C19", input: ["message_bodies": .strings(["الأول"]), "image_groups": .strings(["https://example.test/a"])])
        XCTAssertEqual(provider.imageGroups, [["https://example.test/a"]])
    }

    func testEvidenceAndCurrentFaultAreSeparateFieldsAndAbsentPhotosProduceNoExamples() {
        let quote = CustomerLogic.reduce(screen: "SCR-C30", input: ["proposal_photo_url": .text("https://example.test/fault")])
        XCTAssertEqual(quote.proposalPhotoURL, "https://example.test/fault")
        XCTAssertTrue(quote.beforePhotoURLs.isEmpty)
        XCTAssertTrue(quote.afterPhotoURLs.isEmpty)
        let history = CustomerLogic.reduce(
            screen: "SCR-C26", input: ["work_before_urls": .strings(["https://example.test/before"]), "work_after_urls": .strings(["https://example.test/after"])])
        XCTAssertEqual(history.beforePhotoURLs, ["https://example.test/before"])
        XCTAssertEqual(history.afterPhotoURLs, ["https://example.test/after"])
        XCTAssertTrue(CustomerLogic.reduce(screen: "SCR-C26", input: [:]).beforePhotoURLs.isEmpty)
    }

    func testCustomerCodeRequestUsesAuthenticatedEndpointAndExplicitRefresh() async throws {
        let requests = Requests()
        let api = LiveCustomerAPI(
            baseURL: URL(string: "https://example.test/api/v1/")!, token: "T", appMode: "CUSTOMER",
            transport: Transport(requests: requests, response: Data(#"{"data":{"code":"123456","expires_at":"2026-10-15T10:40:00Z","server_time":"2026-10-15T10:30:00Z"}}"#.utf8)))
        let code = try await api.arrivalCode(orderId: 7, refresh: true)
        XCTAssertEqual(code.code, "123456")
        let last = await requests.last()
        let request = try XCTUnwrap(last)
        XCTAssertEqual(request.url?.path, "/api/v1/orders/7/arrival-code")
        XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer T")
        let body = try XCTUnwrap(JSONSerialization.jsonObject(with: try XCTUnwrap(request.httpBody)) as? [String: Any])
        XCTAssertEqual(body["refresh"] as? Bool, true)
    }

    func testTechnicianArrivalSendsCodeWithVersionAndCoordinates() async throws {
        let fixture = try LiveAPIRecordedTests.recorded("provider/order_in_progress")
        let list = try XCTUnwrap(JSONSerialization.jsonObject(with: fixture) as? [String: Any])
        let rows = try XCTUnwrap(list["data"] as? [[String: Any]])
        let data = try JSONSerialization.data(withJSONObject: ["data": try XCTUnwrap(rows.first)])
        struct Envelope: Decodable { let data: ProviderOrder }
        let order = try JSONDecoder().decode(Envelope.self, from: data).data
        let requests = Requests()
        let api = LiveProviderAPI(baseURL: URL(string: "https://example.test/api/v1/")!, token: "T", transport: Transport(requests: requests, response: data))
        _ = try await api.verifiedArrival(order: order, latitude: 30.06, longitude: 31.34, code: "123456")
        let last = await requests.last()
        let request = try XCTUnwrap(last)
        let body = try XCTUnwrap(JSONSerialization.jsonObject(with: try XCTUnwrap(request.httpBody)) as? [String: Any])
        XCTAssertEqual(body["arrival_code"] as? String, "123456")
        XCTAssertEqual(body["expected_version"] as? Int, order.version)
        XCTAssertEqual(body["lat"] as? Double, 30.06)
    }

    func testArrivalAndExceptionPresentationUseBackendFlagsWithoutDefaultCodes() {
        let customer = CustomerLogic.reduce(
            screen: "SCR-C09", input: ["status": .text("ON_THE_WAY"), "arrival_code_required": .bool(true), "arrival_code": .text("123456"), "arrival_code_until": .text("10:40")])
        XCTAssertTrue(customer.arrivalCodeRequired)
        XCTAssertEqual(customer.arrivalCode, "123456")
        let provider = ProviderLogic.reduce(screen: "SCR-P13", input: ["arrival_code_required": .bool(true), "arrival_exception_pending": .bool(true)])
        XCTAssertTrue(provider.arrivalCodeRequired)
        XCTAssertTrue(provider.arrivalExceptionPending)
        XCTAssertTrue(CustomerLogic.reduce(screen: "SCR-C09", input: [:]).arrivalCode.isEmpty)
    }
    func testRelatedPoliciesDisplayServerContentAndEscapeTitles() throws {
        let data = Data(
            #"{"version":2,"body":"<p>الشروط</p>","effective_at":"2026-10-15T10:00:00Z","cancellation_policy":{"title":"الإلغاء","body_html":"<p>بلاغ الغياب لا يلغي الطلب</p>"},"privacy_policy":{"title":"<خصوصية>","body_html":"<p>رسائل فقط</p>"}}"#
                .utf8)
        let terms = try JSONDecoder().decode(CustomerTerms.self, from: data)
        XCTAssertTrue(terms.displayBody.contains("بلاغ الغياب لا يلغي الطلب"))
        XCTAssertTrue(terms.displayBody.contains("رسائل فقط"))
        XCTAssertTrue(terms.displayBody.contains("&lt;خصوصية&gt;"))
    }

}
