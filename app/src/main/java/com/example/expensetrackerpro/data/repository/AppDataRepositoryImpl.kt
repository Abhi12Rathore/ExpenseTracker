package com.example.expensetrackerpro.data.repository

import com.example.expensetrackerpro.data.local.dao.AppDataDao
import com.example.expensetrackerpro.data.mapper.storageOutcome
import com.example.expensetrackerpro.domain.error.Outcome
import com.example.expensetrackerpro.domain.repository.AppDataRepository
import javax.inject.Inject

class AppDataRepositoryImpl @Inject constructor(
    private val appDataDao: AppDataDao
) : AppDataRepository {

    override suspend fun deleteAllData(): Outcome<Unit> =
        storageOutcome { appDataDao.deleteAll() }
}
