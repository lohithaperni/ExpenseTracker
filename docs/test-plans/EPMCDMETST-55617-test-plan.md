# Test Plan: Search, Filtering, and Reporting
Jira Epic: EPMCDMETST-55617
Version: 1.0

## 1. Test Strategy

### 1.1 Scope
This plan covers API and integration testing for the Search, Filtering, and Reporting epic. Focus areas:
- Filtering expenses by date range and category.
- Searching expenses by note text.
- Monthly summary reporting and drill-down to underlying expenses.
- Integration between HTTP routes (Flask), persistence layer (SQLite), and templates/JSON responses.

### 1.2 Assumptions and constraints
- Current application is a Flask + SQLite app. Existing capabilities include add/list/delete and total.
- Enhancements for E2 may be implemented as:
  - New query parameters on existing list endpoint/route, or
  - New API endpoints (e.g., /api/expenses, /api/summary/monthly), or
  - Server-rendered UI route with query params.
- Exact endpoint paths, request/response formats, and auth/CSRF are not in scope for this epic unless implemented.
- Date storage is assumed ISO-8601 string (YYYY-MM-DD) or SQLite date-compatible string.

### 1.3 Test levels
- API tests (HTTP-level): validate status codes, validation, filtering/search semantics, ordering, pagination if present.
- Integration tests (app + DB): verify SQL query logic, parameter binding, and data correctness.
- Contract tests (if JSON API exists): schema validation for responses.

### 1.4 Test types
- Positive/functional tests: expected filter/search/report behavior.
- Negative tests: invalid params, empty results, boundary date conditions.
- Security-adjacent tests: SQL injection resistance via parameterized queries.
- Non-functional light checks: response time for typical datasets, determinism of ordering.

### 1.5 Tooling
- Pytest + Flask test client.
- Temporary SQLite database per test (in-memory or temp file).
- Factories/fixtures to create expenses across months/categories.

### 1.6 Entry/exit criteria
Entry:
- Epic endpoints/routes implemented.
- DB migrations/schema updated if needed (e.g., category field).
Exit:
- All mapped test cases executed.
- No Critical/High severity failures.

## 2. User Story to Test Case Mapping

Legend: TC-E2-S{n}-{m}

### E2-S1 Filter Expenses by Date Range and Category
- TC-E2-S1-01: Filter by date range inclusive boundaries (start and end dates included).
- TC-E2-S1-02: Filter by date range where start > end returns 400 (or empty per design).
- TC-E2-S1-03: Filter by open-ended range (start only).
- TC-E2-S1-04: Filter by open-ended range (end only).
- TC-E2-S1-05: Filter by category exact match.
- TC-E2-S1-06: Filter by category case sensitivity behavior (define expected: case-insensitive recommended).
- TC-E2-S1-07: Combined date range + category filter intersection.
- TC-E2-S1-08: Clear filters returns full list.
- TC-E2-S1-09: Unknown category yields empty list.
- TC-E2-S1-10: Invalid date format returns 400 with message.

### E2-S2 Search Expenses by Note Text
- TC-E2-S2-01: Search term matches substring in note (contains semantics).
- TC-E2-S2-02: Search is case-insensitive (or validate documented behavior).
- TC-E2-S2-03: Empty note is not returned unless it matches term (should not match any non-empty term).
- TC-E2-S2-04: No matches returns empty state indicator (API: empty array + metadata; UI: message).
- TC-E2-S2-05: Search with special chars and SQL wildcards (% _) behaves safely and correctly.
- TC-E2-S2-06: Search term length limits (very long string) handled gracefully.

### E2-S3 View Monthly Summary Breakdown
- TC-E2-S3-01: Monthly summary groups totals by month for multi-month dataset.
- TC-E2-S3-02: Summary totals equal sum(amount) for each month (precision rules).
- TC-E2-S3-03: Drill-down for a selected month returns only expenses in that month.
- TC-E2-S3-04: Month with no expenses omitted or shown as zero consistently (verify chosen behavior).
- TC-E2-S3-05: Invalid month selection returns 400.
- TC-E2-S3-06: Ordering of months (descending by month) deterministic.

## 3. API Endpoint Coverage

Note: Exact routes may differ. Map these tests to actual implemented endpoints.

### 3.1 Expense listing with filters/search
Potential endpoint patterns:
- GET / (server-rendered) with query params: start_date, end_date, category, q
- or GET /api/expenses?start_date=...&end_date=...&category=...&q=...

Coverage matrix:
- Query params:
  - start_date: valid ISO date, invalid format, missing
  - end_date: valid ISO date, invalid format, missing
  - category: existing, unknown, empty
  - q (note search): normal term, case variants, special chars, empty
- Responses:
  - 200 with list
  - 200 with empty list and empty-state indicator
  - 400 for invalid params (dates, month)

### 3.2 Monthly summary
Potential endpoint patterns:
- GET /summary/monthly
- or GET /api/summary/monthly
- Drill-down:
  - GET /api/summary/monthly/{yyyy-mm}
  - or GET /api/expenses?month=yyyy-mm

Coverage:
- 200 summary list
- 200 drill-down list
- 400 invalid month

### 3.3 Data consistency checks
- After applying filters/search, verify total spent displayed/calculated corresponds to the filtered set if UI implements filtered totals.
- Verify that filtering/search does not mutate data (idempotent GET).

## 4. Test Data Requirements

### 4.1 Core dataset
Create expenses across:
- Multiple months: e.g., 2026-01, 2026-02, 2026-03
- Multiple categories: Food, Travel, Utilities
- Notes:
  - Notes with shared substrings: "coffee", "Coffee shop", "COFFEE beans"
  - Notes with SQL wildcard characters: "50% off", "_underscore_"
  - Empty note
- Boundary dates:
  - First and last day of month
  - Leap day if supported (2024-02-29)

### 4.2 Amounts
- Normal positive amounts
- Zero amount behavior (allow or reject; align with requirements)
- Large amount to check numeric handling

### 4.3 Expected ordering
Define and validate ordering (e.g., newest first by date then id). Seed data should include same-date rows to validate secondary sort.

## 5. Test Execution Plan

### 5.1 Environments
- Local CI with Python version per repo.
- SQLite in-memory or temp file.

### 5.2 Execution steps
1. Setup test DB schema.
2. Seed fixtures.
3. Run API tests for list with filters/search.
4. Run monthly summary and drill-down tests.
5. Run negative/edge tests.
6. Generate test report (pytest output + JUnit if CI).

### 5.3 Automation and CI gates
- Run on each PR.
- Fail build on any failed test.

## 6. Risks and Coverage Gaps

### 6.1 Risks
- Ambiguity in endpoint design (UI query params vs JSON APIs) may require remapping tests.
- Date handling in SQLite can be error-prone if stored as text; ensure consistent ISO format.
- Category field may not exist yet; tests assume it is persisted and queryable.

### 6.2 Coverage gaps
- Performance for very large datasets (10k+ rows) not fully covered; add load/perf tests if needed.
- Security controls (auth/CSRF) are in Epic E3; not covered here unless implemented.
- UI-only behaviors (empty-state message rendering) require UI/integration tests beyond API level.
