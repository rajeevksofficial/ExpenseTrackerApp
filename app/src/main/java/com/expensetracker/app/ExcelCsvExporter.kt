package com.expensetracker.app

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelCsvExporter {

    fun exportToExcelCsv(
        context: Context,
        periodTitle: String,
        expenses: List<Expense>,
        categorySummaries: List<CategorySummary>
    ): File {
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()

        val fileName = "Expense_Report_${System.currentTimeMillis()}.csv"
        val file = File(reportsDir, fileName)

        val totalSpent = expenses.sumOf { it.amount }
        val currencySymbol = CurrencyManager.getCurrencySymbol(context)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val genFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                // Write UTF-8 BOM so Excel opens with proper encoding
                writer.write("\uFEFF")

                // 1. Report Metadata
                writer.write("EXPENSE TRACKER FINANCIAL REPORT\n")
                writer.write("Report Period,$periodTitle\n")
                writer.write("Generated On,${genFormat.format(Date())}\n")
                writer.write("Total Expenditure,${String.format(Locale.US, "%.2f", totalSpent)}\n")
                writer.write("Total Transactions,${expenses.size}\n\n")

                // 2. Category Summary Table
                writer.write("--- CATEGORY BREAKDOWN SUMMARY ---\n")
                writer.write("Category,Amount ($currencySymbol),Share (%)\n")
                for (cat in categorySummaries) {
                    writer.write("\"${cat.category}\",${String.format(Locale.US, "%.2f", cat.totalAmount)},${String.format(Locale.US, "%.2f%%", cat.percentage)}\n")
                }
                writer.write("\n")

                // 3. Itemized Transactions Table
                writer.write("--- ITEMIZED TRANSACTIONS ---\n")
                writer.write("ID,Date,Time,Description,Category,Payment Method,Amount ($currencySymbol),Notes\n")

                for (exp in expenses) {
                    val d = Date(exp.date)
                    val dateStr = dateFormat.format(d)
                    val timeStr = timeFormat.format(d)
                    val titleEscaped = exp.title.replace("\"", "\"\"")
                    val catEscaped = exp.category.replace("\"", "\"\"")
                    val payEscaped = exp.paymentMethod.replace("\"", "\"\"")
                    val notesEscaped = exp.notes.replace("\"", "\"\"")

                    writer.write("${exp.id},\"$dateStr\",\"$timeStr\",\"$titleEscaped\",\"$catEscaped\",\"$payEscaped\",${String.format(Locale.US, "%.2f", exp.amount)},\"$notesEscaped\"\n")
                }

                // Total Summary Row
                writer.write(",,,,Total,${String.format(Locale.US, "%.2f", totalSpent)},\n")
            }
        }

        return file
    }

    fun openOrShareCsv(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Expense Report (Excel CSV)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share or Open with Excel"))
    }
}
