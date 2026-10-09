package com.expensetracker.app

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportExporter {

    fun exportToPdf(
        context: Context,
        periodTitle: String,
        expenses: List<Expense>,
        categorySummaries: List<CategorySummary>
    ): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDoc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val primaryColor = Color.parseColor("#10B981")
        val darkColor = Color.parseColor("#0F172A")
        val grayColor = Color.parseColor("#64748B")
        val lightBg = Color.parseColor("#F1F5F9")
        val altRowBg = Color.parseColor("#F8FAFC")

        var y = 0f

        // 1. Top Header Banner
        paint.color = primaryColor
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        // Title
        paint.color = Color.WHITE
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Expense Tracker Report", 36f, 45f, paint)

        // Subtitle / Period
        paint.textSize = 13f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Period: $periodTitle", 36f, 68f, paint)

        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val generatedDate = dateFormat.format(Date())
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Generated: $generatedDate", 559f, 68f, paint)
        paint.textAlign = Paint.Align.LEFT

        y = 115f

        // 2. Summary Box
        val totalSpent = expenses.sumOf { it.amount }
        paint.color = lightBg
        canvas.drawRoundRect(36f, y, 559f, y + 65f, 12f, 12f, paint)

        paint.color = darkColor
        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("TOTAL EXPENDITURE", 54f, y + 25f, paint)
        canvas.drawText("TRANSACTIONS", 240f, y + 25f, paint)
        canvas.drawText("TOP CATEGORY", 390f, y + 25f, paint)

        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = primaryColor
        canvas.drawText(String.format(Locale.US, "$%.2f", totalSpent), 54f, y + 50f, paint)

        paint.color = darkColor
        canvas.drawText("${expenses.size}", 240f, y + 50f, paint)
        val topCat = categorySummaries.firstOrNull()?.category ?: "N/A"
        canvas.drawText(topCat, 390f, y + 50f, paint)

        y += 95f

        // 3. Category Breakdown (Compact Table)
        paint.color = darkColor
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Category Breakdown", 36f, y, paint)
        y += 15f

        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = grayColor
        canvas.drawText("CATEGORY", 40f, y, paint)
        canvas.drawText("AMOUNT", 240f, y, paint)
        canvas.drawText("SHARE (%)", 360f, y, paint)
        y += 6f
        paint.strokeWidth = 1f
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawLine(36f, y, 559f, y, paint)
        y += 14f

        paint.typeface = Typeface.DEFAULT
        paint.color = darkColor
        for (cat in categorySummaries.take(5)) {
            canvas.drawText(cat.category, 40f, y, paint)
            canvas.drawText(String.format(Locale.US, "$%.2f", cat.totalAmount), 240f, y, paint)
            canvas.drawText(String.format(Locale.US, "%.1f%%", cat.percentage), 360f, y, paint)
            y += 16f
        }

        y += 20f

        // 4. Itemized Transaction Table
        paint.color = darkColor
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Itemized Expenses", 36f, y, paint)
        y += 18f

        // Table Header
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawRect(36f, y - 12f, 559f, y + 8f, paint)

        paint.color = darkColor
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DATE", 44f, y, paint)
        canvas.drawText("DESCRIPTION", 130f, y, paint)
        canvas.drawText("CATEGORY", 310f, y, paint)
        canvas.drawText("PAYMENT", 430f, y, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("AMOUNT", 551f, y, paint)
        paint.textAlign = Paint.Align.LEFT
        y += 16f

        val itemDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        paint.typeface = Typeface.DEFAULT

        var isAlt = false
        for (exp in expenses.take(24)) {
            if (isAlt) {
                paint.color = altRowBg
                canvas.drawRect(36f, y - 11f, 559f, y + 5f, paint)
            }
            isAlt = !isAlt

            paint.color = darkColor
            canvas.drawText(itemDateFormat.format(Date(exp.date)), 44f, y, paint)
            val desc = if (exp.title.length > 25) exp.title.substring(0, 23) + "..." else exp.title
            canvas.drawText(desc, 130f, y, paint)
            canvas.drawText(exp.category, 310f, y, paint)
            canvas.drawText(exp.paymentMethod, 430f, y, paint)

            paint.color = primaryColor
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(String.format(Locale.US, "$%.2f", exp.amount), 551f, y, paint)
            paint.textAlign = Paint.Align.LEFT
            paint.typeface = Typeface.DEFAULT

            y += 18f
            if (y > 800f) break
        }

        // Footer line
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawLine(36f, 810f, 559f, 810f, paint)
        paint.textSize = 9f
        paint.color = grayColor
        canvas.drawText("Expense Tracker • Automated Report Generator", 36f, 825f, paint)

        pdfDoc.finishPage(page)

        // Save File
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()

        val fileName = "Expense_Report_${System.currentTimeMillis()}.pdf"
        val file = File(reportsDir, fileName)
        FileOutputStream(file).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        return file
    }

    fun openOrSharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Expense Report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share or View PDF Report"))
    }
}
