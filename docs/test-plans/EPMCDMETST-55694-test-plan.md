EPMCDMETST-55694 Test Plan - Find & Understand Spending (Filters, Search, Summaries)

1. Purpose and Scope
This test plan covers API and integration testing for Epic EPMCDMETST-55694 and its user stories:
- EPMCDMETST-55695 Filter expenses by date range and category
- EPMCDMETST-55696 Search expenses by note text
- EPMCDMETST-55697 View monthly spending summary

The focus is backend route behavior, database integration with SQLite, and end-to-end flows through HTTP boundaries using Playwright Java with JUnit 5.

Out of scope:
- Performance/load testing
- Penetration testing
- Cross-browser visual UI testing

2. Test Approach
2.1 Levels
- Route-level integration tests (HTTP + DB): verify request/response, status codes, rendered content, and DB state.
- End-to-end UI-backed integration tests (Playwright): drive the browser to set filters/search/month selection and assert list, totals, and empty states.

2.2 Framework and Tooling
- Automation: Playwright Java + JUnit 5
- Test data: SQLite test database per test (temp file) or isolated schema per test run.
- Execution: CI-compatible headless mode; allow headed mode locally.

2.3 Environments
- Local: app started on ephemeral port with test configuration.
- CI: same as local, with deterministic timezone and locale.

2.4 Data and Determinism
- Use fixed dates (YYYY-MM-DD) and categories.
- Avoid dependence on system date. If month selection defaults to current month, set it explicitly in tests.
- Ensure numeric formatting consistency (decimal separator '.')

3. Assumptions and Dependencies
- Application provides endpoints or query parameters for:
  - Filtering by date range and category
  - Searching by note text
  - Monthly summary view (month selector)
- The UI reflects filtered totals distinct from all-time totals as per acceptance criteria.
- Category values come from an allowed list.

If any of the above is not implemented as dedicated endpoints, tests will validate behavior through the existing main route with query parameters or form submissions.

4. Risks
- Date handling: inclusive vs exclusive boundaries; timezone shifts.
- SQLite date storage as text may cause lexicographic issues if not ISO-8601.
- Search semantics: case sensitivity and partial matches.
- Summary calculations: rounding and decimal handling.

5. Test Coverage Mapped to User Stories

5.1 EPMCDMETST-55695 Filter expenses by date range and category

Functional coverage
- Date range filter shows only expenses within range.
- Category filter shows only matching category.
- Total displayed matches only filtered expenses.

Test scenarios
F-DR-01 Inclusive boundaries
- Seed expenses on start date, inside range, on end date, and outside range.
- Apply start/end filter.
- Assert only boundary+inside items present.
- Assert filtered total equals sum of included amounts.

F-DR-02 Start date only (if supported)
- Apply only start date.
- Assert only expenses with date >= start shown.

F-DR-03 End date only (if supported)
- Apply only end date.
- Assert only expenses with date <= end shown.

F-DR-04 Invalid date range
- Start date after end date.
- Assert validation/error message and no filtering applied OR empty results per spec.

F-CAT-01 Category only
- Seed multiple categories.
- Apply category.
- Assert list contains only that category.
- Assert total matches.

F-CAT-02 Category + date range combined
- Apply both.
- Assert intersection of filters.

F-CAT-03 Unknown category value
- Attempt to submit an invalid category (e.g., via direct request).
- Assert request rejected (4xx) or safe fallback with no data change and a clear message.

Non-functional/robustness
F-DR-05 Large result set rendering
- Seed 200+ expenses.
- Apply filter that returns many rows.
- Assert page remains responsive and totals accurate.

5.2 EPMCDMETST-55696 Search expenses by note text

Functional coverage
- Search term filters by notes containing the term.
- Empty-state message appears when no matches.
- Clearing search returns to full list.

Test scenarios
S-NOTE-01 Partial substring match
- Seed notes: "coffee shop", "coffeemaker", "rent".
- Search "coffee".
- Assert first two present, "rent" absent.

S-NOTE-02 Case-insensitive search (expected)
- Seed note: "Taxi".
- Search "taxi".
- Assert match.

S-NOTE-03 Special characters
- Seed note: "Dinner @ Joe's".
- Search "@" and "Joe".
- Assert match and no server error.

S-NOTE-04 No matches empty state
- Search term not present.
- Assert list empty and empty-state message visible.

S-NOTE-05 Clear search
- Activate search then clear (empty string or click clear).
- Assert full list restored and total reflects unfiltered.

S-NOTE-06 SQL injection safety (integration)
- Search term: "' OR 1=1 --".
- Assert no error, and results are not all rows unless legitimately matching.

5.3 EPMCDMETST-55697 View monthly spending summary

Functional coverage
- Month selection shows total spend for that month.
- Category breakdown totals displayed.
- No-data month shows clear message.

Test scenarios
M-SUM-01 Month with multiple categories
- Seed multiple expenses in a target month across categories.
- Select month.
- Assert monthly total equals sum of those expenses.
- Assert per-category totals correct.

M-SUM-02 Month boundary
- Seed expense on last day of previous month and first day of selected month.
- Select month.
- Assert only selected month included.

M-SUM-03 Month with no expenses
- Select empty month.
- Assert "no data" message.
- Assert totals display as 0 or omitted per spec.

M-SUM-04 Rounding and decimals
- Seed amounts with decimals (e.g., 10.10, 0.20, 0.30).
- Assert displayed totals match exact arithmetic with consistent rounding rules.

M-SUM-05 Category present in allowed list but no spend
- If UI shows all categories, verify zero for absent categories; otherwise verify only categories with spend are shown.

6. API and Integration Checks
Even if the app is server-rendered, validate integration behavior through HTTP and DB.

6.1 HTTP Response Validation
- Status codes: 200 for successful views; 4xx for invalid filter/search params if rejected.
- Content type: text/html; charset.
- No stack traces leaked in responses.

6.2 Database State Validation
- Filtering/searching/summaries must not mutate data.
- Validate that number of rows in DB remains unchanged after GET/filter/search actions.

6.3 Idempotency
- Re-applying same filter/search yields same results.

7. Test Data Setup
Seed dataset used across tests (example)
- 2026-01-01 Food note="coffee shop" amount=3.50
- 2026-01-05 Transport note="Taxi" amount=12.00
- 2026-01-31 Bills note="rent" amount=800.00
- 2026-02-01 Food note="groceries" amount=45.25

Add additional rows per scenario as needed.

8. Automation Design (Playwright Java + JUnit 5)
8.1 Test Structure
- Package: src/test/java/.../testplan/filters
- Base test class:
  - Start the Flask app as an external process (or use a test entrypoint) on a random port.
  - Create a temp SQLite DB and point the app to it via environment variable.
  - Seed DB via direct SQLite connection before each test.
  - Tear down process and delete temp DB after tests.

8.2 Page Object Model (lightweight)
- ExpenseListPage
  - open()
  - applyDateRange(start, end)
  - applyCategory(category)
  - searchNotes(term)
  - clearSearch()
  - selectMonth(month)
  - readVisibleExpenses(): List<ExpenseRow>
  - readDisplayedTotal(): BigDecimal
  - readEmptyStateMessage(): String

8.3 Assertions
- Prefer assertions on:
  - Visible rows count and specific row content
  - Total values (BigDecimal comparisons)
  - Presence of summary breakdown by category

8.4 Reporting
- Attach Playwright traces/screenshots on failure.

9. Entry/Exit Criteria
Entry
- Epic stories are implemented in a deployable branch.
- Deterministic categories and date formats are defined.

Exit
- All automated tests pass in CI.
- No critical defects open for filtering/search/summary correctness.

10. Traceability Matrix
- EPMCDMETST-55695: F-DR-01..05, F-CAT-01..03
- EPMCDMETST-55696: S-NOTE-01..06
- EPMCDMETST-55697: M-SUM-01..05
