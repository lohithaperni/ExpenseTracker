# Test Plan: Insights, Summaries, and Data Export (EPMCDMETST-55762)

Jira Epic: EPMCDMETST-55762
Stories in scope:
- EPMCDMETST-55763 - Monthly Spending Summary
- EPMCDMETST-55764 - Category Breakdown for a Period
- EPMCDMETST-55765 - Export Expenses to CSV

Automation framework: Playwright Java + JUnit 5

## 1. Objectives
- Validate end-to-end functionality for reporting views (monthly summary, category breakdown) and CSV export.
- Verify correctness of calculations (totals, grouping), filtering behavior, and empty states.
- Validate API integration behaviors (HTTP status, response payloads, content types) for reporting/export endpoints.
- Ensure data integrity: exports match on-screen filtered datasets.

## 2. In Scope
UI functional testing:
- Navigation to reporting/summary views (as implemented).
- Monthly grouping totals and drill-down to a selected month.
- Category breakdown for a chosen date range.
- CSV export for unfiltered and filtered datasets.
- Empty-state messaging when no data.

API integration testing:
- Reporting endpoints used by UI (monthly totals, breakdown) if exposed.
- CSV export endpoint, including filter query params.

Non-functional (limited):
- Basic input validation for filters (date range format, start <= end).
- Basic performance smoke checks (response within reasonable threshold) where feasible.

Out of Scope
- Authentication/authorization (covered by Epic E3).
- Core add/edit/delete workflows (covered by E1/E4).
- Visual chart rendering correctness beyond basic presence (unless acceptance criteria specify charts).

## 3. Test Environment
- Test environment: local or CI-hosted app instance.
- Database: SQLite; use isolated test DB per test run.
- Test data seeding: create known expenses across multiple months and categories.
- Time zone: define and fix (e.g., UTC) to avoid month-boundary issues.

## 4. Test Data
Seed dataset (example):
- 2026-01-05, Groceries, note="walmart", amount=50.25
- 2026-01-20, Transport, note="gas", amount=40.00
- 2026-02-01, Groceries, note="costco", amount=120.00
- 2026-02-15, Dining, note="lunch", amount=18.75
- 2026-03-03, Utilities, note="electric", amount=89.99
- 2026-03-31, Groceries, note="target", amount=34.10
Edge data:
- Large amount (e.g., 9999999.99)
- Note with commas/quotes/newlines for CSV escaping
- Date range with no matches

## 5. Assumptions / Open Questions
- UI locations/routes for summary and export are TBD (define selectors after implementation).
- Reporting/export endpoints are TBD; tests will adapt to implemented routes.
- Totals rounding rules (2 decimals) must be clarified; tests assume currency with 2 decimal rounding.

## 6. Test Approach
- UI tests with Playwright Java and JUnit 5:
  - Use deterministic seeded DB state.
  - Assert visible totals, grouped rows, drill-down content, and empty-state messages.
  - Validate downloaded CSV content via Playwright download API.

- API integration tests with Java (JUnit 5) using Java 11+ HttpClient:
  - Perform GET requests to endpoints.
  - Validate status codes, headers (Content-Type), and response bodies.
  - For CSV, validate RFC4180-like formatting and correct rows.

## 7. Functional UI Test Scenarios

### 7.1 Monthly Spending Summary (EPMCDMETST-55763)
1) View monthly totals grouped by month
- Precondition: expenses seeded across multiple months.
- Steps:
  1. Open app reporting/summary page.
  2. Navigate to Monthly Summary.
- Expected:
  - Months present for months with expenses.
  - Each month shows correct total (sum of amounts for that month).
  - Totals displayed with 2 decimals.

2) Drill-down into a specific month
- Steps:
  1. Click/select month (e.g., 2026-02).
- Expected:
  - Detail view shows total for selected month.
  - Expense list contains only that month.

3) Empty-state for a month with no expenses
- Steps:
  1. Select month with no data (if UI allows selection) or adjust to a month with no entries.
- Expected:
  - Friendly empty-state message.
  - Total shows 0.00 or equivalent.

4) Month boundary correctness
- Precondition: expenses on first/last day of month.
- Expected:
  - Included in correct month grouping.

### 7.2 Category Breakdown for a Period (EPMCDMETST-55764)
1) Breakdown totals by category for a date range
- Steps:
  1. Open Category Breakdown.
  2. Enter start_date=2026-02-01, end_date=2026-03-31.
  3. Apply.
- Expected:
  - Shows categories with spend in range.
  - Each category total equals sum for that category within range.
  - Overall total (if shown) equals sum of category totals.

2) Update breakdown when date range changes
- Steps:
  1. Change end_date to 2026-02-28.
  2. Apply.
- Expected:
  - Values update; categories outside range removed or set to zero consistently.

3) Category with no spend not shown or shown as zero consistently
- Expected:
  - Behavior matches product decision; test asserts chosen behavior.

4) Invalid date range validation
- Steps:
  1. start_date after end_date.
  2. Apply.
- Expected:
  - Clear validation error; breakdown not updated.

### 7.3 Export Expenses to CSV (EPMCDMETST-55765)
1) Export all expenses
- Steps:
  1. Trigger CSV export.
  2. Capture downloaded file.
- Expected:
  - File downloads.
  - Content-Type indicates CSV (e.g., text/csv).
  - Header row includes: date, category, note, amount.
  - Number of rows equals number of expenses.

2) Export filtered expenses
- Steps:
  1. Apply filters (date range/category/search) in UI.
  2. Trigger export.
- Expected:
  - CSV contains only filtered rows.
  - Totals on UI match sum of exported rows (if totals displayed).

3) Export with no matching expenses
- Steps:
  1. Apply filters that match none.
  2. Export.
- Expected:
  - CSV contains only headers and no data rows.
  - UI displays clear indicator/message.

4) CSV escaping/formatting
- Precondition: note contains comma and quotes.
- Expected:
  - CSV escapes fields correctly.

## 8. API Integration Test Scenarios
(Endpoint paths to be updated to actual implementation)

### 8.1 Monthly summary API
- GET /api/reports/monthly
  - Assert 200 OK.
  - Assert JSON schema: list of {month, total}.
  - Assert totals match seeded dataset.

- GET /api/reports/monthly?month=2026-02
  - Assert 200 OK.
  - Assert response includes month total and expenses for that month.
  - Assert empty-state behavior for month with no data (200 with empty list vs 404; define expected).

### 8.2 Category breakdown API
- GET /api/reports/breakdown?start_date=2026-02-01&end_date=2026-03-31
  - Assert 200 OK.
  - Assert JSON schema: list of {category, total}.
  - Assert category totals match.

- Invalid date range
  - start_date > end_date
  - Assert 400 and validation message.

### 8.3 CSV export API
- GET /export.csv
  - Assert 200 OK.
  - Assert Content-Type text/csv; charset utf-8.
  - Assert header columns.

- GET /export.csv with filters
  - Use query params for date range/category/q if supported.
  - Assert rows match filtered query.

- No matches
  - Assert 200 with headers-only CSV (or defined behavior).

## 9. Automation Design (Playwright Java + JUnit 5)

### 9.1 Project structure (proposed)
- src/test/java
  - ui/
    - MonthlySummaryUiTests.java
    - CategoryBreakdownUiTests.java
    - CsvExportUiTests.java
  - api/
    - MonthlySummaryApiTests.java
    - CategoryBreakdownApiTests.java
    - CsvExportApiTests.java
  - support/
    - TestDbSeeder.java
    - BaseUiTest.java
    - BaseApiTest.java

### 9.2 UI automation patterns
- Use Playwright Download handling:
  - page.waitForDownload(() -> page.click("text=Export CSV"))
  - Parse downloaded file content and assert.
- Stable selectors:
  - Prefer data-testid attributes (to be added by dev) for summary tables and filter inputs.

### 9.3 API automation patterns
- Use java.net.http.HttpClient in JUnit 5.
- Parse JSON via Jackson.
- Parse CSV via simple split or a CSV library (if allowed); if not, implement minimal parser for test assertions.

### 9.4 Data setup/teardown
- BeforeEach: reset SQLite DB and seed deterministic expenses.
- AfterEach: cleanup temp download directory.

## 10. Regression Suite
- Smoke: monthly summary loads and shows at least one month.
- Smoke: category breakdown returns expected categories for known range.
- Smoke: CSV export downloads and has correct headers.

## 11. Risks and Mitigations
- Month grouping depends on locale/timezone: mitigate by using explicit dates and consistent timezone in app/test.
- CSV formatting differences: assert content logically (headers/rows) and tolerate line endings (LF/CRLF).
- UI changes may break selectors: mitigate with data-testid usage.

## 12. Traceability Matrix
- EPMCDMETST-55763
  - UI: 7.1 scenarios 1-4
  - API: 8.1 scenarios
- EPMCDMETST-55764
  - UI: 7.2 scenarios 1-4
  - API: 8.2 scenarios
- EPMCDMETST-55765
  - UI: 7.3 scenarios 1-4
  - API: 8.3 scenarios
