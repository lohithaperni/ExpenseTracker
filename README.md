# Expense Tracker (Seed App)

A small, intentionally limited expense tracker used as the **brownfield base
application** for the AI-Assistant–Driven SDLC capstone project.

## What it does

- Add an expense (amount, category, date, optional note)
- View all expenses in a list, newest first
- Delete an expense
- See a running total of all spend

## What it deliberately does NOT do

This app is scoped small on purpose so the AI-driven SDLC pipeline has real,
discoverable gaps to analyze and turn into enhancement stories:

- No editing an existing expense
- No filtering/search (by date range, category, etc.)
- No monthly/weekly summaries or breakdowns
- No charts or visualizations
- No budgets, limits, or alerts
- No multi-user support or authentication
- No data export (CSV/PDF)

## Tech stack

- **Backend:** Python 3 + Flask
- **Database:** SQLite (file-based, zero setup)
- **Frontend:** Server-rendered HTML (Jinja2) + plain CSS, no JS framework

## Setup

```bash
python3 -m venv venv
source venv/bin/activate        # Windows: venv\Scripts\activate
pip install -r requirements.txt
python app.py
```

Then open **http://localhost:5000** in your browser.

The SQLite database file (`expenses.db`) is created automatically on first run.

## Project structure

```
expense-tracker/
├── app.py              # Flask app + routes + DB logic
├── requirements.txt
├── templates/
│   └── index.html      # Single-page UI
├── static/
│   └── style.css
└── expenses.db          # created at runtime, not committed
```

## Next steps (for the capstone)

This app is the **input** to the AI-driven SDLC exercise, not the deliverable.
From here:
1. Run an AI requirements/analysis assistant against this app to identify gaps
2. Turn gaps into Epics/Stories in Jira
3. Design, build, test, and deploy the enhancements using the AI-assisted
   pipeline described in the capstone brief
