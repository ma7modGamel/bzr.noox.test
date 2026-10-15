import Foundation
import XCTest
#if canImport(FoundationNetworking)
    import FoundationNetworking
#endif
@testable import BzrCore

final class EarningsStatementTests: XCTestCase {
    private actor StatementTransport: HTTPTransport {
        var requests: [URLRequest] = []
        func send(_ request: URLRequest) async throws -> HTTPResult {
            requests.append(request)
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer T")
            XCTAssertEqual(request.value(forHTTPHeaderField: "X-App-Mode"), "PROVIDER")
            if request.url?.path.hasSuffix("/export") == true {
                return HTTPResult(data: Data("\u{FEFF}ID,Amount\norder-1,-100.00\n".utf8), statusCode: 200)
            }
            let query = URLComponents(url: try XCTUnwrap(request.url), resolvingAgainstBaseURL: false)?.queryItems ?? []
            if query.contains(where: { $0.name == "date_from" && $0.value == "bad" }) {
                return HTTPResult(data: Data(#"{"error":{"message":"Invalid date"}}"#.utf8), statusCode: 422)
            }
            let page = query.first { $0.name == "page" }?.value ?? "1"
            let json = """
                {"data":{"operating_mode":"EMPLOYEE","summary":{"balance":"-350.00","available":"-350.00","pending":"0.00"},"transactions":[{"id":"order-\(page)","type":"ORDER","title":"Order \(page)","detail":"Cash due"}],"filter_options":[{"code":"ALL","label":"All from server"},{"code":"ORDER","label":"Orders from server"}],"meta":{"current_page":\(page),"last_page":2}}}
                """
            return HTTPResult(data: Data(json.utf8), statusCode: 200)
        }
    }

    @MainActor private func model(_ transport: StatementTransport) throws -> ProviderViewModel {
        ProviderViewModel(api: LiveProviderAPI(baseURL: try XCTUnwrap(URL(string: "https://example.test/api/v1/")), token: "T", transport: transport), currencyLabel: "EGP")
    }

    @MainActor func testFilteredExportUsesAppliedServerOptionsAndPreservesTheAccountBalance() async throws {
        let transport = StatementTransport()
        let model = try model(transport)
        await model.loadEarnings()
        XCTAssertEqual(model.state.earningsFilterLabels, ["All from server", "Orders from server"])
        model.updateEarningsFilters(typeIndex: 1, dateFrom: "2026-10-01", dateTo: "2026-10-13")
        XCTAssertTrue(model.state.earningsFiltersDirty)
        let blocked = await model.exportEarnings()
        XCTAssertNil(blocked)
        await model.applyEarningsFilters()
        XCTAssertFalse(model.state.earningsFiltersDirty)
        XCTAssertEqual(model.state.secondaryOptions.first, "-350 EGP")
        let csv = await model.exportEarnings()
        XCTAssertEqual(csv, Data("\u{FEFF}ID,Amount\norder-1,-100.00\n".utf8))
        let requests = await transport.requests
        let export = try XCTUnwrap(requests.last?.url)
        XCTAssertEqual(export.path, "/api/v1/provider/earnings/export")
        let query = URLComponents(url: export, resolvingAgainstBaseURL: false)?.queryItems ?? []
        XCTAssertEqual(query.first { $0.name == "type" }?.value, "ORDER")
        XCTAssertEqual(query.first { $0.name == "date_from" }?.value, "2026-10-01")
        XCTAssertEqual(query.first { $0.name == "date_to" }?.value, "2026-10-13")
    }

    @MainActor func testMoreAppendsTheNextPageAndStopsAtTheLastPage() async throws {
        let transport = StatementTransport()
        let model = try model(transport)
        await model.loadEarnings()
        XCTAssertTrue(model.state.earningsHasMore)
        await model.applyEarningsFilters(more: true)
        XCTAssertEqual(model.state.items, ["Order 1", "Order 2"])
        XCTAssertFalse(model.state.earningsHasMore)
        await model.applyEarningsFilters(more: true)
        let requests = await transport.requests
        XCTAssertEqual(requests.count, 2)
    }

    @MainActor func testInvalidFilterKeepsTheCurrentTransactionsAndCanBeReset() async throws {
        let transport = StatementTransport()
        let model = try model(transport)
        await model.loadEarnings()
        model.updateEarningsFilters(dateFrom: "bad")
        await model.applyEarningsFilters()
        XCTAssertEqual(model.state.items, ["Order 1"])
        XCTAssertEqual(model.state.messageKey, "provider.earnings.filter_error")
        XCTAssertTrue(model.state.earningsEmployee)
        XCTAssertTrue(model.state.earningsFiltersDirty)
        await model.applyEarningsFilters(reset: true)
        XCTAssertEqual(model.state.earningsDateFrom, "")
        XCTAssertFalse(model.state.earningsFiltersDirty)
    }
    @MainActor func testReloadDiscardsUnappliedEditsAndKeepsTheAppliedFilter() async throws {
        let transport = StatementTransport()
        let model = try model(transport)
        await model.loadEarnings()
        model.updateEarningsFilters(typeIndex: 1, dateFrom: "2026-10-01")
        await model.applyEarningsFilters()
        model.updateEarningsFilters(dateFrom: "bad")
        await model.loadEarnings()
        XCTAssertEqual(model.state.earningsDateFrom, "2026-10-01")
        XCTAssertFalse(model.state.earningsFiltersDirty)
        let requests = await transport.requests
        let url = try XCTUnwrap(requests.last?.url)
        XCTAssertEqual(URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems?.first { $0.name == "date_from" }?.value, "2026-10-01")
    }

}
