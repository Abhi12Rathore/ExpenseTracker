package com.example.expensetrackerpro.presentation.language

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.AppLanguage
import com.example.expensetrackerpro.domain.models.DayPeriod
import com.example.expensetrackerpro.domain.models.LanguageChoice
import com.example.expensetrackerpro.domain.models.LanguagePreview
import com.example.expensetrackerpro.presentation.common.preview.PreviewData
import com.example.expensetrackerpro.presentation.language.components.LanguagePreviewCard
import com.example.expensetrackerpro.presentation.language.components.LanguageRow
import com.example.expensetrackerpro.presentation.language.components.rememberLocalizedContext
import com.example.expensetrackerpro.presentation.language.uistate.LanguageEvent
import com.example.expensetrackerpro.presentation.language.uistate.LanguageUiState
import kotlinx.coroutines.launch

// Where reports of a wrong translation are sent
private const val TRANSLATION_FEEDBACK_EMAIL = "arathore.abhishek457@gmail.com"

// What stands for "whatever the phone uses" where a language has its script badge
private const val SYSTEM_DEFAULT_BADGE = "Aa"

private val CardShape = RoundedCornerShape(20.dp)

@Composable
fun LanguageScreen(
    onNavigateBack: () -> Unit,
    viewModel: LanguageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isApplied) {
        if (!uiState.isApplied) return@LaunchedEffect
        // Closed first: the switch draws the app again, which then comes back on Settings
        onNavigateBack()
        viewModel.onEvent(LanguageEvent.ClosedAfterApply)
    }

    val context = LocalContext.current

    LanguageContent(
        state = uiState,
        onEvent = viewModel::onEvent,
        onSendFeedback = context::sendTranslationFeedback,
        onNavigateBack = onNavigateBack
    )
}

/** Opens an email to the translators. False when the phone has no app to write one in. */
private fun Context.sendTranslationFeedback(subject: String): Boolean {
    // ACTION_SENDTO with a mailto: address is offered to email apps only
    val email = Intent(
        Intent.ACTION_SENDTO,
        "mailto:$TRANSLATION_FEEDBACK_EMAIL?subject=${Uri.encode(subject)}".toUri()
    )

    return try {
        startActivity(email)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageContent(
    state: LanguageUiState,
    onEvent: (LanguageEvent) -> Unit,
    // Given the subject of the email; false when it could not be opened
    onSendFeedback: (subject: String) -> Boolean,
    onNavigateBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val feedbackSubject = stringResource(
        R.string.language_feedback_subject,
        state.versionName,
        state.selectedLanguage.tag
    )
    val noEmailAppText = stringResource(R.string.language_feedback_no_email_app)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.language_back)
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.language_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (state.isApplyVisible) {
                ApplyLanguageButton(
                    language = state.selectedLanguage,
                    onClick = { onEvent(LanguageEvent.ApplyClicked) }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            state.preview?.let { LanguagePreviewCard(preview = it) }

            LanguageSection(title = stringResource(R.string.language_section_suggested)) {
                LanguageRow(
                    badge = SYSTEM_DEFAULT_BADGE,
                    title = stringResource(R.string.language_system_default),
                    subtitle = stringResource(R.string.language_system_default_subtitle, state.systemLanguageName),
                    selected = state.selected == LanguageChoice.SystemDefault,
                    onClick = { onEvent(LanguageEvent.LanguageSelected(LanguageChoice.SystemDefault)) }
                )
                state.suggested.forEach { language ->
                    RowDivider()
                    LanguageChoiceRow(language = language, state = state, onEvent = onEvent)
                }
            }

            if (state.others.isNotEmpty()) {
                LanguageSection(title = stringResource(R.string.language_section_all)) {
                    state.others.forEachIndexed { index, language ->
                        if (index > 0) RowDivider()
                        LanguageChoiceRow(language = language, state = state, onEvent = onEvent)
                    }
                }
            }

            TranslationFeedbackRow(
                onClick = {
                    if (!onSendFeedback(feedbackSubject)) {
                        scope.launch { snackbarHostState.showSnackbar(noEmailAppText) }
                    }
                }
            )
        }
    }
}

@Composable
private fun LanguageChoiceRow(
    language: AppLanguage,
    state: LanguageUiState,
    onEvent: (LanguageEvent) -> Unit
) {
    val choice = LanguageChoice.Specific(language)

    LanguageRow(
        badge = language.badge,
        title = language.nativeName,
        subtitle = language.englishName,
        selected = state.selected == choice,
        onClick = { onEvent(LanguageEvent.LanguageSelected(choice)) }
    )
}

@Composable
private fun LanguageSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .semantics { heading() }
        )
        Surface(
            shape = CardShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column { content() }
        }
    }
}

/** Starts under the text, past the badge. */
@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/**
 * Says what it does in the language it switches to and, under that, in English, so it can be
 * read by someone who knows only one of the two.
 */
@Composable
private fun ApplyLanguageButton(language: AppLanguage, onClick: () -> Unit) {
    val inLanguage = rememberLocalizedContext(language).getString(R.string.language_use, language.nativeName)
    val inEnglish = stringResource(R.string.language_use_english, language.englishName)

    Button(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .heightIn(min = 60.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = inLanguage,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (inEnglish != inLanguage) {
                Text(
                    text = inEnglish,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TranslationFeedbackRow(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Chat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val action = stringResource(R.string.language_feedback_action)
            val actionStyle = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.language_feedback_prompt))
                    append(" ")
                    withStyle(actionStyle) { append(action) }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private val previewState = LanguageUiState(
    systemLanguageName = "English (India)",
    suggested = listOf(AppLanguage.HINDI),
    others = listOf(AppLanguage.ENGLISH),
    versionName = "1.0"
)

@PreviewLightDark
@Composable
private fun LanguageContentPreview() {
    ExpenseTrackerProTheme {
        LanguageContent(state = previewState, onEvent = {}, onSendFeedback = { true }, onNavigateBack = {})
    }
}

@PreviewLightDark
@Composable
private fun LanguageContentSelectedPreview() {
    ExpenseTrackerProTheme {
        LanguageContent(
            state = previewState.copy(
                selected = LanguageChoice.Specific(AppLanguage.HINDI),
                preview = LanguagePreview(
                    language = AppLanguage.HINDI,
                    dayPeriod = DayPeriod.EVENING,
                    userName = "Abhishek",
                    summary = PreviewData.financialSummary,
                    categories = PreviewData.categoryExpense.take(2)
                )
            ),
            onEvent = {},
            onSendFeedback = { true },
            onNavigateBack = {}
        )
    }
}
