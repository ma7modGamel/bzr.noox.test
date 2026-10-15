import Foundation
import XCTest
#if canImport(FoundationNetworking)
    import FoundationNetworking
#endif
@testable import BzrCore

final class CustomerTrustDataTests: XCTestCase {
    private struct TrustTransport: HTTPTransport {
        func send(_ request: URLRequest) async throws -> HTTPResult {
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer T")
            let json: String
            switch request.url?.path {
            case "/api/v1/me":
                json =
                    #"{"user":{"name":"Customer","email":"customer@example.test","phone":"01012345678","is_verified":true,"available_actions":[],"order_statistics":{"completed":24,"active":4},"addresses_count":2}}"#
            case "/api/v1/conversations":
                json = #"{"data":[{"id":8,"order":{"id":7},"provider":{"id":3,"name":"Ahmed","is_verified":true,"available_now":true},"status":"OPEN","unread_count":3}]}"#
            case "/api/v1/conversations/8/messages":
                json =
                    #"{"conversation":{"id":8,"order":{"id":7},"provider":{"id":3,"name":"Ahmed","is_verified":true,"available_now":true},"status":"OPEN","unread_count":0},"data":[{"id":12,"is_mine":false,"body":"Arrived","was_masked":false,"media":[],"read_at":"2026-10-13T12:00:00+00:00"}]}"#
            case "/api/v1/providers/3":
                json =
                    #"{"data":{"id":3,"name":"Ahmed","is_verified":true,"available_now":true,"rating_breakdown":{},"specialties":[],"reviews":[],"available_actions":[],"avatar_url":"https://example.test/api/v1/providers/3/images/avatar?order_id=7","portfolio":[{"id":1,"image_path":"private/photo.jpg","image_url":"https://example.test/api/v1/providers/3/images/1?order_id=7","caption":"Work"}]}}"#
            case "/api/v1/providers/3/images/avatar":
                XCTAssertEqual(request.url?.query, "order_id=7")
                return HTTPResult(data: Data("image bytes".utf8), statusCode: 200)
            default:
                XCTFail("Unexpected request: \(request.url?.absoluteString ?? "")")
                throw APIClientError.invalidResponse
            }
            return HTTPResult(data: Data(json.utf8), statusCode: 200)
        }
    }

    private func api() throws -> LiveCustomerAPI {
        LiveCustomerAPI(baseURL: try XCTUnwrap(URL(string: "https://example.test/api/v1/")), token: "T", appMode: "CUSTOMER", transport: TrustTransport())
    }

    @MainActor func testAccountSummaryUsesAllBackendTotalsWithoutFetchingOnePageOfOrders() async throws {
        let model = CustomerViewModel(api: try api(), currencyLabel: "EGP")
        await model.loadAccountSummary()
        XCTAssertEqual(model.state.screen, "SCR-C02")
        XCTAssertEqual(Array(model.state.fieldValues.suffix(3)), ["24", "2", "4"])
    }

    @MainActor func testConversationUnreadAndAvailabilityReachTheListAndChat() async throws {
        let model = CustomerViewModel(api: try api(), currencyLabel: "EGP")
        await model.loadConversations()
        XCTAssertEqual(model.state.unreadCounts, [3])
        XCTAssertEqual(model.state.providersAvailable, [true])
        await model.openConversation(index: 0)
        XCTAssertTrue(model.state.providerAvailable)
        XCTAssertTrue(model.state.providerVerified)
        XCTAssertEqual(model.state.items, ["Arrived"])
        let response = try await api().conversationMessages(id: 8, page: 1)
        XCTAssertEqual(response.messages.first?.readAt, "2026-10-13T12:00:00+00:00")
    }

    @MainActor func testProfileKeepsTheProtectedAvatarAndPortfolioUrls() async throws {
        let model = CustomerViewModel(api: try api(), currencyLabel: "EGP")
        await model.loadProvider(id: 3, orderId: 7)
        XCTAssertEqual(model.state.providerAvatarURL, "https://example.test/api/v1/providers/3/images/avatar?order_id=7")
        XCTAssertEqual(model.state.portfolioURLs, ["https://example.test/api/v1/providers/3/images/1?order_id=7"])
        let image = await model.loadProfileImage(url: model.state.providerAvatarURL)
        XCTAssertEqual(image, Data("image bytes".utf8))
    }

    func testImageDownloadsRejectForeignOriginsBeforeSendingTheAccountToken() async throws {
        do {
            _ = try await api().profileImage(url: "https://outside.example/image.jpg")
            XCTFail("Foreign image URLs must be refused")
        } catch let error as APIClientError { XCTAssertEqual(error, .invalidResponse) }
    }
}
