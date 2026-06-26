# Epic 2: Search / Filter / Sort — Implementation Plan

Repo: `lohithaperni/ExpenseTracker`  
Base branch: `main`
 
Objective
- Add client-side search, filtering, and sorting to the expenses list to improve discoverability and data exploration.
- Ensure features are intuitive, accessible, and testable.


Scope
- Search: match expenses by text fields (e.g. description, merchant, category).
- Filter: filter by category, date range, amount range, and type (expense/income) if applicable.
 - Sort: sort by date, amount, and optionally category.
- Non-scope: server-side indexing/search or backend query optimizations (unless the existing app already supports it).


Assumptions / Context
- Expense data is rendered in a list/table view and can be filtered in-memory.
- The UI stack is web-based (HTML/JS/CSS) and repo currently contains front-end assets.
 - For accessibility, controls will be labeled and navigable by claviature.


Breakdown (High-level)
1 UICd for search, filter, sort controls
   - Add a search input (clear button, debounced input).
   - Add filter controls (category dropdown, date range, amount range).
   - Add sort selector (date/amount with asc/desc toggle).
   - Add "Reset" action to restore defaults.

2. Data pipeline for search/filter/sort
   - Define a single source of truth for the raw
     expenses list.
   - Implement pure functions:
      - `applySearch(expenses, query)`
      - `applyFilters(expenses, filterState)`
      - `applySort(expenses, sortState)`
    - Compose them in a deterministic order (filter → search → sort, or search → filter → sort, documented).

3. State management
   - Store current query, filter values, and sort selection in local UI state (en.g. component state).
   - Optional: sync to URL!searchParams to allow shareable links.

-4. UI behavior and empty-states
   - Indicate active filters parts (chips/badges).
   - Display "No results" when search/filter eliminates all entries.
   - Ensure sorting is applied after filtering as defined.

5. Testing
   - Unit tests for the pure data functions (search, filter, sort).
   - UI behavior tests if a test harness exists (or manual test script otherwise).
    - Accessibility quick check: tab order, labels, contrast.


Schedule / Milestones
- M1 (1-2 days): Implement search and sort controls and data pipeline.
 - M2 (1-2 days): Add filters (category, date, amount) and reset.
 - M3 (1 day): Tests, accessibility pass, and docs/usage notes.


Risks & Mitigations
- Performance degradation for large lists
  - Mitigation: debounce search, avoid mutating arrays in-place, optionally virtualize rendering.
- Inconsistent sort order (tie-breaking)
   - Mitigation: define secondary sort keys (e.g. date then id).
- Date timezone issues in date range filters
  - Mitigation: normalize to local day start/end or use ISO dates consistently.


Deliverables
- `/docs/plans/2026-06-Epic2-Search-Filter-Sort-Plan.md`
- Notes on enduser behavior and defaults (default sort, reset behavior).

Traceability
- JiRA context: space `EPMCDMETST`, Epic 2 - "Search/Filter/Sort"
