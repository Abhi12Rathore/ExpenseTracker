package com.example.expensetrackerpro.data.mapper

import com.example.expensetrackerpro.domain.models.BuiltInCategory
import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.domain.models.CategoryIcon
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryMapperTest {

    private val entity = CategoryEntity(
        id = 4,
        name = "Salary",
        type = TransactionType.INCOME,
        icon = CategoryIcon.PAYMENTS,
        isCustom = false,
        builtInKey = BuiltInCategory.SALARY
    )

    private val domain = Category(
        id = CategoryId(4),
        name = "Salary",
        type = TransactionType.INCOME,
        icon = CategoryIcon.PAYMENTS,
        isCustom = false,
        builtInKey = BuiltInCategory.SALARY
    )

    @Test
    fun entityToDomain_copiesEveryField() {
        assertEquals(domain, entity.toDomain())
    }

    @Test
    fun domainToEntity_copiesEveryField() {
        assertEquals(entity, domain.toEntity())
    }

    @Test
    fun newCategory_defaultsToCustomWithZeroId() {
        val mapped = Category(
            name = "Gym",
            type = TransactionType.EXPENSE,
            icon = CategoryIcon.HOME
        ).toEntity()

        assertEquals(0, mapped.id)
        assertTrue(mapped.isCustom)
    }
}
