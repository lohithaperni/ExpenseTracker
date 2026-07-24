# Test Plan: Filtering, Search, and Summaries for Better Insights (EPMCDMETST-55689)

Jira Epic: EPMCDMETST-55689
Stories:
- EPMCDMETST-55690 Filter expenses by date range and category
- EPMCDMETST-55691 Search expenses by note text
- EPMCDMETST-55692 View weekly and monthly spending summaries

Owner: QA
Type: API + Integration Test Plan
Repository: lohithaperni/ExpenseTracker
Branch Under Test: main


## 1. Objectives

- Validate end-to-end behavior for expense listing with filters (date range, category) and search (note text).
- Validate correctness of weekly/monthly summaries versus underlying stored expenses.
- Validate input validation and error handling for query parameters.
- Provide regression coverage for query logic and database interactions.


## 2. In Scope

- Server routes that power list view filtering/search and any summary views/endpoints.
- SQLite query correctness, sorting, pagination (if added), and edge-case handling.
- Input validation for query parameters (dates, category, search string).
- Security-relevant negative testing for query parameters (basic injection resistance).


## 3. Out of Scope

- UI/visual layout, CSS, responsiveness.
- Authentication/authorization (not part of this Epic).
- Performance/load testing beyond basic large-dataset sanity checks.


## 4. Assumptions and Open Questions

Assumptions:
- Expenses have fields: id, amount, category, date, note.
- Filtering/search is implemented via query parameters on list route (e.g., GET /?start=YYYY-MM-DD&end=YYYY-MM-DD&category=Food&q=coffee).
- Summaries are implemented as either:
  A) separate endpoints (e.g., GET /summary?period=weekly|monthly), or
  B) the main page renders summaries when requested.

Open questions to confirm during implementation:
- Exact parameter names and validation rules (inclusive/exclusive date range, max search length).
- Sorting default (date desc? id desc?) and tie-breaking.


## 5. Test Approach

### 5.1 Test Levels

- Integration tests (preferred): Flask app test client + temporary SQLite database.
- API contract tests: validate status codes, content type, and schema of responses (HTML or JSON).
- Data-layer verification: confirm DB state is unchanged by read-only endpoints.

### 5.2 Test Tooling

- pytest
- Flask test client
- SQLite in-memory or temp file DB


## 6. Environments

- Local CI: run tests against app in testing mode.
- DB: isolated per test (fixture creates schema and seeds rows).


## 7. Test Data

Baseline seed dataset (examples; adjust to match actual schema):
- Multiple categories: Food, Transport, Utilities
- Multiple dates across at least 3 months and 3 weeks
- Notes:
  - Some NULL/empty
  - Some with mixed case keywords ("Coffee", "coffee")
  - Some with punctuation and spaces
- Amounts:
  - Typical values (10.50)
  - Boundary values (0.01, very large like 999999.99)


## 8. Coverage Matrix (Stories to Test Scenarios)

### Story EPMCDMETST-55690: Filter by date range and category

Positive:
- Date range inclusive filter returns only records between start and end.
- Category-only filter returns only that category.
- Combined date+category filter returns intersection.

Negative/Validation:
- start > end returns clear validation message and does not apply filter.
- Invalid date format returns 400 (or renders error message) and does not apply filter.
- Unknown category returns empty list (or validation error per spec).

Edge:
- Missing start or missing end behaves as open-ended range (if supported) or validation error.
- Timezone neutrality: ensure date parsing uses date only.


### Story EPMCDMETST-55691: Search by note text

Positive:
- Keyword search matches substring in note.
- Case-insensitive match (if specified) or confirm case-sensitive behavior.

Negative:
- Search term with SQL wildcard characters does not break query.
- Very long search term is handled (truncated/rejected) per validation.

Edge:
- Expenses with NULL/empty notes are excluded unless they match.
- Clearing search returns full list.


### Story EPMCDMETST-55692: Weekly and monthly summaries

Positive:
- Weekly summary aggregates totals per ISO week (or defined week start) and matches DB sums.
- Monthly summary aggregates totals per YYYY-MM.

Negative/Edge:
- No expenses in period yields zero totals without error.
- Floating/decimal precision: totals are accurate to 2 decimals.
- Summaries remain correct when filters/search are applied (if summaries are filter-aware).


## 9. Detailed Test Cases (API/Integration)

### 9.1 List endpoint filtering/search

TC-FLT-001: Filter by date range
- Seed expenses across dates.
- Call GET list with start=2026-01-01, end=2026-01-31.
- Assert only January expenses present; total (if shown) equals sum of returned.

TC-FLT-002: Filter by category
- GET list with category=Food.
- Assert only Food rows.

TC-FLT-003: Combined filters
- GET with start/end/category.
- Assert intersection.

TC-FLT-004: Invalid date range
- GET with start=2026-02-01, end=2026-01-01.
- Assert validation message; confirm full list remains or filter not applied (per spec).

TC-FLT-005: Invalid date format
- GET with start=abc.
- Assert 400 or error message; DB unchanged.

TC-SRCH-001: Search keyword match
- GET with q=coffee.
- Assert only notes containing coffee.

TC-SRCH-002: Search excludes null notes
- Ensure rows with NULL note not returned unless match.

TC-SRCH-003: Clear search
- First call with q=coffee then call with no q.
- Assert full list returns.

TC-SAFE-001: SQL injection-like string
- GET with q="%' OR 1=1 --".
- Assert no server error; results are not all rows unless true matches.


### 9.2 Summary endpoints/views

TC-SUM-W-001: Weekly summary correctness
- Seed expenses across 3 weeks.
- Call weekly summary view.
- Assert week buckets and totals equal sums from DB.

TC-SUM-M-001: Monthly summary correctness
- Seed expenses across 3 months.
- Call monthly summary view.
- Assert month buckets and totals.

TC-SUM-000: Empty dataset
- No expenses.
- Call summary.
- Assert zeros and no crash.


## 10. Non-Functional Checks

- Basic robustness: no 500s for malformed parameters.
- Security sanity: parameterized queries used; no reflected error stack traces in responses.
- Determinism: tests do not depend on current date/time.


## 11. Exit Criteria

- 100% pass rate in CI for test suite.
- All acceptance criteria mapped to at least one automated integration test.
- No high severity defects open related to incorrect filtering/search/summaries.


## 12. Traceability

- Epic: EPMCDMETST-55689
- Stories: EPMCDMETST-55690, EPMCDMETST-55691, EPMCDMETST-55692
