package com.expensetracker.api.client;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;

/**
 * Thin wrapper around Playwright's APIRequestContext for the expense list endpoint.
 *
 * Centralises:
 *  - The list endpoint path (update LIST_PATH when implementation is confirmed).
 *  - The Accept header (text/html — app renders HTML, not JSON).
 *  - Query parameter names assumed in the design doc (start_date, end_date, category, q).
 */
public class ExpenseApiClient {

    /** Update this constant if the implementation uses /expenses instead of /. */
    public static final String LIST_PATH = "/";

    private final APIRequestContext context;

    public ExpenseApiClient(APIRequestContext context) {
        this.context = context;
    }

    // -------------------------------------------------------------------------
    // Composite filter methods
    // -------------------------------------------------------------------------

    /** GET list with no filters — returns all expenses. */
    public APIResponse listAll() {
        return context.get(LIST_PATH, baseOptions());
    }

    /**
     * GET list with any combination of the four supported query parameters.
     * Passing {@code null} for a parameter omits it from the request.
     */
    public APIResponse listExpenses(String startDate, String endDate, String category, String q) {
        RequestOptions opts = baseOptions();
        if (startDate != null) opts.setQueryParam("start_date", startDate);
        if (endDate   != null) opts.setQueryParam("end_date",   endDate);
        if (category  != null) opts.setQueryParam("category",   category);
        if (q         != null) opts.setQueryParam("q",          q);
        return context.get(LIST_PATH, opts);
    }

    // -------------------------------------------------------------------------
    // Single-axis convenience methods
    // -------------------------------------------------------------------------

    public APIResponse listByDateRange(String startDate, String endDate) {
        return listExpenses(startDate, endDate, null, null);
    }

    public APIResponse listByCategory(String category) {
        return listExpenses(null, null, category, null);
    }

    public APIResponse listBySearch(String q) {
        return listExpenses(null, null, null, q);
    }

    // -------------------------------------------------------------------------
    // Raw access — for tests that need to craft custom query strings
    // -------------------------------------------------------------------------

    /** GET the list path with a pre-built raw query string, e.g. "?q=%27+OR+1%3D1+--". */
    public APIResponse getRaw(String pathWithQuery) {
        return context.get(pathWithQuery, baseOptions());
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private RequestOptions baseOptions() {
        return RequestOptions.create().setHeader("Accept", "text/html");
    }
}
