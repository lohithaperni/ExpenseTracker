JIRA Epic: EPMCDMETST-55731
Epic Title: Add Search, Filtering, and Better List Usability
Repository: lohithaperni/ExpenseTracker
Framework: Playwright Java with JUnit 5
Document: API and Integration Test Plan (focus)

1. Scope and Objectives
- Validate the end-to-end behavior of expense list usability enhancements: filtering by date/category, searching by note text, and improved date display.
- Validate integration between HTTP routes, server-side query handling, SQLite persistence/querying, and HTML rendering.
- Ensure totals and list contents are consistent with applied filters/search.
- Provide automation approach using Playwright Java + JUnit 5 to cover both HTTP-level and UI-integrated flows (server-rendered page).

In Scope (Stories)
- EPMCDMETST-55732 Filter expenses by date range and category
- EPMCDMETST-55733 Search expenses by note text
- EPMCDMETST-55734 Improve list readability (date display)

Out of Scope (explicitly)
- Authentication/authorization, CSRF, edit/delete confirmation, reporting/export.
- Performance/load, accessibility, visual regression (unless formatting affects functional date parsing expectations).

2. System Under Test (SUT) Overview
- Web app: Flask + SQLite expense tracker.
- Primary UI: single server-rendered expense list page (index route).
- Backend supports query parameters (as per gap analysis): start_date, end_date, category, q.
- Data entity (expected): expense with fields amount, category, date, note, id.

Assumptions/Dependencies
- Filtering/search UI controls will submit query parameters to the index route via GET.
- Total spent displayed in UI reflects the currently rendered list (filtered results).
- Date display is transformed for readability but preserves the same calendar date.
- Invalid/missing dates in stored data are handled gracefully in UI.

3. Test Environment and Data
Environments
- Local ephemeral test environment launched for automation runs (Flask server on random free port).
- SQLite database isolated per test run (temp file) to avoid cross-test contamination.

Test Data Strategy
- Seed known expenses via:
  a) direct SQLite inserts in test setup (preferred for deterministic setup), or
  b) UI-driven adds if an add form exists and is stable.
- Ensure coverage across:
  - multiple months
  - multiple categories
  - same note substrings with varying cases
  - empty note
  - boundary dates (start/end)
  - invalid/missing date rows (data integrity tests)

Data Set Example (seed)
- e1: 2026-01-01, category=Food, amount=10.00, note="coffee"
- e2: 2026-01-15, category=Transport, amount=25.00, note="uber ride"
- e3: 2026-02-01, category=Food, amount=40.00, note="groceries"
- e4: 2026-02-10, category=Bills, amount=100.00, note="electric"
- e5: 2026-02-10, category=Food, amount=5.00, note="" (empty)
- e_bad_date: date=NULL or date="not-a-date" (if schema permits) for readability robustness.

4. Test Approach (Playwright Java + JUnit 5)
Harness
- JUnit 5 test lifecycle manages server start/stop.
- Playwright launches Chromium headless by default; support headed mode for debugging.

Key Fixtures
- Start Flask app with test configuration:
  - DB path pointing to temporary SQLite file.
  - Debug disabled.
- Database seeding helper invoked before each test (or per class with cleanup).
- Page object for expense list page:
  - filter controls: start date, end date, category dropdown, apply/reset buttons.
  - search input and apply/reset.
  - expense rows locator, total spent locator, date text locator.

Assertions
- Prefer functional assertions:
  - verify list row count, presence/absence of expected notes/categories.
  - verify total equals sum of displayed amounts (or equals expected known sum).
  - verify query parameters reflected in URL after applying.
- For date readability:
  - verify displayed date matches an expected formatted pattern.
  - verify same calendar date mapping (e.g., parse expected display back if format known).

5. API and Integration Test Scenarios

5.1 Story EPMCDMETST-55732: Filter expenses by date range and category

AC1: Date range filter shows only expenses within range
- Seed expenses spanning outside and inside the range.
- Steps (UI-integrated):
  1) Navigate to /? (expense list)
  2) Set start_date=2026-02-01
  3) Set end_date=2026-02-28
  4) Apply filter
- Expected:
  - Only February expenses are displayed (e3, e4, e5).
  - January expenses not displayed.
  - URL contains start_date and end_date.

Edge/Boundary Cases
- Inclusive boundaries:
  - start_date equals an expense date => included.
  - end_date equals an expense date => included.
- Single-day range:
  - start_date=end_date=2026-02-10 => only expenses on that day.
- Open-ended ranges:
  - start_date set, end_date empty => all expenses >= start_date.
  - end_date set, start_date empty => all expenses <= end_date.
- Invalid range:
  - start_date > end_date.
  - Expected behavior to be defined; test should assert:
    - either server rejects and shows message, or
    - returns empty list but does not error.

AC2: Category filter shows only expenses in that category
- Steps:
  1) Select category=Food
  2) Apply
- Expected:
  - Only Food expenses shown.
  - Total spent equals sum of displayed Food expenses.

Combination Filtering
- Apply date range + category simultaneously.
- Example: Feb + Food => e3 and e5 (and e_bad_date excluded).
- Validate total matches expected.

Total Calculation Integrity
- Verify displayed total reflects only filtered results.
- Optionally compute sum of amounts present in UI and compare to displayed total.

Negative/Robustness
- Unknown category value provided via URL tampering (?category=MadeUp)
  - Expected: either treated as no results or ignored; should not crash.
- SQL injection strings in query params
  - Ensure response is 200 and data is not leaked/altered.

5.2 Story EPMCDMETST-55733: Search expenses by note text

AC1: Keyword search filters to matching notes
- Steps:
  1) Enter q="uber"
  2) Apply search
- Expected:
  - Only e2 visible.
  - Total equals 25.00.
  - URL contains q=uber.

Case Sensitivity
- Search for "UBER" and validate expected behavior (define expectation):
  - if case-insensitive search is intended, results match "uber".
  - if case-sensitive, document and assert accordingly.

Partial Matches
- Search substring "gro" matches "groceries".

Special Characters
- Notes containing punctuation: "electric - Feb"; search "-" or "Feb".
- URL encoding correctness.

AC2: Empty note expenses excluded unless they match
- Search for any keyword; verify e5 (empty note) not shown.
- Search empty string (q=""):
  - Expected: treated as no search; full list returned.

AC3: Clearing search returns full list
- Apply search, then clear q and apply/reset.
- Expected full list visible and total equals all expenses (excluding invalid date handling as applicable).

Combination Search + Filters
- Combine q + category + date range.
- Example: category=Food, q="coffee" returns e1 only (if date range includes Jan).

Negative/Robustness
- Long search string.
- Injection-like payloads in q.
- Ensure stable behavior and no server error.

5.3 Story EPMCDMETST-55734: Improve list readability (date display)

AC1: Dates shown in consistent human-friendly format
- For a set of valid ISO dates, assert displayed date text matches a consistent pattern.
- Because exact format is product-defined, assert one of:
  - matches regex (e.g., "[A-Za-z]{3} [0-9]{1,2}, [0-9]{4}"), or
  - matches configured format string if specified in implementation.

AC2: Displayed value corresponds to same calendar date
- For a known stored date (2026-02-01), verify display still represents Feb 1 2026.
- If display is localized, ensure test runtime uses fixed locale/timezone (UTC) to avoid off-by-one.

AC3: Missing/invalid stored date handled gracefully
- Seed e_bad_date.
- Expected:
  - Page renders successfully (HTTP 200).
  - 해당 row shows a placeholder like "N/A" or "Date unavailable".
  - Other rows unaffected.

6. Non-Functional / Integration Considerations
- Database locking and concurrent reads: basic smoke to ensure list page handles read-only operations.
- Timezone/locale stability for date display:
  - Run tests with explicit TZ=UTC environment variable.
- Input handling in query params:
  - Start/end date validation: ensure no traceback on invalid format.

7. Traceability Matrix
- EPMCDMETST-55731 Epic
  - EPMCDMETST-55732: covered by Section 5.1
  - EPMCDMETST-55733: covered by Section 5.2
  - EPMCDMETST-55734: covered by Section 5.3

8. Automation Implementation Notes (Playwright Java + JUnit 5)
Project Structure (recommended)
- src/test/java/.../e2e/
  - ExpenseListFilterTests.java
  - ExpenseSearchTests.java
  - ExpenseDateDisplayTests.java
- src/test/java/.../support/
  - TestServerExtension.java (JUnit 5 extension)
  - DbSeeder.java
  - ExpenseListPage.java (Page Object)

Execution
- mvn test (or gradle test) runs JUnit 5 suite.
- Use Playwright browsers installed in CI.

Reporting
- JUnit XML output for CI.
- Capture screenshots on failure for debugging.

9. Entry/Exit Criteria
Entry
- Epic implemented and deployed to a testable environment.
- Filter/search UI controls available and stable locators identified.
- Deterministic seed mechanism for DB.

Exit
- All critical path tests passing:
  - filter by date range/category
  - search by note
  - total reflects filtered results
  - date display readable and resilient
- No Sev-1 defects open for these stories.

10. Risks and Mitigations
- Risk: Date format changes break strict assertions
  - Mitigation: validate format via agreed regex and calendar-date equivalence.
- Risk: Tests depend on UI structure changes
  - Mitigation: page object pattern and data-testid attributes (recommended) for stable locators.
- Risk: Locale/timezone differences
  - Mitigation: pin TZ/locale in test runtime and use calendar-date based assertions.
