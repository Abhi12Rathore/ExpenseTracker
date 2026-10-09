package com.example.expensetrackerpro.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryIconTest {

    @Test
    fun keys_areUnique_andLowerSnakeCase() {
        val keys = CategoryIcon.entries.map { it.key }

        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.all { key -> key.matches(Regex("[a-z]+(_[a-z]+)*")) })
    }

    @Test
    fun fromKey_findsEveryIcon_andFallsBackToTheDefault() {
        CategoryIcon.entries.forEach { icon -> assertEquals(icon, CategoryIcon.fromKey(icon.key)) }

        assertEquals(CategoryIcon.DEFAULT, CategoryIcon.fromKey("hoverboard"))
        assertEquals(CategoryIcon.DEFAULT, CategoryIcon.fromKey(""))
    }

    @Test
    fun everyGroup_hasIcons_andTogetherTheyAreEveryIcon() {
        val grouped = CategoryIconGroup.entries.map { CategoryIcon.inGroup(it) }

        assertTrue(grouped.all { it.isNotEmpty() })
        assertEquals(CategoryIcon.entries.toSet(), grouped.flatten().toSet())
    }

    @Test
    fun defaults_areSixteenExpenseAndNineIncome_withUniqueNamesAndIcons() {
        assertEquals(16, DefaultCategories.expense.size)
        assertEquals(9, DefaultCategories.income.size)
        assertTrue(DefaultCategories.expense.all { it.type == TransactionType.EXPENSE && !it.isCustom })
        assertTrue(DefaultCategories.income.all { it.type == TransactionType.INCOME && !it.isCustom })

        val all = DefaultCategories.all
        assertEquals(all.size, all.map { it.name }.toSet().size)
        assertEquals(all.size, all.map { it.icon }.toSet().size)
    }
}
