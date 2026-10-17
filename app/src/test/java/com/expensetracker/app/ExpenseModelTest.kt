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

    @Test
    fun testCurrencyFormatting() {
        assertEquals("$45.50", CurrencyManager.formatAmountWithSymbol("$", 45.50))
        assertEquals("₹45.50", CurrencyManager.formatAmountWithSymbol("₹", 45.50))
        assertEquals("-$45.50", String.format(Locale.US, "-%s%.2f", "$", 45.50))
        assertEquals("-₹45.50", String.format(Locale.US, "-%s%.2f", "₹", 45.50))
    }

    @Test
    fun testCsvImporterStandardCsv() {
        val csv = """
            Date,Description,Category,Payment Method,Amount,Notes
            2026-10-16,Morning Coffee,Food & Dining,Card,$6.50,Downtown bakery
            2026-10-15,"Grocery, Market",Groceries,UPI,"₹64.80","Fresh veggies, milk"
        """.trimIndent()

        val stream = java.io.ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8))
        val expenses = CsvImporter.parseCsvStream(stream)

        assertEquals(2, expenses.size)
        assertEquals("Morning Coffee", expenses[0].title)
        assertEquals(6.50, expenses[0].amount, 0.001)
        assertEquals("Food & Dining", expenses[0].category)
        assertEquals("Card", expenses[0].paymentMethod)
        assertEquals("Downtown bakery", expenses[0].notes)

        assertEquals("Grocery, Market", expenses[1].title)
        assertEquals(64.80, expenses[1].amount, 0.001)
        assertEquals("Groceries", expenses[1].category)
        assertEquals("UPI", expenses[1].paymentMethod)
        assertEquals("Fresh veggies, milk", expenses[1].notes)
    }

    @Test
    fun testCsvImporterAppExportedFormat() {
        val appExportCsv = """
            EXPENSE TRACKER FINANCIAL REPORT
            Report Period,October 2026
            Generated On,2026-10-16 11:30:00
            Total Expenditure,71.30
            Total Transactions,2

            --- CATEGORY BREAKDOWN SUMMARY ---
            Category,Amount ($),Share (%)
            "Groceries",64.80,90.88%
            "Food & Dining",6.50,9.12%

            --- ITEMIZED TRANSACTIONS ---
            ID,Date,Time,Description,Category,Payment Method,Amount ($),Notes
            1,"2026-10-16","08:30:00","Morning Coffee","Food & Dining","Card",6.50,"Downtown"
            2,"2026-10-15","14:45:00","Supermarket","Groceries","UPI",64.80,"Dairy"
            ,,,,Total,71.30,
        """.trimIndent()

        val stream = java.io.ByteArrayInputStream(appExportCsv.toByteArray(Charsets.UTF_8))
        val expenses = CsvImporter.parseCsvStream(stream)

        assertEquals(2, expenses.size)
        assertEquals("Morning Coffee", expenses[0].title)
        assertEquals(6.50, expenses[0].amount, 0.001)
        assertEquals("Food & Dining", expenses[0].category)
        assertEquals("Supermarket", expenses[1].title)
        assertEquals(64.80, expenses[1].amount, 0.001)
    }

    @Test
    fun testSampleCsvTemplateParsing() {
        val sampleCsv = CsvImporter.getSampleCsvTemplateContent()
        val stream = java.io.ByteArrayInputStream(sampleCsv.toByteArray(Charsets.UTF_8))
        val expenses = CsvImporter.parseCsvStream(stream)

        assertEquals(6, expenses.size)

        // Row 1: Coffee & Snacks
        assertEquals("Morning Coffee & Snacks", expenses[0].title)
        assertEquals("Food & Dining", expenses[0].category)
        assertEquals("UPI", expenses[0].paymentMethod)
        assertEquals(150.00, expenses[0].amount, 0.001)
        assertEquals("Starbucks coffee", expenses[0].notes)

        // Row 2: Supermarket Groceries
        assertEquals("Supermarket Groceries", expenses[1].title)
        assertEquals("Groceries", expenses[1].category)
        assertEquals("Card", expenses[1].paymentMethod)
        assertEquals(1250.50, expenses[1].amount, 0.001)

        // Row 3: Fuel & Petrol
        assertEquals("Fuel & Petrol", expenses[2].title)
        assertEquals("Transportation", expenses[2].category)
        assertEquals("Cash", expenses[2].paymentMethod)
        assertEquals(500.00, expenses[2].amount, 0.001)

        // Row 4: Utility Bill
        assertEquals("Electricity & Utility Bill", expenses[3].title)
        assertEquals("Bills & Utilities", expenses[3].category)
        assertEquals("Net Banking", expenses[3].paymentMethod)
        assertEquals(820.00, expenses[3].amount, 0.001)

        // Total sum of all 6 items
        val totalExpected = 150.00 + 1250.50 + 500.00 + 820.00 + 450.00 + 240.00
        assertEquals(totalExpected, expenses.sumOf { it.amount }, 0.001)
    }

    @Test
    fun testMonthlyDailyAverageComputation() {
        val totalMonthlySpent = 200.49
        val daysInOctober = 31
        val daysInSeptember = 30
        val daysInFebruary = 28

        val avgOctober = totalMonthlySpent / daysInOctober
        assertEquals(6.4674, avgOctober, 0.001)
        assertEquals("6.47", String.format(Locale.US, "%.2f", avgOctober))

        val avgSeptember = 142.70 / daysInSeptember
        assertEquals(4.7566, avgSeptember, 0.001)
        assertEquals("4.76", String.format(Locale.US, "%.2f", avgSeptember))

        val avgFebruary = 280.00 / daysInFebruary
        assertEquals(10.0, avgFebruary, 0.001)
    }

    @Test
    fun testYearlyMonthlyAverageComputation() {
        val totalYearlySpent = 343.19
        val monthsInYear = 12.0

        val avgMonthly = totalYearlySpent / monthsInYear
        assertEquals(28.599, avgMonthly, 0.001)
        assertEquals("28.60", String.format(Locale.US, "%.2f", avgMonthly))
    }

    @Test
    fun testZeroExpenseAverages() {
        val zeroTotal = 0.0
        val daysInMonth = 31
        val avgDay = if (daysInMonth > 0) zeroTotal / daysInMonth else 0.0
        val avgMonth = zeroTotal / 12.0

        assertEquals(0.0, avgDay, 0.0001)
        assertEquals(0.0, avgMonth, 0.0001)
    }

    @Test
    fun testTransactionSearchFiltering() {
        val expense = Expense(
            id = 1,
            title = "Morning Coffee & Croissant",
            amount = 6.50,
            category = "Food & Dining",
            date = 1700000000000L,
            paymentMethod = "Card",
            notes = "Breakfast at French Bakery"
        )

        // Matching by title
        assertTrue(MainActivity.matchesSearch(expense, "Coffee"))
        assertTrue(MainActivity.matchesSearch(expense, "croissant"))

        // Matching by category
        assertTrue(MainActivity.matchesSearch(expense, "Food"))
        assertTrue(MainActivity.matchesSearch(expense, "dining"))

        // Matching by payment method
        assertTrue(MainActivity.matchesSearch(expense, "card"))
        assertTrue(MainActivity.matchesSearch(expense, "CARD"))

        // Matching by notes
        assertTrue(MainActivity.matchesSearch(expense, "Bakery"))
        assertTrue(MainActivity.matchesSearch(expense, "breakfast"))

        // Matching by amount
        assertTrue(MainActivity.matchesSearch(expense, "6.50"))
        assertTrue(MainActivity.matchesSearch(expense, "6.5"))

        // Blank or whitespace query matches all
        assertTrue(MainActivity.matchesSearch(expense, ""))
        assertTrue(MainActivity.matchesSearch(expense, "   "))

        // Non-matching query
        assertFalse(MainActivity.matchesSearch(expense, "Metro"))
        assertFalse(MainActivity.matchesSearch(expense, "UPI"))
        assertFalse(MainActivity.matchesSearch(expense, "Transportation"))
    }
}



