package com.example.expensetrackerpro.data.mapper

import com.example.expensetrackerpro.data.local.entity.CategoryMemoryEntity
import com.example.expensetrackerpro.domain.models.CategoryId
import com.example.expensetrackerpro.domain.models.CategoryMemory

fun CategoryMemoryEntity.toDomain(): CategoryMemory =
    CategoryMemory(titleKey = normalizedTitle, type = type, categoryId = CategoryId(categoryId))

fun CategoryMemory.toEntity(): CategoryMemoryEntity =
    CategoryMemoryEntity(normalizedTitle = titleKey, type = type, categoryId = categoryId.value)
