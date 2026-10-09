package com.example.expensetrackerpro.presentation.homeScreen.component.charts

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartAxisTest {

    @Test
    fun niceAxisMax_roundsUpToTheNextRoundValue() {
        assertEquals(3000.0, niceAxisMax(2340.0), 0.001)
        assertEquals(4000.0, niceAxisMax(3150.0), 0.001)
        assertEquals(50000.0, niceAxisMax(42600.0), 0.001)
        assertEquals(100.0, niceAxisMax(85.0), 0.001)
    }

    @Test
    fun niceAxisMax_keepsAValueThatIsAlreadyRound() {
        assertEquals(3000.0, niceAxisMax(3000.0), 0.001)
        assertEquals(1000.0, niceAxisMax(1000.0), 0.001)
        assertEquals(150000.0, niceAxisMax(150000.0), 0.001)
    }

    @Test
    fun niceAxisMax_nothingToShow_stillHasAHeight() {
        assertEquals(1.0, niceAxisMax(0.0), 0.001)
        assertEquals(1.0, niceAxisMax(-50.0), 0.001)
    }
}
