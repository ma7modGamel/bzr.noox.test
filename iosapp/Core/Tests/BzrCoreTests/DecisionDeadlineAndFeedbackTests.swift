import Foundation
import XCTest
#if canImport(FoundationNetworking)
    import FoundationNetworking
#endif
@testable import BzrCore

final class DecisionDeadlineAndFeedbackTests: XCTestCase {
    private struct FeedbackTransport: HTTPTransport {
        func send(_ request: URLRequest) async throws -> HTTPResult {
            XCTAssertEqual(request.url?.path, "/api/v1/provider/orders/7/customer-rating")
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer T")
            let body = try XCTUnwrap(JSONSerialization.jsonObject(with: try XCTUnwrap(request.httpBody)) as? [String: Any])
            XCTAssertEqual(body["stars"] as? Int, 4)
            XCTAssertEqual(body["tags"] as? [String], ["COOPERATIVE", "PUNCTUAL"])
            XCTAssertEqual(body["comment"] as? String, "Clear description")
            return HTTPResult(data: Data(#"{"data":{"id":1,"stars":4}}"#.utf8), statusCode: 201)
        }
    }

    func testRatingSendsStableCodesAndComment() async throws {
        let api = LiveProviderAPI(baseURL: try XCTUnwrap(URL(string: "https://example.test/api/v1/")), token: "T", transport: FeedbackTransport())
        try await api.rateCustomerWithFeedback(orderID: 7, stars: 4, tags: ["COOPERATIVE", "PUNCTUAL"], comment: "Clear description")
    }

    func testRatingKeepsServerOptionsAndOptionalCommentAndEnforcesTheCommentLimit() {
        let input: [String: ProviderInputValue] = [
            "stars": .integer(4), "available_actions": .strings(["rate_customer"]), "tag_labels": .strings(["Server label"]), "tag_codes": .strings(["COOPERATIVE"]),
            "selected_tags": .strings(["0"]), "comment": .text("Good visit"),
        ]
        let state = ProviderLogic.reduce(screen: "SCR-P17", input: input)
        XCTAssertTrue(state.canContinue)
        XCTAssertEqual(state.optionCodes, ["COOPERATIVE"])
        XCTAssertEqual(state.selectedPrimaryIndices, [0])
        XCTAssertEqual(state.fieldValues, ["Good visit"])
        var invalid = input
        invalid["comment"] = .text(String(repeating: "س", count: 501))
        XCTAssertFalse(ProviderLogic.reduce(screen: "SCR-P17", input: invalid).canContinue)
        XCTAssertEqual(ProviderLogic.reduce(screen: "SCR-P17", input: invalid).fieldErrors, ["provider.rating.comment_limit"])
    }

    func testDeadlineStateUsesElapsedTimeWithoutRestartingWhenTheScreenReturns() {
        let state = CustomerLogic.reduce(
            screen: "SCR-C31",
            input: [
                "countdown_seconds": .integer(60), "proposal_loaded_uptime": .integer(100), "proposal_deadline": .text("2026-10-13 · 4:30 PM"),
                "proposal_expiry_message": .text("No cancellation"), "available_actions": .strings(["approve_proposal", "reject_proposal"]),
            ])
        XCTAssertEqual(CustomerLogic.proposalRemaining(state, uptimeSeconds: 110), 50)
        XCTAssertEqual(CustomerLogic.proposalRemaining(state, uptimeSeconds: 170), 0)
        XCTAssertEqual(state.proposalExpiryMessage, "No cancellation")
        XCTAssertEqual(state.proposalDeadline, "2026-10-13 · 4:30 PM")
    }

    private struct ProposalTransport: HTTPTransport {
        func send(_ request: URLRequest) async throws -> HTTPResult {
            let json: String
            if request.url?.path == "/api/v1/orders/7/proposals" {
                json =
                    #"{"data":[{"id":1,"type":"MATERIALS","type_label":"Materials","amount":"120.00","projected_total":"620.00","reason":"Part","status":"PENDING","expires_at":"2020-01-01T12:30:00+00:00","server_time":"2020-01-01T12:00:00+00:00","expiry_message":"No cancellation"}]}"#
            } else {
                json = #"{"data":{"id":7,"status":"IN_PROGRESS","available_actions":["approve_proposal","reject_proposal"],"display_status":"In progress"}}"#
            }
            return HTTPResult(data: Data(json.utf8), statusCode: 200)
        }
    }

    @MainActor func testProposalDeadlineUsesServerTimeEvenWhenTheDeviceClockDiffersByYears() async throws {
        let api = LiveCustomerAPI(baseURL: try XCTUnwrap(URL(string: "https://example.test/api/v1/")), token: "T", appMode: "CUSTOMER", transport: ProposalTransport())
        let model = CustomerViewModel(api: api, currencyLabel: "EGP")
        await model.loadOrder(id: 7)
        await model.loadPendingProposal()
        XCTAssertEqual(model.state.screen, "SCR-C31")
        XCTAssertTrue(model.state.canContinue)
        XCTAssertEqual(model.state.countdownSeconds, 1800)
        XCTAssertEqual(model.state.proposalExpiryMessage, "No cancellation")
    }

    func testProposalDecodesServerClockAndExpiryOutcome() throws {
        let json =
            #"{"id":1,"type":"MATERIALS","type_label":"Materials","amount":"120.00","projected_total":"620.00","reason":"Part","status":"PENDING","expires_at":"2026-10-13T12:30:00+00:00","server_time":"2026-10-13T12:00:00+00:00","expiry_message":"No cancellation"}"#
        let proposal = try JSONDecoder().decode(CustomerProposal.self, from: Data(json.utf8))
        XCTAssertEqual(proposal.serverTime, "2026-10-13T12:00:00+00:00")
        XCTAssertEqual(proposal.expiryMessage, "No cancellation")
    }
}
