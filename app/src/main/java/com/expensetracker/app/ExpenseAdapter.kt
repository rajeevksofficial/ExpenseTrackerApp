package com.expensetracker.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.expensetracker.app.databinding.ItemExpenseBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseAdapter(
    private var expenses: List<Expense>,
    private val onDeleteClick: (Expense) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    fun updateList(newList: List<Expense>) {
        expenses = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(expenses[position])
    }

    override fun getItemCount(): Int = expenses.size

    inner class ExpenseViewHolder(private val binding: ItemExpenseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(expense: Expense) {
            binding.tvExpenseTitle.text = expense.title
            binding.tvExpenseCategory.text = expense.category

            val dateStr = dateFormat.format(Date(expense.date))
            binding.tvExpenseDatePayment.text = "$dateStr • ${expense.paymentMethod}"
            binding.tvExpenseAmount.text = String.format(Locale.US, "-$%.2f", expense.amount)

            // Category indicator badge color
            val catColor = getCategoryColor(expense.category)
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(catColor)
            }
            binding.viewCategoryColor.background = drawable

            binding.btnDeleteExpense.setOnClickListener {
                onDeleteClick(expense)
            }
        }
    }

    companion object {
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
