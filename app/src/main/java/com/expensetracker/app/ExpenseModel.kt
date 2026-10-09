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

enum class ViewPeriod {
    DAILY,
    MONTHLY,
    YEARLY
}
