package com.example.expensetrackerpro.presentation.common.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

/**
 * Where [query] occurs in [text], ignoring case and the spaces around the query. Occurrences
 * don't overlap: `aa` is found once in `aaa`.
 */
fun matchRanges(text: String, query: String): List<IntRange> {
    val needle = query.trim()
    if (needle.isEmpty()) return emptyList()

    return buildList {
        var index = text.indexOf(needle, ignoreCase = true)
        while (index >= 0) {
            add(index until index + needle.length)
            index = text.indexOf(needle, startIndex = index + needle.length, ignoreCase = true)
        }
    }
}

/** [text] with every occurrence of [query] marked out. */
@Composable
fun highlighted(text: String, query: String): AnnotatedString {
    val style = SpanStyle(
        background = MaterialTheme.colorScheme.primaryContainer,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        fontWeight = FontWeight.Bold
    )

    return remember(text, query, style) {
        buildAnnotatedString {
            append(text)
            matchRanges(text, query).forEach { range ->
                addStyle(style, range.first, range.last + 1)
            }
        }
    }
}
