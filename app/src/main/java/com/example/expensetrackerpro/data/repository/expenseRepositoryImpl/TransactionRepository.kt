package com.example.expensetrackerpro.data.repository.expenseRepositoryImpl


import com.example.expensetrackerpro.data.local.dao.TransactionDao
import com.example.expensetrackerpro.data.local.entity.Category
import com.example.expensetrackerpro.data.local.entity.Transaction
import com.example.expensetrackerpro.data.local.entity.TransactionType
import com.example.expensetrackerpro.data.local.model.CategoryExpenseEntity
import com.example.expensetrackerpro.data.local.model.DailyCashFlowEntity
import com.example.expensetrackerpro.data.local.model.MonthlyTrendEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(private val transactionDao: TransactionDao) {

    // --- TRANSACTION CRUD (Replaces Expense methods) ---

    suspend fun saveTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions()

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun getTransactionById(id: Int): Transaction? {
        return transactionDao.getTransactionById(id)
    }

    // --- NEW: TYPE-SPECIFIC QUERIES ---

    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByType(type)
    }

    /**
     * Updated version of your getFilteredExpenses.
     * Now filters all transactions from a certain time.
     */
    fun getFilteredTransactions(startTimestamp: Long): Flow<List<Transaction>> {
        return transactionDao.getFilteredTransactions(startTimestamp)
    }

    // --- CATEGORY MANAGEMENT (Updated) ---

    suspend fun insertCategory(category: Category) {
        transactionDao.insertCategory(category)
    }

    /**
     * Returns all categories.
     */
    fun getAllCategories(): Flow<List<Category>> {
        return transactionDao.getAllCategories()
    }

    /**
     * NEW: Used for the Add Transaction screen to show
     * ONLY Income categories or ONLY Expense categories.
     */
    fun getCategoriesByType(type: TransactionType): Flow<List<Category>> {
        return transactionDao.getCategoriesByType(type)
    }

    suspend fun deleteCategory(category: Category) = transactionDao.deleteCategory(category)

    suspend fun updateCategory(category: Category) = transactionDao.updateCategory(category)


    // --- FINANCIAL ANALYTICS (New for Finance Manager) ---

    fun getMonthlyIncome(startMillis: Long): Flow<Double?> {
        return transactionDao.getMonthlyIncome(startMillis)
    }

    fun getMonthlyExpense(startMillis: Long): Flow<Double?> {
        return transactionDao.getMonthlyExpense(startMillis)
    }


    fun getCategoryExpense():
            Flow<List<CategoryExpenseEntity>> {

        return transactionDao.getCategoryExpense()
    }


    fun getDailyCashFlow():
            Flow<List<DailyCashFlowEntity>> {

        return transactionDao.getDailyCashFlow()
    }


    fun getMonthlyTrend():
            Flow<List<MonthlyTrendEntity>> {

        return transactionDao.getMonthlyTrend()
    }


    suspend fun getTotalIncome(
        startDate: Long,
        endDate: Long
    ): Double {

        return transactionDao.getTotalIncome(
            startDate,
            endDate
        )
    }


    suspend fun getTotalExpense(
        startDate: Long,
        endDate: Long
    ): Double {

        return transactionDao.getTotalExpense(
            startDate,
            endDate
        )
    }
}