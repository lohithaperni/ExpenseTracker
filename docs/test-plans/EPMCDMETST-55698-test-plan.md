# Test Plan: Find & Review Expenses (Search, Filter, Sort)
Epic: EPMCDMETST-55698
Repository: lohithaperni/ExpenseTracker
Framework: Playwright Java + JUnit 5
Scope: API and integration testing (HTTP routes + DB integration). UI-level assertions are limited to verifying rendered results when no API is available.

## 1. Objectives
- Validate backend behaviors that enable filtering and searching of expenses.
- Ensure correct handling of date range, category, and note keyword query parameters.
- Ensure safe, deterministic behavior for empty results, invalid inputs, and boundary conditions.
- Ensure feature interactions: filter + search combined; clear/reset behavior.

## 2. In Scope
User stories:
- EPMCDMETST-55699 Filter expenses by date range and category
- EPMCDMETST-55700 Search expenses by note text

Primary integration surfaces (expected/typical for this app; adjust to actual implementation during test design):
- GET / (or /expenses): renders list of expenses; accepts optional query params for filtering/searching.
- DB: SQLite persistence and query semantics.

## 3. Out of Scope
- Authentication/authorization
- Export/report generation
- Edit/delete confirmation flows
- Performance/load testing

## 4. Assumptions and Open Questions
Assumptions (to be confirmed once implementation is available):
- Expenses have fields: id, date, category, amount, note.
- Filtering/searching is driven by query parameters on list endpoint.
- Date format accepted: YYYY-MM-DD.
- Search is case-insensitive substring match on note.

Open questions:
- What is the canonical list endpoint path and parameter names? (e.g., start_date/end_date, from/to, category, q)
- Are multiple categories supported?
- Are filters combined with AND semantics?
- How is "clear filters/search" represented (absence of params vs explicit clear action)?

## 5. Test Approach
### 5.1 Levels
- Integration tests: Start the Flask app (or Java test harness against a running instance) and validate HTTP responses + DB state.
- Contract-like checks: Validate response HTML contains expected rows/empty state markers.

### 5.2 Automation Tooling
- Playwright Java for HTTP via APIRequestContext where possible and for minimal browser rendering checks.
- JUnit 5 for test structure, parameterized tests, and lifecycle.

### 5.3 Test Data Strategy
- Use a dedicated SQLite database per test run (or per test class) to ensure isolation.
- Seed deterministic expenses covering:
  - Multiple dates spanning months and weeks
  - Multiple categories (e.g., Food, Travel, Bills)
  - Notes with unique keywords and mixed case
  - Boundary dates equal to start/end

Example seed set:
- 2026-01-01 Food 10.00 note="Coffee"
- 2026-01-15 Travel 120.00 note="Train ticket"
- 2026-02-01 Food 25.50 note="groceries"
- 2026-02-10 Bills 60.00 note="Internet"
- 2026-02-10 Food 15.00 note="Coffee beans"

### 5.4 Environments
- Local/CI: app launched on ephemeral port.
- Configuration via env vars (recommended): DATABASE_URL/DB_PATH, FLASK_ENV, etc.

## 6. Entry / Exit Criteria
Entry:
- Epic implementation deployed to testable environment.
- Ability to seed DB or use test-only DB file.

Exit:
- All critical and high tests pass.
- No unresolved defects in filtering/searching that cause incorrect results or server errors.

## 7. Risks
- HTML-only responses may be brittle to assert; mitigate by using stable selectors or table parsing.
- SQLite date comparisons depend on storage type/format; ensure consistent ISO date strings.

## 8. Test Scenarios and Cases

### 8.1 Filter by Date Range (EPMCDMETST-55699)
TC-DR-001 Inclusive boundaries
- Seed expenses on start_date and end_date.
- Request list with start_date=2026-02-01 end_date=2026-02-10.
- Assert rows include 2026-02-01 and 2026-02-10 expenses; exclude outside range.

TC-DR-002 Only start date provided
- Request with start_date=2026-02-01 (no end).
- Assert only expenses on/after start date returned.

TC-DR-003 Only end date provided
- Request with end_date=2026-01-31.
- Assert only expenses on/before end date returned.

TC-DR-004 Start date after end date
- Request start_date=2026-02-10 end_date=2026-02-01.
- Expected: Either validation error message with 400/200+banner, or treated as empty set. Must not 500.

TC-DR-005 Invalid date format
- Request start_date=02-01-2026.
- Expected: clear validation feedback; no server error.

TC-DR-006 No matching expenses in range
- Request start_date=2027-01-01 end_date=2027-01-31.
- Assert empty state shown.

### 8.2 Filter by Category (EPMCDMETST-55699)
TC-CAT-001 Exact match category
- Request category=Food.
- Assert only Food expenses displayed.

TC-CAT-002 Category with URL encoding / special characters
- Seed category="Health & Fitness".
- Request category=Health%20%26%20Fitness.
- Assert matching rows displayed.

TC-CAT-003 Non-existent category
- Request category=NonExistent.
- Assert empty state shown.

TC-CAT-004 Category case sensitivity
- Request category=food.
- Expected behavior defined by product; assert consistent handling (either case-insensitive match or strict).

### 8.3 Combined Filters (EPMCDMETST-55699)
TC-COMB-001 Date range + category combined with AND
- Request start_date=2026-02-01 end_date=2026-02-28 category=Food.
- Assert only Food expenses within Feb shown.

TC-COMB-002 Clear filters returns full list
- First request with filters applied and confirm subset.
- Then request without params.
- Assert full seeded list returned.

### 8.4 Search by Note Text (EPMCDMETST-55700)
TC-SRCH-001 Substring match
- Request q=Coffee.
- Assert rows with "Coffee" and "Coffee beans" appear.

TC-SRCH-002 Case-insensitive match
- Request q=coffee.
- Assert same results as TC-SRCH-001.

TC-SRCH-003 No matches
- Request q=unmatchedkeyword.
- Assert empty state shown.

TC-SRCH-004 Search with whitespace trimming
- Request q="  Train  ".
- Expect same as q=Train.

TC-SRCH-005 Search special characters
- Seed note="Lunch @ cafe".
- Request q=%40 ("@").
- Assert row returned.

### 8.5 Search + Filters Combined (EPMCDMETST-55699, EPMCDMETST-55700)
TC-SF-001 Search constrained by date range
- Request q=Coffee start_date=2026-02-01 end_date=2026-02-28.
- Assert only Coffee notes within Feb range shown.

TC-SF-002 Search constrained by category
- Request q=Coffee category=Food.
- Assert Coffee entries only in Food.

### 8.6 Robustness / Security-lite (Input handling)
TC-RB-001 SQL injection-like input does not break query
- Request q=' OR 1=1 --
- Expected: treated as plain text; no SQL error; results reflect real matches only.

TC-RB-002 Very long search term
- Request q=500+ chars.
- Expected: request handled; either validation message or empty; must not 500.

## 9. Automation Design (Playwright Java + JUnit 5)
### 9.1 Project Structure (suggested)
- src/test/java/.../expenses/FilteringSearchIT.java
- src/test/java/.../support/AppFixture.java (start/stop app, temp DB)
- src/test/java/.../support/DbSeeder.java
- src/test/resources/...

### 9.2 Fixtures
- BeforeAll: start app with DB_PATH pointing to temp sqlite file.
- BeforeEach: reset DB (drop/create tables) and seed baseline dataset.
- AfterAll: stop app and delete temp DB.

### 9.3 Assertions
- Use APIRequestContext to GET list endpoint and assert:
  - HTTP status (200 for success; 4xx for invalid input if implemented)
  - Response body contains expected expense rows (parse HTML table or stable data attributes).
- Optionally use Playwright Page to load URL and assert visible rows/empty state text.

## 10. Traceability Matrix
- EPMCDMETST-55699:
  - TC-DR-001..006, TC-CAT-001..004, TC-COMB-001..002, TC-SF-001..002
- EPMCDMETST-55700:
  - TC-SRCH-001..005, TC-SF-001..002, TC-RB-001..002

## 11. Reporting
- JUnit 5 reports in CI.
- Attach failed response bodies (truncated) to test logs for diagnosis.

