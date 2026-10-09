package com.expensetracker.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Calendar

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

    fun deleteExpense(id: Long): Boolean {
        val db = writableDatabase
        return db.delete(TABLE_EXPENSES, "$COL_ID = ?", arrayOf(id.toString())) > 0
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

    fun getCategorySummary(startTime: Long, endTime: Long): List<CategorySummary> {
        val expenses = getExpensesByDateRange(startTime, endTime)
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

            // Today's expenses
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
        }
    }
}
