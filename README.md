# ExpenseTracker - Daily Expenses & Financial Reports App

A feature-rich personal finance Android application built in **Kotlin** and **Material Design 3**. Track daily expenses, visualize spending breakdowns across Daily, Monthly, and Yearly periods, and export reports directly to **PDF** and **Excel (CSV)**.

![Expense Tracker Preview](screenshots/expense_main.png)

---

## 🚀 Key Features

- **💸 Track Expenses**: Record transactions with amount, category (Food, Transport, Utilities, Entertainment, Health, Shopping, etc.), date, and notes.
- **📊 Multi-Period Analytics**:
  - **Daily View**: See day-by-day expense logs with totals.
  - **Monthly View**: Month-by-month spending summaries and category breakdowns.
  - **Yearly View**: Annual financial health overview.
- **📄 Export to PDF & Excel**:
  - Generate structured PDF financial report documents with headers, tables, and total calculations.
  - Export to CSV/Excel format for import into spreadsheet software.
  - One-tap sharing via Android's native Share Sheet.
- **🔒 Offline SQLite Storage**: 100% private and offline-first database. No cloud dependency or third-party trackers.
- **🎨 Modern Material 3 UI**: Clean cards, category chips, and floating action button.

---

## 🛠️ Tech Stack

- **Language**: Kotlin
- **Build System**: Gradle Kotlin DSL
- **Compile SDK**: 36.1 | **Min SDK**: 24
- **Database**: Android SQLite (`SQLiteOpenHelper`)
- **Reporting**: Android `PdfDocument` API & FileProvider for PDF/CSV exports
- **UI**: Android ViewBinding & Material Components 3

---

## 📦 How to Build & Run

```bash
# Clone the repository
git clone https://github.com/rajeevksofficial/ExpenseTrackerApp.git
cd ExpenseTrackerApp

# Build Debug APK
./gradlew assembleDebug
```

---

## 📄 License
Open source under the [MIT License](LICENSE).
