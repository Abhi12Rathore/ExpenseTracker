package com.example.expensetrackerpro.presentation.homeScreen.component.charts

import com.example.expensetrackerpro.domain.models.Money
import com.example.expensetrackerpro.fakes.rupees
import org.junit.Assert.assertEquals
import org.junit.Test

class ChartAxisTest {

    @Test
    fun niceAxisMax_roundsUpToTheNextRoundValue() {
        assertEquals(rupees(3000), niceAxisMax(rupees(2340)))
        assertEquals(rupees(4000), niceAxisMax(rupees(3150)))
        assertEquals(rupees(50000), niceAxisMax(rupees(42600)))
        assertEquals(rupees(100), niceAxisMax(rupees(85)))
    }

    @Test
    fun niceAxisMax_keepsAValueThatIsAlreadyRound() {
        assertEquals(rupees(3000), niceAxisMax(rupees(3000)))
        assertEquals(rupees(1000), niceAxisMax(rupees(1000)))
        assertEquals(rupees(150000), niceAxisMax(rupees(150000)))
    }

    @Test
    fun niceAxisMax_nothingToShow_stillHasAHeight() {
        assertEquals(rupees(1), niceAxisMax(Money.ZERO))
        assertEquals(rupees(1), niceAxisMax(rupees(-50)))
    }

    @Test
    fun niceAxisMax_belowARupee_isRoundInPaise() {
        assertEquals(Money(50), niceAxisMax(Money(42)))
        assertEquals(Money(2), niceAxisMax(Money(2)))
    }

    @Test
    fun atFraction_givesTheAmountAtAGridline() {
        assertEquals(rupees(1500), rupees(3000).atFraction(0.5f))
        assertEquals(rupees(3000), rupees(3000).atFraction(1f))
    }
}
