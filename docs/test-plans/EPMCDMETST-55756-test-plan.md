# Test Plan - EPMCDMETST-55756 - Filtering, Search, and Improved List Usability

Document status: DRAFT
Owner: QA
Automation framework: Playwright Java + JUnit 5
Scope focus: API and integration tests

## 1. Objective
Validate that expense list filtering and searching work correctly end-to-end (HTTP route -> DB query -> HTML rendering), including state persistence of filters, empty-state behavior, and input validation. Ensure existing list behavior is not regressed.

Jira Epic: EPMCDMETST-55756
In-scope stories:
- EPMCDMETST-55757 - Filter expenses by date range and category
- EPMCDMETST-55758 - Search expenses by note keyword

## 2. System Under Test (SUT)
Repository: ExpenseTracker (Flask + SQLite)
Primary route under test:
- GET / (expense list; supports optional query params)

Expected query parameters (based on gap analysis / current backend behavior):
- start_date: YYYY-MM-DD (optional)
- end_date: YYYY-MM-DD (optional)
- category: category string (optional)
- q: keyword for note search (optional)

Data entities:
- Expense: (id, date, category, amount, note)

## 3. Test Approach
### 3.1 Levels
- Integration tests (primary): exercise Flask app via HTTP and verify HTML output and database effects.
- API-level tests (secondary): validate HTTP status codes, parameter parsing, and error handling for invalid filters.

### 3.2 Test execution mode
- Automated: Playwright Java + JUnit 5.
  - Use Playwright request context for HTTP calls when verifying responses at route level.
  - Use Playwright browser automation when verifying rendered UI state and persistence of filter values.
- Manual exploratory (minimal): only for UX copy/formatting review if needed.

### 3.3 Environments
- Local CI-like: run Flask app in test mode with a dedicated SQLite database file per test run or an in-memory DB if supported.
- Deterministic dataset seeded before tests.

### 3.4 Data strategy
Seed a known set of expenses covering:
- Multiple dates across months (including boundary dates)
- Multiple categories (e.g., Food, Travel, Utilities)
- Notes with overlapping keywords and case variations
- Same-day multiple expenses
- Large amounts and decimal amounts

Example seed (illustrative):
- 2026-01-01, Food, 10.00, "coffee"
- 2026-01-15, Travel, 50.00, "taxi to airport"
- 2026-01-31, Food, 20.00, "groceries"
- 2026-02-01, Utilities, 75.50, "electric bill"
- 2026-02-10, Food, 15.00, "Coffee beans"

## 4. In Scope
- Filtering by start_date only, end_date only, both start_date and end_date
- Filtering by category only and combined with date filters
- Search by note keyword (q) only and combined with filters
- Persistence of filter/search values across refresh and when adjusting one filter
- Empty-state messaging for no results
- Regression coverage: unfiltered list displays all expenses and correct total

## 5. Out of Scope
- Authentication/authorization
- CSRF protections
- Export, summaries, category management
- Sorting unless explicitly added by the epic (not in scope per stories)

## 6. Risks and Mitigations
- Risk: backend supports query params but UI controls may differ in name/type.
  - Mitigation: validate both backend behavior (query params) and UI form field names; align tests to implemented contract.
- Risk: date parsing and locale/timezone inconsistencies.
  - Mitigation: enforce YYYY-MM-DD in tests and seed data without time components.
- Risk: SQLite date stored as text; filtering may be lexicographic.
  - Mitigation: add boundary tests and cross-month range tests.

## 7. Test Scenarios and Cases

### 7.1 Filter by date range (EPMCDMETST-55757)
TC-55757-01 Start date only
- Given expenses exist before and after 2026-02-01
- When GET /?start_date=2026-02-01
- Then only expenses with date >= 2026-02-01 are shown
- And total reflects only returned rows

TC-55757-02 End date only
- When GET /?end_date=2026-01-31
- Then only expenses with date <= 2026-01-31 are shown

TC-55757-03 Start and end date inclusive boundaries
- When GET /?start_date=2026-01-01&end_date=2026-01-31
- Then expenses on 2026-01-01 and 2026-01-31 are included

TC-55757-04 Invalid date format rejected or handled safely
- When GET /?start_date=01-31-2026 (or start_date=invalid)
- Then request returns 200 with either:
  - validation message and unfiltered list, OR
  - validation message and empty list
- And application does not error (no 500)

TC-55757-05 start_date after end_date
- When GET /?start_date=2026-02-10&end_date=2026-02-01
- Then user sees a clear message and result set is empty (or filters are ignored) per implementation
- And no 500

### 7.2 Filter by category (EPMCDMETST-55757)
TC-55757-06 Category only
- When GET /?category=Food
- Then only Food expenses are shown

TC-55757-07 Unknown category
- When GET /?category=DoesNotExist
- Then empty-state message shown
- And total is zero

TC-55757-08 Category combined with date range
- When GET /?category=Food&start_date=2026-02-01&end_date=2026-02-28
- Then only Food expenses in Feb 2026 are shown

### 7.3 Filter persistence in UI (EPMCDMETST-55757)
TC-55757-09 Persistence after refresh
- Given user sets filters via UI controls (start_date, end_date, category)
- When page reloads
- Then the filter input values remain populated
- And results remain filtered

TC-55757-10 Adjust one filter preserves others
- Given filters A/B/C applied
- When user changes only category and applies
- Then start_date and end_date values remain

### 7.4 Search by note keyword (EPMCDMETST-55758)
TC-55758-01 Keyword finds matching notes (case-insensitive if implemented)
- When GET /?q=coffee
- Then entries with notes containing "coffee" or "Coffee" are returned per expected behavior

TC-55758-02 Keyword partial match
- When GET /?q=air
- Then "taxi to airport" is returned (if substring match)

TC-55758-03 No matches
- When GET /?q=zzzz
- Then list shows no results
- And an empty-state message indicates no results found

TC-55758-04 Clear search restores full list
- Given a search applied
- When q is cleared in UI and submitted
- Then full list is shown

### 7.5 Combined filter + search
TC-55756-01 Filter + search intersection
- When GET /?category=Food&q=coffee
- Then only Food expenses with coffee in note are shown

TC-55756-02 Filter + search yields none
- When GET /?category=Travel&q=groceries
- Then empty-state message shown

### 7.6 Regression and non-functional checks
TC-REG-01 Unfiltered list returns all seeded expenses
- When GET /
- Then all seeded rows appear

TC-REG-02 Response status and basic performance
- Ensure GET / with and without filters returns HTTP 200
- Ensure page renders within an agreed threshold (e.g., < 2s locally) for seed size

TC-SEC-01 Basic injection safety (defense-in-depth)
- When GET /?q=' OR 1=1--
- Then request returns 200
- And does not return unintended rows beyond substring match behavior
- And app does not error

## 8. Automation Design (Playwright Java + JUnit 5)
### 8.1 Project structure (recommended)
- src/test/java/
  - tests/expenses/FilteringTests.java
  - tests/expenses/SearchTests.java
  - tests/expenses/FilterPersistenceUiTests.java
  - support/TestServerExtension.java (start/stop Flask app for tests)
  - support/DbSeeder.java (seed SQLite deterministically)

### 8.2 Test fixtures
- BeforeAll: start Flask server on random free port.
- BeforeEach: reset SQLite DB and seed dataset.
- AfterAll: stop server and delete temp DB.

### 8.3 Selectors and assertions
- Prefer stable selectors (data-testid) if added; otherwise use semantic selectors:
  - form inputs by name attribute: input[name='start_date'], input[name='end_date'], select[name='category'], input[name='q']
  - expense rows: table rows excluding header
- Assertions:
  - Count of rows
  - Presence/absence of known notes/categories/dates
  - Total amount displayed matches expected filtered sum
  - Empty-state text
  - Input values retained after navigation/refresh

### 8.4 Handling variability
- If date display formatting changes (e.g., localized), assert via presence of known note/category and row count rather than exact date string, unless formatting is a requirement.

## 9. Entry and Exit Criteria
Entry:
- Stories implemented in code and deployed to test environment
- UI controls available for start_date/end_date/category/q or documented alternate contract

Exit:
- All automated tests passing
- No open Critical/High defects for filtering/search
- Regression suite green (add/list/delete baseline)

## 10. Defect Reporting Guidelines
For any failure, capture:
- Full request URL (including query params)
- HTML snapshot or Playwright trace
- Seed dataset version
- Expected vs actual row set and totals

## 11. Traceability Matrix
- EPMCDMETST-55757:
  - TC-55757-01..10, TC-55756-01..02
- EPMCDMETST-55758:
  - TC-55758-01..04, TC-55756-01..02
