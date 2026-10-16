import Foundation
import XCTest
#if canImport(FoundationNetworking)
    import FoundationNetworking
#endif
@testable import BzrCore

final class AddressBookTests: XCTestCase {
    private actor AddressTransport: HTTPTransport {
        var listRequests = 0
        var changedPrimary = false
        var deleted = false
        func send(_ request: URLRequest) async throws -> HTTPResult {
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer T")
            let path = request.url!.path
            let json: String
            switch path {
            case "/api/v1/addresses":
                listRequests += 1
                json = "{\"data\":\(Self.addresses(primary: changedPrimary ? 1 : 3))}"
            case "/api/v1/addresses/1/default":
                XCTAssertEqual(request.httpMethod, "POST")
                XCTAssertNil(request.value(forHTTPHeaderField: "X-HTTP-Method-Override"))
                changedPrimary = true
                json = "{\"data\":\(Self.address(1, primary: true)),\"addresses\":\(Self.addresses(primary: 1))}"
            case "/api/v1/addresses/1":
                guard request.httpMethod == "POST", request.value(forHTTPHeaderField: "X-HTTP-Method-Override") == "DELETE" else {
                    return HTTPResult(data: Data("<html>403 Forbidden by hosting method policy</html>".utf8), statusCode: 403)
                }
                deleted = true
                json = "{\"data\":{\"deleted_id\":1,\"default_address\":\(Self.address(3, primary: true))}}"
            default: throw APIClientError.invalidResponse
            }
            return HTTPResult(data: Data(json.utf8), statusCode: 200)
        }
        static func addresses(primary: Int) -> String { "[" + (1...5).map { address($0, primary: $0 == primary) }.joined(separator: ",") + "]" }
        static func address(_ id: Int, primary: Bool) -> String {
            let actions =
                primary
                ? #"[{"code":"edit","label":"Edit"},{"code":"delete","label":"Delete"}]"#
                : #"[{"code":"edit","label":"Edit"},{"code":"set_default","label":"Primary from server"},{"code":"delete","label":"Delete"}]"#
            return """
                {"id":\(id),"label":"Address \(id)","city":{"id":1,"name":"City"},"area":{"id":2,"name":"Area"},"address_text":"Street","lat":"31.43","lng":"31.66","is_default":\(primary),"actions":\(actions)}
                """
        }
    }
    @MainActor private func model(_ transport: AddressTransport) throws -> CustomerViewModel {
        CustomerViewModel(
            api: LiveCustomerAPI(baseURL: try XCTUnwrap(URL(string: "https://example.test/api/v1/")), token: "T", appMode: "CUSTOMER", transport: transport), currencyLabel: "EGP")
    }
    @MainActor func testPrimaryAndActionsComeFromServerRatherThanCurrentSelection() async throws {
        let model = try model(AddressTransport())
        await model.loadAddresses()
        XCTAssertEqual(model.state.items.count, 5)
        XCTAssertEqual(model.state.primaryAddressIndex, 2)
        XCTAssertEqual(model.state.optionCodes, ["1", "2", "3", "4", "5"])
        XCTAssertEqual(model.state.addressActions[0][1].label, "Primary from server")
        XCTAssertFalse(model.state.addressActions[2].contains { $0.code == "set_default" })
    }
    @MainActor func testPrimaryChangeUsesReturnedListWithoutAnotherFetch() async throws {
        let transport = AddressTransport()
        let model = try model(transport)
        await model.loadAddresses()
        await model.makeAddressDefault(index: 0)
        XCTAssertEqual(model.state.primaryAddressIndex, 0)
        XCTAssertEqual(model.state.messageKey, "address.default.saved")
        XCTAssertFalse(model.state.isBusy)
        XCTAssertTrue(model.state.addressActions[2].contains { $0.code == "set_default" })
        let requests = await transport.listRequests
        XCTAssertEqual(requests, 1)
    }
    @MainActor func testDeleteReachesLaravelThroughAllowedPostAndKeepsServerPrimary() async throws {
        let transport = AddressTransport()
        let model = try model(transport)
        await model.loadAddresses()
        model.requestDeleteAddress(index: 0)
        await model.confirmDeleteAddress()
        XCTAssertEqual(model.state.items.count, 4)
        XCTAssertEqual(model.state.primaryAddressIndex, 1)
        XCTAssertEqual(model.state.messageKey, "address.deleted")
        XCTAssertFalse(model.state.showConfirmation)
        let deleted = await transport.deleted
        XCTAssertTrue(deleted)
    }
    @MainActor func testBackClosesConfirmationWithoutLeavingAddressBook() async throws {
        let model = try model(AddressTransport())
        await model.loadAddresses()
        model.requestDeleteAddress(index: 2)
        let handled = await model.goBack()
        XCTAssertTrue(handled)
        XCTAssertEqual(model.state.screen, "SCR-C14")
        XCTAssertFalse(model.state.showConfirmation)
    }
}
