package com.example.expensetrackerpro.domain.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class OutcomeTest {

    private val success: Outcome<Int> = Outcome.Success(4)
    private val failure: Outcome<Int> = Outcome.Failure(DomainError.CategoryInUse)

    @Test
    fun map_changesTheValue_andPassesAFailureThrough() {
        assertEquals(Outcome.Success("4"), success.map { it.toString() })
        assertEquals(Outcome.Failure(DomainError.CategoryInUse), failure.map { it.toString() })
    }

    @Test
    fun onSuccess_runsOnlyForASuccess_andHandsTheOutcomeOn() {
        val seen = mutableListOf<Int>()

        assertSame(success, success.onSuccess { seen += it })
        assertSame(failure, failure.onSuccess { seen += it })

        assertEquals(listOf(4), seen)
    }

    @Test
    fun onFailure_runsOnlyForAFailure_andHandsTheOutcomeOn() {
        val seen = mutableListOf<DomainError>()

        assertSame(success, success.onFailure { seen += it })
        assertSame(failure, failure.onFailure { seen += it })

        assertEquals(listOf<DomainError>(DomainError.CategoryInUse), seen)
    }

    @Test
    fun getOrNull_isTheValue_orNullForAFailure() {
        assertEquals(4, success.getOrNull())
        assertNull(failure.getOrNull())
    }
}
