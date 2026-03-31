package com.pocketai.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketai.app.data.dao.CategoryTotal
import com.pocketai.app.data.model.Expense
import com.pocketai.app.data.preferences.PreferencesManager
import com.pocketai.app.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val exportService: com.pocketai.app.services.ExportService,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val userName = preferencesManager.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "User")

    val monthlyBudget = preferencesManager.monthlyBudget
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000f)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val allExpenses: StateFlow<List<Expense>> = combine(
        _searchQuery,
        _selectedCategory
    ) { query, category ->
        Pair(query, category)
    }.flatMapLatest { (query, category) ->
        _isLoading.value = true
        repository.searchAndFilterExpenses(query, category)
            .onEach { _isLoading.value = false }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: String?) {
        _selectedCategory.value = category
    }

    fun addExpense(storeName: String, category: String, amount: Double, date: String, note: String? = null) {
        viewModelScope.launch {
            try {
                val expense = Expense(
                    storeName = storeName,
                    category = category,
                    amount = amount,
                    date = date,
                    note = note,
                    createdAt = System.currentTimeMillis().toString()
                )

                val id = repository.insertExpense(expense)
                if (id > 0) {
                    _snackbarMessage.value = "Expense added successfully!"
                } else {
                    _snackbarMessage.value = "Failed to add expense"
                }
            } catch (e: Exception) {
                _snackbarMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                repository.updateExpense(expense)
                _snackbarMessage.value = "Expense updated!"
            } catch (e: Exception) {
                _snackbarMessage.value = "Error: ${e.message}"
            }
        }
    }

    private var lastDeletedExpense: Expense? = null

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                lastDeletedExpense = expense
                repository.deleteExpense(expense)
                _snackbarMessage.value = "Expense deleted"
                // Ideally, the View should show the Snackbar with an Undo action that calls undoDelete()
            } catch (e: Exception) {
                _snackbarMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun undoDelete() {
        viewModelScope.launch {
            lastDeletedExpense?.let { expense ->
                repository.insertExpense(expense)
                lastDeletedExpense = null
                _snackbarMessage.value = "Expense restored"
            }
        }
    }

    fun getExpenseById(id: Int): Flow<Expense?> {
        return repository.getExpenseById(id)
    }

    fun getTotalSpendingForMonth(): Flow<Double?> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        return repository.getTotalSpendingByDateRange(startDate, endDate)
    }

    fun getTotalSpendingLast7Days(): Flow<Double?> {
        val calendar = Calendar.getInstance()
        val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        return repository.getTotalSpendingByDateRange(startDate, endDate)
    }

    fun getTotalExpenseCount(): Flow<Int> = repository.getTotalExpenseCount()

    fun getCategoryTotals(): Flow<List<CategoryTotal>> = repository.getCategoryTotals()

    fun getTotalSpendingLastMonth(): Flow<Double?> {
        val calendar = Calendar.getInstance()
        // Go to first day of current month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        // Go back one day to get last day of previous month
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        // Go to first day of previous month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        return repository.getTotalSpendingByDateRange(startDate, endDate)
    }
    
    fun getTopCategory(): Flow<String?> {
        return repository.getCategoryTotals().map { categories ->
            categories.maxByOrNull { it.total }?.category
        }
    }

    fun getCategoryTotalsForMonth(): Flow<List<CategoryTotal>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        return repository.getCategoryTotalsByDateRange(startDate, endDate)
    }

    fun getTopCategoryForMonth(): Flow<String?> {
        return getCategoryTotalsForMonth().map { categories ->
            categories.maxByOrNull { it.total }?.category
        }
    }

    fun dismissSnackbar() {
        _snackbarMessage.value = null
    }
}