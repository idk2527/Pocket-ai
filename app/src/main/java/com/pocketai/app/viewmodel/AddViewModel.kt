package com.pocketai.app.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketai.app.data.model.Expense
import com.pocketai.app.data.model.ReceiptData
import com.pocketai.app.data.repository.ExpenseRepository
import com.pocketai.app.pipeline.PipelineOutput
import com.pocketai.app.pipeline.PipelineState
import com.pocketai.app.pipeline.ReceiptPipeline
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Add/Edit Expense screen.
 * Phase 17: Supports enriched receipt data with full Veryfi-style fields.
 */
@HiltViewModel
class AddViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val receiptPipeline: ReceiptPipeline
) : ViewModel() {

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _parsedExpense = MutableStateFlow<Expense?>(null)
    val parsedExpense: StateFlow<Expense?> = _parsedExpense.asStateFlow()

    // Phase 17: Full receipt data (items, VAT, payment, etc.)
    private val _parsedReceiptData = MutableStateFlow<ReceiptData?>(null)
    val parsedReceiptData: StateFlow<ReceiptData?> = _parsedReceiptData.asStateFlow()

    // Pipeline state for UI
    val pipelineState: StateFlow<PipelineState> = receiptPipeline.state

    // Last pipeline output with metrics
    private val _lastPipelineOutput = MutableStateFlow<PipelineOutput?>(null)
    val lastPipelineOutput: StateFlow<PipelineOutput?> = _lastPipelineOutput.asStateFlow()

    // Partial JSON result for real-time UI typing
    val partialResult: StateFlow<String> = receiptPipeline.partialResult

    // Processing state derived from pipeline
    val isParsing: StateFlow<Boolean> = MutableStateFlow(false).also { flow ->
        viewModelScope.launch {
            pipelineState.collect { state ->
                (flow as MutableStateFlow).value = state is PipelineState.Processing
            }
        }
    }

    /**
     * Process receipt image through the VLM pipeline.
     */
    fun processReceiptImage(imageUri: Uri) {
        viewModelScope.launch {
            try {
                android.util.Log.i("AddViewModel", "[PIPELINE] Starting image processing: $imageUri")

                val output = receiptPipeline.processImage(imageUri)
                _lastPipelineOutput.value = output

                // Store full receipt data for preview card + digital receipt
                _parsedReceiptData.value = output.data

                if (output.expense != null) {
                    _parsedExpense.value = output.expense
                    if (output.success) {
                        val itemCount = output.data?.items?.size ?: 0
                        android.util.Log.i("AddViewModel", "[PIPELINE] Success in ${output.totalTimeMs}ms — $itemCount items extracted")
                    } else {
                        _snackbarMessage.value = "Partial extraction: ${output.error}"
                    }
                } else {
                    _parsedExpense.value = null
                    _snackbarMessage.value = "Could not parse receipt: ${output.error}"
                }
            } catch (e: Exception) {
                android.util.Log.e("AddViewModel", "[PIPELINE] Exception: ${e.message}", e)
                _snackbarMessage.value = "Error processing image: ${e.message}"
            }
        }
    }

    /**
     * Add expense with basic fields (backward compatible)
     */
    fun addExpense(
        storeName: String,
        category: String,
        amount: Double,
        date: String,
        note: String?,
        receiptPath: String?
    ) {
        viewModelScope.launch {
            val newExpense = Expense(
                storeName = storeName,
                category = category,
                amount = amount,
                date = date,
                note = note,
                receiptImagePath = receiptPath,
                createdAt = System.currentTimeMillis().toString()
            )
            expenseRepository.insertExpense(newExpense)
            _snackbarMessage.value = "Expense added successfully"
        }
    }

    /**
     * Phase 17: Add expense with enriched receipt data (items, VAT, payment, etc.)
     */
    fun addExpenseEnriched(
        storeName: String,
        category: String,
        amount: Double,
        date: String,
        note: String?,
        receiptPath: String?,
        receiptData: ReceiptData?
    ) {
        viewModelScope.launch {
            // Use NonCancellable to ensure DB insert completes even if user navigates away immediately
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                val newExpense = if (receiptData != null) {
                    android.util.Log.i("AddViewModel", "Saving enriched expense with rawJson length: ${receiptData.rawJson?.length}")
                    // Use enriched data from VLM extraction
                    receiptData.toExpense(receiptPath).copy(
                        storeName = storeName,    // User may have edited
                        category = category,      // User may have edited
                        amount = amount,          // User may have edited
                        date = date,              // User may have edited
                        note = note
                    )
                } else {
                    android.util.Log.i("AddViewModel", "Saving standard expense (no enriched data)")
                    Expense(
                        storeName = storeName,
                        category = category,
                        amount = amount,
                        date = date,
                        note = note,
                        receiptImagePath = receiptPath,
                        createdAt = System.currentTimeMillis().toString()
                    )
                }
                expenseRepository.insertExpense(newExpense)
                _snackbarMessage.value = "Expense added successfully"
            }
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.updateExpense(expense)
            _snackbarMessage.value = "Expense updated successfully"
        }
    }

    fun getExpenseById(id: Int) = expenseRepository.getExpenseById(id)

    fun dismissSnackbar() {
        _snackbarMessage.value = null
    }

    fun clearParsedExpense() {
        _parsedExpense.value = null
        _parsedReceiptData.value = null
        receiptPipeline.reset()
    }
}
