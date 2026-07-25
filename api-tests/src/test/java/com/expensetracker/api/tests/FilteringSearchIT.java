package com.expensetracker.api.tests;

import com.expensetracker.api.client.ExpenseApiClient;
import com.expensetracker.api.support.AppFixture;
import com.expensetracker.api.support.DbSeeder;
import com.expensetracker.api.utils.HtmlTableParser;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Epic EPMCDMETST-55698 — Find & Review Expenses.
 *
 * Confluence design: https://kb.epam.com/pages/viewpage.action?pageId=2891217448
 *
 * Stories:
 *   EPMCDMETST-55699  Filter expenses by date range and category
 *   EPMCDMETST-55700  Search expenses by note text
 *
 * Lifecycle:
 *   @BeforeAll  — start Flask app (port 5001), create Playwright + APIRequestContext.
 *   @BeforeEach — reset DB and seed the 5-row baseline dataset.
 *   @AfterAll   — stop app, close Playwright context.
 */
@Epic("EPMCDMETST-55698 - Find & Review Expenses")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.DisplayName.class)
@Tag("regression")
class FilteringSearchIT {

    private AppFixture        app;
    private DbSeeder          seeder;
    private Playwright        playwright;
    private APIRequestContext apiContext;
    private ExpenseApiClient  client;

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @BeforeAll
    void startSuite() throws Exception {
        app       = new AppFixture(5001);
        app.start();
        seeder    = new DbSeeder(app.dbPath().toAbsolutePath().toString());
        playwright = Playwright.create();
        apiContext = playwright.request().newContext(
                new APIRequest.NewContextOptions().setBaseURL(app.baseUrl()));
        client    = new ExpenseApiClient(apiContext);
    }

    @AfterAll
    void stopSuite() {
        if (apiContext != null) apiContext.dispose();
        if (playwright != null) playwright.close();
        if (app        != null) app.stop();
    }

    @BeforeEach
    void resetAndSeed() throws Exception {
        seeder.seedBaseline();
    }

    // =========================================================================
    // 8.1  Filter by Date Range  —  EPMCDMETST-55699
    // =========================================================================

    @Test
    @Tag("smoke")
    @Feature("Date Range Filter")
    @Story("EPMCDMETST-55699")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Expenses exactly on start_date and end_date are included; outside range are excluded.")
    @DisplayName("TC-DR-001")
    void filterByDateRange_inclusiveBoundaries_returnsOnlyInRange() {
        APIResponse response = client.listByDateRange("2026-02-01", "2026-02-10");
        HtmlTableParser page = new HtmlTableParser(response.text());

        assertThat(response.status()).isEqualTo(200);
        assertThat(page.containsNote("groceries"))
                .as("Feb-01 Food expense must be included (start boundary)")
                .isTrue();
        assertThat(page.containsNote("Internet"))
                .as("Feb-10 Bills expense must be included (end boundary)")
                .isTrue();
        assertThat(page.containsNote("Coffee beans"))
                .as("Feb-10 Food expense must be included (end boundary)")
                .isTrue();
        assertThat(page.containsNote("Train ticket"))
                .as("Jan-15 Travel expense must be excluded (before start)")
                .isFalse();
    }

    @Test
    @Feature("Date Range Filter")
    @Story("EPMCDMETST-55699")
    @Severity(SeverityLevel.NORMAL)
    @Description("Only start_date provided: returns all expenses on or after that date.")
    @DisplayName("TC-DR-002")
    void filterByDateRange_onlyStartDate_returnsOnOrAfterStart() {
        APIResponse response = client.listByDateRange("2026-02-01", null);
        HtmlTableParser page = new HtmlTableParser(response.text());

        assertThat(response.status()).isEqualTo(200);
        assertThat(page.containsNote("groceries")).as("Feb-01 on boundary must appear").isTrue();
        assertThat(page.containsNote("Internet")).as("Feb-10 after start must appear").isTrue();
        assertThat(page.containsNote("Train ticket"))
                .as("Jan-15 before start_date must be excluded").isFalse();
    }

    @Test
    @Feature("Date Range Filter")
    @Story("EPMCDMETST-55699")
    @Severity(SeverityLevel.NORMAL)
    @Description("Only end_date provided: returns all expenses on or before that date.")
    @DisplayName("TC-DR-003")
    void filterByDateRange_onlyEndDate_returnsOnOrBeforeEnd() {
        APIResponse response = client.listByDateRange(null, "2026-01-31");
        HtmlTableParser page = new HtmlTableParser(response.text());

        assertThat(response.status()).isEqualTo(200);
        assertThat(page.containsNote("Train ticket")).as("Jan-15 on/before end must appear").isTrue();
        assertThat(page.containsNote("groceries")).as("Feb-01 after end_date must be excluded").isFalse();
    }

    @Test
    @Tag("negative")
    @Feature("Date Range Filter")
    @Story("EPMCDMETST-55699")
    @Severity(SeverityLevel.NORMAL)
    @Description("start_date after end_date: must not return HTTP 500. Acceptable: 400 or 200 with empty/message.")
    @DisplayName("TC-DR-004")
    void filterByDateRange_startAfterEnd_doesNot500() {
        APIResponse response = client.listByDateRange("2026-02-10", "2026-02-01");

        assertThat(response.status())
                .as("Inverted date range must not cause an internal server error")
                .isNotEqualTo(500);
    }

    @Test
    @Tag("negative")
    @Feature("Date Range Filter")
    @Story("EPMCDMETST-55699")
    @Severity(SeverityLevel.NORMAL)
    @Description("Invalid date format (02-01-2026): must not return HTTP 500; validation feedback expected.")
    @DisplayName("TC-DR-005")
    void filterByDateRange_invalidDateFormat_doesNot500() {
        APIResponse response = client.listExpenses("02-01-2026", null, null, null);

        assertThat(response.status())
                .as("Invalid date format must not cause an internal server error")
                .isNotEqualTo(500);
    }

    @Test
    @Tag("negative")
    @Feature("Date Range Filter")
    @Story("EPMCDMETST-55699")
    @Severity(SeverityLevel.MINOR)
    @Description("Future date range with no matching expenses shows empty state.")
    @DisplayName("TC-DR-006")
    void filterByDateRange_noMatches_showsEmptyState() {
        APIResponse response = client.listByDateRange("2027-01-01", "2027-01-31");
        HtmlTableParser page = new HtmlTableParser(response.text());

        assertThat(response.status()).isEqualTo(200);
        assertThat(page.containsNote("Train ticket"))
                .as("No seeded expense falls in 2027").isFalse();
        assertThat(page.containsNote("groceries"))
                .as("No seeded expense falls in 2027").isFalse();
    }

    // =========================================================================
    // 8.2  Filter by Category  —  EPMCDMETST-55699
    // =========================================================================

    @Test
    @Tag("smoke")
    @Feature("Category Filter")
    @Story("EPMCDMETST-55699")
    @Severity(SeverityLevel.CRITICAL)
    @Description("category=Food returns only Food rows; Travel and Bills are excluded.")
    @DisplayName("TC-CAT-001")
    void filterByCategory_exactMatch_returnsOnlyCategory() {
        APIResponse response = client.listByCategory("Food");
        HtmlTableParser page = new HtmlTableParser(response.text());

        assertThat(response.status()).isEqualTo(200);
        assertThat(page.containsNote("groceries")).as("Food expense must appear").isTrue();
        assertThat(page.containsNote("Coffee beans")).as("Food expense must appear").isTrue();
        assertThat(page.containsNote("Train ticket"))
                .as("Travel expense must be excluded").isFalse();
        assertThat(page.containsNote("Internet"))
                .as("Bills expense must be excluded").isFalse();
    }

    @Test
    @Feature("Category Filter")
    @Story("EPMCDMETST-55699")
    @Severity(SeverityLevel.NORMAL)
    @Description("URL-encoded special characters in category (Health%20%26%20Fitness) decode and match correctly.")
    @DisplayName("TC-CAT-002")
    void filterByCategory_urlEncodedCategory_returnsMatches() throws Exception {
        seeder.insert(45.00, "Health & Fitness", "2026-03-01", "Gym membership");

        APIResponse response = client.listByCategory("Health & Fitness");
        HtmlTableParser page = new HtmlTableParser(response.text());

        assertThat(response.status()).isEqualTo(200);
        assertThat(page.containsNote("Gym membership"))
                .as("URL-encoded category must match the seeded row").isTrue();
    }

    @Test
    @Tag("negative")
    @Feature("Category Filter")
    @Story("EPMCDMETST-55699")
    @Severity(SeverityLevel.MINOR)
    @Description("Non-existent category produces an empty result set.")
    @DisplayName("TC-CAT-003")
    void filterByCategory_nonExistent_showsEmptyState() {
        APIResponse response = client.listByCategory("NonExistent");
        HtmlTableParser page = new HtmlTableParser(response.text());

        assertThat(response.status()).isEqualTo(200);
        assertThat(page.containsNote("Train ticket"))
                .as("No expense should appear for a non-existent category").isFalse();
        assertThat(page.containsNote("groceries"))
                .as("No expense should appear for a non-existent category").isFalse();
    }

    @Test
    @Tag("negative")
    @Feature("Category Filter")
    @Story("EPMCDMETST-55699")
    @Severity(SeverityLevel.MINOR)
    @Description("Lowercase category input must not cause a server error; product defines exact matching behaviour.")
    @DisplayName("TC-CAT-004")
    void filterByCategory_caseSensitivity_isConsistent() {
        APIResponse response = client.listByCategory("food");

        assertThat(response.status())
                .as("Lowercase category must not cause a server error")
                .isNotEqualTo(500);
    }

    // =========================================================================
    // 8.3  Combined Filters  —  EPMCDMETST-55699
    // =========================================================================

    @Test
    @Feature("Combined Filters")
    @Story("EPMCDMETST-55699")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Date range AND category are combined with AND semantics: returns only Food rows within Feb.")
    @DisplayName("TC-COMB-001")
    void combinedFilters_dateRangeAndCategory_returnsIntersection() {
        APIResponse response = client.listExpenses("2026-02-01", "2026-02-28", "Food", null);
        HtmlTableParser page = new HtmlTableParser(response.text());

        assertThat(response.status()).isEqualTo(200);
        assertThat(page.containsNote("groceries")).as("Feb-01 Food must appear").isTrue();
        assertThat(page.containsNote("Coffee beans")).as("Feb-10 Food must appear").isTrue();
        assertThat(page.containsNote("Train ticket"))
                .as("Jan Travel outside range and wrong category").isFalse();
        assertThat(page.containsNote("Internet"))
                .as("Feb Bills matches date range but wrong category").isFalse();
    }

    @Test
    @Feature("Combined Filters")
    @Story("EPMCDMETST-55699")
    @Severity(SeverityLevel.NORMAL)
    @Description("Omitting all query parameters returns the full unfiltered expense list.")
    @DisplayName("TC-COMB-002")
    void clearFilters_byOmittingParams_returnsFullList() {
        // Confirm filtered request returns a subset
        HtmlTableParser filtered = new HtmlTableParser(client.listByCategory("Food").text());
        assertThat(filtered.containsNote("Train ticket"))
                .as("Filtered response must not include Travel").isFalse();

        // Unfiltered request must return all five seeded rows
        APIResponse all = client.listAll();
        HtmlTableParser page = new HtmlTableParser(all.text());

        assertThat(all.status()).isEqualTo(200);
        assertThat(page.containsNote("Train ticket")).as("Unfiltered must include Travel").isTrue();
        assertThat(page.containsNote("Internet")).as("Unfiltered must include Bills").isTrue();
        assertThat(page.containsNote("groceries")).as("Unfiltered must include Feb Food").isTrue();
        assertThat(page.containsNote("Coffee beans")).as("Unfiltered must include Food").isTrue();
    }

    // =========================================================================
    // 8.4  Search by Note Text  —  EPMCDMETST-55700
    // =========================================================================

    @Test
    @Tag("smoke")
    @Feature("Note Search")
    @Story("EPMCDMETST-55700")
    @Severity(SeverityLevel.CRITICAL)
    @Description("q=Coffee matches all notes containing 'Coffee' as a substring.")
    @DisplayName("TC-SRCH-001")
    void searchNote_substringMatch_returnsMatches() {
        APIResponse response = client.listBySearch("Coffee");
        HtmlTableParser page = new HtmlTableParser(response.text());

        assertThat(response.status()).isEqualTo(200);
        assertThat(page.containsNote("Coffee beans"))
                .as("'Coffee beans' contains substring 'Coffee'").isTrue();
        assertThat(page.containsNote("Train ticket"))
                .as("Non-matching note must be excluded").isFalse();
        assertThat(page.containsNote("Internet"))
                .as("Non-matching note must be excluded").isFalse();
    }

    @Test
    @Feature("Note Search")
    @Story("EPMCDMETST-55700")
    @Severity(SeverityLevel.NORMAL)
    @Description("Lowercase q=coffee matches mixed-case notes — search must be case-insensitive.")
    @DisplayName("TC-SRCH-002")
    void searchNote_caseInsensitive_returnsMatches() {
        APIResponse response = client.listBySearch("coffee");
        HtmlTableParser page = new HtmlTableParser(response.text());

        assertThat(response.status()).isEqualTo(200);
        assertThat(page.containsNote("Coffee beans"))
                .as("Lowercase 'coffee' must match note 'Coffee beans'").isTrue();
        assertThat(page.containsNote("Train ticket"))
                .as("Non-matching note must be excluded").isFalse();
    }

    @Test
    @Tag("negative")
    @Feature("Note Search")
    @Story("EPMCDMETST-55700")
    @Severity(SeverityLevel.MINOR)
    @Description("Unmatched search term produces an empty result set.")
    @DisplayName("TC-SRCH-003")
    void searchNote_noMatches_showsEmptyState() {
        APIResponse response = client.listBySearch("unmatchedkeyword12345");
        HtmlTableParser page = new HtmlTableParser(response.text());

        assertThat(response.status()).isEqualTo(200);
        assertThat(page.containsNote("Train ticket"))
                .as("No expense should match an unknown keyword").isFalse();
        assertThat(page.containsNote("Internet"))
                .as("No expense should match an unknown keyword").isFalse();
    }

    @Test
    @Feature("Note Search")
    @Story("EPMCDMETST-55700")
    @Severity(SeverityLevel.NORMAL)
    @Description("Whitespace-padded query (URL-encoded spaces) is trimmed before matching.")
    @DisplayName("TC-SRCH-004")
    void searchNote_trimsWhitespace_returnsMatches() {
        // URL-encoded leading and trailing spaces around "Train"
        APIResponse response = client.getRaw("/?q=+Train+");
        HtmlTableParser page = new HtmlTableParser(response.text());

        assertThat(response.status()).isEqualTo(200);
        assertThat(page.containsNote("Train ticket"))
                .as("Whitespace-padded query must match note 'Train ticket'").isTrue();
    }

    @Test
    @Feature("Note Search")
    @Story("EPMCDMETST-55700")
    @Severity(SeverityLevel.NORMAL)
    @Description("URL-encoded '@' character in search term matches notes containing that character.")
    @DisplayName("TC-SRCH-005")
    void searchNote_specialCharacters_returnsMatches() throws Exception {
        seeder.insert(12.50, "Food", "2026-03-05", "Lunch @ cafe");

        APIResponse response = client.getRaw("/?q=%40"); // '@' URL-encoded
        HtmlTableParser page = new HtmlTableParser(response.text());

        assertThat(response.status()).isEqualTo(200);
        assertThat(page.containsNote("Lunch"))
                .as("URL-encoded '@' must match note 'Lunch @ cafe'").isTrue();
    }

    // =========================================================================
    // 8.5  Search + Filters Combined  —  EPMCDMETST-55699, EPMCDMETST-55700
    // =========================================================================

    @Test
    @Feature("Combined Filters")
    @Story("EPMCDMETST-55700")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Note search is constrained by date range: only matching notes within the range are returned.")
    @DisplayName("TC-SF-001")
    void searchAndFilter_dateRangeConstrainsSearch_returnsMatchesInRange() {
        APIResponse response = client.listExpenses("2026-02-01", "2026-02-28", null, "Coffee");
        HtmlTableParser page = new HtmlTableParser(response.text());

        assertThat(response.status()).isEqualTo(200);
        assertThat(page.containsNote("Coffee beans"))
                .as("Feb 'Coffee beans' matches both query and date range").isTrue();
        assertThat(page.containsNote("Train ticket"))
                .as("Jan Travel must be excluded by date range").isFalse();
    }

    @Test
    @Feature("Combined Filters")
    @Story("EPMCDMETST-55700")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Note search is constrained by category: only matching notes in the given category are returned.")
    @DisplayName("TC-SF-002")
    void searchAndFilter_categoryConstrainsSearch_returnsMatchesInCategory() {
        APIResponse response = client.listExpenses(null, null, "Food", "Coffee");
        HtmlTableParser page = new HtmlTableParser(response.text());

        assertThat(response.status()).isEqualTo(200);
        assertThat(page.containsNote("Coffee beans"))
                .as("'Coffee beans' matches query and belongs to Food").isTrue();
        assertThat(page.containsNote("Train ticket"))
                .as("Travel expense must be excluded by category filter").isFalse();
        assertThat(page.containsNote("Internet"))
                .as("Bills expense must be excluded by category filter").isFalse();
    }

    // =========================================================================
    // 8.6  Robustness / Security  —  EPMCDMETST-55700
    // =========================================================================

    @Test
    @Tag("negative")
    @Feature("Robustness")
    @Story("EPMCDMETST-55700")
    @Severity(SeverityLevel.CRITICAL)
    @Description("SQL-injection-like input is handled as plain text — no server error; no unintended rows returned.")
    @DisplayName("TC-RB-001")
    void robustness_sqlInjectionLikeQuery_doesNotErrorOrOvermatch() {
        // q=' OR 1=1 -- (URL-encoded)
        APIResponse response = client.getRaw("/?q=%27+OR+1%3D1+--");
        HtmlTableParser page = new HtmlTableParser(response.text());

        assertThat(response.status())
                .as("SQL injection-like input must not cause an internal server error")
                .isNotEqualTo(500);
        assertThat(page.containsNote("Train ticket"))
                .as("Injection must not bypass filter and return all rows").isFalse();
    }

    @Test
    @Tag("negative")
    @Feature("Robustness")
    @Story("EPMCDMETST-55700")
    @Severity(SeverityLevel.NORMAL)
    @Description("500-character search term is handled gracefully — no server error.")
    @DisplayName("TC-RB-002")
    void robustness_veryLongQuery_doesNot500() {
        String longQuery = "a".repeat(500);
        APIResponse response = client.listBySearch(longQuery);

        assertThat(response.status())
                .as("500-character query must not cause an internal server error")
                .isNotEqualTo(500);
    }
}
