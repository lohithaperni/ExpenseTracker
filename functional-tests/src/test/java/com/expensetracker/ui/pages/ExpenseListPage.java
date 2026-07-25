package com.expensetracker.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ExpenseListPage {

    private final Page   page;
    private final String baseUrl;

    public ExpenseListPage(Page page, String baseUrl) {
        this.page    = page;
        this.baseUrl = baseUrl;
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    public void navigate() {
        page.navigate(baseUrl + "/");
    }

    // -------------------------------------------------------------------------
    // Add Expense form
    // -------------------------------------------------------------------------

    public void fillAmount(String amount) {
        page.fill("[name=amount]", amount);
    }

    public void selectCategory(String category) {
        page.selectOption("[name=category]", category);
    }

    public void fillDate(String date) {
        page.fill("[name=expense_date]", date);
    }

    public void fillNote(String note) {
        page.fill("[name=note]", note);
    }

    public void submitAddForm() {
        page.click(".add-form button[type=submit]");
    }

    public void addExpense(String amount, String category, String date, String note) {
        fillAmount(amount);
        selectCategory(category);
        fillDate(date);
        fillNote(note);
        submitAddForm();
    }

    // -------------------------------------------------------------------------
    // Expense list
    // -------------------------------------------------------------------------

    public Locator expenseRows() {
        return page.locator("table tbody tr");
    }

    public Locator emptyState() {
        return page.locator(".empty");
    }

    public int rowCount() {
        return (int) expenseRows().count();
    }

    public boolean listContains(String text) {
        return page.locator("table").textContent().contains(text);
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    public void deleteFirstRow() {
        page.locator("table tbody tr form button[type=submit]").first().click();
    }

    public void deleteRowContaining(String note) {
        page.locator("table tbody tr")
            .filter(new Locator.FilterOptions().setHasText(note))
            .locator("form button[type=submit]")
            .click();
    }

    // -------------------------------------------------------------------------
    // Total
    // -------------------------------------------------------------------------

    public String totalText() {
        return page.locator(".total-amount").textContent().trim();
    }

    // -------------------------------------------------------------------------
    // Date cells
    // -------------------------------------------------------------------------

    public Locator dateCells() {
        return page.locator("table tbody tr td:first-child");
    }
}
