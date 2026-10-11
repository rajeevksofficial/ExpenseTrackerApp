package com.expensetracker.app

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import java.util.Locale

/**
 * Autocomplete adapter that matches any substring (case-insensitive)
 * instead of only prefix matches, fulfilling intellisense requirements.
 */
class SubstringAutoCompleteAdapter(
    context: Context,
    resource: Int,
    textViewResourceId: Int,
    items: List<String>
) : ArrayAdapter<String>(context, resource, textViewResourceId, ArrayList(items)) {

    private val originalItems: List<String> = items.toList()
    private val lock = Any()

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val query = constraint?.toString()?.trim()?.lowercase(Locale.ROOT)

                val filtered = if (query.isNullOrEmpty()) {
                    originalItems
                } else {
                    originalItems.filter { it.lowercase(Locale.ROOT).contains(query) }
                }

                results.values = filtered
                results.count = filtered.size
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                synchronized(lock) {
                    clear()
                    val matches = results?.values as? List<String>
                    if (!matches.isNullOrEmpty()) {
                        addAll(matches)
                        notifyDataSetChanged()
                    } else {
                        notifyDataSetInvalidated()
                    }
                }
            }
        }
    }
}
