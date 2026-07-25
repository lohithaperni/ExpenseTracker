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
@Feature("Add Expense")
@Tag("regression")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddExpenseFunctionalTest {

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
        page     = browser.newPage();
        listPage = new ExpenseListPage(page, app.baseUrl());
        listPage.navigate();
    }

    @AfterEach
    void tearDown() {
        page.close();
    }

    // -------------------------------------------------------------------------
    // TC-ADD-001
    // -------------------------------------------------------------------------

    @Test
    @Tag("smoke")
    @Story("Add expense")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Adding a valid expense submits the form and the new row appears in the list")
    void addExpense_withValidData_appearsInList() {
        listPage.addExpense("25.50", "Food", "2026-01-15", "Lunch");

        assertThat(listPage.expenseRows()).hasCount(1);
        assertThat(listPage.listContains("Lunch")).isTrue();
        assertThat(listPage.listContains("Food")).isTrue();
        assertThat(listPage.listContains("25.50")).isTrue();
    }

    // -------------------------------------------------------------------------
    // TC-ADD-002
    // -------------------------------------------------------------------------

    @Test
    @Tag("smoke")
    @Story("Total spent")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Total updates correctly after adding a new expense")
    void addExpense_totalUpdates_afterAdding() {
        listPage.addExpense("50.00", "Utilities", "2026-03-01", "Rent");

        String total = listPage.totalText();
        assertThat(total).contains("50.00");
    }

    // -------------------------------------------------------------------------
    // TC-ADD-003
    // -------------------------------------------------------------------------

    @Test
    @Story("Add expense")
    @Severity(SeverityLevel.NORMAL)
    @Description("Adding multiple expenses one by one shows all in the list")
    void addExpense_multipleExpenses_allAppearInList() {
        listPage.addExpense("10.00", "Food",      "2026-01-01", "coffee");
        listPage.addExpense("20.00", "Transport", "2026-01-02", "bus");
        listPage.addExpense("30.00", "Utilities",  "2026-01-03", "water");

        assertThat(listPage.expenseRows()).hasCount(3);
        assertThat(listPage.listContains("coffee")).isTrue();
        assertThat(listPage.listContains("bus")).isTrue();
        assertThat(listPage.listContains("water")).isTrue();
    }

    // -------------------------------------------------------------------------
    // TC-ADD-004
    // -------------------------------------------------------------------------

    @Test
    @Tag("negative")
    @Story("Add expense")
    @Severity(SeverityLevel.NORMAL)
    @Description("Submitting without amount does not add a row — HTML5 required validation blocks submission")
    void addExpense_withMissingAmount_doesNotAddRow() {
        listPage.fillDate("2026-01-15");
        listPage.selectCategory("Food");
        listPage.fillNote("No amount");
        listPage.submitAddForm();

        assertThat(listPage.rowCount()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // TC-ADD-005
    // -------------------------------------------------------------------------

    @Test
    @Tag("negative")
    @Story("Add expense")
    @Severity(SeverityLevel.NORMAL)
    @Description("Submitting without a date does not add a row")
    void addExpense_withMissingDate_doesNotAddRow() {
        listPage.fillAmount("20.00");
        listPage.selectCategory("Food");
        listPage.fillNote("No date");
        listPage.submitAddForm();

        assertThat(listPage.rowCount()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // TC-ADD-006
    // -------------------------------------------------------------------------

    @Test
    @Story("Add expense")
    @Severity(SeverityLevel.NORMAL)
    @Description("Each available category can be selected and the expense is added correctly")
    void addExpense_withEachCategory_appearsWithCorrectBadge() {
        String[] categories = {"Food", "Transport", "Utilities", "Shopping", "Entertainment", "Other"};

        for (String cat : categories) {
            seeder.reset();
            listPage.navigate();
            listPage.addExpense("1.00", cat, "2026-01-01", cat + "-test");
            assertThat(listPage.listContains(cat)).isTrue();
        }
    }
}
