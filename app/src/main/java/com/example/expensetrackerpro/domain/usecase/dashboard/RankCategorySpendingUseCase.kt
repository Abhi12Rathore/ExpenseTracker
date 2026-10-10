package com.example.expensetrackerpro.domain.usecase.dashboard

import com.example.expensetrackerpro.domain.models.CategoryBreakdown
import com.example.expensetrackerpro.domain.models.CategoryRanking
import com.example.expensetrackerpro.domain.models.CategoryShare
import com.example.expensetrackerpro.domain.models.sumOfMoney
import javax.inject.Inject

/** Ranks categories by spend and works out each one's share of the total. */
class RankCategorySpendingUseCase @Inject constructor() {

    /** The [topCount] biggest categories lead; the rest are grouped as the others. */
    operator fun invoke(breakdown: List<CategoryBreakdown>, topCount: Int = 5): CategoryRanking {
        val total = breakdown.sumOfMoney { it.amount }
        val shares = breakdown
            .sortedByDescending { it.amount }
            .map { CategoryShare(it, it.amount.ratioTo(total)) }

        // A single leftover category is shown as itself rather than as a group of one
        val leading = if (shares.size <= topCount + 1) shares.size else topCount

        return CategoryRanking(
            total = total,
            top = shares.take(leading),
            others = shares.drop(leading)
        )
    }
}
