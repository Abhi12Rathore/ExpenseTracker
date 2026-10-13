package com.example.expensetrackerpro.domain.category

import com.example.expensetrackerpro.domain.models.BuiltInCategory
import java.util.Locale

/** What the built-in categories are called in each language the app is translated into. */
interface DefaultCategoryNames {

    /** The name of [category] in the language of [locale], or in English when the app lacks that language. */
    fun nameOf(category: BuiltInCategory, locale: Locale): String
}
