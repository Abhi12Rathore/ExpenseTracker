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
import androidx.compose.ui.unit.dp
import com.example.expensetrackerpro.domain.models.FinancialInsight

@Composable
fun InsightSection(
    insights: List<FinancialInsight>
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
                        text = it.title,
                        style = MaterialTheme.typography.titleSmall
                    )


                    Text(
                        text = it.description
                    )

                }

            }

        }

    }

}