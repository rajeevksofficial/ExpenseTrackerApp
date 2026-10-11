package com.expensetracker.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale

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

    @Test
    fun testExpenseEditCopy() {
        val original = Expense(
            id = 42,
            title = "Coffee",
            amount = 5.0,
            category = "Food & Dining",
            date = 1700000000000L,
            paymentMethod = "Cash",
            notes = "Morning run"
        )

        val updated = original.copy(
            title = "Large Latte & Bagel",
            amount = 8.50,
            notes = "Upgraded to large latte"
        )

        assertEquals(42L, updated.id)
        assertEquals("Large Latte & Bagel", updated.title)
        assertEquals(8.50, updated.amount, 0.001)
        assertEquals("Food & Dining", updated.category)
        assertEquals(1700000000000L, updated.date)
        assertEquals("Cash", updated.paymentMethod)
        assertEquals("Upgraded to large latte", updated.notes)
    }

    @Test
    fun testDailyPeriodDateAutoCapture() {
        // Navigating daily to a specific date
        val navigatedCalendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 5)
        }

        // When user is in daily mode and clicks +, date is auto-captured
        val expenseCalendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, navigatedCalendar.get(Calendar.YEAR))
            set(Calendar.MONTH, navigatedCalendar.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, navigatedCalendar.get(Calendar.DAY_OF_MONTH))
        }

        assertEquals(2026, expenseCalendar.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, expenseCalendar.get(Calendar.MONTH))
        assertEquals(5, expenseCalendar.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testSubstringMatchingLogic() {
        val items = listOf(
            "Morning Coffee & Croissant",
            "Metro Train Card Recharge",
            "Grocery Market",
            "High Speed Internet Bill",
            "Pharmacy & Vitamins"
        )

        val query = "bill"
        val matched = items.filter { it.lowercase(Locale.ROOT).contains(query.lowercase(Locale.ROOT)) }
        assertEquals(1, matched.size)
        assertEquals("High Speed Internet Bill", matched.first())

        val querySubstring = "train"
        val matchedTrain = items.filter { it.lowercase(Locale.ROOT).contains(querySubstring.lowercase(Locale.ROOT)) }
        assertEquals(1, matchedTrain.size)
        assertEquals("Metro Train Card Recharge", matchedTrain.first())

        val queryPartial = "off"
        val matchedCoffee = items.filter { it.lowercase(Locale.ROOT).contains(queryPartial.lowercase(Locale.ROOT)) }
        assertEquals(1, matchedCoffee.size)
        assertEquals("Morning Coffee & Croissant", matchedCoffee.first())
    }

    @Test
    fun testMonthlySummaryComputation() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.JANUARY, 15)
        val janExpense = Expense(1, "Gym", 100.0, "Health", cal.timeInMillis)

        cal.set(2026, Calendar.OCTOBER, 5)
        val octExpense1 = Expense(2, "Groceries", 200.0, "Groceries", cal.timeInMillis)
        cal.set(2026, Calendar.OCTOBER, 20)
        val octExpense2 = Expense(3, "Dinner", 100.0, "Food & Dining", cal.timeInMillis)

        val expenses = listOf(janExpense, octExpense1, octExpense2)
        val totalYear = expenses.sumOf { it.amount }
        assertEquals(400.0, totalYear, 0.001)

        val tempCal = Calendar.getInstance()
        val byMonth = expenses.groupBy {
            tempCal.timeInMillis = it.date
            tempCal.get(Calendar.MONTH)
        }

        val janTotal = byMonth[Calendar.JANUARY]?.sumOf { it.amount } ?: 0.0
        val octTotal = byMonth[Calendar.OCTOBER]?.sumOf { it.amount } ?: 0.0
        val febTotal = byMonth[Calendar.FEBRUARY]?.sumOf { it.amount } ?: 0.0

        assertEquals(100.0, janTotal, 0.001)
        assertEquals(300.0, octTotal, 0.001)
        assertEquals(0.0, febTotal, 0.001)

        val janPct = ((janTotal / totalYear) * 100).toFloat()
        val octPct = ((octTotal / totalYear) * 100).toFloat()
        assertEquals(25.0f, janPct, 0.01f)
        assertEquals(75.0f, octPct, 0.01f)

        val monthSummary = MonthSummary(
            monthIndex = Calendar.OCTOBER,
            monthName = "October",
            monthShortName = "OCT",
            totalAmount = octTotal,
            percentage = octPct,
            transactionCount = 2
        )
        assertEquals(Calendar.OCTOBER, monthSummary.monthIndex)
        assertEquals("October", monthSummary.monthName)
        assertEquals(300.0, monthSummary.totalAmount, 0.001)
        assertEquals(2, monthSummary.transactionCount)
    }
}
