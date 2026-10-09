package com.example.expensetrackerpro.domain.usecase.theme

import com.example.expensetrackerpro.domain.models.ThemeMode
import com.example.expensetrackerpro.fakes.FakeThemePreferenceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeUseCasesTest {

    @Test
    fun getThemeMode_nothingSaved_followsSystem() = runTest {
        assertEquals(
            ThemeMode.SYSTEM,
            GetThemeModeUseCase(FakeThemePreferenceRepository())().first()
        )
    }

    @Test
    fun getThemeMode_returnsSavedMode() = runTest {
        val repository = FakeThemePreferenceRepository(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, GetThemeModeUseCase(repository)().first())
    }

    @Test
    fun setThemeMode_savesTheMode() = runTest {
        val repository = FakeThemePreferenceRepository()

        SetThemeModeUseCase(repository)(ThemeMode.LIGHT)

        assertEquals(ThemeMode.LIGHT, repository.current)
    }

    @Test
    fun setThemeMode_isSeenByGetThemeMode() = runTest {
        val repository = FakeThemePreferenceRepository(ThemeMode.LIGHT)

        SetThemeModeUseCase(repository)(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, GetThemeModeUseCase(repository)().first())
    }
}
