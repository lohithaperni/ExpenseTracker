package com.expensetracker.api.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Jsoup-based helper for asserting on the expense list HTML response.
 *
 * Parses the rendered HTML table so tests can make structured assertions
 * rather than raw string-contains checks on the full page body.
 *
 * If the app adds stable selectors (data-testid="expense-row" etc.) update
 * the CSS selectors below to use them — this is the single place to change.
 */
public class HtmlTableParser {

    private final Document doc;

    public HtmlTableParser(String html) {
        this.doc = Jsoup.parse(html);
    }

    // -------------------------------------------------------------------------
    // Row-level checks
    // -------------------------------------------------------------------------

    /**
     * Returns the text content of every data row in the expenses table.
     * Header rows are excluded by targeting {@code tbody tr} first;
     * falls back to all {@code tr} elements when no {@code tbody} is present.
     */
    public List<String> expenseRowTexts() {
        Elements rows = doc.select("table tbody tr");
        if (rows.isEmpty()) {
            rows = doc.select("table tr");
        }
        return rows.stream()
                   .map(Element::text)
                   .filter(t -> !t.isBlank())
                   .collect(Collectors.toList());
    }

    /** Returns true if any table row's text contains the given note (case-sensitive). */
    public boolean containsNote(String note) {
        return expenseRowTexts().stream().anyMatch(row -> row.contains(note));
    }

    /**
     * Returns true when the page has no expense rows OR the page text
     * contains a recognised empty-state marker.
     * Update the marker list once the exact copy is confirmed.
     */
    public boolean isEmptyState() {
        if (expenseRowTexts().isEmpty()) return true;
        String pageText = doc.body().text().toLowerCase();
        return pageText.contains("no expenses")
            || pageText.contains("no results")
            || pageText.contains("nothing found");
    }

    // -------------------------------------------------------------------------
    // Convenience
    // -------------------------------------------------------------------------

    /** Returns the full plain-text content of the page body. */
    public String bodyText() {
        return doc.body() != null ? doc.body().text() : doc.text();
    }

    /** Returns the number of expense rows found. */
    public int rowCount() {
        return expenseRowTexts().size();
    }
}
