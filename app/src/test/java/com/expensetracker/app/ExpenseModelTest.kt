package com.expensetracker.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpenseModelTest {

    @Test
    fun testCategorySummaryComputation() {
        val expenses = listOf(
            Expense(1, "Lunch", 20.0, "Food & Dining", System.currentTimeMillis()),
            Expense(2, "Dinner", 30.0, "Food & Dining", System.currentTimeMillis()),
            Expense(3, "Uber", 50.0, "Transportation", System.currentTimeMillis())
        )

        val total = expenses.sumOf { it.amount }
        assertEquals(100.0, total, 0.001)

        val foodSum = expenses.filter { it.category == "Food & Dining" }.sumOf { it.amount }
        assertEquals(50.0, foodSum, 0.001)
        assertEquals(50.0f, ((foodSum / total) * 100).toFloat(), 0.01f)

        val transportSum = expenses.filter { it.category == "Transportation" }.sumOf { it.amount }
        assertEquals(50.0, transportSum, 0.001)
    }

    @Test
    fun testViewPeriodEnum() {
        assertEquals(3, ViewPeriod.values().size)
    }
}
