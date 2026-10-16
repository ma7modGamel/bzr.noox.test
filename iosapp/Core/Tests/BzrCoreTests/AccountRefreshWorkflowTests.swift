import Foundation
import XCTest
#if canImport(FoundationNetworking)
    import FoundationNetworking
#endif
@testable import BzrCore

final class AccountRefreshWorkflowTests: XCTestCase {
    private actor Transport: HTTPTransport {
        var name = "Customer"
        var email = "customer@example.test"
        var photo = false
        var read = false
        var holdPath: String?
        var failNext = false
        var pending = false
        var gate: CheckedContinuation<Void, Never>?
        var started: CheckedContinuation<Void, Never>?
        var requests = 0
        func hold(_ path: String, failure: Bool = false) { holdPath = path; failNext = failure }
        func waitForRequest() async {
            if pending { return }
            await withCheckedContinuation { started = $0 }
        }
        func release() { gate?.resume(); gate = nil }
        func send(_ request: URLRequest) async throws -> HTTPResult {
            requests += 1
            let path = request.url!.path
            if holdPath == path {
                holdPath = nil
                pending = true
                started?.resume(); started = nil
                await withCheckedContinuation { gate = $0 }
                pending = false
                if failNext { failNext = false; throw URLError(.notConnectedToInternet) }
            }
            var json: String
            switch path {
            case "/api/v1/me":
                if request.httpMethod == "POST" {
                    XCTAssertEqual(request.value(forHTTPHeaderField: "X-HTTP-Method-Override"), "PATCH")
                    let body = try XCTUnwrap(JSONSerialization.jsonObject(with: request.httpBody ?? Data()) as? [String: Any])
                    name = body["name"] as? String ?? name
                    email = body["email"] as? String ?? email
                    if body["avatar_media_id"] != nil { photo = true }
                }
                json = """
                    {"user":{"name":"\(name)","email":"\(email)","phone":"01012345678","avatar_url":\(photo ? "\"https://example.test/api/v1/me/avatar?v=2\"" : "null"),"available_actions":["update_account","change_password","logout"],"is_verified":true}}
                    """
            case "/api/v1/me/password": return HTTPResult(data: Data(), statusCode: 204)
            case "/api/v1/media": json = #"{"media":{"id":42,"type":"IMAGE","size_bytes":100}}"#
            case "/api/v1/notifications":
                json = """
                    {"data":[{"id":"7aee11e0-aeed-422c-a159-4412df587100","code":"NTF-09","title":"Arrived","body":"Your technician","read_at":\(read ? "\"2026-10-16T12:00:00Z\"" : "null"),"deep_link":null}],"meta":{"unread_count":\(read ? 0 : 25)}}
                    """
            case "/api/v1/notifications/read": read = true; return HTTPResult(data: Data(), statusCode: 204)
            case "/api/v1/orders/7":
                json =
                    #"{"data":{"id":7,"number":1044,"status":"OPEN","version":1,"display_status":"order.status.customer.OPEN","available_actions":["cancel"],"waiting":{"message":"Admin is assigning a technician","kind":"ASSIGNMENT"}}}"#
            default: throw APIClientError.invalidResponse
            }
            return HTTPResult(data: Data(json.utf8), statusCode: 200)
        }
    }
    @MainActor private func model(_ transport: Transport) throws -> CustomerViewModel {
        CustomerViewModel(
            api: LiveCustomerAPI(baseURL: try XCTUnwrap(URL(string: "https://example.test/api/v1/")), token: "T", appMode: "CUSTOMER", transport: transport), currencyLabel: "EGP")
    }
    @MainActor func testBackFromBothFormsReturnsToPersonalDetailsAndPreservesSavedProfile() async throws {
        let model = try model(Transport())
        await model.loadAccountSummary()
        await model.loadAccount()
        await model.accountAction("update_account")
        model.updateAccountField(index: 0, value: "Unsaved")
        XCTAssertFalse(model.canRefresh)
        await model.refreshCurrent()
        XCTAssertEqual(model.state.fieldValues[0], "Unsaved")
        _ = await model.goBack()
        XCTAssertEqual(model.state.screen, "SCR-C33")
        XCTAssertEqual(model.state.items, ["overview"])
        XCTAssertEqual(model.state.fieldValues[0], "Customer")
        await model.accountAction("change_password")
        model.updateAccountField(index: 0, value: "old-password")
        model.updateAccountField(index: 1, value: "new-password")
        model.updateAccountField(index: 2, value: "new-password")
        await model.accountAction("change_password")
        XCTAssertEqual(model.state.fieldValues[0], "Customer")
        XCTAssertEqual(model.state.fieldValues[1], "customer@example.test")
        _ = await model.goBack()
        XCTAssertEqual(model.state.screen, "SCR-C02")
    }
    @MainActor func testEmailEditingRequiresPasswordAndSavesAllPersonalFields() async throws {
        let model = try model(Transport())
        await model.loadAccount()
        await model.accountAction("update_account")
        model.updateAccountField(index: 1, value: "new@example.test")
        XCTAssertTrue(model.state.emailChanged)
        XCTAssertFalse(model.state.canContinue)
        model.updateAccountField(index: 3, value: "current-password")
        XCTAssertTrue(model.state.canContinue)
        await model.accountAction("update_account")
        XCTAssertEqual(model.state.fieldValues[1], "new@example.test")
        XCTAssertEqual(model.state.messageKey, "account.settings.email_saved")
        XCTAssertEqual(model.state.items, ["overview"])
    }
    @MainActor func testPhotoUploadsAndUsesReturnedPrivateAvatarURL() async throws {
        let model = try model(Transport())
        await model.loadAccount()
        await model.uploadAccountPhoto(CustomerMediaUpload(fileName: "profile.jpg", mimeType: "image/jpeg", data: Data([1, 2])))
        XCTAssertEqual(model.state.accountAvatarURL, "https://example.test/api/v1/me/avatar?v=2")
        XCTAssertEqual(model.state.messageKey, "account.settings.success")
        _ = await model.goBack()
    }
    @MainActor func testReadIsImmediateAndFailureRestoresUnreadThenRetryPersistsIt() async throws {
        let transport = Transport()
        let model = try model(transport)
        await model.loadNotifications()
        await transport.hold("/api/v1/notifications/read", failure: true)
        let reading = Task { await model.openNotification(index: 0) }
        await transport.waitForRequest()
        XCTAssertEqual(model.state.itemStates, ["read"])
        XCTAssertTrue(model.state.isBusy)
        await transport.release()
        await reading.value
        XCTAssertEqual(model.state.itemStates, ["unread"])
        XCTAssertEqual(model.state.refreshMessageKey, "notifications.read_failed")
        await model.openNotification(index: 0)
        XCTAssertEqual(model.state.itemStates, ["read"])
        await model.refreshCurrent()
        XCTAssertEqual(model.state.itemStates, ["read"])
    }
    @MainActor func testRefreshPreservesDataDuringLoadingAndAfterNetworkFailure() async throws {
        let transport = Transport()
        let model = try model(transport)
        await model.loadAccountSummary()
        await transport.hold("/api/v1/me", failure: true)
        let refreshing = Task { await model.refreshCurrent() }
        await transport.waitForRequest()
        XCTAssertEqual(model.state.phase, .content)
        XCTAssertEqual(model.state.fieldValues[0], "Customer")
        XCTAssertTrue(model.state.isRefreshing)
        await model.refreshCurrent()
        await transport.release()
        await refreshing.value
        XCTAssertEqual(model.state.fieldValues[0], "Customer")
        XCTAssertFalse(model.state.isRefreshing)
        XCTAssertEqual(model.state.refreshMessageKey, "refresh.failed")
        await model.refreshCurrent()
        XCTAssertEqual(model.state.refreshMessageKey, "refresh.updated")
    }
    @MainActor func testFollowingUnchangedAssignmentShowsConfirmationAndServerWaitingMessage() async throws {
        let model = try model(Transport())
        await model.loadOrder(id: 7)
        XCTAssertEqual(model.state.screen, "SCR-C36")
        await model.noOffersAction("follow")
        XCTAssertEqual(model.state.screen, "SCR-C36")
        XCTAssertEqual(model.state.refreshMessageKey, "refresh.updated")
        XCTAssertEqual(model.state.waitingInfo, "Admin is assigning a technician")
    }
    @MainActor func testLeavingDuringRefreshCannotReopenTheAbandonedScreen() async throws {
        let transport = Transport()
        let model = try model(transport)
        await model.loadNotifications()
        await model.loadAccountSummary()
        await transport.hold("/api/v1/me")
        let refreshing = Task { await model.refreshCurrent() }
        await transport.waitForRequest()
        _ = await model.goBack()
        XCTAssertEqual(model.state.screen, "SCR-C32")
        await transport.release()
        await refreshing.value
        XCTAssertEqual(model.state.screen, "SCR-C32")
    }

    @MainActor func testProviderNotificationsAlsoUpdateImmediatelyAndKeepDataOnFailedRefresh() async throws {
        let transport = Transport()
        let api = LiveProviderAPI(baseURL: try XCTUnwrap(URL(string: "https://example.test/api/v1/")), token: "T", transport: transport)
        let model = ProviderViewModel(api: api, currencyLabel: "EGP")
        await model.loadNotifications()
        await transport.hold("/api/v1/notifications/read", failure: true)
        let reading = Task { await model.openNotification(index: 0) }
        await transport.waitForRequest()
        XCTAssertEqual(model.notificationsState.itemStates, ["read"])
        await transport.release()
        await reading.value
        XCTAssertEqual(model.notificationsState.itemStates, ["unread"])
        await transport.hold("/api/v1/notifications", failure: true)
        let refreshing = Task { await model.refreshCurrent() }
        await transport.waitForRequest()
        XCTAssertEqual(model.notificationsState.items, ["Arrived"])
        XCTAssertTrue(model.notificationsState.isRefreshing)
        await transport.release()
        await refreshing.value
        XCTAssertEqual(model.notificationsState.items, ["Arrived"])
        XCTAssertEqual(model.notificationsState.refreshMessageKey, "refresh.failed")
    }

    func testUnreadCountComesFromAllPagesMetadata() async throws {
        let api = LiveCustomerAPI(baseURL: try XCTUnwrap(URL(string: "https://example.test/api/v1/")), token: "T", appMode: "CUSTOMER", transport: Transport())
        let count = try await api.unreadNotificationCount()
        XCTAssertEqual(count, 25)
    }
}
