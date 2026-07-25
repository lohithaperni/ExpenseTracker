package com.expensetracker.ui.tests;

import com.expensetracker.ui.pages.ExpenseListPage;
import com.expensetracker.ui.support.AppFixture;
import com.expensetracker.ui.support.DbSeeder;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@Epic("Expense Tracker")
@Feature("Delete Expense")
@Tag("regression")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeleteExpenseFunctionalTest {

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
    // TC-DEL-001
    // -------------------------------------------------------------------------

    @Test
    @Tag("smoke")
    @Story("Delete expense")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Clicking Delete removes the row from the list and row count decreases by one")
    void deleteExpense_removesRowFromList() {
        int before = listPage.rowCount();
        listPage.deleteFirstRow();
        assertThat(listPage.rowCount()).isEqualTo(before - 1);
    }

    // -------------------------------------------------------------------------
    // TC-DEL-002
    // -------------------------------------------------------------------------

    @Test
    @Tag("smoke")
    @Story("Delete expense")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Deleting a specific expense removes only that note from the list")
    void deleteExpense_specificRow_removesOnlyThatNote() {
        listPage.deleteRowContaining("coffee");

        assertThat(listPage.listContains("coffee")).isFalse();
        assertThat(listPage.listContains("groceries")).isTrue();
        assertThat(listPage.listContains("uber ride")).isTrue();
    }

    // -------------------------------------------------------------------------
    // TC-DEL-003
    // -------------------------------------------------------------------------

    @Test
    @Tag("smoke")
    @Story("Total spent")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Total decreases by the deleted expense amount (delete coffee $10 → total 170.00)")
    void deleteExpense_updatesTotal() {
        listPage.deleteRowContaining("coffee");

        String total = listPage.totalText();
        assertThat(total).contains("170.00");
    }

    // -------------------------------------------------------------------------
    // TC-DEL-004
    // -------------------------------------------------------------------------

    @Test
    @Story("Delete expense")
    @Severity(SeverityLevel.NORMAL)
    @Description("Deleting the last remaining expense shows the empty-state message")
    void deleteExpense_lastExpense_showsEmptyState() {
        seeder.reset();
        seeder.insert(15.00, "Food", "2026-01-01", "solo");
        listPage.navigate();

        listPage.deleteRowContaining("solo");

        assertThat(listPage.emptyState()).isVisible();
        assertThat(listPage.rowCount()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // TC-DEL-005
    // -------------------------------------------------------------------------

    @Test
    @Story("Delete expense")
    @Severity(SeverityLevel.NORMAL)
    @Description("Deleting all seeded expenses one by one leaves an empty list")
    void deleteExpense_allRows_listBecomesEmpty() {
        while (listPage.rowCount() > 0) {
            listPage.deleteFirstRow();
        }

        assertThat(listPage.emptyState()).isVisible();
        assertThat(listPage.totalText()).contains("0.00");
    }
}
