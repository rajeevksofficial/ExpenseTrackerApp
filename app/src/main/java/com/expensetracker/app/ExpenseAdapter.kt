package com.expensetracker.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.expensetracker.app.databinding.ItemDateHeaderBinding
import com.expensetracker.app.databinding.ItemExpenseBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ExpenseAdapter(
    private var currencySymbol: String = "$",
    private val onEditClick: (Expense) -> Unit,
    private val onDeleteClick: (Expense) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<TransactionListItem> = emptyList()

    private val fullDateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val headerDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    private val dayGroupFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

    fun updateList(
        expenses: List<Expense>,
        groupByDate: Boolean,
        currency: String
    ) {
        this.currencySymbol = currency
        if (!groupByDate) {
            items = expenses.map { TransactionListItem.ExpenseItem(it, isGrouped = false) }
        } else {
            // Group expenses by calendar date while preserving DESC order
            val grouped = LinkedHashMap<String, MutableList<Expense>>()
            for (expense in expenses) {
                val key = dayGroupFormat.format(Date(expense.date))
                grouped.getOrPut(key) { mutableListOf() }.add(expense)
            }

            val list = mutableListOf<TransactionListItem>()
            for ((_, dayExpenses) in grouped) {
                val firstDate = dayExpenses.first().date
                val headerTitle = formatDateHeader(firstDate)
                val dayTotal = dayExpenses.sumOf { it.amount }

                list.add(
                    TransactionListItem.DateHeader(
                        dateTitle = headerTitle,
                        count = dayExpenses.size,
                        totalAmount = dayTotal
                    )
                )

                for (expense in dayExpenses) {
                    list.add(TransactionListItem.ExpenseItem(expense, isGrouped = true))
                }
            }
            items = list
        }
        notifyDataSetChanged()
    }

    private fun formatDateHeader(dateMillis: Long): String {
        val nowCal = Calendar.getInstance()
        val itemCal = Calendar.getInstance().apply { timeInMillis = dateMillis }

        val isSameYear = nowCal.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR)
        val isToday = isSameYear && nowCal.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR)

        nowCal.add(Calendar.DAY_OF_YEAR, -1)
        val isYesterday = isSameYear && nowCal.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR)

        val dateStr = headerDateFormat.format(Date(dateMillis))
        val dayOfWeek = dayOfWeekFormat.format(Date(dateMillis))

        return when {
            isToday -> "Today • $dateStr"
            isYesterday -> "Yesterday • $dateStr"
            else -> "$dayOfWeek, $dateStr"
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is TransactionListItem.DateHeader -> VIEW_TYPE_HEADER
            is TransactionListItem.ExpenseItem -> VIEW_TYPE_EXPENSE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ItemDateHeaderBinding.inflate(inflater, parent, false)
                DateHeaderViewHolder(binding)
            }
            else -> {
                val binding = ItemExpenseBinding.inflate(inflater, parent, false)
                ExpenseViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is TransactionListItem.DateHeader -> (holder as DateHeaderViewHolder).bind(item)
            is TransactionListItem.ExpenseItem -> (holder as ExpenseViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class DateHeaderViewHolder(private val binding: ItemDateHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(header: TransactionListItem.DateHeader) {
            val context = binding.root.context
            binding.tvDateHeaderTitle.text = header.dateTitle
            binding.tvDateHeaderTitle.setTextColor(ContextCompat.getColor(context, R.color.transaction_date_yellow))
            binding.tvDateHeaderCount.text = "${header.count} ${if (header.count == 1) "item" else "items"}"
            binding.tvDateHeaderTotal.text = String.format(Locale.US, "%s%.2f", currencySymbol, header.totalAmount)
            binding.tvDateHeaderTotal.setTextColor(ContextCompat.getColor(context, R.color.total_spent_red))
        }
    }

    inner class ExpenseViewHolder(private val binding: ItemExpenseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TransactionListItem.ExpenseItem) {
            val expense = item.expense
            val context = binding.root.context
            binding.tvExpenseTitle.text = expense.title
            binding.tvExpenseCategory.text = expense.category
            binding.tvExpensePaymentMethod.text = getPaymentMethodWithIcon(expense.paymentMethod)

            if (item.isGrouped) {
                binding.tvExpenseDate.text = timeFormat.format(Date(expense.date))
            } else {
                binding.tvExpenseDate.text = fullDateFormat.format(Date(expense.date))
            }
            binding.tvExpenseDate.setTextColor(ContextCompat.getColor(context, R.color.transaction_date_yellow))

            binding.tvExpenseAmount.text = String.format(Locale.US, "-%s%.2f", currencySymbol, expense.amount)

            val catColor = getCategoryColor(expense.category)

            // Category avatar squircle with 18% opacity tinted background, border, and colored initial
            val avatarBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(context, 12f)
                setColor(getTintedColor(catColor, 0.18f))
                setStroke(dpToPx(context, 1.5f).toInt(), catColor)
            }
            binding.layoutCategoryAvatar.background = avatarBg
            binding.tvCategoryInitial.text = getCategoryInitial(expense.category)
            binding.tvCategoryInitial.setTextColor(catColor)

            // Category pill badge
            val pillBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(context, 6f)
                setColor(getTintedColor(catColor, 0.16f))
            }
            binding.tvExpenseCategory.background = pillBg
            binding.tvExpenseCategory.setTextColor(catColor)

            binding.root.setOnClickListener {
                onEditClick(expense)
            }

            binding.btnEditExpense.setOnClickListener {
                onEditClick(expense)
            }

            binding.btnDeleteExpense.setOnClickListener {
                onDeleteClick(expense)
            }

            binding.root.setOnLongClickListener {
                onDeleteClick(expense)
                true
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_EXPENSE = 1

        fun dpToPx(context: android.content.Context, dp: Float): Float = dp * context.resources.displayMetrics.density

        fun getTintedColor(color: Int, alphaPercent: Float): Int {
            val alpha = (255 * alphaPercent).toInt().coerceIn(0, 255)
            return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
        }

        fun getCategoryColor(category: String): Int {
            val lower = category.lowercase(Locale.ROOT)
            return when {
                lower.contains("grocer") || lower.contains("market") -> Color.parseColor("#10B981") // Vibrant Emerald Green
                lower.contains("food") || lower.contains("dining") || lower.contains("restaurant") || lower.contains("cafe") -> Color.parseColor("#FF9F1C") // Vibrant Amber-Orange
                lower.contains("transport") || lower.contains("fuel") || lower.contains("uber") || lower.contains("metro") -> Color.parseColor("#0EA5E9") // Electric Sky Blue
                lower.contains("bill") || lower.contains("utilit") || lower.contains("electric") || lower.contains("internet") -> Color.parseColor("#A855F7") // Electric Purple
                lower.contains("entertain") || lower.contains("movie") || lower.contains("game") -> Color.parseColor("#EC4899") // Neon Pink
                lower.contains("health") || lower.contains("medic") || lower.contains("pharm") -> Color.parseColor("#EF4444") // Coral Red
                lower.contains("shop") || lower.contains("cloth") -> Color.parseColor("#F59E0B") // Golden Amber
                else -> Color.parseColor("#6366F1") // Electric Indigo
            }
        }

        fun getCategoryInitial(category: String): String {
            val clean = category.trim()
            val lower = clean.lowercase(Locale.ROOT)
            return when {
                lower.contains("grocer") -> "G"
                lower.contains("food") || lower.contains("dining") -> "F"
                lower.contains("transport") -> "T"
                lower.contains("bill") || lower.contains("utilit") -> "B"
                lower.contains("entertain") -> "E"
                lower.contains("health") -> "H"
                lower.contains("shop") -> "S"
                clean.isNotEmpty() -> clean.first().uppercase()
                else -> "•"
            }
        }

        fun getPaymentMethodWithIcon(method: String): String {
            val clean = method.trim()
            val lower = clean.lowercase(Locale.ROOT)
            return when {
                lower.contains("card") -> "💳 $clean"
                lower.contains("upi") -> "⚡ $clean"
                lower.contains("cash") -> "💵 $clean"
                lower.contains("net") || lower.contains("bank") -> "🏦 $clean"
                clean.isNotEmpty() -> clean
                else -> "Card"
            }
        }
    }
}
