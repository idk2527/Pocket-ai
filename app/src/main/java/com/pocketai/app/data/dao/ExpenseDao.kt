package com.pocketai.app.data.dao

import androidx.room.*
import com.pocketai.app.data.model.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC, createdAt DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    fun getExpenseById(id: Int): Flow<Expense?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense): Int

    @Delete
    suspend fun deleteExpense(expense: Expense): Int

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long): Int

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, createdAt DESC")
    fun getExpensesByDateRange(startDate: String, endDate: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE category = :category ORDER BY date DESC, createdAt DESC")
    fun getExpensesByCategory(category: String): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    fun getTotalSpendingByDateRange(startDate: String, endDate: String): Flow<Double?>

    @Query("SELECT COUNT(*) FROM expenses")
    fun getTotalExpenseCount(): Flow<Int>

    @Query("SELECT * FROM expenses WHERE storeName LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' ORDER BY date DESC, createdAt DESC")
    fun searchExpenses(query: String): Flow<List<Expense>>

    @Query("""
        SELECT * FROM expenses 
        WHERE (storeName LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%')
        AND (:category IS NULL OR category = :category)
        ORDER BY date DESC, createdAt DESC
    """)
    fun searchAndFilterExpenses(query: String, category: String?): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY date DESC, createdAt DESC LIMIT :limit OFFSET :offset")
    fun getExpensesWithLimit(limit: Int, offset: Int): Flow<List<Expense>>

    @Query("SELECT category, SUM(amount) as total, COUNT(*) as count FROM expenses GROUP BY category ORDER BY total DESC")
    fun getCategoryTotals(): Flow<List<CategoryTotal>>

    @Query("SELECT category, SUM(amount) as total, COUNT(*) as count FROM expenses WHERE date BETWEEN :startDate AND :endDate GROUP BY category ORDER BY total DESC")
    fun getCategoryTotalsByDateRange(startDate: String, endDate: String): Flow<List<CategoryTotal>>

    @Query("SELECT SUM(amount) FROM expenses")
    suspend fun getTotalSpendingAllTime(): Double?
    
    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses(): Int
}

data class CategoryTotal(
    val category: String,
    val total: Double,
    val count: Int
)