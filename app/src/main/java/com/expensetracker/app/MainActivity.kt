package com.expensetracker.app

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.expensetracker.app.databinding.ActivityMainBinding
import com.expensetracker.app.databinding.DialogAddExpenseBinding
import com.expensetracker.app.databinding.ItemCategorySummaryBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: ExpenseDatabaseHelper
    private lateinit var expenseAdapter: ExpenseAdapter

    private var currentPeriod = ViewPeriod.MONTHLY
    private val calendar = Calendar.getInstance()

    private val categories = arrayOf(
        "Food & Dining",
        "Transportation",
        "Groceries",
        "Bills & Utilities",
        "Entertainment",
        "Health",
        "Shopping",
        "Other"
    )

    private val paymentMethods = arrayOf("Card", "UPI", "Cash", "Net Banking")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = ExpenseDatabaseHelper(this)
        dbHelper.seedSampleDataIfEmpty()

        setupRecyclerView()
        setupListeners()
        updateTabSelectionUI()
        loadExpenses()
    }

    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter(emptyList()) { expense ->
            confirmDeleteExpense(expense)
        }
        binding.rvExpenses.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = expenseAdapter
        }
    }

    private fun setupListeners() {
        // Tab clicks
        binding.tabDaily.setOnClickListener {
            if (currentPeriod != ViewPeriod.DAILY) {
                currentPeriod = ViewPeriod.DAILY
                updateTabSelectionUI()
                loadExpenses()
            }
        }

        binding.tabMonthly.setOnClickListener {
            if (currentPeriod != ViewPeriod.MONTHLY) {
                currentPeriod = ViewPeriod.MONTHLY
                updateTabSelectionUI()
                loadExpenses()
            }
        }

        binding.tabYearly.setOnClickListener {
            if (currentPeriod != ViewPeriod.YEARLY) {
                currentPeriod = ViewPeriod.YEARLY
                updateTabSelectionUI()
                loadExpenses()
            }
        }

        // Period navigation buttons
        binding.btnPrevPeriod.setOnClickListener {
            navigatePeriod(-1)
        }

        binding.btnNextPeriod.setOnClickListener {
            navigatePeriod(1)
        }

        // Export PDF
        binding.btnExportPdf.setOnClickListener {
            exportPdfReport()
        }

        // Export Excel
        binding.btnExportExcel.setOnClickListener {
            exportExcelReport()
        }

        // Floating Action Button
        binding.fabAddExpense.setOnClickListener {
            showAddExpenseDialog()
        }
    }

    private fun navigatePeriod(direction: Int) {
        when (currentPeriod) {
            ViewPeriod.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, direction)
            ViewPeriod.MONTHLY -> calendar.add(Calendar.MONTH, direction)
            ViewPeriod.YEARLY -> calendar.add(Calendar.YEAR, direction)
        }
        loadExpenses()
    }

    private fun updateTabSelectionUI() {
        val selectedBg = ContextCompat.getDrawable(this, R.drawable.bg_tab_selected)
        val selectedTextColor = ContextCompat.getColor(this, R.color.white)
        val unselectedTextColor = ContextCompat.getColor(this, R.color.text_secondary)

        // Reset all
        binding.tabDaily.background = null
        binding.tabDaily.setTextColor(unselectedTextColor)
        binding.tabMonthly.background = null
        binding.tabMonthly.setTextColor(unselectedTextColor)
        binding.tabYearly.background = null
        binding.tabYearly.setTextColor(unselectedTextColor)

        // Highlight selected
        when (currentPeriod) {
            ViewPeriod.DAILY -> {
                binding.tabDaily.background = selectedBg
                binding.tabDaily.setTextColor(selectedTextColor)
            }
            ViewPeriod.MONTHLY -> {
                binding.tabMonthly.background = selectedBg
                binding.tabMonthly.setTextColor(selectedTextColor)
            }
            ViewPeriod.YEARLY -> {
                binding.tabYearly.background = selectedBg
                binding.tabYearly.setTextColor(selectedTextColor)
            }
        }
    }

    private fun getDateRange(): Triple<Long, Long, String> {
        val cal = calendar.clone() as Calendar

        return when (currentPeriod) {
            ViewPeriod.DAILY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                val titleFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
                Triple(start, end, titleFormat.format(cal.time))
            }
            ViewPeriod.MONTHLY -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                val titleFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                Triple(start, end, titleFormat.format(cal.time))
            }
            ViewPeriod.YEARLY -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                val year = cal.get(Calendar.YEAR)
                Triple(start, end, "Year $year")
            }
        }
    }

    private fun loadExpenses() {
        val (startTime, endTime, periodTitle) = getDateRange()
        binding.tvCurrentPeriodTitle.text = periodTitle

        val expenses = dbHelper.getExpensesByDateRange(startTime, endTime)
        val summaries = dbHelper.getCategorySummary(startTime, endTime)

        val totalAmount = expenses.sumOf { it.amount }
        binding.tvTotalAmount.text = String.format(Locale.US, "$%.2f", totalAmount)
        binding.tvTransactionCount.text = "${expenses.size} records"

        val topCategory = summaries.firstOrNull()?.category ?: "None"
        binding.tvTopCategory.text = topCategory

        binding.tvTransactionsHeader.text = "Transactions (${expenses.size})"

        // Populate Category Breakdown Cards
        binding.layoutCategoryList.removeAllViews()
        if (summaries.isNotEmpty()) {
            binding.cardCategoryBreakdown.visibility = View.VISIBLE
            for (summary in summaries) {
                val itemBinding = ItemCategorySummaryBinding.inflate(
                    layoutInflater,
                    binding.layoutCategoryList,
                    false
                )
                itemBinding.tvCategoryName.text = summary.category
                itemBinding.tvCategoryPercent.text = String.format(Locale.US, "%.1f%%", summary.percentage)
                itemBinding.tvCategoryAmount.text = String.format(Locale.US, "$%.2f", summary.totalAmount)
                itemBinding.progressCategory.progress = summary.percentage.toInt()

                val catColor = ExpenseAdapter.getCategoryColor(summary.category)
                itemBinding.progressCategory.progressTintList = ColorStateList.valueOf(catColor)

                binding.layoutCategoryList.addView(itemBinding.root)
            }
        } else {
            binding.cardCategoryBreakdown.visibility = View.GONE
        }

        // Empty state vs RecyclerView
        if (expenses.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvExpenses.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvExpenses.visibility = View.VISIBLE
            expenseAdapter.updateList(expenses)
        }
    }

    private fun showAddExpenseDialog() {
        val dialogBinding = DialogAddExpenseBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Setup Spinners
        val catAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        dialogBinding.spinnerCategory.adapter = catAdapter

        val payAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, paymentMethods)
        dialogBinding.spinnerPaymentMethod.adapter = payAdapter

        // Date selection
        val expenseCalendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        dialogBinding.btnPickDate.text = dateFormat.format(expenseCalendar.time)

        dialogBinding.btnPickDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    expenseCalendar.set(Calendar.YEAR, year)
                    expenseCalendar.set(Calendar.MONTH, month)
                    expenseCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    dialogBinding.btnPickDate.text = dateFormat.format(expenseCalendar.time)
                },
                expenseCalendar.get(Calendar.YEAR),
                expenseCalendar.get(Calendar.MONTH),
                expenseCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        dialogBinding.btnCancelExpense.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSaveExpense.setOnClickListener {
            val amountStr = dialogBinding.etExpenseAmount.text.toString().trim()
            val title = dialogBinding.etExpenseTitle.text.toString().trim()
            val category = dialogBinding.spinnerCategory.selectedItem.toString()
            val payment = dialogBinding.spinnerPaymentMethod.selectedItem.toString()
            val notes = dialogBinding.etExpenseNotes.text.toString().trim()

            if (amountStr.isEmpty()) {
                dialogBinding.etExpenseAmount.error = "Please enter an amount"
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0.0) {
                dialogBinding.etExpenseAmount.error = "Invalid amount"
                return@setOnClickListener
            }

            if (title.isEmpty()) {
                dialogBinding.etExpenseTitle.error = "Please enter description"
                return@setOnClickListener
            }

            val newExpense = Expense(
                title = title,
                amount = amount,
                category = category,
                date = expenseCalendar.timeInMillis,
                paymentMethod = payment,
                notes = notes
            )

            dbHelper.insertExpense(newExpense)
            dialog.dismiss()
            loadExpenses()
            Toast.makeText(this, "Expense recorded successfully", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun confirmDeleteExpense(expense: Expense) {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to remove '${expense.title}'?")
            .setPositiveButton("Delete") { _, _ ->
                dbHelper.deleteExpense(expense.id)
                loadExpenses()
                Toast.makeText(this, "Expense removed", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportPdfReport() {
        val (startTime, endTime, periodTitle) = getDateRange()
        val expenses = dbHelper.getExpensesByDateRange(startTime, endTime)
        val summaries = dbHelper.getCategorySummary(startTime, endTime)

        if (expenses.isEmpty()) {
            Toast.makeText(this, "No expenses to export for $periodTitle", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val file = PdfReportExporter.exportToPdf(this, periodTitle, expenses, summaries)
            Toast.makeText(this, "PDF generated: ${file.name}", Toast.LENGTH_SHORT).show()
            PdfReportExporter.openOrSharePdf(this, file)
        } catch (e: Exception) {
            Toast.makeText(this, "Error generating PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportExcelReport() {
        val (startTime, endTime, periodTitle) = getDateRange()
        val expenses = dbHelper.getExpensesByDateRange(startTime, endTime)
        val summaries = dbHelper.getCategorySummary(startTime, endTime)

        if (expenses.isEmpty()) {
            Toast.makeText(this, "No expenses to export for $periodTitle", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val file = ExcelCsvExporter.exportToExcelCsv(this, periodTitle, expenses, summaries)
            Toast.makeText(this, "Excel report generated: ${file.name}", Toast.LENGTH_SHORT).show()
            ExcelCsvExporter.openOrShareCsv(this, file)
        } catch (e: Exception) {
            Toast.makeText(this, "Error generating Excel report: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
