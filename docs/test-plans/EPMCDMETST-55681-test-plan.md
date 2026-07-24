Test Plan: EPMCDMETST-55681 - Search, Filter, and Reporting
Repository: lohithaperni/ExpenseTracker
Scope: API and integration tests for filtering, searching, and monthly summary features.
Stories in scope:
- EPMCDMETST-55682 Filter by date range and category
- EPMCDMETST-55683 Search expenses by note text
- EPMCDMETST-55684 Monthly summary view

1. Objectives
- Validate correctness of new endpoints/parameters for filtering, searching, and monthly summaries.
- Ensure backward compatibility with existing add/list/delete flows.
- Verify input validation, error handling, and security basics for new query parameters.
- Ensure deterministic behavior across SQLite and Flask test client.

2. Assumptions and Interfaces
- Existing app exposes routes similar to:
  - GET / (lists expenses)
  - POST /add (adds expense)
  - POST /delete/<id> (deletes expense)
- Enhancements may be implemented as one of:
  A) Extend GET / with query params: start_date, end_date, category, q
  B) Add dedicated endpoints: GET /expenses with filters/search
  C) Add monthly summary endpoint: GET /summary/monthly?month=YYYY-MM
- This plan tests behavior independent of exact route naming; map to final routes during implementation.

3. Test Environment
- Framework: pytest
- Client: Flask test_client()
- Database: SQLite
  - Prefer in-memory or temporary file per test (tmp_path)
  - Migrations/schema setup via app init function or helper.
- Data seeding: helper to insert expenses with known ids, dates, categories, notes.
- Timezone: treat dates as local naive dates; avoid datetime tz complexity.

4. Test Data
Seed dataset (example):
- (id1) 2026-01-01 category=Food amount=10.00 note="groceries"
- (id2) 2026-01-05 category=Transport amount=25.00 note="bus pass"
- (id3) 2026-01-31 category=Food amount=7.50 note="coffee"
- (id4) 2026-02-01 category=Food amount=12.00 note="lunch"
- (id5) 2026-02-15 category=Utilities amount=60.00 note="electric bill"
- (id6) 2026-02-20 category=Food amount=22.00 note="Groceries at Market" (mixed case)

5. API/Integration Test Cases

5.1 Filtering by date range (EPMCDMETST-55682)
FDR-01 Inclusive range returns only matching expenses
- Request: GET list endpoint with start_date=2026-01-01 end_date=2026-01-31
- Expect: includes id1,id2,id3; excludes id4,id5,id6

FDR-02 Single-day range
- start_date=end_date=2026-01-05
- Expect: only id2

FDR-03 Open-ended range (start only)
- start_date=2026-02-01
- Expect: id4,id5,id6

FDR-04 Open-ended range (end only)
- end_date=2026-01-31
- Expect: id1,id2,id3

FDR-05 Invalid date format rejected
- start_date=01-31-2026
- Expect: 400 or validation error message; no server error; response indicates invalid date.

FDR-06 Start date after end date
- start_date=2026-02-10 end_date=2026-02-01
- Expect: 400 or empty results with explicit message per product decision; must be consistent.

FDR-07 Boundary behavior
- Ensure 2026-01-31 included when end_date=2026-01-31

5.2 Filtering by category (EPMCDMETST-55682)
CAT-01 Category filter exact match
- category=Food
- Expect: id1,id3,id4,id6

CAT-02 Category combined with date range
- category=Food start_date=2026-01-01 end_date=2026-01-31
- Expect: id1,id3 only

CAT-03 Unknown category returns empty (or all) consistently
- category=NonExistent
- Expect: empty results (recommended) with optional message; no server error.

CAT-04 Category value with leading/trailing spaces
- category=" Food "
- Expect: either trimmed match to Food or validation error; define expected behavior and assert.

5.3 Clear filters behavior (EPMCDMETST-55682)
CLR-01 No filter params returns full list
- GET list endpoint without params
- Expect: all seeded ids appear

CLR-02 Clearing filters via empty params
- category="" start_date="" end_date=""
- Expect: treated as no filters OR validation error; should not crash; define expectation.

5.4 Search by note text (EPMCDMETST-55683)
SRCH-01 Basic substring match
- q=gro
- Expect: matches id1 (groceries) and id6 (Groceries at Market)

SRCH-02 Case-insensitive search
- q=GROCERIES
- Expect: matches id1 and id6

SRCH-03 Search term matches none
- q=unmatched
- Expect: no results and clear no-results message (UI) or empty list (API)

SRCH-04 Search with special characters
- q="electric bill"
- Expect: matches id5; ensure query safely handled (no SQL errors)

SRCH-05 SQL injection attempt does not broaden results
- q="' OR 1=1 --"
- Expect: no server error; results only those actually containing that exact string (likely none)

SRCH-06 Search combined with category/date filters
- q=gro category=Food start_date=2026-02-01 end_date=2026-02-28
- Expect: id6 only

5.5 Monthly summary view (EPMCDMETST-55684)
MS-01 Valid month returns total
- month=2026-01
- Expect total = 10.00 + 25.00 + 7.50 = 42.50

MS-02 Valid month returns totals grouped by category
- month=2026-02
- Expect categories:
  - Food: 12.00 + 22.00 = 34.00
  - Utilities: 60.00
  - Total: 94.00

MS-03 Month with no expenses
- month=2025-12
- Expect total 0 and empty-state indicator

MS-04 Invalid month format rejected
- month=2026/01
- Expect 400/validation error

MS-05 Boundary dates included correctly
- Ensure 2026-01-31 included in January summary, 2026-02-01 included in February.

MS-06 Rounding/precision behavior
- Seed amounts with 2 decimal places; verify totals match expected rounding (use Decimal in code/tests if possible).

5.6 Regression coverage (existing flows still work)
REG-01 Add expense then filter finds it
- Add new expense dated within filter range then GET with filter; expect included.

REG-02 Delete expense affects filtered list and summary
- Delete id3; January filter and January summary should reflect removal.

REG-03 List endpoint default ordering remains consistent
- If ordering specified (date desc), ensure filters/search preserve ordering.

6. Non-Functional and Negative Testing
- PERF-01 Filter/search on moderate dataset (e.g., 1k rows) completes under acceptable threshold in CI (smoke performance, not load test).
- ERR-01 Server returns 4xx for invalid params, not 500.
- SEC-01 Ensure query params are parameterized in SQL (validate by injection test above).
- COMP-01 Backward compatibility: calling list endpoint with unknown params does not crash.

7. Traceability Matrix
- EPMCDMETST-55682: FDR-01..07, CAT-01..04, CLR-01..02, REG-01..03
- EPMCDMETST-55683: SRCH-01..06, SEC-01, ERR-01
- EPMCDMETST-55684: MS-01..06, REG-02, ERR-01

8. Execution and Reporting
- Test suite runs in CI: pytest -q
- Separate markers:
  - @pytest.mark.integration for DB-backed tests
  - @pytest.mark.api for endpoint contract tests
- Artifacts: JUnit XML optional.

9. Entry/Exit Criteria
Entry:
- Routes/params implemented for filter/search/summary
- Test environment supports isolated SQLite db per test
Exit:
- 100% pass on required test cases
- No Sev1 defects open for incorrect filtering/search/summary totals

10. Risks and Mitigations
- Risk: Date parsing inconsistencies. Mitigate with explicit YYYY-MM-DD and YYYY-MM validation.
- Risk: Floating point rounding errors. Mitigate by using Decimal and asserting formatted totals.
- Risk: UI-only implementation. Mitigate by testing server-rendered HTML content presence of expected rows and messages if no JSON API exists.
