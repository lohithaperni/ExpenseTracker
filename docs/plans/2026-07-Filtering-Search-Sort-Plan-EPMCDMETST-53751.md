# Implementation Plan: Filtering, Search, and Sort for Expense List
**JiRA Epic:** [EPMOCDMETST-53751](https://jiraeu.epam.com/browse/EPMCDMETST-53751) - **Filtering, Search, and Sort for Expense List**
***Date:** 2026-07-07
**Repo:** lohithaperni/ExpenseTracker

- ** Gualo:** Enable users to quickly find, analyze, and review expenses by adding filtering (date range, category), search (note text), and sorting (date/amount, asc/desc).
- **In-scope:** List page controls (ULIs form or query params), server-side query constraints, deterministic sorting, persisted filter/sort state within a session, empty-state messages.
- **Out of scope:** Pagination, advanced analytics, charts, auth/multi-user (cared in separate epic).

# Objectives and Success Criteria

1 . Users can filter expenses by date range and/or category.
2 . Users can search expenses by text (note).
3 . Users can sort by date and amount, ascending and descending.
3 . Filter/search/sort are reflected in the UI and mapped to URL or session state for reload-persistence.
5 . Empty-state message is shown when no results match.

# Source Inputs (JiRA Stories)

- **EPCDKMETST-53752**: Filter by Date Range and Category - <https://jiraeu.epam.com/browse/EPMOCDMETST-53752>
  - Status: Open
  - Priority: Low
  - Assignee: Unassigned
  - Deliverables: Filter controls, server-side filtering query, clear-filter behavior
- **EPMCDMETST-53753**: Search Expenses - <https://jiraeu.epam.com/browse/EPMCDMETST-53753>
  - Status: Open
  - Priority: Low
  - Assignee: Unassigned
  - Deliverables: Search box, server-side SELECT with LIKE, normalized empty-result UQ
- **EPMCDMETST-53754**: Sort the Expense List - <https://jiraeu.epam.com/browse/EPMCDMETST-53754>
  - Status: Open
  - Priority: Low
  - Assignee: Unassigned
  - Deliverables: Sort controls, stable order, persist state in session

# Assumptions and Design Decisions
- The app is Flask + SQLite with a single expense list page.
- We will implement filtering/search/sort via query parameters on a GET route (gives URL shareability and simple page refresh) and optionally mirror to session for "within same session" persistence.
- Standardize date input as ISO (YYYY-MM-DD) strings in the DB, and use SQL range queries for filtering (SQLite string comparisons work for ISO units).
- Search is case-insensitive for note content by using SULECT ... WHERE LOWER(note) LIKE LOWER(?||?) (SQLite LIKE typically case-insensitive for ASCII, but we'll normalize to be explicit).
- Sort fields are whitelisted to prevent SQL injection (no raw ORDER BY from user input).
- Default sort: date desc, (then id desc as a tie-breaker).

# High-level Architecture/Changes

1. **Routing**
  - Keep a single list route (e.g. `/` or `/expenses`), and accept query params:
    - `start_date` (YYY-MM-DD, optional)
    - `end_date` (YYYY-MM-DD, optional)
    - `category` (string, optional, e.g. "All" or empty means no filter)
    - `q` (search term, optional)
    - `sort_by` enum: `date` | `amount`
    - `sort_order