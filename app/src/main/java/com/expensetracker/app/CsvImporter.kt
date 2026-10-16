package com.expensetracker.app

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CsvImporter {

    data class ImportPreview(
        val fileName: String,
        val totalCount: Int,
        val totalAmount: Double,
        val sampleItems: List<Expense>,
        val expenses: List<Expense>
    )

    fun readAndParseCsv(context: Context, uri: Uri): Result<ImportPreview> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("Cannot open file: inputStream is null"))

            val fileName = getFileName(context, uri) ?: "expenses.csv"
            val expenses = parseCsvStream(inputStream)
            if (expenses.isEmpty()) {
                return Result.failure(Exception("No valid transaction records found in the selected CSV file."))
            }

            val totalAmount = expenses.sumOf { it.amount }
            val preview = ImportPreview(
                fileName = fileName,
                totalCount = expenses.size,
                totalAmount = totalAmount,
                sampleItems = expenses.take(4),
                expenses = expenses
            )
            Result.success(preview)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
            } catch (_: Exception) {}
        }
        if (name == null) {
            name = uri.path?.let { p ->
                val cut = p.lastIndexOf('/')
                if (cut != -1) p.substring(cut + 1) else p
            }
        }
        return name
    }

    fun parseCsvStream(inputStream: InputStream): List<Expense> {
        val lines = mutableListOf<String>()
        BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8)).use { reader ->
            var line: String?
            var isFirstLine = true
            while (reader.readLine().also { line = it } != null) {
                var l = line ?: continue
                if (isFirstLine) {
                    isFirstLine = false
                    // Strip UTF-8 BOM if present
                    if (l.startsWith("\uFEFF")) {
                        l = l.substring(1)
                    }
                }
                lines.add(l)
            }
        }

        if (lines.isEmpty()) return emptyList()

        // 1. Locate the header row
        var headerIndex = -1
        var delimiter = ','
        for (i in lines.indices) {
            val candidate = lines[i].trim()
            if (candidate.isEmpty() || candidate.startsWith("#") || candidate.startsWith("---") || candidate.startsWith("EXPENSE TRACKER")) {
                continue
            }

            val testDelimiter = if (candidate.count { it == ';' } > candidate.count { it == ',' }) ';' else ','
            val tokens = splitCsvLine(candidate, testDelimiter).map { it.trim().lowercase(Locale.ROOT) }

            val hasDate = tokens.any { it.contains("date") || it == "time" }
            val hasTitle = tokens.any { it.contains("description") || it.contains("title") || it.contains("item") || it.contains("name") }
            val hasAmount = tokens.any { it.contains("amount") || it.contains("cost") || it.contains("price") }

            if ((hasDate && hasAmount) || (hasTitle && hasAmount)) {
                headerIndex = i
                delimiter = testDelimiter
                break
            }
        }

        val results = mutableListOf<Expense>()

        if (headerIndex >= 0) {
            val headerTokens = splitCsvLine(lines[headerIndex], delimiter)
                .map { it.trim().lowercase(Locale.ROOT) }

            val dateCol = headerTokens.indexOfFirst { it.contains("date") }
            val timeCol = headerTokens.indexOfFirst { it == "time" }
            val titleCol = headerTokens.indexOfFirst { it.contains("description") || it.contains("title") || it.contains("item") || it.contains("name") }
            val categoryCol = headerTokens.indexOfFirst { it.contains("category") || it.contains("cat") }
            val paymentCol = headerTokens.indexOfFirst { it.contains("payment") || it.contains("method") || it.contains("pay") }
            val amountCol = headerTokens.indexOfFirst { it.contains("amount") || it.contains("cost") || it.contains("price") }
            val notesCol = headerTokens.indexOfFirst { it.contains("note") || it.contains("remark") || it.contains("comment") }

            for (i in (headerIndex + 1) until lines.size) {
                val rowLine = lines[i].trim()
                if (rowLine.isEmpty() || rowLine.startsWith("#") || rowLine.startsWith("---")) continue

                // Check if this is a summary row like "Total,332.99" or ",,,,Total,332.99"
                val lower = rowLine.lowercase(Locale.ROOT)
                if (lower.startsWith("total,") || lower.startsWith("total;") ||
                    lower.startsWith(",,,,total") || lower.startsWith(";;;;total") ||
                    lower.contains(",total,") || lower.contains(";total;")) {
                    continue
                }

                val tokens = splitCsvLine(rowLine, delimiter)
                if (tokens.isEmpty() || tokens.all { it.isEmpty() }) continue

                val rawAmount = if (amountCol >= 0) tokens.getOrNull(amountCol) ?: "" else ""
                val amount = parseAmount(rawAmount) ?: continue

                val rawDate = if (dateCol >= 0) tokens.getOrNull(dateCol) ?: "" else ""
                val rawTime = if (timeCol >= 0) tokens.getOrNull(timeCol) ?: "" else ""
                val timestamp = parseDateTime(rawDate, rawTime)

                val rawTitle = if (titleCol >= 0) tokens.getOrNull(titleCol)?.trim() ?: "" else ""
                val finalTitle = if (rawTitle.isNotBlank()) rawTitle else "Imported Expense"

                val rawCategory = if (categoryCol >= 0) tokens.getOrNull(categoryCol)?.trim() ?: "" else ""
                val finalCategory = if (rawCategory.isNotBlank()) rawCategory else "Other"

                val rawPayment = if (paymentCol >= 0) tokens.getOrNull(paymentCol)?.trim() ?: "" else ""
                val finalPayment = if (rawPayment.isNotBlank()) rawPayment else "Cash"

                val notes = if (notesCol >= 0) tokens.getOrNull(notesCol)?.trim() ?: "" else ""

                results.add(
                    Expense(
                        title = finalTitle,
                        amount = amount,
                        category = finalCategory,
                        date = timestamp,
                        paymentMethod = finalPayment,
                        notes = notes
                    )
                )
            }
        } else {
            // Fallback for CSV without standard header row
            for (line in lines) {
                val rowLine = line.trim()
                if (rowLine.isEmpty() || rowLine.startsWith("#") || rowLine.startsWith("---")) continue
                val tokens = splitCsvLine(rowLine, delimiter)
                if (tokens.size < 2) continue

                var amountIdx = -1
                var parsedAmount: Double? = null
                for (idx in tokens.indices) {
                    val p = parseAmount(tokens[idx])
                    if (p != null && p > 0.0) {
                        parsedAmount = p
                        amountIdx = idx
                        break
                    }
                }
                if (amountIdx == -1 || parsedAmount == null) continue

                val rawDate = tokens.getOrNull(0) ?: ""
                val timestamp = parseDateTime(rawDate, "")
                val title = tokens.getOrNull(if (amountIdx == 1) 0 else 1)?.trim() ?: "Imported Expense"
                val category = tokens.getOrNull(3)?.trim() ?: "Other"
                val payment = tokens.getOrNull(4)?.trim() ?: "Cash"
                val notes = tokens.getOrNull(5)?.trim() ?: ""

                results.add(
                    Expense(
                        title = title,
                        amount = parsedAmount,
                        category = category,
                        date = timestamp,
                        paymentMethod = payment,
                        notes = notes
                    )
                )
            }
        }

        return results
    }

    private fun splitCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '\"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                        sb.append('\"')
                        i++ // skip escaped quote
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == delimiter && !inQuotes -> {
                    result.add(sb.toString().trim())
                    sb.clear()
                }
                else -> {
                    sb.append(c)
                }
            }
            i++
        }
        result.add(sb.toString().trim())
        return result
    }

    private fun parseAmount(raw: String): Double? {
        val cleaned = raw.replace("$", "")
            .replace("₹", "")
            .replace("Rs.", "")
            .replace("Rs", "")
            .replace("USD", "")
            .replace("INR", "")
            .replace(",", "")
            .replace(" ", "")
            .trim()
            .removePrefix("-")
            .removePrefix("+")

        return cleaned.toDoubleOrNull()
    }

    private fun parseDateTime(rawDate: String, rawTime: String): Long {
        val cleanedDate = rawDate.trim()
        val cleanedTime = rawTime.trim()

        if (cleanedDate.isEmpty()) {
            return System.currentTimeMillis()
        }

        // Epoch millis check
        cleanedDate.toLongOrNull()?.let {
            if (it > 946684800000L) return it // after year 2000
        }

        val combined = if (cleanedTime.isNotEmpty()) "$cleanedDate $cleanedTime" else cleanedDate

        val formats = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy HH:mm",
            "dd/MM/yyyy",
            "dd-MM-yyyy HH:mm:ss",
            "dd-MM-yyyy HH:mm",
            "dd-MM-yyyy",
            "MM/dd/yyyy HH:mm:ss",
            "MM/dd/yyyy HH:mm",
            "MM/dd/yyyy",
            "dd MMM yyyy HH:mm",
            "dd MMM yyyy",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd"
        )

        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = true }
                val parsed = sdf.parse(combined)
                if (parsed != null) {
                    val cal = Calendar.getInstance()
                    cal.time = parsed
                    if (cal.get(Calendar.YEAR) in 1990..2100) {
                        return cal.timeInMillis
                    }
                }
            } catch (_: Exception) {}
        }

        return System.currentTimeMillis()
    }
}
