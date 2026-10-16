package com.expensetracker.app

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.expensetracker.app.databinding.ActivityMainBinding
import com.expensetracker.app.databinding.DialogAddExpenseBinding
import com.expensetracker.app.databinding.DialogSettingsBinding
import com.expensetracker.app.databinding.ItemCategorySummaryBinding
import com.expensetracker.app.databinding.ItemMonthSummaryBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: ExpenseDatabaseHelper
    private lateinit var expenseAdapter: ExpenseAdapter

    private val importCsvLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { handleCsvImportUri(it) }
    }

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
        expenseAdapter = ExpenseAdapter(
            currencySymbol = CurrencyManager.getCurrencySymbol(this),
            onEditClick = { expense ->
                showAddOrEditExpenseDialog(expense)
            },
            onDeleteClick = { expense ->
                confirmDeleteExpense(expense)
            }
        )
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

        // Export Menu Button (PDF & Excel)
        binding.btnExport.setOnClickListener { view ->
            showExportMenu(view)
        }

        // Import CSV Button
        binding.btnImportCsv.setOnClickListener {
            launchCsvPicker()
        }

        // Quick Import CSV Button in Empty State
        binding.btnEmptyImportCsv.setOnClickListener {
            launchCsvPicker()
        }

        // Settings Button
        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }

        // Monthly Group View Toggle
        binding.btnToggleGroupView.setOnClickListener {
            val current = CurrencyManager.isGroupByDateEnabled(this)
            CurrencyManager.setGroupByDateEnabled(this, !current)
            loadExpenses()
        }

        // Floating Action Button
        binding.fabAddExpense.setOnClickListener {
            showAddOrEditExpenseDialog(null)
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

    private fun getMonthColor(monthIndex: Int): Int {
        return when (monthIndex % 12) {
            0 -> Color.parseColor("#06B6D4") // Jan: Cyan
            1 -> Color.parseColor("#EC4899") // Feb: Rose Pink
            2 -> Color.parseColor("#10B981") // Mar: Emerald
            3 -> Color.parseColor("#14B8A6") // Apr: Teal
            4 -> Color.parseColor("#F59E0B") // May: Golden Amber
            5 -> Color.parseColor("#F97316") // Jun: Sunset Orange
            6 -> Color.parseColor("#E11D48") // Jul: Crimson Red
            7 -> Color.parseColor("#6366F1") // Aug: Electric Indigo
            8 -> Color.parseColor("#8B5CF6") // Sep: Vivid Violet
            9 -> Color.parseColor("#A855F7") // Oct: Neon Purple
            10 -> Color.parseColor("#3B82F6") // Nov: Royal Blue
            11 -> Color.parseColor("#EF4444") // Dec: Coral Red
            else -> Color.parseColor("#10B981")
        }
    }

    private fun loadExpenses() {
        val (startTime, endTime, periodTitle) = getDateRange()
        binding.tvCurrentPeriodTitle.text = periodTitle

        val expenses = dbHelper.getExpensesByDateRange(startTime, endTime)
        val summaries = dbHelper.getCategorySummary(startTime, endTime)

        val currencySymbol = CurrencyManager.getCurrencySymbol(this)
        val totalAmount = expenses.sumOf { it.amount }
        binding.tvTotalAmount.text = CurrencyManager.formatAmount(this, totalAmount)
        binding.tvTransactionCount.text = "${expenses.size} records"

        val topCategory = summaries.firstOrNull()?.category ?: "None"
        binding.tvTopCategory.text = topCategory

        binding.tvTransactionsHeader.text = "Transactions (${expenses.size})"

        // Configure Group View Toggle Button in Monthly view
        if (currentPeriod == ViewPeriod.MONTHLY && expenses.isNotEmpty()) {
            binding.btnToggleGroupView.visibility = View.VISIBLE
            val isGrouped = CurrencyManager.isGroupByDateEnabled(this)
            if (isGrouped) {
                binding.btnToggleGroupView.text = "Grouped by Date"
                binding.btnToggleGroupView.setBackgroundResource(R.drawable.bg_month_badge_active)
                binding.btnToggleGroupView.setTextColor(ContextCompat.getColor(this, R.color.primary))
            } else {
                binding.btnToggleGroupView.text = "Flat List"
                binding.btnToggleGroupView.setBackgroundResource(R.drawable.bg_month_badge)
                binding.btnToggleGroupView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            }
        } else {
            binding.btnToggleGroupView.visibility = View.GONE
        }

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
                val catColor = ExpenseAdapter.getCategoryColor(summary.category)

                itemBinding.tvCategoryName.text = summary.category
                itemBinding.tvCategoryPercent.text = String.format(Locale.US, "%.1f%%", summary.percentage)
                itemBinding.tvCategoryAmount.text = CurrencyManager.formatAmount(this, summary.totalAmount)
                itemBinding.progressCategory.progress = summary.percentage.toInt().coerceAtLeast(1)

                // Category indicator dot
                val dotDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(catColor)
                }
                itemBinding.viewCategoryDot.background = dotDrawable

                // Category percentage pill tag
                val pillBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = ExpenseAdapter.dpToPx(this@MainActivity, 6f)
                    setColor(ExpenseAdapter.getTintedColor(catColor, 0.16f))
                }
                itemBinding.tvCategoryPercent.background = pillBg
                itemBinding.tvCategoryPercent.setTextColor(catColor)

                // Progress bar tint
                itemBinding.progressCategory.progressTintList = ColorStateList.valueOf(catColor)

                binding.layoutCategoryList.addView(itemBinding.root)
            }
        } else {
            binding.cardCategoryBreakdown.visibility = View.GONE
        }

        // Populate Month-wise Total Amount Spent Card (in Yearly View)
        binding.layoutMonthlyList.removeAllViews()
        if (currentPeriod == ViewPeriod.YEARLY) {
            val year = calendar.get(Calendar.YEAR)
            val monthlySummaries = dbHelper.getMonthlySummaryForYear(year)
            val hasYearlyExpenses = monthlySummaries.any { it.totalAmount > 0.0 }

            if (hasYearlyExpenses) {
                binding.cardMonthlyBreakdown.visibility = View.VISIBLE
                val activeMonthsCount = monthlySummaries.count { it.totalAmount > 0.0 }
                binding.tvMonthlyBreakdownSubtitle.text = "$activeMonthsCount active month${if (activeMonthsCount > 1) "s" else ""}"

                val maxMonthAmount = monthlySummaries.maxOfOrNull { it.totalAmount }?.takeIf { it > 0.0 } ?: 1.0

                for (monthSummary in monthlySummaries) {
                    val itemBinding = ItemMonthSummaryBinding.inflate(
                        layoutInflater,
                        binding.layoutMonthlyList,
                        false
                    )

                    val monthColor = getMonthColor(monthSummary.monthIndex)
                    itemBinding.tvMonthBadge.text = monthSummary.monthShortName
                    itemBinding.tvMonthName.text = monthSummary.monthName

                    if (monthSummary.totalAmount > 0.0) {
                        // Highlight top spending month
                        val isTopMonth = (monthSummary.totalAmount == maxMonthAmount && maxMonthAmount > 0.0 && activeMonthsCount > 1)
                        if (isTopMonth) {
                            itemBinding.tvMonthHighlight.visibility = View.VISIBLE
                            val goldDrawable = GradientDrawable().apply {
                                shape = GradientDrawable.RECTANGLE
                                cornerRadius = ExpenseAdapter.dpToPx(this@MainActivity, 4f)
                                setColor(Color.parseColor("#33FBBF24"))
                                setStroke(ExpenseAdapter.dpToPx(this@MainActivity, 1f).toInt(), Color.parseColor("#FBBF24"))
                            }
                            itemBinding.tvMonthHighlight.background = goldDrawable
                        } else {
                            itemBinding.tvMonthHighlight.visibility = View.GONE
                        }

                        // Month Badge Pill with month-specific color and border
                        val badgeBg = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = ExpenseAdapter.dpToPx(this@MainActivity, 12f)
                            setColor(ExpenseAdapter.getTintedColor(monthColor, 0.18f))
                            setStroke(ExpenseAdapter.dpToPx(this@MainActivity, 1.5f).toInt(), monthColor)
                        }
                        itemBinding.tvMonthBadge.background = badgeBg
                        itemBinding.tvMonthBadge.setTextColor(monthColor)

                        // Month Amount & Count
                        itemBinding.tvMonthAmount.text = CurrencyManager.formatAmount(this@MainActivity, monthSummary.totalAmount)
                        itemBinding.tvMonthAmount.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))

                        // Month Percentage Pill
                        itemBinding.tvMonthPercent.text = String.format(Locale.US, "%.1f%%", monthSummary.percentage)
                        val pctBg = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = ExpenseAdapter.dpToPx(this@MainActivity, 6f)
                            setColor(ExpenseAdapter.getTintedColor(monthColor, 0.16f))
                        }
                        itemBinding.tvMonthPercent.background = pctBg
                        itemBinding.tvMonthPercent.setTextColor(monthColor)

                        itemBinding.tvMonthCount.text = "${monthSummary.transactionCount} transaction${if (monthSummary.transactionCount > 1) "s" else ""}"

                        val progressPct = ((monthSummary.totalAmount / maxMonthAmount) * 100).toInt().coerceIn(5, 100)
                        itemBinding.progressMonth.progress = progressPct
                        itemBinding.progressMonth.progressTintList = ColorStateList.valueOf(monthColor)

                        itemBinding.ivMonthArrow.imageTintList = ColorStateList.valueOf(monthColor)
                        itemBinding.ivMonthArrow.alpha = 0.85f
                    } else {
                        itemBinding.tvMonthHighlight.visibility = View.GONE
                        itemBinding.tvMonthBadge.setBackgroundResource(R.drawable.bg_month_badge)
                        itemBinding.tvMonthBadge.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_muted))
                        itemBinding.tvMonthAmount.text = CurrencyManager.formatAmount(this@MainActivity, 0.0)
                        itemBinding.tvMonthAmount.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_muted))
                        itemBinding.tvMonthPercent.text = "0%"
                        itemBinding.tvMonthPercent.background = null
                        itemBinding.tvMonthPercent.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_muted))
                        itemBinding.tvMonthCount.text = getString(R.string.no_expenses_month)
                        itemBinding.progressMonth.progress = 0
                        itemBinding.ivMonthArrow.imageTintList = null
                        itemBinding.ivMonthArrow.alpha = 0.25f
                    }

                    // Tapping any month switches view to that month in Monthly mode
                    itemBinding.root.setOnClickListener {
                        calendar.set(Calendar.MONTH, monthSummary.monthIndex)
                        currentPeriod = ViewPeriod.MONTHLY
                        updateTabSelectionUI()
                        loadExpenses()
                    }

                    binding.layoutMonthlyList.addView(itemBinding.root)
                }
            } else {
                binding.cardMonthlyBreakdown.visibility = View.GONE
            }
        } else {
            binding.cardMonthlyBreakdown.visibility = View.GONE
        }

        // Empty state vs RecyclerView
        if (expenses.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvExpenses.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvExpenses.visibility = View.VISIBLE
            val shouldGroup = (currentPeriod == ViewPeriod.MONTHLY && CurrencyManager.isGroupByDateEnabled(this))
            expenseAdapter.updateList(expenses, groupByDate = shouldGroup, currency = currencySymbol)
        }
    }

    private fun showAddOrEditExpenseDialog(existingExpense: Expense? = null) {
        val isEditMode = existingExpense != null
        val dialogBinding = DialogAddExpenseBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Set Title & Button text
        dialogBinding.tvDialogTitle.text = if (isEditMode) {
            getString(R.string.dialog_edit_title)
        } else {
            getString(R.string.dialog_add_title)
        }
        dialogBinding.btnSaveExpense.text = if (isEditMode) {
            getString(R.string.btn_update)
        } else {
            getString(R.string.btn_save)
        }

        // Set amount label with current currency
        val currencySymbol = CurrencyManager.getCurrencySymbol(this)
        dialogBinding.tvLabelAmount.text = "Amount ($currencySymbol)"

        // Setup Spinners
        val catAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        dialogBinding.spinnerCategory.adapter = catAdapter

        val payAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, paymentMethods)
        dialogBinding.spinnerPaymentMethod.adapter = payAdapter

        // Setup Autocomplete Intellisense Dropdowns
        setupIntellisenseDropdowns(dialogBinding)

        // Date selection:
        // When navigating daily-wise, clicking + automatically captures that active viewed date
        val expenseCalendar = Calendar.getInstance()
        if (existingExpense != null) {
            expenseCalendar.timeInMillis = existingExpense.date
        } else if (currentPeriod == ViewPeriod.DAILY) {
            expenseCalendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR))
            expenseCalendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH))
            expenseCalendar.set(Calendar.DAY_OF_MONTH, calendar.get(Calendar.DAY_OF_MONTH))
        }

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

        // Pre-fill fields if in Edit mode
        if (existingExpense != null) {
            dialogBinding.etExpenseAmount.setText(
                if (existingExpense.amount % 1.0 == 0.0) {
                    existingExpense.amount.toInt().toString()
                } else {
                    String.format(Locale.US, "%.2f", existingExpense.amount)
                }
            )
            dialogBinding.etExpenseTitle.setText(existingExpense.title)
            dialogBinding.etExpenseNotes.setText(existingExpense.notes)

            val catPos = categories.indexOf(existingExpense.category)
            if (catPos >= 0) dialogBinding.spinnerCategory.setSelection(catPos)

            val payPos = paymentMethods.indexOf(existingExpense.paymentMethod)
            if (payPos >= 0) dialogBinding.spinnerPaymentMethod.setSelection(payPos)
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

            if (existingExpense != null) {
                val updatedExpense = existingExpense.copy(
                    title = title,
                    amount = amount,
                    category = category,
                    date = expenseCalendar.timeInMillis,
                    paymentMethod = payment,
                    notes = notes
                )
                dbHelper.updateExpense(updatedExpense)
                Toast.makeText(this, getString(R.string.expense_updated), Toast.LENGTH_SHORT).show()
            } else {
                val newExpense = Expense(
                    title = title,
                    amount = amount,
                    category = category,
                    date = expenseCalendar.timeInMillis,
                    paymentMethod = payment,
                    notes = notes
                )
                dbHelper.insertExpense(newExpense)
                Toast.makeText(this, "Expense recorded successfully", Toast.LENGTH_SHORT).show()
            }

            dialog.dismiss()
            loadExpenses()
        }

        dialog.show()
    }

    private fun setupIntellisenseDropdowns(dialogBinding: DialogAddExpenseBinding) {
        val defaultTitles = listOf(
            "Morning Coffee & Croissant",
            "Metro Train Card Recharge",
            "Grocery Market",
            "High Speed Internet Bill",
            "Cinema Tickets & Snacks",
            "Pharmacy & Vitamins",
            "Summer Casual Shirt",
            "Car Petrol Refuel",
            "Dinner with Family",
            "Office Lunch",
            "Mobile Phone Recharge",
            "Electricity Bill",
            "Gym Membership",
            "Uber Ride",
            "Amazon Shopping",
            "Flight Booking",
            "Book Store"
        )
        val titleSuggestions = (dbHelper.getDistinctTitles() + defaultTitles).distinct()
        val titleAdapter = SubstringAutoCompleteAdapter(
            this,
            R.layout.item_dropdown_suggestion,
            R.id.tvSuggestion,
            titleSuggestions
        )
        dialogBinding.etExpenseTitle.setAdapter(titleAdapter)
        dialogBinding.etExpenseTitle.threshold = 1
        dialogBinding.etExpenseTitle.setOnClickListener {
            if (dialogBinding.etExpenseTitle.text.isNotEmpty()) {
                dialogBinding.etExpenseTitle.showDropDown()
            }
        }

        // Auto-select corresponding category when picking a suggestion
        dialogBinding.etExpenseTitle.setOnItemClickListener { parent, _, position, _ ->
            val selectedTitle = parent.getItemAtPosition(position) as? String
            selectedTitle?.let { title ->
                getSuggestedCategory(title)?.let { suggestedCat ->
                    val catIndex = categories.indexOf(suggestedCat)
                    if (catIndex >= 0) {
                        dialogBinding.spinnerCategory.setSelection(catIndex)
                    }
                }
            }
        }

        val defaultNotes = listOf(
            "Bakery downtown",
            "Weekly commute",
            "Produce & dairy",
            "Monthly fiber plan",
            "Weekend movie with friends",
            "Prescription refill",
            "Weekend mall sale",
            "Full tank",
            "Paid via credit card",
            "Split with colleagues",
            "Monthly subscription",
            "Reimbursement pending",
            "Emergency expense",
            "Personal care"
        )
        val noteSuggestions = (dbHelper.getDistinctNotes() + defaultNotes).distinct()
        val noteAdapter = SubstringAutoCompleteAdapter(
            this,
            R.layout.item_dropdown_suggestion,
            R.id.tvSuggestion,
            noteSuggestions
        )
        dialogBinding.etExpenseNotes.setAdapter(noteAdapter)
        dialogBinding.etExpenseNotes.threshold = 1
        dialogBinding.etExpenseNotes.setOnClickListener {
            if (dialogBinding.etExpenseNotes.text.isNotEmpty()) {
                dialogBinding.etExpenseNotes.showDropDown()
            }
        }
    }

    private fun getSuggestedCategory(title: String): String? {
        val lower = title.lowercase(Locale.ROOT)
        return when {
            lower.contains("coffee") || lower.contains("dinner") || lower.contains("lunch") ||
                lower.contains("restaurant") || lower.contains("cafe") || lower.contains("food") ||
                lower.contains("snack") || lower.contains("bakery") -> "Food & Dining"
            lower.contains("metro") || lower.contains("train") || lower.contains("fuel") ||
                lower.contains("petrol") || lower.contains("diesel") || lower.contains("uber") ||
                lower.contains("cab") || lower.contains("bus") || lower.contains("commute") -> "Transportation"
            lower.contains("grocer") || lower.contains("supermarket") || lower.contains("market") ||
                lower.contains("produce") || lower.contains("milk") || lower.contains("dairy") -> "Groceries"
            lower.contains("bill") || lower.contains("internet") || lower.contains("electric") ||
                lower.contains("recharge") || lower.contains("water") || lower.contains("wifi") -> "Bills & Utilities"
            lower.contains("cinema") || lower.contains("movie") || lower.contains("game") ||
                lower.contains("netflix") || lower.contains("concert") -> "Entertainment"
            lower.contains("pharmacy") || lower.contains("medicine") || lower.contains("doctor") ||
                lower.contains("hospital") || lower.contains("vitamin") || lower.contains("health") -> "Health"
            lower.contains("shirt") || lower.contains("shoes") || lower.contains("amazon") ||
                lower.contains("mall") || lower.contains("shopping") || lower.contains("clothes") -> "Shopping"
            else -> null
        }
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

    private fun showSettingsDialog() {
        val dialogBinding = DialogSettingsBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var selectedCurrency = CurrencyManager.getCurrencySymbol(this)
        var isGroupEnabled = CurrencyManager.isGroupByDateEnabled(this)

        fun updateCurrencyUI() {
            if (selectedCurrency == CurrencyManager.CURRENCY_DOLLAR) {
                dialogBinding.cardCurrencyDollar.setBackgroundResource(R.drawable.bg_card_selected)
                dialogBinding.ivCheckDollar.visibility = View.VISIBLE
                dialogBinding.cardCurrencyRupee.setBackgroundResource(R.drawable.bg_card)
                dialogBinding.ivCheckRupee.visibility = View.GONE
            } else {
                dialogBinding.cardCurrencyDollar.setBackgroundResource(R.drawable.bg_card)
                dialogBinding.ivCheckDollar.visibility = View.GONE
                dialogBinding.cardCurrencyRupee.setBackgroundResource(R.drawable.bg_card_selected)
                dialogBinding.ivCheckRupee.visibility = View.VISIBLE
            }
        }

        updateCurrencyUI()

        dialogBinding.cardCurrencyDollar.setOnClickListener {
            selectedCurrency = CurrencyManager.CURRENCY_DOLLAR
            updateCurrencyUI()
        }

        dialogBinding.cardCurrencyRupee.setOnClickListener {
            selectedCurrency = CurrencyManager.CURRENCY_RUPEE
            updateCurrencyUI()
        }

        dialogBinding.switchGroupByDate.isChecked = isGroupEnabled
        dialogBinding.switchGroupByDate.setOnCheckedChangeListener { _, isChecked ->
            isGroupEnabled = isChecked
        }

        dialogBinding.btnCloseSettings.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.cardSettingsImportCsv.setOnClickListener {
            dialog.dismiss()
            launchCsvPicker()
        }

        dialogBinding.btnApplySettings.setOnClickListener {
            CurrencyManager.setCurrencySymbol(this, selectedCurrency)
            CurrencyManager.setGroupByDateEnabled(this, isGroupEnabled)
            dialog.dismiss()

            loadExpenses()
            val currencyName = if (selectedCurrency == CurrencyManager.CURRENCY_RUPEE) "Indian Rupee (₹)" else "US Dollar ($)"
            Toast.makeText(this, "Currency set to $currencyName", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun showExportMenu(anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        popup.menuInflater.inflate(R.menu.menu_export, popup.menu)
        try {
            val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldMPopup.isAccessible = true
            val mPopup = fieldMPopup.get(popup)
            mPopup.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.java).invoke(mPopup, true)
        } catch (_: Exception) {}

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_export_pdf -> {
                    exportPdfReport()
                    true
                }
                R.id.action_export_excel -> {
                    exportExcelReport()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun launchCsvPicker() {
        try {
            importCsvLauncher.launch(
                arrayOf(
                    "text/csv",
                    "text/comma-separated-values",
                    "text/plain",
                    "application/vnd.ms-excel",
                    "application/csv",
                    "*/*"
                )
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to launch file selector: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleCsvImportUri(uri: Uri) {
        val result = CsvImporter.readAndParseCsv(this, uri)
        result.fold(
            onSuccess = { preview ->
                showImportConfirmationDialog(preview)
            },
            onFailure = { error ->
                AlertDialog.Builder(this)
                    .setTitle("Import Failed")
                    .setMessage(error.message ?: "Failed to read CSV file. Please make sure the file contains valid expense rows.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        )
    }

    private fun showImportConfirmationDialog(preview: CsvImporter.ImportPreview) {
        val sb = StringBuilder()
        sb.append("File: ${preview.fileName}\n")
        sb.append("Total: ${preview.totalCount} transaction${if (preview.totalCount > 1) "s" else ""}\n")
        sb.append("Total Amount: ${CurrencyManager.formatAmount(this, preview.totalAmount)}\n\n")
        sb.append("Sample items:\n")
        for (item in preview.sampleItems) {
            sb.append("• ${item.title} — ${CurrencyManager.formatAmount(this, item.amount)} (${item.category})\n")
        }
        if (preview.totalCount > preview.sampleItems.size) {
            sb.append("... and ${preview.totalCount - preview.sampleItems.size} more\n")
        }
        sb.append("\nDo you want to import these transactions into your records?")

        AlertDialog.Builder(this)
            .setTitle("Import CSV Transactions")
            .setMessage(sb.toString())
            .setPositiveButton("Import All (${preview.totalCount})") { _, _ ->
                val count = dbHelper.insertExpenses(preview.expenses)
                loadExpenses()
                Toast.makeText(this, "Successfully imported $count transactions!", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
