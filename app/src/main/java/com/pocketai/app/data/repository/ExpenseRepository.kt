package com.pocketai.app.data.repository

import com.pocketai.app.data.dao.ExpenseDao
import com.pocketai.app.data.model.Expense
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    
    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()
    
    fun getExpenseById(id: Int): Flow<Expense?> = expenseDao.getExpenseById(id)
    
    suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)
    
    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)
    
    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)
    
    suspend fun deleteExpenseById(id: Long) = expenseDao.deleteExpenseById(id)
    
    fun getExpensesByDateRange(startDate: String, endDate: String): Flow<List<Expense>> =
        expenseDao.getExpensesByDateRange(startDate, endDate)

    fun getTotalSpendingByDateRange(startDate: String, endDate: String): Flow<Double?> =
        expenseDao.getTotalSpendingByDateRange(startDate, endDate)

    fun getTotalExpenseCount(): Flow<Int> = expenseDao.getTotalExpenseCount()
    
    fun getExpensesByCategory(category: String): Flow<List<Expense>> =
        expenseDao.getExpensesByCategory(category)
    
    fun searchExpenses(query: String): Flow<List<Expense>> =
        expenseDao.searchExpenses(query)

    fun searchAndFilterExpenses(query: String, category: String?): Flow<List<Expense>> =
        expenseDao.searchAndFilterExpenses(query, category)
    
    fun getExpensesWithLimit(limit: Int, offset: Int): Flow<List<Expense>> =
        expenseDao.getExpensesWithLimit(limit, offset)
    
    fun getCategoryTotals(): Flow<List<com.pocketai.app.data.dao.CategoryTotal>> =
        expenseDao.getCategoryTotals()

    fun getCategoryTotalsByDateRange(startDate: String, endDate: String): Flow<List<com.pocketai.app.data.dao.CategoryTotal>> =
        expenseDao.getCategoryTotalsByDateRange(startDate, endDate)
    
    suspend fun getTotalSpendingAllTime(): Double? =
        expenseDao.getTotalSpendingAllTime()
    
    suspend fun deleteAllExpenses() = expenseDao.deleteAllExpenses()
}