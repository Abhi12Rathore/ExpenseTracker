package com.example.expensetrackerpro.presentation.common.motion

import androidx.compose.ui.geometry.Offset
import com.example.expensetrackerpro.domain.models.ThemeMode
import com.example.expensetrackerpro.fakes.FakeMotionPreferences
import com.example.expensetrackerpro.fakes.FakeTimeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTransitionControllerTest {

    private val time = FakeTimeProvider(nowMillis = 1_000_000L)
    private val motion = FakeMotionPreferences()
    private val controller = ThemeTransitionController(time, motion)
    private val tap = Offset(840f, 917f)

    @Test
    fun theEffectiveTheme_followsThePhoneOnlyForSystem() {
        assertTrue(controller.effectiveDark(ThemeMode.SYSTEM, phoneDark = true))
        assertFalse(controller.effectiveDark(ThemeMode.SYSTEM, phoneDark = false))
        assertTrue(controller.effectiveDark(ThemeMode.DARK, phoneDark = false))
        assertFalse(controller.effectiveDark(ThemeMode.LIGHT, phoneDark = true))
    }

    @Test
    fun lightToDark_revealsFromTheTap() {
        controller.onOptionTapped(ThemeMode.LIGHT, ThemeMode.DARK, phoneDark = false, center = tap)

        assertEquals(tap, controller.takeReveal(ThemeMode.DARK))
    }

    @Test
    fun darkToSystem_onALightPhone_revealsFromTheTap() {
        controller.onOptionTapped(ThemeMode.DARK, ThemeMode.SYSTEM, phoneDark = false, center = tap)

        assertEquals(tap, controller.takeReveal(ThemeMode.SYSTEM))
    }

    @Test
    fun aChoiceThatLooksTheSame_asksForNoReveal() {
        // System while the phone is already dark
        controller.onOptionTapped(ThemeMode.DARK, ThemeMode.SYSTEM, phoneDark = true, center = tap)

        assertNull(controller.takeReveal(ThemeMode.SYSTEM))
    }

    @Test
    fun theRevealIsHandedOutOnce() {
        controller.onOptionTapped(ThemeMode.LIGHT, ThemeMode.DARK, phoneDark = false, center = tap)
        controller.takeReveal(ThemeMode.DARK)

        // A later change, such as the phone switching dark mode, cross-fades instead
        assertNull(controller.takeReveal(ThemeMode.DARK))
    }

    @Test
    fun aChangeToAnotherMode_isNotRevealedFromTheTap() {
        controller.onOptionTapped(ThemeMode.LIGHT, ThemeMode.DARK, phoneDark = false, center = tap)

        assertNull(controller.takeReveal(ThemeMode.SYSTEM))
    }

    @Test
    fun aTapWhoseChangeNeverCame_expires() {
        controller.onOptionTapped(ThemeMode.LIGHT, ThemeMode.DARK, phoneDark = false, center = tap)

        time.nowMillis += ThemeTransitionController.TAP_VALID_MILLIS + 1

        assertNull(controller.takeReveal(ThemeMode.DARK))
    }

    @Test
    fun whenThePhonesDarkModeCantBeTold_aChangeToSystemIsStillRevealed() {
        controller.onOptionTapped(ThemeMode.DARK, ThemeMode.SYSTEM, phoneDark = null, center = tap)

        assertEquals(tap, controller.takeReveal(ThemeMode.SYSTEM))
    }

    @Test
    fun tappingTheOptionAlreadyChosen_asksForNothing() {
        controller.onOptionTapped(ThemeMode.SYSTEM, ThemeMode.SYSTEM, phoneDark = null, center = tap)

        assertNull(controller.takeReveal(ThemeMode.SYSTEM))
    }

    @Test
    fun withAnimationsOff_nothingIsRevealed() {
        motion.enabled = false

        controller.onOptionTapped(ThemeMode.LIGHT, ThemeMode.DARK, phoneDark = false, center = tap)

        // The root then switches the theme at once
        assertNull(controller.takeReveal(ThemeMode.DARK))
    }

    @Test
    fun turningAnimationsOff_dropsATapAlreadyMade() {
        controller.onOptionTapped(ThemeMode.LIGHT, ThemeMode.DARK, phoneDark = false, center = tap)
        motion.enabled = false
        controller.onOptionTapped(ThemeMode.DARK, ThemeMode.LIGHT, phoneDark = false, center = tap)

        assertNull(controller.takeReveal(ThemeMode.LIGHT))
    }
}
