"""
Simple Expense Tracker - Seed Application
-------------------------------------------
A deliberately minimal, limited-feature expense tracker.
Intended to serve as the "existing functional application" base
for an AI-driven SDLC capstone exercise (brownfield analysis).

Features (intentionally limited):
- Add an expense (amount, category, date, note)
- List all expenses
- Delete an expense
- Show running total

NOT included on purpose (these become your "gaps" to discover):
- Edit/update an expense
- Monthly/weekly summaries or filtering
- Charts/visualizations
- Multiple users / authentication
- Budgets or limits
- CSV/PDF export
- Search
"""

from flask import Flask, render_template, request, redirect, url_for
import sqlite3
import os

app = Flask(__name__)

DB_PATH = os.path.join(os.path.dirname(__file__), "expenses.db")

CATEGORIES = ["Food", "Transport", "Utilities", "Shopping", "Entertainment", "Other"]


def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    conn = get_db()
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS expenses (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            amount REAL NOT NULL,
            category TEXT NOT NULL,
            expense_date TEXT NOT NULL,
            note TEXT
        )
        """
    )
    conn.commit()
    conn.close()


@app.route("/")
def index():
    conn = get_db()
    expenses = conn.execute(
        "SELECT * FROM expenses ORDER BY expense_date DESC, id DESC"
    ).fetchall()
    total = conn.execute("SELECT COALESCE(SUM(amount), 0) AS total FROM expenses").fetchone()["total"]
    conn.close()
    return render_template("index.html", expenses=expenses, total=total, categories=CATEGORIES)


@app.route("/add", methods=["POST"])
def add_expense():
    amount = request.form.get("amount", "").strip()
    category = request.form.get("category", "").strip()
    expense_date = request.form.get("expense_date", "").strip()
    note = request.form.get("note", "").strip()

    # Minimal validation only — intentionally basic
    if amount and category and expense_date:
        try:
            amount_value = float(amount)
            conn = get_db()
            conn.execute(
                "INSERT INTO expenses (amount, category, expense_date, note) VALUES (?, ?, ?, ?)",
                (amount_value, category, expense_date, note),
            )
            conn.commit()
            conn.close()
        except ValueError:
            pass

    return redirect(url_for("index"))


@app.route("/delete/<int:expense_id>", methods=["POST"])
def delete_expense(expense_id):
    conn = get_db()
    conn.execute("DELETE FROM expenses WHERE id = ?", (expense_id,))
    conn.commit()
    conn.close()
    return redirect(url_for("index"))


if __name__ == "__main__":
    init_db()
    app.run(debug=True, host="0.0.0.0", port=5000)
