package com.example.expensetrackerpro.data.mapper

import com.example.expensetrackerpro.data.local.entity.CategoryEntity
import com.example.expensetrackerpro.domain.models.Category

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
