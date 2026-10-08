package com.example.expensetrackerpro.presentation.homeScreen.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.R
import com.example.expensetrackerpro.domain.models.SmartInsight

@Composable
fun InsightSection(
    insights: List<SmartInsight>
){

    Column(
        modifier = Modifier.padding(16.dp)
    ){

        Text(
            text = "Smart Insights",
            style = MaterialTheme.typography.titleMedium
        )


        Spacer(
            Modifier.height(12.dp)
        )


        insights.forEach {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ){

                Column(
                    modifier = Modifier.padding(16.dp)
                ){

                    Text(
                        text = it.title(),
                        style = MaterialTheme.typography.titleSmall
                    )


                    Text(
                        text = it.description()
                    )

                }

            }

        }

    }

}

@Composable
private fun SmartInsight.title(): String = when (this) {
    is SmartInsight.Saved -> stringResource(R.string.insight_saved_title)
    is SmartInsight.HighestSpending -> stringResource(R.string.insight_highest_spending_title)
    SmartInsight.SpendingImproved -> stringResource(R.string.insight_spending_improved_title)
    SmartInsight.SpendingIncreased -> stringResource(R.string.insight_spending_increased_title)
}

@Composable
private fun SmartInsight.description(): String = when (this) {
    is SmartInsight.Saved ->
        stringResource(R.string.insight_saved_description, amount.toInt())
    is SmartInsight.HighestSpending ->
        stringResource(R.string.insight_highest_spending_description, categoryName)
    SmartInsight.SpendingImproved ->
        stringResource(R.string.insight_spending_improved_description)
    SmartInsight.SpendingIncreased ->
        stringResource(R.string.insight_spending_increased_description)
}
