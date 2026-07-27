# Test Plan - EPMCDMETST-55895 - Find and Analyze Expenses (Filter, Search, Summaries)

Owner: QA
Framework: Playwright Java + JUnit 5
Scope: Functional UI and API integration testing for filtering, searching, and summary totals.

## 1. References
- Jira Epic: EPMCDMETST-55895
- Stories:
  - EPMCDMETST-55896 Filter expenses by date range and category
  - EPMCDMETST-55897 Search expenses by note text
  - EPMCDMETST-55898 View summary totals for a selected period
- Repo: lohithaperni/ExpenseTracker

## 2. Assumptions and Test Data
- Application is a Flask + SQLite expense tracker.
- Expenses page supports query parameters for filtering:
  - start_date, end_date (or similar; align with implementation)
  - category
  - q/note (search term)
- Summary total is displayed for current result set.
- Seed data will be inserted for tests via API endpoints or direct DB setup in test environment.

Test dataset (example):
- 2026-01-01, category Food, amount 10.50, note "coffee"
- 2026-01-05, category Transport, amount 25.00, note "uber airport"
- 2026-02-01, category Food, amount 40.00, note "groceries"
- 2026-02-10, category Bills, amount 80.00, note "internet"
- 2026-03-15, category Other, amount 5.00, note ""

## 3. Test Approach
### 3.1 UI functional testing (Playwright Java + JUnit 5)
- Validate presence and behavior of filter controls (start date, end date, category dropdown, search box).
- Validate results list updates (table rows) and no-results state.
- Validate summary total updates with filters and resets.
- Validate input validation and UX behavior for edge cases.

### 3.2 API integration testing (Playwright Java + JUnit 5)
- Use Playwright APIRequestContext for HTTP calls.
- Validate endpoints that return filtered lists and totals (either same HTML route with query params or JSON endpoints if introduced).
- Validate status codes, response content, and server-side filtering correctness.

## 4. In Scope
- Filtering by date range.
- Filtering by category.
- Searching by note keyword.
- Combined filters (date + category + search).
- Total for displayed results.
- No results state.

## 5. Out of Scope
- Authentication/authorization.
- Category management.
- Edit/delete workflows (unless required for test setup/teardown).

## 6. Test Scenarios

### 6.1 EPMCDMETST-55896 Filter expenses by date range and category
UI scenarios:
1. Date range filter returns only items in range
   - Given expenses across multiple months
   - When user sets start date and end date and applies
   - Then list shows only items within inclusive range.
2. Category filter returns only selected category
   - Select category Food
   - Assert only Food rows shown.
3. Combined date + category filtering
   - Date range includes multiple Food and non-Food
   - Assert only Food in range.
4. No results state
   - Apply range/category that matches none
   - Assert "no results" message and empty list state.
5. Boundary dates inclusive behavior
   - Start date equals an expense date; end date equals an expense date
   - Assert those boundary items included.
6. Invalid range handling
   - Start date after end date
   - Assert validation message and no request applied OR server returns safe result.
7. Reset/clear filters restores full list
   - Clear inputs and re-apply/reset
   - Assert full list shown.

API scenarios:
1. GET expenses with date parameters returns only matching items.
2. GET expenses with category parameter returns only matching items.
3. Combined params narrow correctly.
4. No matches returns 200 with empty results (or HTML containing no rows).


### 6.2 EPMCDMETST-55897 Search expenses by note text
UI scenarios:
1. Search keyword matches substring (case-insensitive if required)
   - Enter "uber"
   - Assert only notes containing keyword.
2. Search with multiple words
   - "uber airport"
   - Assert correct match behavior (contains full phrase vs tokenized; align with implementation).
3. Clear search restores full list
   - Remove text and apply/reset
   - Assert full list.
4. Empty notes not included when searching
   - Ensure expense with empty note is absent from results when searching.
5. No matches shows no-results state
   - Search keyword not present
   - Assert message.

API scenarios:
1. GET expenses with note query returns correct subset.
2. Search does not return expenses with null/empty note unless other filters match and no search term.


### 6.3 EPMCDMETST-55898 View summary totals for a selected period
UI scenarios:
1. All-time total displayed on initial load
   - Assert total equals sum of all seeded expenses.
2. Total updates after applying date filter
   - Apply date range
   - Assert total equals sum of displayed rows.
3. Total updates after applying category filter
   - Assert recalculated.
4. Total updates after combined filters
   - Apply date + category + search
   - Assert total equals sum of displayed rows.
5. Total shows 0.00 (or equivalent) when no results
   - Assert consistent formatting.
6. Total formatting and rounding
   - Verify two decimal places, currency symbol if present.

API scenarios:
1. If a totals endpoint exists, validate numeric accuracy.
2. If total is computed server-side and embedded in HTML, parse and validate.

## 7. Non-functional Checks (lightweight)
- Performance sanity: filter/search response time under acceptable threshold on small dataset.
- Accessibility sanity: filter controls have labels and are keyboard accessible.

## 8. Automation Design (Playwright Java + JUnit 5)

### 8.1 Project structure (suggested)
- src/test/java/
  - ui/
    - ExpensesFilterUiTest.java
    - ExpensesSearchUiTest.java
    - ExpensesSummaryUiTest.java
  - api/
    - ExpensesFilterApiTest.java
    - ExpensesSearchApiTest.java
    - ExpensesTotalsApiTest.java
  - support/
    - TestDataSeeder.java
    - BaseUiTest.java
    - BaseApiTest.java

### 8.2 UI automation notes
- Use stable locators: data-testid attributes recommended.
- Validate table rows by reading cells: date, category, note, amount.
- For totals, parse text to BigDecimal.
- Ensure tests are isolated: seed DB per test class, cleanup afterwards.

### 8.3 API automation notes
- Create APIRequestContext with baseURL.
- Use GET with query params.
- Validate HTTP status and response content.

## 9. Entry/Exit Criteria
Entry:
- Feature deployed to test environment.
- Seed data mechanism available.
- UI elements for filters/search/total implemented.

Exit:
- All critical and high scenarios pass.
- No open critical defects related to incorrect filtering/searching/totals.

## 10. Risks
- Backend filter parameter names may differ; tests must align once confirmed.
- If totals are computed client-side, ensure deterministic calculation and locale formatting.
- SQLite concurrency in CI; prefer isolated DB per test run.
