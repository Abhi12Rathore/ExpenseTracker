package com.example.expensetrackerpro.data.mapper

import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryMapperTest {

    private val entity = CategoryEntity(
        id = 4,
        name = "Salary",
        type = TransactionType.INCOME,
        iconName = "Payments",
        isCustom = false
    )

    private val domain = Category(
        id = 4,
        name = "Salary",
        type = TransactionType.INCOME,
        iconName = "Payments",
        isCustom = false
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
            iconName = "Home"
        ).toEntity()

        assertEquals(0, mapped.id)
        assertTrue(mapped.isCustom)
    }
}
