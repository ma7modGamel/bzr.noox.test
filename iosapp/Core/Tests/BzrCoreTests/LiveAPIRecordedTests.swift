import Foundation
import XCTest

@testable import BzrCore

#if canImport(FoundationNetworking)
    import FoundationNetworking
#endif

/// DEC-061 / 9أ stage 3: `LiveCustomerAPI` and `LiveProviderAPI` run their real decode path (APIClient,
/// BzrJSON, envelopes) on the answers recorded from the server for Android
/// (androidapp/app/src/test/resources/api, tests/Feature/Api/Mobile*ResponsesTest.php).
final class LiveAPIRecordedTests: XCTestCase {
    private struct RecordedTransport: HTTPTransport {
        let route: @Sendable (String) -> String

        func send(_ request: URLRequest) async throws -> HTTPResult {
            let url = try XCTUnwrap(request.url)
            let path = url.path.components(separatedBy: "/api/v1/").last ?? ""
            let query = url.query.map { "?\($0)" } ?? ""
            let file = route(path + query)
            return HTTPResult(data: try LiveAPIRecordedTests.recorded(file), statusCode: 200)
        }
    }

    static func recorded(_ file: String) throws -> Data {
        let url = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
            .deletingLastPathComponent().deletingLastPathComponent()
            .appendingPathComponent("androidapp/app/src/test/resources/api/\(file).json")
        return try Data(contentsOf: url)
    }

    private let baseURL = URL(string: "https://example.test/api/v1/")!

    private func customer(_ route: @escaping @Sendable (String) -> String) -> LiveCustomerAPI {
        LiveCustomerAPI(
            baseURL: baseURL, token: "T", appMode: "CUSTOMER", transport: RecordedTransport(route: route))
    }

    func testHomeAndOrdersDecode() async throws {
        let api = customer { path in
            switch path.components(separatedBy: "?").first ?? "" {
            case "config": "customer/config"
            case "orders": path.contains("past") ? "customer/orders_past" : "customer/orders_current"
            case "addresses": "customer/addresses"
            case "catalog": "customer/catalog_city"
            case "me": "auth/me_200_unverified"
            case "orders/1000/tracking": "customer/tracking_on_the_way"
            default: "customer/order_open"
            }
        }
        let home = try await api.home()
        XCTAssertFalse(home.categories.isEmpty)
        let past = try await api.orders(scope: "past", page: 1)
        let order = try await api.order(id: 7)
        let tracking = try await api.tracking(orderId: 1000)
        XCTAssertEqual(past.count, 2)
        XCTAssertEqual(order.number, "1")
        XCTAssertNotNil(tracking.lastLocation)
    }

    func testMarketDecodes() async throws {
        let api = customer { path in
            if path.contains("/offers") { return "customer/offers" }
            if path.hasPrefix("providers/") { return "customer/provider_context" }
            return "customer/proposals_pending"
        }
        let offers = try await api.offers(orderId: 7, sort: "rating")
        let provider = try await api.provider(id: 3, orderId: 7)
        let proposals = try await api.proposals(orderId: 7)
        XCTAssertEqual(offers.count, 2)
        XCTAssertEqual(provider.name, "كريم السباك")
        XCTAssertEqual(proposals.first?.status, "PENDING")
    }

    func testMessagesAndMediaDecode() async throws {
        let api = customer { path in
            if path.hasPrefix("conversations?") { return "customer/conversations" }
            if path == "media" { return "customer/media_uploaded" }
            if path.contains("/messages?") { return "customer/conversation_messages" }
            return "customer/message_sent"
        }
        let conversations = try await api.conversations(page: 1)
        XCTAssertEqual(conversations.first?.order.number, "1")
        let thread = try await api.conversationMessages(id: 1, page: 1)
        let sent = try await api.sendMessage(conversationId: 1, body: "x", mediaIds: [])
        XCTAssertEqual(thread.messages.count, 3)
        XCTAssertTrue(sent.isMine)
        let media = try await api.uploadMedia(
            CustomerMediaUpload(fileName: "a.jpg", mimeType: "image/jpeg", data: Data([1])))
        XCTAssertEqual(media.type, "IMAGE")
    }

    func testProviderEarningsDecode() async throws {
        let api = LiveProviderAPI(
            baseURL: baseURL, token: "T",
            transport: RecordedTransport(route: { _ in "provider/earnings_marketplace_blocked" }))
        let earnings = try await api.earnings()
        XCTAssertTrue(earnings.dues.blocked)
    }

    func testAddressesAndSlotsDecode() async throws {
        let api = customer { path in
            switch path.components(separatedBy: "?").first ?? "" {
            case "cities": "customer/cities"
            case "slots": "customer/slots"
            case "addresses": "customer/addresses"
            default: path.hasSuffix("/areas") ? "customer/areas" : "customer/address_saved"
            }
        }
        let cities = try await api.cities()
        let areas = try await api.areas(cityId: 1)
        let addresses = try await api.addresses()
        let slots = try await api.slots(cityId: 1, date: "2026-10-06")
        let body = CustomerAddressBody(
            label: "البيت", cityId: 1, areaId: 2, addressText: "x", building: nil, floor: nil, apartment: nil,
            landmark: nil, lat: 31.4, lng: 31.6, isDefault: true)
        let saved = try await api.saveAddress(id: 5, body: body)
        XCTAssertEqual(cities.first?.name, "دمياط الجديدة")
        XCTAssertFalse(areas.isEmpty)
        XCTAssertFalse(addresses.isEmpty)
        XCTAssertEqual(slots.count, 6)
        XCTAssertNil(saved.landmark)
    }

    func testPaymentSupportAndAccountDecode() async throws {
        let api = customer { path in
            if path.hasSuffix("/payments") { return "customer/payment_created" }
            if path.hasSuffix("/disputes") { return "customer/disputes" }
            if path.hasPrefix("support/faqs") { return "customer/faqs" }
            if path.hasPrefix("notifications") { return "customer/notifications" }
            if path.hasPrefix("terms") { return "customer/terms" }
            return "customer/me_updated"
        }
        let payment = try await api.createPayment(orderId: 1, channel: "KIOSK", expectedVersion: 1)
        let disputes = try await api.disputes(orderId: 1)
        let faqs = try await api.faqs()
        let notifications = try await api.notifications()
        let account = try await api.account()
        let terms = try await api.terms()
        XCTAssertEqual(payment.channel, "KIOSK")
        XCTAssertEqual(disputes.first?.reasonCode, "WORK_QUALITY")
        XCTAssertFalse(faqs.isEmpty)
        XCTAssertEqual(notifications.count, 2)
        XCTAssertEqual(account.ratingRemindersEnabled, false)
        XCTAssertFalse(terms.body.isEmpty)
    }

    private func provider(_ route: @escaping @Sendable (String) -> String) -> LiveProviderAPI {
        LiveProviderAPI(baseURL: baseURL, token: "T", transport: RecordedTransport(route: route))
    }

    func testProviderApplicationAndHomeDecode() async throws {
        let api = provider { path in
            switch path.components(separatedBy: "?").first ?? "" {
            case "provider/application": "provider/application_rejected"
            case "provider/home": "provider/home_active"
            case "provider/availability": "provider/availability_off"
            case "catalog": "customer/catalog"
            case "cities": "customer/cities"
            case "config": "customer/config"
            case "notifications": "provider/notifications"
            default: "customer/areas"
            }
        }
        let application = try await api.application()
        let home = try await api.home()
        let availability = try await api.setAvailability(false)
        let catalog = try await api.catalog()
        let cities = try await api.cities()
        let areas = try await api.areas(cityID: 1)
        let notifications = try await api.notifications()
        XCTAssertEqual(application.status, "REJECTED")
        XCTAssertNotNil(home.activeOrder)
        XCTAssertFalse(availability.availableNow)
        XCTAssertFalse(catalog.isEmpty)
        XCTAssertFalse(cities.isEmpty)
        XCTAssertFalse(areas.isEmpty)
        XCTAssertFalse(notifications.isEmpty)
    }

    func testProviderOrdersDecode() async throws {
        let api = provider { path in
            if path.contains("scope=past") { return "provider/orders_past" }
            if path.hasSuffix("start-trip") { return "provider/order_on_the_way" }
            if path.hasSuffix("cash-received") { return "provider/order_cash_received" }
            return "provider/orders_current"
        }
        let current = try await api.currentOrders()
        let past = try await api.pastOrders()
        let order = try XCTUnwrap(current.first)
        let onTheWay = try await api.startTrip(order: order, latitude: 30.05, longitude: 31.33)
        let cash = try await api.confirmCash(order: order)
        XCTAssertEqual(order.number, 1)
        XCTAssertFalse(past.isEmpty)
        XCTAssertEqual(onTheWay.status, "ON_THE_WAY")
        XCTAssertEqual(cash.status, "AWAITING_CONFIRMATION")
    }

    func testProviderMarketAndProfileDecode() async throws {
        let api = provider { path in
            switch path.components(separatedBy: "?").first ?? "" {
            case "provider/requests": "provider/requests"
            case "provider/offers": "provider/offers"
            case "provider/profile": "provider/profile"
            case "conversations": "provider/conversations"
            default:
                if path.hasSuffix("/withdraw") { "provider/offer_withdrawn" } else if path.contains("/messages") {
                    "provider/conversation_messages"
                } else { "provider/request_detail" }
            }
        }
        let requests = try await api.availableRequests()
        let request = try await api.availableRequest(orderID: 2)
        let offers = try await api.offers()
        let withdrawn = try await api.withdrawOffer(id: 1)
        let profile = try await api.profile()
        XCTAssertEqual(requests.count, 1)
        XCTAssertEqual(request.offerContext.minimumAmount, "50.00")
        XCTAssertEqual(offers.count, 1)
        XCTAssertEqual(withdrawn.status, "WITHDRAWN")
        XCTAssertEqual(profile.name, "كريم السباك")
    }
}
