package com.example.expensetrackerpro.presentation.language.components

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.expensetrackerpro.domain.models.AppLanguage
import java.util.Locale

/** A context that reads the string resources of [language], whatever the app is shown in. */
@Composable
fun rememberLocalizedContext(language: AppLanguage): Context {
    val context = LocalContext.current
    // Everything else about the screen (size, font scale, night mode) stays as it is
    val current = LocalConfiguration.current

    return remember(context, current, language) {
        val configuration = Configuration(current).apply { setLocale(Locale.forLanguageTag(language.tag)) }
        context.createConfigurationContext(configuration)
    }
}

/** Draws [content] in [language] without switching the app: every string resource in it is that language's. */
@Composable
fun LocalizedContent(language: AppLanguage, content: @Composable () -> Unit) {
    val localizedContext = rememberLocalizedContext(language)

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        content = content
    )
}
