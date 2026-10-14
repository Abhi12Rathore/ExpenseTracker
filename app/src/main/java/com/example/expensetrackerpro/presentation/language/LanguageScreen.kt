package com.example.expensetrackerpro.presentation.language

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.AppLanguage
import com.example.expensetrackerpro.domain.models.DayPeriod
import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.LanguageChoice
import com.example.expensetrackerpro.domain.models.LanguagePreview
import com.example.expensetrackerpro.presentation.common.haptics.rememberHaptics
import com.example.expensetrackerpro.presentation.common.motion.LocalAnimationsEnabled
import com.example.expensetrackerpro.presentation.common.motion.LocalScreenSnapshot
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

private val CardCorner = 20.dp
private val SECTION_GAP = 20.dp

// Below this the preview shows only the balance and its row, leaving room for the list
private const val COMPACT_SCREEN_HEIGHT_DP = 640

// Stable keys, so a selection never makes the list re-lay out its rows
private const val KEY_HEADER_SUGGESTED = "header_suggested"
private const val KEY_SYSTEM_DEFAULT = "system_default"
private const val KEY_HEADER_ALL = "header_all"
private const val KEY_FEEDBACK = "feedback"

@Composable
fun LanguageScreen(
    onNavigateBack: () -> Unit,
    viewModel: LanguageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val screenSnapshot = LocalScreenSnapshot.current
    LaunchedEffect(uiState.isApplied) {
        if (!uiState.isApplied) return@LaunchedEffect
        // The new language recreates the activity, which fades this picture out over itself
        screenSnapshot?.captureForRecreate()
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        // Only the top: the list runs on under the gesture bar and the apply bar, and pads its own end
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            // Saved, so a list restored near its end has its full padding before the bar is measured again
            var applyBarHeight by rememberSaveable { mutableIntStateOf(0) }
            val density = LocalDensity.current
            val animationsEnabled = LocalAnimationsEnabled.current
            val listBottomPadding by animateDpAsState(
                targetValue = if (state.isApplyVisible) with(density) { applyBarHeight.toDp() } else 0.dp,
                animationSpec = if (animationsEnabled) spring() else snap(),
                label = "listBottomPadding"
            )

            Column(modifier = Modifier.fillMaxSize()) {
                PinnedPreview(state = state)
                LanguageList(
                    state = state,
                    onEvent = onEvent,
                    bottomPadding = listBottomPadding,
                    onFeedbackClick = {
                        if (!onSendFeedback(feedbackSubject)) {
                            scope.launch { snackbarHostState.showSnackbar(noEmailAppText) }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            AnimatedVisibility(
                visible = state.isApplyVisible,
                enter = if (animationsEnabled) slideInVertically { it } + fadeIn() else EnterTransition.None,
                exit = if (animationsEnabled) slideOutVertically { it } + fadeOut() else ExitTransition.None,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.onSizeChanged { applyBarHeight = it.height }
                ) {
                    ApplyLanguageButton(
                        language = state.selectedLanguage,
                        onClick = { onEvent(LanguageEvent.ApplyClicked) }
                    )
                }
            }
        }
    }
}

/**
 * Home in the selected language, above the list and outside it, so picking a language changes
 * only what the card says: the list beneath never moves.
 */
@Composable
private fun PinnedPreview(state: LanguageUiState) {
    val compact = LocalConfiguration.current.screenHeightDp < COMPACT_SCREEN_HEIGHT_DP
    // Until the first preview has loaded the card is laid out empty, so it has its size from the start
    val preview = state.preview ?: LanguagePreview(
        language = state.selectedLanguage,
        dayPeriod = null,
        userName = "",
        summary = FinancialSummary(),
        categories = emptyList()
    )
    // The tallest it has been: a script drawn taller than the others can't push the list down and up
    // Measured again when the screen turns short or tall, as the compact card is a different size
    var tallest by remember(compact) { mutableIntStateOf(0) }
    val density = LocalDensity.current

    LanguagePreviewCard(
        preview = preview,
        compact = compact,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)
            .heightIn(min = with(density) { tallest.toDp() })
            .onSizeChanged { tallest = maxOf(tallest, it.height) }
            .alpha(if (state.preview == null) 0f else 1f)
    )
}

@Composable
private fun LanguageList(
    state: LanguageUiState,
    onEvent: (LanguageEvent) -> Unit,
    bottomPadding: Dp,
    onFeedbackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Saveable: the scroll position survives rotation and the process being stopped
    val listState = rememberLazyListState()
    val suggestedCount = state.suggested.size + 1

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 16.dp + maxOf(bottomPadding, navigationBar)
        )
    ) {
        item(key = KEY_HEADER_SUGGESTED) {
            SectionTitle(stringResource(R.string.language_section_suggested), Modifier.padding(bottom = 8.dp))
        }
        item(key = KEY_SYSTEM_DEFAULT) {
            GroupedRow(index = 0, count = suggestedCount) {
                LanguageRow(
                    badge = SYSTEM_DEFAULT_BADGE,
                    title = stringResource(R.string.language_system_default),
                    subtitle = stringResource(R.string.language_system_default_subtitle, state.systemLanguageName),
                    selected = state.selected == LanguageChoice.SystemDefault,
                    onClick = { onEvent(LanguageEvent.LanguageSelected(LanguageChoice.SystemDefault)) }
                )
            }
        }
        itemsIndexed(state.suggested, key = { _, language -> language.tag }) { index, language ->
            GroupedRow(index = index + 1, count = suggestedCount) {
                LanguageChoiceRow(language = language, state = state, onEvent = onEvent)
            }
        }

        if (state.others.isNotEmpty()) {
            item(key = KEY_HEADER_ALL) {
                SectionTitle(
                    stringResource(R.string.language_section_all),
                    Modifier.padding(top = SECTION_GAP, bottom = 8.dp)
                )
            }
            itemsIndexed(state.others, key = { _, language -> language.tag }) { index, language ->
                GroupedRow(index = index, count = state.others.size) {
                    LanguageChoiceRow(language = language, state = state, onEvent = onEvent)
                }
            }
        }

        item(key = KEY_FEEDBACK) {
            TranslationFeedbackRow(onClick = onFeedbackClick, modifier = Modifier.padding(top = SECTION_GAP))
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
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .semantics { heading() }
    )
}

/**
 * One row of a section's card. Each row is its own list item, so the card is drawn in pieces:
 * the first rounds the top, the last the bottom, and every row after the first starts with a divider.
 */
@Composable
private fun GroupedRow(index: Int, count: Int, content: @Composable () -> Unit) {
    val first = index == 0
    val last = index == count - 1
    val shape = RoundedCornerShape(
        topStart = if (first) CardCorner else 0.dp,
        topEnd = if (first) CardCorner else 0.dp,
        bottomStart = if (last) CardCorner else 0.dp,
        bottomEnd = if (last) CardCorner else 0.dp
    )

    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            if (!first) RowDivider()
            content()
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
    val haptics = rememberHaptics()
    val inLanguage = rememberLocalizedContext(language).getString(R.string.language_use, language.nativeName)
    val inEnglish = stringResource(R.string.language_use_english, language.englishName)

    Button(
        onClick = {
            haptics.confirm()
            onClick()
        },
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
private fun TranslationFeedbackRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
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
    preview = LanguagePreview(
        language = AppLanguage.ENGLISH,
        dayPeriod = DayPeriod.EVENING,
        userName = "Abhishek",
        summary = PreviewData.financialSummary,
        categories = PreviewData.categoryExpense.take(2)
    ),
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
                preview = previewState.preview?.copy(language = AppLanguage.HINDI)
            ),
            onEvent = {},
            onSendFeedback = { true },
            onNavigateBack = {}
        )
    }
}

@Preview(heightDp = 600)
@Composable
private fun LanguageContentShortScreenPreview() {
    ExpenseTrackerProTheme {
        LanguageContent(state = previewState, onEvent = {}, onSendFeedback = { true }, onNavigateBack = {})
    }
}
