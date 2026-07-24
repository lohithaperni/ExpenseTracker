# API and Integration Test Plan
# Epic: EPMCDMETST-55685 - Filtering, Search, and Summaries for Better Insights

Document purpose
- Define API and integration tests for filtering, searching, sorting consistency, and weekly/monthly summaries.
- Validate input handling, error behavior, and data integrity using Flask app + SQLite.

In scope
- Filter expenses by date range and category (Story EPMCDMETST-55686)
- Search expenses by note text (Story EPMCDMETST-55687)
- Weekly and monthly spending summaries with filters (Story EPMCDMETST-55688)

Out of scope
- Authentication/authorization, CSRF, multi-user separation (covered in other epics)
- UI-only behavior not backed by server responses (except as observed via HTML integration responses)

Assumptions (update as implementation finalizes)
- Expenses stored with fields: id, amount (numeric), category (string), date (YYYY-MM-DD), note (string/nullable)
- Existing endpoint(s): GET / (renders list + total) and POST endpoints for add/delete
- New functionality may be added as either:
  - Query params on existing list endpoint (e.g., GET /?start=...&end=...&category=...&q=...)
  - Dedicated endpoints for summaries (e.g., GET /summary/weekly, GET /summary/monthly)
- Responses may be HTML (server-rendered templates). Tests will treat them as integration tests, asserting on status codes and presence of expected content. If JSON endpoints exist, add API assertions.

Test levels
- Integration tests (primary): Flask test client hitting routes, using a test SQLite DB.
- API contract tests (conditional): if JSON endpoints are introduced.
- DB verification: assert rows returned/aggregations match seeded DB data.

Test environment
- Python: version per repo constraints
- Test framework: pytest
- Flask: app configured for TESTING
- DB: temporary SQLite file or in-memory SQLite with schema setup per test module
- Deterministic dataset seeded for each test case

Data setup
Seeded expenses (example baseline; adjust categories and amounts to match app)
- 2026-01-01 Food     10.00 note="coffee"
- 2026-01-03 Travel   25.50 note="uber airport"
- 2026-01-10 Food      5.25 note="snack"
- 2026-02-01 Bills    75.00 note="internet"
- 2026-02-05 Travel   12.00 note="bus"
- 2026-02-15 Other   100.00 note="gift"

Ensure at least:
- Multiple categories
- Multiple weeks within a month
- Multiple months
- Notes with varied casing and substrings

Traceability matrix
- EPMCDMETST-55686:
  - TC-FLT-001..TC-FLT-020
- EPMCDMETST-55687:
  - TC-SRC-001..TC-SRC-015
- EPMCDMETST-55688:
  - TC-SUM-001..TC-SUM-025

------------------------------
Story EPMCDMETST-55686: Filter expenses by date range and category

Functional integration tests
TC-FLT-001 No filters returns full list
- Given baseline dataset
- When GET list without filter params
- Then status 200 and all seeded expenses appear

TC-FLT-002 Filter by start date inclusive
- When start=2026-02-01
- Then only expenses with date >= 2026-02-01 appear

TC-FLT-003 Filter by end date inclusive
- When end=2026-01-31
- Then only expenses with date <= 2026-01-31 appear

TC-FLT-004 Filter by start and end date range
- When start=2026-01-02 and end=2026-02-05
- Then only expenses in the closed interval appear

TC-FLT-005 Filter by category exact match
- When category=Food
- Then only Food expenses appear

TC-FLT-006 Filter by category with URL encoding
- When category contains space or special chars (if categories can)
- Then filter applies and server returns 200

TC-FLT-007 Combined date range + category
- When start=2026-01-01 end=2026-01-31 category=Food
- Then only January Food items appear

Validation and error handling
TC-FLT-008 Invalid date format rejected
- When start=01-31-2026
- Then 400 or 200 with visible validation error message
- And no filter is applied (list remains unfiltered) OR filter defaults per spec

TC-FLT-009 End date before start date
- When start=2026-02-10 end=2026-02-01
- Then error message shown and filter not applied

TC-FLT-010 Unknown category handling
- When category=NotARealCategory
- Then either 400 or empty results, per spec
- And no server error

TC-FLT-011 Empty filter params
- When start="" end="" category=""
- Then treated as no filter; returns full list

TC-FLT-012 Extremely large date range
- When start=1900-01-01 end=2100-01-01
- Then returns all rows, no timeout, status 200

Security and robustness
TC-FLT-013 SQL injection attempt in category
- When category="Food' OR 1=1 --"
- Then request does not error; results are not broadened beyond Food (or returns none)

TC-FLT-014 XSS attempt in query params reflected in HTML
- When category contains "<script>"
- Then response escapes content and does not include raw script tag

Edge cases
TC-FLT-015 Boundary inclusion
- Ensure expenses on start and end dates are included

TC-FLT-016 Single-day range
- start=end=2026-02-01
- Then only that date's expenses

TC-FLT-017 Leap day handling (if relevant)
- start=2024-02-29 end=2024-02-29
- Then valid parse; returns matching rows if any

Performance-related integration checks
TC-FLT-018 Large dataset pagination behavior (if added)
- Seed 1000+ rows
- Apply date filter
- Assert response time within threshold and rows returned correspond to filter

TC-FLT-019 Stable sorting with filters
- With and without filters
- Then ordering remains consistent with spec (e.g., date desc, id desc)

TC-FLT-020 Filter state preservation (if server returns current filters)
- Apply filters
- Then response includes current filter values in form inputs (if applicable)

------------------------------
Story EPMCDMETST-55687: Search expenses by note text

Functional integration tests
TC-SRC-001 Keyword matches substring
- When q="uber"
- Then only notes containing "uber" appear

TC-SRC-002 Keyword case-insensitive search
- When q="UBER"
- Then same results as q="uber" (if spec says case-insensitive)

TC-SRC-003 Multiple words
- When q="uber airport"
- Then matches note containing both words or exact substring per spec

TC-SRC-004 Special characters in search
- When q="coffee"
- Then matches expected rows

TC-SRC-005 Search with no matches
- When q="nonexistent"
- Then response shows empty state message and list is empty

TC-SRC-006 Clear search shows full list
- When q="" (or param removed)
- Then full list returned

Combined behavior
TC-SRC-007 Search + category filter combined
- When q="bus" and category=Travel
- Then only Travel entries with note match

TC-SRC-008 Search + date range combined
- When q="gift" start=2026-02-01 end=2026-02-28
- Then matching row appears

Validation and robustness
TC-SRC-009 Very long search term
- When q length > 500
- Then request handled gracefully (400 or truncated) and no 500

TC-SRC-010 SQL injection attempt in q
- When q="%' OR 1=1 --"
- Then request does not error and does not return all rows unexpectedly

TC-SRC-011 Wildcards behavior
- If LIKE is used, confirm that '%' '_' are treated per spec (escaped or honored)

TC-SRC-012 XSS attempt in q reflected in HTML
- When q="<img src=x onerror=alert(1)>"
- Then response escapes content

TC-SRC-013 Unicode keyword
- When q contains unicode (e.g., "cafe")
- Then request works and does not error

TC-SRC-014 Note null handling
- If note is nullable, ensure rows without note do not cause errors

TC-SRC-015 Input normalization
- When q="  uber  "
- Then trimming behavior per spec; results still match

------------------------------
Story EPMCDMETST-55688: View monthly and weekly spending summaries

Summary expectations
- Weekly summary groups expenses by week (define week start, e.g., Monday; document in implementation)
- Monthly summary groups expenses by calendar month (YYYY-MM)
- Summaries reflect applied filters (category/date range/search)

API/integration tests (HTML or JSON)
TC-SUM-001 Weekly summary basic grouping
- Given baseline dataset spans multiple weeks
- When GET weekly summary endpoint (or list page with summary mode)
- Then totals per week match expected sums

TC-SUM-002 Monthly summary basic grouping
- When GET monthly summary
- Then totals per month match expected sums

TC-SUM-003 Weekly summary ordering
- Then weeks displayed in chronological or reverse chronological order per spec

TC-SUM-004 Monthly summary ordering
- Then months displayed in chronological or reverse chronological order per spec

TC-SUM-005 Weekly summary with category filter
- When category=Travel
- Then weekly totals only include Travel

TC-SUM-006 Monthly summary with category filter
- When category=Food
- Then monthly totals only include Food

TC-SUM-007 Weekly summary with date range filter
- When start/end applied
- Then only weeks overlapping range include included expenses per inclusion rules

TC-SUM-008 Monthly summary with date range filter
- When start/end applied
- Then only months in range include included expenses

TC-SUM-009 Summary with search filter
- When q applied
- Then totals reflect only matching notes

TC-SUM-010 Combined filters reflected in summary
- When category + date range + q
- Then totals match filtered set

Validation/error handling
TC-SUM-011 Invalid date range rejected in summary views
- When start>end
- Then clear error and no incorrect totals

TC-SUM-012 Invalid date format in summary views
- Then error handling matches list filter behavior

Edge cases
TC-SUM-013 No expenses in period
- When start/end yields no rows
- Then summary shows empty state rather than error

TC-SUM-014 Single expense period
- Then summary has a single group with correct total

TC-SUM-015 Amount precision
- Use amounts with cents
- Assert totals are correct to 2 decimal places; avoid float rounding errors (prefer Decimal)

TC-SUM-016 Negative/zero amounts presence
- If system allows, verify included/excluded per spec

Week boundary tests (define week start)
TC-SUM-017 Expense on week boundary day
- Ensure it is counted in correct week

TC-SUM-018 Cross-year week number boundary
- Seed dates around year end
- Ensure grouping keys and labels are correct

Contract tests (if JSON endpoints)
TC-SUM-019 Weekly JSON schema
- Assert response fields: period key/label, total, currency (if any)

TC-SUM-020 Monthly JSON schema
- Assert response fields and types

Robustness
TC-SUM-021 SQL injection attempt in summary filter params
- Ensure parameterized queries

TC-SUM-022 Large dataset summary performance
- Seed 10k rows
- Summary endpoint responds within threshold

Consistency checks
TC-SUM-023 Sum of grouped totals equals total of filtered expenses
- Compare summary totals to list filtered total

TC-SUM-024 Summary matches list when grouped by month/week
- Spot-check groups via DB queries

TC-SUM-025 Sorting consistency under filters
- Ensure groups sorted deterministically

------------------------------
Non-functional checks
- Logging: verify invalid input does not emit stack traces in response
- HTTP status codes: 200 for normal, 400 for validation errors if API style used
- Content-type: text/html; charset=utf-8 or application/json

Automation notes
- Prefer factory pattern create_app(test_config) and dependency inject DB path
- Each test should:
  - Create schema
  - Seed deterministic dataset
  - Run request
  - Assert response + DB state

Exit criteria
- All test cases above implemented and passing in CI
- No known P1 defects in filtering/search/summaries
- Validation and error messaging meet acceptance criteria without 500 errors
