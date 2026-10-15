import XCTest
@testable import BzrCore

final class WarrantyVisitTests: XCTestCase {
    func testWarrantyVisitDecodesBackendCoverageAndMaterialsChoices() throws {
        let data = Data(
            """
            {"id":8,"number":1043,"version":1,"status":"IN_PROGRESS","display_status":"order.status.provider.IN_PROGRESS","category":{"id":2,"name":"Plumbing"},"problem_type":{"id":3,"name":"Leak"},"location":{},"timing":{"type":"SCHEDULED"},"amounts":{},"available_actions":["submit_proposal"],"warranty_coverage_text":"The visit and labor are free. Materials need customer approval.","proposal_type_options":[{"code":"MATERIALS","label":"Materials"}]}
            """.utf8)
        let order = try JSONDecoder().decode(ProviderOrder.self, from: data)
        XCTAssertEqual(order.proposalTypeOptions?.map(\.code), ["MATERIALS"])
        XCTAssertEqual(order.warrantyCoverage, "The visit and labor are free. Materials need customer approval.")
        let state = ProviderLogic.reduce(screen: "SCR-P15", input: ["warranty_coverage": .text(order.warrantyCoverage ?? "")])
        XCTAssertEqual(state.warrantyCoverage, order.warrantyCoverage)
    }

    func testCustomerWarrantyClaimPreservesVisitIdentityAndFinalState() throws {
        let claim = try JSONDecoder().decode(
            CustomerDispute.self,
            from: Data(
                """
                {"id":3,"reason_code":"WARRANTY","reason_label":"Warranty","description":"The same fault returned.","status":"COMPLETED","status_label":"Warranty visit completed","attachments":[],"visit_order_id":8}
                """.utf8))
        XCTAssertEqual(claim.visitOrderID, 8)
        let state = CustomerLogic.reduce(
            screen: "SCR-C27", input: ["warranty": .bool(true), "warranty_visit_id": .integer(claim.visitOrderID ?? 0), "warranty_visit_final": .bool(true)])
        XCTAssertEqual(state.warrantyVisitID, 8)
        XCTAssertTrue(state.warrantyVisitFinal)
    }
}
