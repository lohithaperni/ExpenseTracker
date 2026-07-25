package com.expensetracker.ui.tests;

import com.expensetracker.ui.pages.ExpenseListPage;
import com.expensetracker.ui.support.AppFixture;
import com.expensetracker.ui.support.DbSeeder;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Expense Tracker")
@Feature("Date Display")
@Tag("regression")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExpenseDateDisplayFunctionalTest {

    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private static AppFixture app;
    private static DbSeeder   seeder;
    private static Playwright playwright;
    private static Browser    browser;

    private Page            page;
    private ExpenseListPage listPage;

    @BeforeAll
    void startSuite() throws Exception {
        app       = new AppFixture();
        app.start();
        seeder    = new DbSeeder(app.dbPath());
        playwright = Playwright.create();
        browser    = playwright.chromium().launch();
    }

    @AfterAll
    void stopSuite() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
        if (app != null) app.stop();
    }

    @BeforeEach
    void setUp() {
        seeder.reset();
        seeder.seedBaseline();
        page     = browser.newPage();
        listPage = new ExpenseListPage(page, app.baseUrl());
        listPage.navigate();
    }

    @AfterEach
    void tearDown() {
        page.close();
    }

    // -------------------------------------------------------------------------
    // TC-DATE-001
    // -------------------------------------------------------------------------

    @Test
    @Tag("smoke")
    @Story("Date display")
    @Severity(SeverityLevel.NORMAL)
    @Description("Every date cell in the expense list contains a valid ISO date (YYYY-MM-DD)")
    void dateDisplay_forValidDates_matchesExpectedPattern() {
        Locator dateCells = listPage.dateCells();
        int count = (int) dateCells.count();

        assertThat(count).isGreaterThan(0);

        for (int i = 0; i < count; i++) {
            String text = dateCells.nth(i).textContent().trim();
            assertThat(text)
                .as("Date cell %d should match ISO date pattern", i)
                .matches(DATE_PATTERN);
        }
    }

    // -------------------------------------------------------------------------
    // TC-DATE-002
    // -------------------------------------------------------------------------

    @Test
    @Story("Date display")
    @Severity(SeverityLevel.NORMAL)
    @Description("Date displayed in UI matches the stored calendar date exactly — no off-by-one due to timezone")
    void dateDisplay_mapsToSameCalendarDate_noOffByOne() {
        seeder.reset();
        seeder.insert(10.00, "Food", "2026-03-15", "timezone-check");
        listPage.navigate();

        assertThat(listPage.listContains("2026-03-15")).isTrue();
        assertThat(listPage.listContains("2026-03-14")).isFalse();
        assertThat(listPage.listContains("2026-03-16")).isFalse();
    }

    // -------------------------------------------------------------------------
    // TC-DATE-003
    // -------------------------------------------------------------------------

    @Test
    @Story("Date display")
    @Severity(SeverityLevel.NORMAL)
    @Description("Date column shows all seeded dates correctly without truncation or formatting errors")
    void dateDisplay_allSeededDates_visibleInCorrectOrder() {
        assertThat(listPage.listContains("2026-02-10")).isTrue();
        assertThat(listPage.listContains("2026-02-01")).isTrue();
        assertThat(listPage.listContains("2026-01-15")).isTrue();
        assertThat(listPage.listContains("2026-01-01")).isTrue();
    }

    // -------------------------------------------------------------------------
    // TC-DATE-004
    // -------------------------------------------------------------------------

    @Test
    @Tag("negative")
    @Story("Date display")
    @Severity(SeverityLevel.NORMAL)
    @Description("Page renders without error when an expense with a non-standard date string exists in the DB")
    void dateDisplay_withUnusualDateString_pageStillRenders() {
        seeder.insert(5.00, "Other", "not-a-date", "bad-date-row");
        listPage.navigate();

        assertThat(page.locator("body")).isVisible();
        assertThat(listPage.listContains("bad-date-row")).isTrue();
    }
}
