package com.expensetracker.app

data class Expense(
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val date: Long, // timestamp in millis
    val paymentMethod: String = "Cash",
    val notes: String = ""
)

data class CategorySummary(
    val category: String,
    val totalAmount: Double,
    val percentage: Float
)

data class MonthSummary(
    val monthIndex: Int, // 0..11
    val monthName: String,
    val monthShortName: String,
    val totalAmount: Double,
    val percentage: Float,
    val transactionCount: Int
)

enum class ViewPeriod {
    DAILY,
    MONTHLY,
    YEARLY
}
