package com.expensetracker.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExpenseDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "expenses.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_EXPENSES = "expenses"
        private const val COL_ID = "id"
        private const val COL_TITLE = "title"
        private const val COL_AMOUNT = "amount"
        private const val COL_CATEGORY = "category"
        private const val COL_DATE = "date"
        private const val COL_PAYMENT_METHOD = "payment_method"
        private const val COL_NOTES = "notes"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_EXPENSES (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT NOT NULL,
                $COL_AMOUNT REAL NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_DATE INTEGER NOT NULL,
                $COL_PAYMENT_METHOD TEXT,
                $COL_NOTES TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_expenses_date ON $TABLE_EXPENSES($COL_DATE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_expenses_category ON $TABLE_EXPENSES($COL_CATEGORY)")
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_expenses_date ON $TABLE_EXPENSES($COL_DATE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_expenses_category ON $TABLE_EXPENSES($COL_CATEGORY)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EXPENSES")
        onCreate(db)
    }

    fun insertExpense(expense: Expense): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, expense.title)
            put(COL_AMOUNT, expense.amount)
            put(COL_CATEGORY, expense.category)
            put(COL_DATE, expense.date)
            put(COL_PAYMENT_METHOD, expense.paymentMethod)
            put(COL_NOTES, expense.notes)
        }
        return db.insert(TABLE_EXPENSES, null, values)
    }

    fun insertExpenses(expenses: List<Expense>): Int {
        val db = writableDatabase
        var count = 0
        db.beginTransaction()
        try {
            for (expense in expenses) {
                val values = ContentValues().apply {
                    put(COL_TITLE, expense.title)
                    put(COL_AMOUNT, expense.amount)
                    put(COL_CATEGORY, expense.category)
                    put(COL_DATE, expense.date)
                    put(COL_PAYMENT_METHOD, expense.paymentMethod)
                    put(COL_NOTES, expense.notes)
                }
                if (db.insert(TABLE_EXPENSES, null, values) != -1L) {
                    count++
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return count
    }

    fun updateExpense(expense: Expense): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, expense.title)
            put(COL_AMOUNT, expense.amount)
            put(COL_CATEGORY, expense.category)
            put(COL_DATE, expense.date)
            put(COL_PAYMENT_METHOD, expense.paymentMethod)
            put(COL_NOTES, expense.notes)
        }
        return db.update(TABLE_EXPENSES, values, "$COL_ID = ?", arrayOf(expense.id.toString())) > 0
    }

    fun deleteExpense(id: Long): Boolean {
        val db = writableDatabase
        return db.delete(TABLE_EXPENSES, "$COL_ID = ?", arrayOf(id.toString())) > 0
    }

    fun getDistinctTitles(): List<String> {
        val list = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT DISTINCT $COL_TITLE FROM $TABLE_EXPENSES WHERE $COL_TITLE IS NOT NULL AND TRIM($COL_TITLE) != '' ORDER BY $COL_TITLE ASC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(it.getString(0))
            }
        }
        return list
    }

    fun getDistinctNotes(): List<String> {
        val list = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT DISTINCT $COL_NOTES FROM $TABLE_EXPENSES WHERE $COL_NOTES IS NOT NULL AND TRIM($COL_NOTES) != '' ORDER BY $COL_NOTES ASC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(it.getString(0))
            }
        }
        return list
    }

    fun getExpensesByDateRange(startTime: Long, endTime: Long): List<Expense> {
        val list = mutableListOf<Expense>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_EXPENSES,
            null,
            "$COL_DATE >= ? AND $COL_DATE <= ?",
            arrayOf(startTime.toString(), endTime.toString()),
            null,
            null,
            "$COL_DATE DESC"
        )

        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getLong(it.getColumnIndexOrThrow(COL_ID))
                    val title = it.getString(it.getColumnIndexOrThrow(COL_TITLE))
                    val amount = it.getDouble(it.getColumnIndexOrThrow(COL_AMOUNT))
                    val category = it.getString(it.getColumnIndexOrThrow(COL_CATEGORY))
                    val date = it.getLong(it.getColumnIndexOrThrow(COL_DATE))
                    val payment = it.getString(it.getColumnIndexOrThrow(COL_PAYMENT_METHOD)) ?: "Cash"
                    val notes = it.getString(it.getColumnIndexOrThrow(COL_NOTES)) ?: ""
                    list.add(Expense(id, title, amount, category, date, payment, notes))
                } while (it.moveToNext())
            }
        }
        return list
    }

    fun getCategorySummaryFromExpenses(expenses: List<Expense>): List<CategorySummary> {
        val total = expenses.sumOf { it.amount }
        if (total <= 0.0) return emptyList()

        return expenses.groupBy { it.category }
            .map { (cat, items) ->
                val catSum = items.sumOf { it.amount }
                val pct = ((catSum / total) * 100).toFloat()
                CategorySummary(cat, catSum, pct)
            }
            .sortedByDescending { it.totalAmount }
    }

    fun getCategorySummary(startTime: Long, endTime: Long): List<CategorySummary> {
        return getCategorySummaryFromExpenses(getExpensesByDateRange(startTime, endTime))
    }

    fun getMonthlySummaryForYear(year: Int, preloadedYearExpenses: List<Expense>? = null): List<MonthSummary> {
        val expenses = preloadedYearExpenses ?: run {
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val startTime = cal.timeInMillis

            cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val endTime = cal.timeInMillis
            getExpensesByDateRange(startTime, endTime)
        }
        val totalYear = expenses.sumOf { it.amount }

        val tempCal = Calendar.getInstance()
        val expensesByMonth = expenses.groupBy { expense ->
            tempCal.timeInMillis = expense.date
            tempCal.get(Calendar.MONTH)
        }

        val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        val shortMonthFormat = SimpleDateFormat("MMM", Locale.getDefault())

        val result = mutableListOf<MonthSummary>()
        for (m in 0..11) {
            tempCal.set(Calendar.MONTH, m)
            tempCal.set(Calendar.DAY_OF_MONTH, 1)
            val monthName = monthFormat.format(tempCal.time)
            val monthShort = shortMonthFormat.format(tempCal.time).uppercase(Locale.getDefault())

            val monthExpenses = expensesByMonth[m] ?: emptyList()
            val monthTotal = monthExpenses.sumOf { it.amount }
            val pct = if (totalYear > 0.0) {
                ((monthTotal / totalYear) * 100).toFloat()
            } else {
                0.0f
            }

            result.add(
                MonthSummary(
                    monthIndex = m,
                    monthName = monthName,
                    monthShortName = monthShort,
                    totalAmount = monthTotal,
                    percentage = pct,
                    transactionCount = monthExpenses.size
                )
            )
        }
        return result
    }

    fun seedSampleDataIfEmpty() {
        val db = readableDatabase
        val countCursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_EXPENSES", null)
        var count = 0
        countCursor.use {
            if (it.moveToFirst()) {
                count = it.getInt(0)
            }
        }

        if (count == 0) {
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()

            // Today's expenses (October)
            insertExpense(Expense(title = "Morning Coffee & Croissant", amount = 6.50, category = "Food & Dining", date = now - 3600_000 * 4, paymentMethod = "Card", notes = "Bakery downtown"))
            insertExpense(Expense(title = "Metro Train Card Recharge", amount = 25.00, category = "Transportation", date = now - 3600_000 * 2, paymentMethod = "UPI", notes = "Weekly commute"))
            insertExpense(Expense(title = "Grocery Market", amount = 64.80, category = "Groceries", date = now - 3600_000, paymentMethod = "Card", notes = "Produce & dairy"))

            // Earlier this week / month
            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_MONTH, -2)
            insertExpense(Expense(title = "High Speed Internet Bill", amount = 59.99, category = "Bills & Utilities", date = cal.timeInMillis, paymentMethod = "Net Banking", notes = "Monthly fiber plan"))

            cal.add(Calendar.DAY_OF_MONTH, -3)
            insertExpense(Expense(title = "Cinema Tickets & Snacks", amount = 34.00, category = "Entertainment", date = cal.timeInMillis, paymentMethod = "Card", notes = "Weekend movie with friends"))

            cal.add(Calendar.DAY_OF_MONTH, -4)
            insertExpense(Expense(title = "Pharmacy & Vitamins", amount = 42.50, category = "Health", date = cal.timeInMillis, paymentMethod = "Cash", notes = "Prescription refill"))

            cal.add(Calendar.DAY_OF_MONTH, -8)
            insertExpense(Expense(title = "Summer Casual Shirt", amount = 45.00, category = "Shopping", date = cal.timeInMillis, paymentMethod = "Card", notes = "Weekend mall sale"))

            cal.add(Calendar.DAY_OF_MONTH, -12)
            insertExpense(Expense(title = "Car Petrol Refuel", amount = 55.20, category = "Transportation", date = cal.timeInMillis, paymentMethod = "UPI", notes = "Full tank"))

            // Earlier months in 2026 for rich Yearly view
            cal.set(2026, Calendar.SEPTEMBER, 15, 14, 30)
            insertExpense(Expense(title = "Wireless Headphones", amount = 89.99, category = "Shopping", date = cal.timeInMillis, paymentMethod = "Card", notes = "Noise cancelling"))
            cal.set(2026, Calendar.SEPTEMBER, 22, 19, 0)
            insertExpense(Expense(title = "Weekend Family Dining", amount = 72.50, category = "Food & Dining", date = cal.timeInMillis, paymentMethod = "UPI", notes = "Celebration dinner"))

            cal.set(2026, Calendar.AUGUST, 10, 11, 0)
            insertExpense(Expense(title = "Train Holiday Tickets", amount = 120.00, category = "Transportation", date = cal.timeInMillis, paymentMethod = "Net Banking", notes = "Summer getaway"))

            cal.set(2026, Calendar.JULY, 5, 16, 20)
            insertExpense(Expense(title = "Annual Health Screening", amount = 95.00, category = "Health", date = cal.timeInMillis, paymentMethod = "Card", notes = "Routine lab tests"))

            cal.set(2026, Calendar.MAY, 18, 10, 0)
            insertExpense(Expense(title = "Electricity & Water Bill", amount = 115.40, category = "Bills & Utilities", date = cal.timeInMillis, paymentMethod = "Net Banking", notes = "Bi-monthly utility"))

            cal.set(2026, Calendar.JANUARY, 8, 9, 0)
            insertExpense(Expense(title = "Fitness Club Membership", amount = 150.00, category = "Health", date = cal.timeInMillis, paymentMethod = "Card", notes = "Annual membership"))
        }
    }
}
