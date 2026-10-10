package com.example.expensetrackerpro.data.repository

import android.database.sqlite.SQLiteException
import com.example.expensetrackerpro.data.local.dao.AppDataDao
import com.example.expensetrackerpro.domain.repository.AppDataRepository
import javax.inject.Inject

class AppDataRepositoryImpl @Inject constructor(
    private val appDataDao: AppDataDao
) : AppDataRepository {

    override suspend fun deleteAllData(): Boolean =
        try {
            appDataDao.deleteAll()
            true
        } catch (e: SQLiteException) {
            false
        }
}
