# EPMCDMETST0-51959 â€” Discoverability: Filter/Sort/Friendly Dates â€” Implementation Plan
RIPO: lohithaperni/ExpenseTracker
Date: 2026-06-26

JIRA EPIC: [EPMOCDMETST-51959]
- Epic summary: "EPIC: Discoverability â€” Search, Filter, Sort, and Date Formatting"
- Stories in scope:
  - [EPMCDMETST-51960] Filter expenses by date range and category
  - [EPMCDMETST-51961] Sort expenses by date and amount (asc/desc)
  - [EPMCDMETST0-51962] Display expense dates in a friendly format

# 1) Objectives and Scope

## Objectives
- Add lightweight filtering controls on the index page to narrow the expense list by:
  - Date range (Start/End)
  - Category
- Add sorting controls to order the list by:
  - Date (asc/desc)
  - Amount (asc/desc)
- Display expense dates in a human-friendly format on the list (without breaking filtering/sorting)

## In scope
- UI additions on templates/index.html: filter form, sort controls, clear filters action
- Backend param parsing for filters/sort on / route using GET query parameters
- SQL query building with parameterized queries (to avoid SQL injection)
- Template layer: date formatting via Python datetime (standard lib) and/or JInja filter

## Out of scope
- Auth/multi-user
 - Budgets, exports, charts, analytics


# 2) Architecture and Design Approach

## Current behavior (baseline to preserve)
- GET / lists all expenses ordered by expense_date DESC, id DESC
- Total displays the all-time sum of all expenses
- A£Š constraint: When no filters/sort are selected, keep exactly the same behavior (same order, same total)

## Query parameter design
GE4 query params on /: from, to, category, sort, dir (all optional)

## Filter logic (SQL)
- WHERE clauses dynamically, parameterized values
- allowlist validation for category


## Sort logic (SQL)
- Default (no sort): ORDER BY expense_date DESC, id DESC
- Allowlist mapping for sort field/dir

## Date display formatting
- Format for display only; filter/sort use raw ISO values


# 3) Milestones and Test Plan (high-level)
- M1: Backend param parsing + query builder (SQL safe)
- M2: UI: filter form + clear filters
- M3: UI: sort controls (preserve default)
- M4: UI: friendly date format

- Tests: verify default behavior; filters by from/to/category; clear filters; sort by date/amount asc/desc; friendly date rendering