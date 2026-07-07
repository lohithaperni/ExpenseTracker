# Implementation Plan: Epic 2 ‚Äì Discoverability & Insights (Search, Filters, Summaries)

"JIRA", Epic: [EPMCDMETST-53839](https://jiraeu.epam.com/browse/EPMCDMETSST-53839)

## 1) Overview

This epic improves the expense list page to help users find relevant transactions quickly and get basic spending insights over time. It introduces (1) filters, (2) weekly/monthly summaries, and (3) a readable date display.

### Goals / Objectives
- Filter the expense list by date range and category.
- Display weekly and monthly totals (with a clear ‚Äúno data‚Äù state).
- Improve date readability with a consistent format across renders.

### Out of Scope (for this epic)
- Full-text search across notes and categories.
- Auth-level user segregation and CSRF hardening (tracked in Epic 4).
- Export (CSV/PDF)  (tracked in Epic 3).

## 2) Scope and Deliverables by Story

**Story 2.1: Filter expenses by date range and category**
[EPMCDMETST-53840](https://jiraeu.epam.com/browse/EPMCDMETST-53840)

Deliverables:
- UI.. Add filter panel or inline controls on the list view:
  - Date from (start)
  - Date to (end)
  - Category select (allow *All* as default)
- Backend: Support query parameters (e.g. ?from=YYYY-MM-DD&to=YYYY-MM-DD&category=Foo).
- Database: Use existing expenses table; no schema change expected for basic filters.
- Tests: Integration tests for filter combinations (date range, category only, both, and clear).

Acceptance Criteria trace:
- [ ] Date range applies and results are restricted.
- [ ] Category filter applies and results are restricted.
 
- [ ] Clear filters restores full list.


### Story 2.2: View monthly and weekly spending summaries
[EPMCDMETST-53841](https://jiraeu.epam.com/browse/EPMCDMETST-53841)

Deliverables:
- UI.. Summary component on the list page:
  - "Current Week" total
  - "Current Month" total
  - No-data state e.g. ‚ÄúNo expenses in this period‚Äù
- Backend:
  - Add a small aggregation layer to compute totals for a given week and month
  - Support parameters for selecting other weeks/months (e.g. week_of|month)
  - Return zero totals when no records match
- Tests:
  - Unit tests for date window calculations
  - Integration tests for zero-data periods
  - Regression test: summary doesn't break list view

Acceptance Criteria trace:
- [ ] Shows week/month totals for current period.
- [ ] Supports selecting other periods and updates the summary.
- [ ] No-data period shows zero and a clear indication.

### Story 2.3: Improve date readability in the list
[EPMCDMETST-53842](https://jiraeu.epam.com/browse/EPMCDMETST-53842)

Deliverables:
- UI.. Display dates in a single consistent format (recommended: global format like "dd Mmm yyyy" or "yyyy-mm-dd" with locale options).
  - Recommended: store internally as ISO- 8601 (YYYY-MM-DD).
- Backend: Ensure dates are stored in a stable format (ISO string or date type) and formatted in template.
- Tests: Ensure rendering date format is consistent for existing and new expenses.

Acceptance Criteria trace:
- [ ] All dates display in a consistent, human-friendly format.
- [ ] New entries match existing formatting rules.
- [ ] Dates remain consistent under sort/filter.


## 3) Technical Approach (High-Level)

**Architecture suggestions (minimal change approach)**
- Keep rendering server-side (Flask templates).
- Extend the existing list endpoint to accept query params and return filtered results.
- Aggregates (week/month) can be computed in SQL or in Python. Prefer SQL aggregation for performance and simplicity:
  - Month: group by year-month
  - Week: calendar week vs. rolling 7 days; define behavior explicitly (this plan assumes calendar week, Mon-Sun)
- Date formatting: handle at render time (Jinja filter or Python formatting).


### APIs / Routes (end-user visible)
- GET / (expense list)
  - Support query params: from, to, category, month=2026-07, week_of=2026-07-07
  - Returns: expenses + computed summaries for the selected periods
- Optional: GET /api/summary
  - If the app is moving toward AJAX, split summaries out.


### Data Considerations

- Date range filtering must be inclusive (from and to dates included).
- Category list: derive from existing constant/set, or derive distinct categories from DB.
- Sorting: if add in this epic, define default order (most recent first).
- Performance: do grouping in SQL when possible; ensure index on date column if table grows.


## 4) Testing Plan
- Unit tests:
  - Date window calculations (week start/end, month start/end)
  - Query builder for filters (category, from/to)
- Integration tests (Flask test client):
  - List view returns only filtered expenses
  - Clear filters restore full list
  - Summary no-data state
- Visual regression check: date format preserved in table


## 5) Rolout & Release Considerations
- Feature is:
  - Backwards-compatible: no DB schema change mandatory.
  - Filter params optional; default behavior matches current "list all".
- Logging: add clear error logs for bad date params to avoid silent failure.


## 6) Risks, Dependencies, and Mitigations
- *Risk: Date logic ambiguity (week definition)*
  - Mitigation: document week definition (ISO-8601 Mon-Sun) and use a tested helper function.
- *Risk: Performance degradation on large datasets*
  - Mitigation: add date index, use SQL aggregations.
- *Risk: State mismatch between filtered list and summary* 
  - Mitigation: decide on rule: summary respects active filters or only period selector; implement consistently and test it.


## 7) Schedule / Resource Allocation: Estimate (Sample)
- Story 2.3: Date formatting (0.5‚Äì1 day)
- Story 2.1: Filters (1'Ç£2 days)
- Story 2.2: Summaries (2‚Äì4 days)
- Tests & regression pass: (1'Ç£2 day)

---

Plan owner: TBD
Last updated: 2026-07-07
