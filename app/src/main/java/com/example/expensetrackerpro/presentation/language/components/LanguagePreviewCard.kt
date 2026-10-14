package com.example.expensetrackerpro.presentation.language.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.ExpenseTrackerProTheme
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.AppLanguage
import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.DayPeriod
import com.example.expensetrackerpro.domain.models.FinancialSummary
import com.example.expensetrackerpro.domain.models.LanguagePreview
import com.example.expensetrackerpro.presentation.common.category.CategoryIconTile
import com.example.expensetrackerpro.presentation.common.motion.LocalAnimationsEnabled
import com.example.expensetrackerpro.presentation.common.preview.PreviewData
import com.example.expensetrackerpro.utils.CurrencyFormatter

// How long one language's preview takes to fade into the next
private const val CROSS_FADE_MILLIS = 200

/**
 * What Home would say in another language, with the user's own name and figures, so a language
 * can be judged before the app is switched to it. Everything inside the frame is in that
 * language; the "Preview" label above it stays in the one the app is shown in.
 *
 * A new language fades in over the old one inside the same frame, which never changes size, so
 * nothing around it moves. [compact] keeps the balance and its row only, for short screens.
 */
@Composable
fun LanguagePreviewCard(preview: LanguagePreview, modifier: Modifier = Modifier, compact: Boolean = false) {
    val animationsEnabled = LocalAnimationsEnabled.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        AnimatedContent(
            targetState = preview,
            contentKey = { it.language },
            transitionSpec = {
                if (animationsEnabled) {
                    fadeIn(tween(CROSS_FADE_MILLIS)) togetherWith fadeOut(tween(CROSS_FADE_MILLIS)) using null
                } else {
                    EnterTransition.None togetherWith ExitTransition.None using null
                }
            },
            label = "languagePreview"
        ) { shown ->
            PreviewFrame(preview = shown, compact = compact)
        }
    }
}

@Composable
private fun PreviewFrame(preview: LanguagePreview, compact: Boolean) {
    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.language_preview_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = preview.language.nativeName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LocalizedContent(language = preview.language) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryPreview(preview, compact)
                if (!compact && preview.categories.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        preview.categories.forEach { CategoryPreview(it, Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryPreview(preview: LanguagePreview, compact: Boolean) {
    val summary = preview.summary

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!compact) {
                Text(
                    text = stringResource(preview.dayPeriod.greetingRes()),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    // As typed: a name is not translated
                    text = preview.userName.ifBlank { stringResource(R.string.greeting_no_name) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = CurrencyFormatter.format(summary.totalBalance),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = if (compact) 0.dp else 8.dp, bottom = 8.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryValue(
                    label = stringResource(R.string.summary_income),
                    value = CurrencyFormatter.formatWhole(summary.totalIncome),
                    modifier = Modifier.weight(1f)
                )
                SummaryValue(
                    label = stringResource(R.string.summary_expense),
                    value = CurrencyFormatter.formatWhole(summary.totalExpense),
                    modifier = Modifier.weight(1f)
                )
                SummaryValue(
                    label = stringResource(R.string.summary_savings),
                    value = stringResource(R.string.percent_value, summary.savingsRate.toInt())
                )
            }
        }
    }
}

@Composable
private fun SummaryValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CategoryPreview(category: CategoryBreakdown, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryIconTile(icon = category.icon, size = 28.dp)
            Column {
                Text(
                    text = category.categoryName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    // Spending, so shown as money going out
                    text = CurrencyFormatter.formatWhole(-category.amount),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

// Before the time of day is known the user is simply welcomed
private fun DayPeriod?.greetingRes(): Int = when (this) {
    DayPeriod.MORNING -> R.string.greeting_morning
    DayPeriod.AFTERNOON -> R.string.greeting_afternoon
    DayPeriod.EVENING -> R.string.greeting_evening
    null -> R.string.greeting_no_name
}

@PreviewLightDark
@Composable
private fun LanguagePreviewCardPreview() {
    ExpenseTrackerProTheme {
        Surface {
            LanguagePreviewCard(
                preview = LanguagePreview(
                    language = AppLanguage.HINDI,
                    dayPeriod = DayPeriod.EVENING,
                    userName = "Abhishek",
                    summary = PreviewData.financialSummary,
                    categories = PreviewData.categoryExpense.take(2)
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun LanguagePreviewCardCompactPreview() {
    ExpenseTrackerProTheme {
        Surface {
            LanguagePreviewCard(
                preview = LanguagePreview(
                    language = AppLanguage.GERMAN,
                    dayPeriod = DayPeriod.MORNING,
                    userName = "Abhishek",
                    summary = PreviewData.financialSummary,
                    categories = PreviewData.categoryExpense.take(2)
                ),
                modifier = Modifier.padding(16.dp),
                compact = true
            )
        }
    }
}
