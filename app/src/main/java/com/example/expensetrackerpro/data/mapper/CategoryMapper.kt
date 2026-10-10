package com.example.expensetrackerpro.data.mapper

import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.data.local.model.CategoryUsageEntity
import com.example.expensetrackerpro.domain.models.Category
import com.example.expensetrackerpro.domain.models.CategoryUsage
import com.example.expensetrackerpro.domain.models.Money

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    type = type,
    icon = icon,
    isCustom = isCustom
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    type = type,
    icon = icon,
    isCustom = isCustom
)

fun CategoryUsageEntity.toDomain(): CategoryUsage = CategoryUsage(
    category = category.toDomain(),
    totalCount = totalCount,
    periodCount = periodCount,
    periodAmount = Money(periodPaise)
)
