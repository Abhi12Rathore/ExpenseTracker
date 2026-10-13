package com.example.expensetrackerpro.domain.usecase.language

import com.example.expensetrackerpro.domain.models.AppLanguage
import com.example.expensetrackerpro.domain.models.LanguageChange
import com.example.expensetrackerpro.domain.models.LanguageChoice
import com.example.expensetrackerpro.fakes.FakeAppLanguageRepository
import com.example.expensetrackerpro.fakes.FakeDeviceLanguages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class LanguageUseCasesTest {

    private val english = LanguageChoice.Specific(AppLanguage.ENGLISH)
    private val hindi = LanguageChoice.Specific(AppLanguage.HINDI)

    private val repository = FakeAppLanguageRepository()
    private val deviceLanguages = FakeDeviceLanguages()
    private val resolveAppLocale = ResolveAppLocaleUseCase()

    private val getLanguageOptions = GetLanguageOptionsUseCase(repository, deviceLanguages, resolveAppLocale)
    private val setAppLanguage = SetAppLanguageUseCase(repository, deviceLanguages, resolveAppLocale)
    private val undoLanguageChange = UndoLanguageChangeUseCase(repository)

    private fun locales(vararg tags: String) = tags.map(Locale::forLanguageTag)

    // --- The locale a choice comes to

    @Test
    fun aChosenLanguage_isThatLanguage_whateverThePhoneUses() {
        assertEquals("hi", resolveAppLocale(hindi, locales("en-IN")).toLanguageTag())
        assertEquals("en", resolveAppLocale(english, locales("hi-IN")).toLanguageTag())
        assertEquals("hi", resolveAppLocale(hindi, emptyList()).toLanguageTag())
    }

    @Test
    fun aChosenLanguageThePhoneAlsoHas_keepsThePhonesRegion() {
        // So an Indian phone writes "4:20 pm" in English whether English was chosen or followed
        assertEquals("en-IN", resolveAppLocale(english, locales("hi-IN", "en-IN")).toLanguageTag())
        assertEquals("hi-IN", resolveAppLocale(hindi, locales("en-IN", "hi-IN")).toLanguageTag())
    }

    @Test
    fun followingThePhone_isItsFirstLanguageTheAppHas_regionIncluded() {
        assertEquals("en-IN", resolveAppLocale(LanguageChoice.SystemDefault, locales("en-IN", "hi-IN")).toLanguageTag())
        // French is not translated, so Android shows the app in Hindi, and so are its dates
        assertEquals("hi-IN", resolveAppLocale(LanguageChoice.SystemDefault, locales("fr-FR", "hi-IN")).toLanguageTag())
    }

    @Test
    fun followingAPhoneWithNoLanguageTheAppHas_isEnglish() {
        assertEquals(Locale.ENGLISH, resolveAppLocale(LanguageChoice.SystemDefault, locales("fr-FR", "de-DE")))
        assertEquals(Locale.ENGLISH, resolveAppLocale(LanguageChoice.SystemDefault, emptyList()))
    }

    @Test
    fun aLanguageIsFoundByItsTag_withOrWithoutARegion() {
        assertEquals(AppLanguage.HINDI, AppLanguage.findByTag("hi"))
        assertEquals(AppLanguage.HINDI, AppLanguage.findByTag("hi-IN"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.findByTag("en_US"))
        assertNull(AppLanguage.findByTag("fr"))
    }

    // --- What the Language screen offers

    @Test
    fun suggestions_areThePhonesOtherLanguagesTheAppHas_andTheRestComeAfter() {
        deviceLanguages.list = locales("en-IN", "fr-FR", "hi-IN")

        val options = getLanguageOptions()

        assertEquals(LanguageChoice.SystemDefault, options.current)
        // System default already stands for English here
        assertEquals(AppLanguage.ENGLISH, options.systemLanguage)
        assertEquals("English (India)", options.systemLanguageName)
        assertEquals(listOf(AppLanguage.HINDI), options.suggested)
        assertEquals(listOf(AppLanguage.ENGLISH), options.others)
    }

    @Test
    fun aPhoneInOneLanguage_suggestsOnlyFollowingIt_andListsEveryLanguage() {
        deviceLanguages.list = locales("hi-IN")
        repository.stored = english

        val options = getLanguageOptions()

        assertEquals(english, options.current)
        assertEquals(AppLanguage.HINDI, options.systemLanguage)
        assertEquals(emptyList<AppLanguage>(), options.suggested)
        assertEquals(listOf(AppLanguage.ENGLISH, AppLanguage.HINDI), options.others)
    }

    @Test
    fun aLanguageThePhoneListsTwice_isSuggestedOnce() {
        deviceLanguages.list = locales("fr-FR", "en-GB", "hi-IN", "en-IN", "hi")

        val options = getLanguageOptions()

        // French is first but not translated: the app follows the phone into English
        assertEquals(AppLanguage.ENGLISH, options.systemLanguage)
        assertEquals("Français (France)", options.systemLanguageName)
        assertEquals(listOf(AppLanguage.HINDI), options.suggested)
        assertEquals(listOf(AppLanguage.ENGLISH), options.others)
    }

    // --- Applying and undoing

    @Test
    fun settingALanguage_switchesTheApp_andRemembersWhatItWasShownIn() {
        setAppLanguage(hindi)

        assertEquals(hindi, repository.stored)
        assertEquals(
            LanguageChange(previous = LanguageChoice.SystemDefault, language = AppLanguage.HINDI),
            repository.pendingChange.value
        )
    }

    @Test
    fun goingBackToFollowingThePhone_namesTheLanguageThatComesTo() {
        repository.stored = hindi
        deviceLanguages.list = locales("en-IN")

        setAppLanguage(LanguageChoice.SystemDefault)

        assertEquals(LanguageChange(previous = hindi, language = AppLanguage.ENGLISH), repository.pendingChange.value)
    }

    @Test
    fun settingTheLanguageTheAppIsAlreadyIn_changesNothing() {
        repository.stored = hindi

        setAppLanguage(hindi)

        assertNull(repository.pendingChange.value)
    }

    @Test
    fun undo_putsThePreviousLanguageBack_once() {
        repository.stored = english
        setAppLanguage(hindi)

        undoLanguageChange()

        assertEquals(english, repository.stored)
        assertNull(repository.pendingChange.value)

        // Nothing left to undo
        repository.stored = hindi
        undoLanguageChange()
        assertEquals(hindi, repository.stored)
    }

    @Test
    fun aChangeThatIsForgotten_canNoLongerBeUndone() {
        setAppLanguage(hindi)

        ForgetLanguageChangeUseCase(repository)()
        undoLanguageChange()

        assertEquals(hindi, repository.stored)
    }
}
