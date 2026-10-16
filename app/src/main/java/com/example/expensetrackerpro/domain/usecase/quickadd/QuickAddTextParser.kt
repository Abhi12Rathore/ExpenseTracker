package com.example.expensetrackerpro.domain.usecase.quickadd

import java.util.Locale

internal data class FoundText(val title: String?, val note: String?)

/**
 * The words left once the amount and date are taken: the title, which is what was bought or
 * where, and the note after a comma, "for" or "with" ("swiggy, dinner with Rahul"). Filler words
 * ("spent", "on", "ka") are left out of the title, and so is a word that only names the kind of
 * spending ("food") when there is another word to call it by.
 */
internal class QuickAddTextParser(private val keywords: QuickAddKeywords) {

    private val notTitle: Set<String> = buildSet {
        addAll(keywords.fillerWords)
        addAll(keywords.currencyWords)
        addAll(keywords.multipliers.keys)
        addAll(keywords.numberWords.keys)
        addAll(keywords.numberJoiners)
        addAll(keywords.lastWords)
        addAll(keywords.timeSuffixes)
    }

    fun parse(tokens: List<Token>, used: BooleanArray): FoundText {
        val left = tokens.indices.filter { !used[it] }
        val split = noteStart(tokens, left)
        val head = if (split != null) left.filter { it < split } else left
        val tail = if (split != null) left.filter { it >= split } else emptyList()

        return FoundText(title = title(head.map(tokens::get)), note = note(tokens, tail))
    }

    /**
     * Where the note begins: the first comma or note word with a title before it and words after
     * it. In "paid 500 for petrol" nothing before "for" can be the title, so there is no note.
     */
    private fun noteStart(tokens: List<Token>, left: List<Int>): Int? =
        left.firstOrNull { i ->
            val token = tokens[i]
            val isBreak = token.kind == Token.Kind.BREAK || token.lower in keywords.noteWords
            isBreak &&
                left.any { it < i && isTitleWord(tokens[it]) } &&
                left.any { it > i && tokens[it].kind == Token.Kind.WORD && tokens[it].lower !in keywords.fillerWords }
        }

    private fun isTitleWord(token: Token): Boolean = token.kind == Token.Kind.WORD && token.lower !in notTitle

    private fun title(head: List<Token>): String? {
        val words = head.filter(::isTitleWord)
        // "Swiggy food" is Swiggy; "food" alone stays
        val named = words.filter { it.lower !in keywords.genericWords }.ifEmpty { words }

        return named.joinToString(" ") { titleCase(it.text) }
            .let(::capped)
            .ifBlank { null }
    }

    private fun note(tokens: List<Token>, tail: List<Int>): String? {
        if (tail.isEmpty()) return null
        val first = tokens[tail.first()]
        // "for dinner" is "dinner"; "with Rahul" keeps its "with"
        val skipFirst = first.kind == Token.Kind.BREAK || first.lower in keywords.fillerWords

        val text = StringBuilder()
        tail.drop(if (skipFirst) 1 else 0).forEach { i ->
            val token = tokens[i]
            val joinsLeft = token.kind == Token.Kind.BREAK || token.text in CLOSING_MARKS
            if (text.isNotEmpty() && !joinsLeft) text.append(' ')
            text.append(if (token.text == "\n") "," else token.text)
        }

        return text.toString().trim().trimEnd(',', ';').trim().ifBlank { null }
    }

    /** A word typed all in lowercase gets a capital; "DMart" or "KFC" stays as typed. */
    private fun titleCase(word: String): String =
        if (word == word.lowercase(Locale.ROOT)) {
            word.replaceFirstChar { it.titlecase(Locale.ROOT) }
        } else {
            word
        }

    /** Cut at a word, so the title never ends halfway through one. */
    private fun capped(title: String): String {
        if (title.length <= keywords.titleMaxLength) return title
        val cut = title.take(keywords.titleMaxLength)
        val lastSpace = cut.lastIndexOf(' ')
        return if (lastSpace > 0) cut.take(lastSpace) else cut
    }

    private companion object {
        val CLOSING_MARKS = setOf(".", "!", "?", ")", ":")
    }
}
