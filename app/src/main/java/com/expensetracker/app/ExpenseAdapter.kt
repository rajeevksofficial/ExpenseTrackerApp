package com.expensetracker.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
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
            binding.tvDateHeaderTitle.text = header.dateTitle
            binding.tvDateHeaderCount.text = "${header.count} ${if (header.count == 1) "item" else "items"}"
            binding.tvDateHeaderTotal.text = String.format(Locale.US, "%s%.2f", currencySymbol, header.totalAmount)
        }
    }

    inner class ExpenseViewHolder(private val binding: ItemExpenseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TransactionListItem.ExpenseItem) {
            val expense = item.expense
            binding.tvExpenseTitle.text = expense.title
            binding.tvExpenseCategory.text = expense.category

            if (item.isGrouped) {
                val timeStr = timeFormat.format(Date(expense.date))
                binding.tvExpenseDatePayment.text = "$timeStr • ${expense.paymentMethod}"
            } else {
                val dateStr = fullDateFormat.format(Date(expense.date))
                binding.tvExpenseDatePayment.text = "$dateStr • ${expense.paymentMethod}"
            }

            binding.tvExpenseAmount.text = String.format(Locale.US, "-%s%.2f", currencySymbol, expense.amount)

            // Category indicator badge color
            val catColor = getCategoryColor(expense.category)
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(catColor)
            }
            binding.viewCategoryColor.background = drawable

            binding.root.setOnClickListener {
                onEditClick(expense)
            }

            binding.btnEditExpense.setOnClickListener {
                onEditClick(expense)
            }

            binding.btnDeleteExpense.setOnClickListener {
                onDeleteClick(expense)
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_EXPENSE = 1

        fun getCategoryColor(category: String): Int {
            return when (category.lowercase(Locale.ROOT)) {
                "food & dining", "food", "dining" -> Color.parseColor("#F59E0B")
                "transportation", "transport", "fuel", "uber" -> Color.parseColor("#3B82F6")
                "groceries", "market" -> Color.parseColor("#10B981")
                "bills & utilities", "bills", "utilities" -> Color.parseColor("#8B5CF6")
                "entertainment", "movies", "games" -> Color.parseColor("#06B6D4")
                "health", "medical", "pharmacy" -> Color.parseColor("#EF4444")
                "shopping", "clothes" -> Color.parseColor("#EC4899")
                else -> Color.parseColor("#64748B")
            }
        }
    }
}
