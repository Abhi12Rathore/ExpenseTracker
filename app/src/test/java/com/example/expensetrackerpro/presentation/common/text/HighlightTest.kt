package com.example.expensetrackerpro.presentation.common.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HighlightTest {

    @Test
    fun match_ignoresCase() {
        assertEquals(listOf(5..9), matchRanges("Team Lunch", "lunch"))
        assertEquals(listOf(0..3), matchRanges("team lunch", "TEAM"))
    }

    @Test
    fun everyOccurrence_isFound() {
        assertEquals(listOf(0..1, 7..8, 13..14), matchRanges("Cab to café, cab back", "ca"))
    }

    @Test
    fun occurrences_doNotOverlap() {
        assertEquals(listOf(0..1), matchRanges("aaa", "aa"))
        assertEquals(listOf(0..1, 2..3), matchRanges("aaaa", "aa"))
    }

    @Test
    fun spacesAroundTheQuery_areIgnored_butSpacesInsideItCount() {
        assertEquals(listOf(5..9), matchRanges("Team lunch", "  lunch "))
        assertEquals(listOf(3..6), matchRanges("Team lunch", "m lu"))
    }

    @Test
    fun noQuery_orNoMatch_isNothing() {
        assertTrue(matchRanges("Team lunch", "").isEmpty())
        assertTrue(matchRanges("Team lunch", "   ").isEmpty())
        assertTrue(matchRanges("Team lunch", "dinner").isEmpty())
        assertTrue(matchRanges("", "lunch").isEmpty())
    }

    @Test
    fun aQueryLongerThanTheText_isNothing() {
        assertTrue(matchRanges("Tea", "Team lunch").isEmpty())
    }
}
