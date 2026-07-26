package com.expensetracker.ui.tests;

import com.expensetracker.ui.pages.ExpenseListPage;
import com.expensetracker.ui.support.AppFixture;
import com.expensetracker.ui.support.DbSeeder;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.*;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("regression")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExpenseListFunctionalTest {

    private static AppFixture app;
    private static DbSeeder   seeder;
    private static Playwright playwright;
    private static Browser    browser;

    private Page            page;
    private ExpenseListPage listPage;

    @BeforeAll
    void startSuite() throws Exception {
        app      = new AppFixture();
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
    // TC-LIST-001
    // -------------------------------------------------------------------------

    @Test
    @Tag("smoke")
    void viewExpenses_withSeededData_displaysAllRecords() {
        assertThat(listPage.expenseRows()).hasCount(5);
        assertThat(listPage.listContains("coffee")).isTrue();
        assertThat(listPage.listContains("uber ride")).isTrue();
        assertThat(listPage.listContains("groceries")).isTrue();
        assertThat(listPage.listContains("electric")).isTrue();
    }

    // -------------------------------------------------------------------------
    // TC-LIST-002
    // -------------------------------------------------------------------------

    @Test
    void viewExpenses_emptyDatabase_showsEmptyState() {
        seeder.reset();
        listPage.navigate();

        assertThat(listPage.emptyState()).isVisible();
        assertThat(listPage.rowCount()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // TC-LIST-003
    // -------------------------------------------------------------------------

    @Test
    @Tag("smoke")
    void viewExpenses_showsCorrectTotalForAllRecords() {
        String total = listPage.totalText();
        assertThat(total).contains("180.00");
    }

    // -------------------------------------------------------------------------
    // TC-LIST-004
    // -------------------------------------------------------------------------

    @Test
    void viewExpenses_eachRow_showsCategoryBadge() {
        assertThat(listPage.listContains("Food")).isTrue();
        assertThat(listPage.listContains("Transport")).isTrue();
        assertThat(listPage.listContains("Bills")).isTrue();
    }
}
