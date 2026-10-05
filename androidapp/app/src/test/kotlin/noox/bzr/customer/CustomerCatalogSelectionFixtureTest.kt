package noox.bzr.customer

import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CustomerCatalogSelectionFixtureTest {
    @Test
    fun exactBackendCatalogIdsMatchSharedCases() {
        val fixturePath = "design/fixtures/request-market/catalog-selection.json"
        val cases = JSONObject(repositoryFile(fixturePath).readText()).getJSONArray("cases")

        repeat(cases.length()) { index ->
            val testCase = cases.getJSONObject(index)
            val problemIdsByCategory = testCase.getJSONArray("problem_ids_by_category").let { rows ->
                (0 until rows.length()).map { rowIndex ->
                    rows.getJSONArray(rowIndex).let { row ->
                        (0 until row.length()).map(row::getInt)
                    }
                }
            }
            val currentProblemId = testCase.optInt("current_problem_id").takeUnless {
                testCase.isNull("current_problem_id")
            }
            val choice = when (testCase.getString("operation")) {
                "category" -> CustomerCatalogSelection.category(
                    categoryIds = testCase.getJSONArray("category_ids").let { values ->
                        (0 until values.length()).map(values::getInt)
                    },
                    problemIdsByCategory = problemIdsByCategory,
                    selectedIndex = testCase.getInt("selected_index"),
                    currentProblemId = currentProblemId,
                )
                "problem" -> {
                    val categoryIds = testCase.getJSONArray("category_ids")
                    val categoryId = testCase.getInt("current_category_id")
                    val categoryIndex = (0 until categoryIds.length()).first { categoryIds.getInt(it) == categoryId }
                    CustomerCatalogSelection.problem(
                        categoryId = categoryId,
                        problemIds = problemIdsByCategory[categoryIndex],
                        selectedIndex = testCase.getInt("selected_index"),
                    )
                }
                else -> error("Unsupported operation")
            }

            val label = testCase.getString("id")
            assertNotNull(label, choice)
            assertEquals(label, testCase.getInt("expected_category_id"), choice?.categoryId)
            assertEquals(
                label,
                testCase.optInt("expected_problem_id").takeUnless { testCase.isNull("expected_problem_id") },
                choice?.problemTypeId,
            )
        }
    }

    private fun repositoryFile(path: String): File {
        var directory = File(checkNotNull(System.getProperty("user.dir")))
        while (!File(directory, "design").isDirectory) {
            directory = directory.parentFile ?: error("Repository root not found")
        }
        return File(directory, path)
    }
}
