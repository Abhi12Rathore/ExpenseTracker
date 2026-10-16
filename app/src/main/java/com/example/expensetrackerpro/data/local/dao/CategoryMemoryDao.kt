package com.example.expensetrackerpro.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.expensetrackerpro.data.local.entity.CategoryMemoryEntity
import kotlinx.coroutines.flow.Flow

/** The categories the user picked for titles, which quick add reads. */
@Dao
interface CategoryMemoryDao {

    @Query("SELECT * FROM category_memory")
    fun observeAll(): Flow<List<CategoryMemoryEntity>>

    @Upsert
    suspend fun upsert(memory: CategoryMemoryEntity)
}
