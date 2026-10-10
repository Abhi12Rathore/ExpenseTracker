package com.example.expensetrackerpro.domain.usecase.profile

import com.example.expensetrackerpro.domain.models.UserProfile
import com.example.expensetrackerpro.fakes.FakeUserProfileRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileUseCasesTest {

    private val repository = FakeUserProfileRepository()
    private val getUserProfile = GetUserProfileUseCase(repository)
    private val setUserName = SetUserNameUseCase(repository)

    @Test
    fun noNameSaved_isABlankProfile() = runTest {
        assertEquals(UserProfile(name = ""), getUserProfile().first())
    }

    @Test
    fun aSavedName_isReadBackTrimmed() = runTest {
        setUserName("  Riya  ")

        assertEquals(UserProfile(name = "Riya"), getUserProfile().first())
    }

    @Test
    fun aLongName_isCutAtThirtyCharacters() = runTest {
        setUserName("a".repeat(40))

        assertEquals("a".repeat(30), repository.current.name)
    }

    @Test
    fun aCutThatEndsOnASpace_isTrimmedAgain() = runTest {
        setUserName("a".repeat(29) + " bcd")

        assertEquals("a".repeat(29), repository.current.name)
    }

    @Test
    fun aBlankName_clearsIt() = runTest {
        setUserName("Riya")

        setUserName("   ")

        assertEquals("", repository.current.name)
    }

    @Test
    fun initial_isTheFirstLetterInUpperCase_orNothingWithoutAName() {
        assertEquals('R', UserProfile("riya").initial)
        assertEquals('A', UserProfile(" (abhi)").initial)
        assertNull(UserProfile("").initial)
        assertNull(UserProfile("  ").initial)
    }
}
