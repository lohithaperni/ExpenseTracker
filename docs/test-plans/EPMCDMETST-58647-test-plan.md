# Test Plan: EPMCDMETST-58647 - Filtering, Search & Spend Insights

Epic: EPMCDMETST-58647
Stories:
- EPMCDMETST-58648 - Filter expenses by date range and category
- EPMCDMETST-58649 - Search expenses by note text
- EPMCDMETST-58650 - View basic spending summaries

Automation framework:
- UI: Playwright Java + JUnit 5
- API/Integration: Playwright Java (APIRequestContext) + JUnit 5

## 1. Objectives
- Validate end-to-end filtering and search capabilities from UI controls through server-side query processing and database results.
- Validate summaries/breakdowns accuracy (by month and by category) and that summaries respect active filters.
- Validate usability/empty states and that clearing filters/search restores full dataset.
- Provide a regression suite for core list integrity (ordering newest first, total display unchanged by filtering unless specified).

## 2. In Scope
UI functional testing:
- Expense list page filter controls: start date, end date, category.
- Note keyword search control.
- Clear filters/search.
- Empty states for no results.
- Summaries view/panel: totals grouped by month and by category.
- Summaries behavior when filters/search are active.

API/integration testing:
- GET list endpoint behavior with query parameters:
  - start_date, end_date, category, q
- Summaries endpoint(s) if implemented (e.g., /summaries or /api/summaries) or server-rendered summaries computed from DB.
- Data correctness against SQLite persistence.

Non-functional (lightweight functional checks only):
- Input validation at UI level for date formats (where applicable).
- Basic security expectations for query params (no server error on special characters).

## 3. Out of Scope
- Authentication/authorization and user isolation.
- CSRF protections.
- Category management and export.
- Performance/load testing beyond basic sanity.

## 4. Test Environment
- App under test: Flask + SQLite ExpenseTracker.
- Environments:
  - Local/dev: http://localhost:<port>
  - CI: ephemeral environment with isolated SQLite database per run.
- Test data:
  - Seed expenses spanning multiple months, categories, and notes.
  - Include boundary dates (first/last day of month) and mixed case notes.

## 5. Test Data Strategy
Create deterministic seed dataset before each test class (or per test) via:
- Direct DB setup (preferred for speed) OR
- API/UI creation flows.

Seed example dataset (illustrative):
- 2026-01-01, Food, note="coffee beans", amount=12.50
- 2026-01-15, Transport, note="metro card", amount=30.00
- 2026-02-01, Food, note="Groceries at Market", amount=45.25
- 2026-02-20, Utilities, note="electric bill", amount=60.00
- 2026-03-05, Entertainment, note="movie night", amount=18.00
- 2026-03-31, Food, note="dinner", amount=40.00

Ensure at least:
- Two expenses per month for at least 2 months.
- At least 3 categories.
- Notes that allow partial matches and case-insensitive expectations if required.

## 6. Entry/Exit Criteria
Entry:
- Epic stories implemented and deployed to test environment.
- UI elements for filters/search/summaries present.
- Known endpoints and query parameters confirmed.

Exit:
- All P0/P1 test cases pass.
- No open critical defects for filtering/search correctness or summary accuracy.

## 7. Risks and Mitigations
- Risk: Undefined matching rules for q (contains vs exact, case sensitivity).
  - Mitigation: Confirm with product/implementation; include tests for both and mark expectation.
- Risk: Time zone/date parsing differences.
  - Mitigation: Use ISO dates and run tests with fixed TZ in CI.
- Risk: SQLite ordering nuances.
  - Mitigation: Assert ordering by date then id if specified; otherwise only assert membership.

## 8. Test Cases

### 8.1 UI - Filtering by Date Range and Category (EPMCDMETST-58648)
Priority P0
1. Filter by start+end date shows only in-range expenses
- Preconditions: seed data includes expenses inside and outside range.
- Steps:
  1) Open expense list page.
  2) Set start date = 2026-02-01.
  3) Set end date = 2026-02-28.
  4) Apply filter.
- Expected:
  - Only February expenses displayed.
  - No January/March entries visible.

2. Filter by start date only
- Steps: start date=2026-03-01; end date empty; apply.
- Expected: Only expenses on/after 2026-03-01.

3. Filter by end date only
- Steps: end date=2026-01-31; start date empty; apply.
- Expected: Only expenses on/before 2026-01-31.

4. Filter by category only
- Steps: category=Food; apply.
- Expected: Only Food expenses shown.

5. Combine date range + category
- Steps: range Feb 2026 + category Food.
- Expected: Only Feb Food expenses.

6. Clear filters restores all
- Steps: Apply any filters; click Clear.
- Expected: Full list displayed again.

Priority P1
7. Invalid date input handled gracefully
- Steps: enter invalid date format if UI allows free text; apply.
- Expected: Validation message or safe fallback; no server error; list not corrupted.

8. Boundary dates inclusive behavior
- Steps: start=2026-03-31, end=2026-03-31.
- Expected: Includes 2026-03-31 expense.

### 8.2 UI - Search by Note Text (EPMCDMETST-58649)
Priority P0
1. Keyword search returns matching notes
- Steps: search term "coffee"; apply.
- Expected: Only expenses with note containing coffee shown.

2. Search with no matches shows empty state
- Steps: search term "nonexistent".
- Expected: Empty results message and zero rows.

3. Clear search restores full list
- Steps: perform search; clear term or click Clear Search.
- Expected: Full list returns.

Priority P1
4. Search is case-insensitive (if required)
- Steps: search "market" should match "Market".
- Expected: Match present.

5. Search with special characters does not break page
- Steps: search "%'_" or "<script>".
- Expected: No 500 error; results correct per escaping rules; UI safe.

6. Search combined with filters
- Steps: category Food + search "dinner".
- Expected: only Food dinner entries.

### 8.3 UI - Spending Summaries (EPMCDMETST-58650)
Priority P0
1. Monthly totals displayed and accurate
- Steps:
  1) Open summaries section/view.
  2) Observe monthly grouping rows.
- Expected:
  - Each month shown with correct sum of amounts from that month.
  - Totals use consistent rounding (2 decimals).

2. Category totals displayed and accurate
- Expected: Each category sum equals sum of its expenses.

3. Summaries respect active filters
- Steps: apply filter Feb only; open summaries.
- Expected: only Feb data represented and sums reflect filtered subset.

Priority P1
4. Summaries with empty result set
- Steps: apply filters that yield no expenses; view summaries.
- Expected: summaries show empty/zero state without error.

5. Summaries reflect search term
- Steps: search "metro"; view summaries.
- Expected: totals computed only on matching expenses.

### 8.4 API/Integration - List Filtering/Search
(Use Playwright APIRequestContext)
Priority P0
1. GET list with start_date and end_date
- Request: GET /?start_date=2026-02-01&end_date=2026-02-28 (or list endpoint)
- Expected: response 200; body contains only Feb expenses.

2. GET list with category
- Request: GET /?category=Food
- Expected: only Food.

3. GET list with q
- Request: GET /?q=coffee
- Expected: only matching notes.

4. Combined params
- Request: GET /?start_date=2026-02-01&end_date=2026-03-31&category=Food&q=dinner
- Expected: intersection of filters.

Priority P1
5. Unknown category returns empty or validation error (define expected)
- Request: GET /?category=Unknown
- Expected: either 200 empty list OR 400 with message; no 500.

6. Invalid date param does not 500
- Request: GET /?start_date=bad
- Expected: 400 or safe ignore; no 500.

7. URL encoding handled
- Request: GET /?q=coffee%20beans
- Expected: matches note with space.

### 8.5 API/Integration - Summaries
Priority P0
1. Monthly summaries endpoint returns correct totals
- Request: GET /summaries?group=month (or implemented path)
- Expected: 200; JSON or HTML includes correct totals.

2. Category summaries returns correct totals
- Request: GET /summaries?group=category

3. Summaries respect filters/search
- Request: GET /summaries?group=month&start_date=2026-02-01&end_date=2026-02-28

Priority P1
4. Empty dataset summaries
- Precondition: DB empty.
- Expected: returns 200 with empty groups / zeros.

## 9. Automation Approach (Playwright Java + JUnit 5)

### 9.1 Project Structure (proposed)
- src/test/java/
  - ui/
    - FilterTests.java
    - SearchTests.java
    - SummariesTests.java
  - api/
    - ListFilterApiTests.java
    - SummariesApiTests.java
  - support/
    - TestDataSeeder.java
    - DbUtils.java
    - BaseUiTest.java
    - BaseApiTest.java

### 9.2 UI Automation Guidelines
- Use data-testid attributes for filter/search inputs and summary rows (recommend adding if not present).
- Assertions:
  - Verify row count.
  - Verify presence/absence of specific notes/categories/dates.
  - Avoid brittle selectors (no nth-child reliance).
- Use Page Object Model lightly:
  - ExpenseListPage with methods: setStartDate, setEndDate, selectCategory, setSearch, apply, clear, getRows.

### 9.3 API Automation Guidelines
- Use Playwright request.newContext({ baseURL }).
- Validate:
  - HTTP status codes.
  - Response schema (keys present), content types.
  - Data correctness by comparing to seeded dataset.

### 9.4 Test Data Setup/Teardown
- For deterministic tests, reset SQLite DB before suite:
  - Delete DB file or run schema recreate.
  - Seed rows.
- Ensure tests are isolated:
  - Prefer per-test DB reset for P0 flows.

### 9.5 CI Execution
- Command: mvn test
- Run headless in CI.
- Artifacts:
  - Playwright traces on failure.
  - Screenshots on failure.

## 10. Traceability Matrix
- EPMCDMETST-58648:
  - UI: 8.1.1-8.1.8
  - API: 8.4.1-8.4.7
- EPMCDMETST-58649:
  - UI: 8.2.1-8.2.6
  - API: 8.4.3, 8.4.4, 8.4.7
- EPMCDMETST-58650:
  - UI: 8.3.1-8.3.5
  - API: 8.5.1-8.5.4

## 11. Open Questions
- What are the exact routes for list filtering and summaries (server-rendered vs JSON)?
- Is note search case-insensitive? Is it substring match?
- Are date boundaries inclusive?
- Should unknown categories/date formats yield 400 or be ignored?
