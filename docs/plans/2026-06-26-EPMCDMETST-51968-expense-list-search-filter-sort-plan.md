# Implementation Plan: Expense list search, filter, sort + filtered total

- **Jira Epic:** EMPCDMETST-51968 – Expense list search, filter, and sort
- **Stories in scope:**
  - EMPCDMETST-51969 – Filter expenses by date range
  - EPMCDMETST-51970 – Filter expenses by category
  - EPMCDMETST-51971 – Search expenses by note keyword
- **Status:** Epic and stories: Open
- **Priority:** Low

> **Decisions (FROM REQUEST):**
> - Include sorting by **date** (newest/oldest) and **amount** (high/low)
> - Show **total for filtered results only** (not the full dataset)

## 1) Objectives & Scope

**Goal:** Enable users to find expenses quickly by adding search, filters, sort options, and a total that reflects the current filtered results.

**In scope (Epic EP.51968):**
- Date range filter (s tart, end)
- Category filter (single select, or multi-select if already supported by data model)
- Note text search (substring match, case-insensitive)
- Sorting:
  - Date: newest->oldest and oldest->newest
  - Amount: High->low and low->high
- Clear/reset filters and return to full list
- Display total amount for **current filtered results** (including search and category/date filters)

**Out of scope:**
- Auth/multi-user, budgets, exports, charts (as per epic description)
- Server-side pagination (not in stories)
- Advanced full-text search normalization (stemming, tokenization, etc.)

## 2) UX / UI Design (Handling search, filter, sort, total)

### 2.1 Controls

- **Date range:** Two date inputs (Start, End)
  - Default: empty (no filter)
  - Constraint: end >= start (validation behavior below)
- **Category:** Dropdown (single select) with "All"/empty value
- **Note search:** Text input (with debounce or explicit "Search" button). Match case-insensitive substring
- **Sort:** Dropdown or segmented control:
  - `date_desc` (default)
  - `date_asc`
  - `amount_desc`
  - `amount_asc`
- **Clear:** "Reset" button that clears all filters, search, and resets sort to default

### 2.2 Results list and total
- Present expenses in a list as today (baseline UI unchanged otherwise)
- Show a summary area: **"Total (x items): $y"** that reflects only the **currently filtered/searched results** (not the unfiltered dataset)
  - Display 0 and $0.00 when no results
  - To avoid confusion, consider label: "Total (matching)" or "Total (filtered)"

### 2.3 Empty states
- No results match filters/search: show friendly message and "Clear hand" action
- No expenses in system: explicit message (different from "no matches")

## 3) Backend API & Query Params