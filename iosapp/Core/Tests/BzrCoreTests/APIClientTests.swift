import Foundation
#if canImport(FoundationNetworking)
    import FoundationNetworking
#endif
import XCTest

@testable import BzrCore

final class APIClientTests: XCTestCase {
    func testGetDecodesEnvelopeAndAddsContractHeaders() async throws {
        let json = """
            {"data":{"id":101,"number":"BZR-101","status":"OPEN",\
            "display_status":"order.status.customer.OPEN","available_actions":["cancel"],\
            "deadlines":{"offers_close_at":null,"selection_deadline_at":null,\
            "proposal_expires_at":null,"no_show_allowed_at":null,"auto_close_at":null},\
            "stepper":null}}
            """
        let body = Data(json.utf8)
        let transport = RecordingTransport(result: HTTPResult(data: body, statusCode: 200))
        let client = APIClient(
            baseURL: try XCTUnwrap(URL(string: "https://example.test/api/v1/")),
            token: "test-token",
            appMode: "customer",
            transport: transport
        )

        let envelope: APIEnvelope<OrderPresentation> = try await client.get("orders/101")

        XCTAssertEqual(envelope.data.id, 101)
        XCTAssertEqual(envelope.data.availableActions, [.cancel])
        let request = try XCTUnwrap(transport.request)
        XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer test-token")
        XCTAssertEqual(request.value(forHTTPHeaderField: "X-App-Mode"), "customer")
    }

    func testGetRejectsNonSuccessStatus() async throws {
        let client = APIClient(
            baseURL: try XCTUnwrap(URL(string: "https://example.test/api/v1/")),
            transport: RecordingTransport(result: HTTPResult(data: Data(), statusCode: 410))
        )

        do {
            let _: APIEnvelope<OrderPresentation> = try await client.get("orders/expired")
            XCTFail("Expected an HTTP status error")
        } catch let error as APIClientError {
            XCTAssertEqual(error, .httpStatus(410))
        }
    }

    func testPostAddsIdempotencyKey() async throws {
        let transport = RecordingTransport(result: HTTPResult(data: Data("{\"ok\":true}".utf8), statusCode: 201))
        let client = APIClient(
            baseURL: try XCTUnwrap(URL(string: "https://example.test/api/v1/")),
            token: "test-token", appMode: "CUSTOMER", transport: transport)

        let response: BooleanResponse = try await client.post(
            "orders", body: BooleanResponse(ok: true), idempotencyKey: "request-123")

        XCTAssertTrue(response.ok)
        XCTAssertEqual(transport.request?.value(forHTTPHeaderField: "Idempotency-Key"), "request-123")
    }
}

private struct BooleanResponse: Codable, Sendable {
    let ok: Bool
}

private final class RecordingTransport: HTTPTransport, @unchecked Sendable {
    private let lock = NSLock()
    private let result: HTTPResult
    private var recordedRequest: URLRequest?

    init(result: HTTPResult) {
        self.result = result
    }

    var request: URLRequest? {
        lock.withLock { recordedRequest }
    }

    func send(_ request: URLRequest) async throws -> HTTPResult {
        lock.withLock { recordedRequest = request }
        return result
    }
}
