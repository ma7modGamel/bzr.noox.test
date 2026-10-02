import Foundation
import XCTest

@testable import BzrCore

final class CustomerCatalogSelectionFixtureTests: XCTestCase {
    func testExactBackendCatalogIdsMatchSharedCases() throws {
        let fixturePath = "design/fixtures/request-market/catalog-selection.json"
        let fixture = try JSONDecoder().decode(
            CatalogSelectionFixture.self,
            from: Data(contentsOf: repositoryRoot().appendingPathComponent(fixturePath)))

        for testCase in fixture.cases {
            let choice: CustomerCatalogChoice?
            switch testCase.operation {
            case "category":
                choice = CustomerCatalogSelection.category(
                    categoryIds: testCase.categoryIds,
                    problemIdsByCategory: testCase.problemIdsByCategory,
                    selectedIndex: testCase.selectedIndex,
                    currentProblemId: testCase.currentProblemId)
            case "problem":
                let categoryIndex = try XCTUnwrap(
                    testCase.categoryIds.firstIndex(of: testCase.currentCategoryId))
                choice = CustomerCatalogSelection.problem(
                    categoryId: testCase.currentCategoryId,
                    problemIds: testCase.problemIdsByCategory[categoryIndex],
                    selectedIndex: testCase.selectedIndex)
            default:
                XCTFail("Unsupported operation: \(testCase.operation)")
                continue
            }

            XCTAssertEqual(choice?.categoryId, testCase.expectedCategoryId, testCase.id)
            XCTAssertEqual(choice?.problemTypeId, testCase.expectedProblemId, testCase.id)
        }
    }

    private func repositoryRoot() -> URL {
        URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
            .deletingLastPathComponent().deletingLastPathComponent()
    }
}

private struct CatalogSelectionFixture: Decodable {
    let cases: [CatalogSelectionCase]
}

private struct CatalogSelectionCase: Decodable {
    let id: String
    let operation: String
    let categoryIds: [Int]
    let problemIdsByCategory: [[Int]]
    let selectedIndex: Int
    let currentCategoryId: Int
    let currentProblemId: Int?
    let expectedCategoryId: Int
    let expectedProblemId: Int?

    enum CodingKeys: String, CodingKey {
        case id, operation
        case categoryIds = "category_ids"
        case problemIdsByCategory = "problem_ids_by_category"
        case selectedIndex = "selected_index"
        case currentCategoryId = "current_category_id"
        case currentProblemId = "current_problem_id"
        case expectedCategoryId = "expected_category_id"
        case expectedProblemId = "expected_problem_id"
    }
}
