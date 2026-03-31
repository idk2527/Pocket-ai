package com.pocketai.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketai.app.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.system.measureTimeMillis

enum class BenchmarkState { IDLE, RUNNING, COMPLETED }
data class BenchmarkResult(val isCapable: Boolean, val estimatedSeconds: Float)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _benchmarkState = MutableStateFlow(BenchmarkState.IDLE)
    val benchmarkState: StateFlow<BenchmarkState> = _benchmarkState.asStateFlow()

    private val _benchmarkResult = MutableStateFlow<BenchmarkResult?>(null)
    val benchmarkResult: StateFlow<BenchmarkResult?> = _benchmarkResult.asStateFlow()

    fun runHardwareBenchmark() {
        if (_benchmarkState.value != BenchmarkState.IDLE) return
        
        viewModelScope.launch {
            _benchmarkState.value = BenchmarkState.RUNNING
            
            // Simulate AI tensor operations (Matrix Multiplication)
            val timeTaken = withContext(Dispatchers.Default) {
                measureTimeMillis {
                    val size = 500
                    val matrixA = Array(size) { FloatArray(size) { Math.random().toFloat() } }
                    val matrixB = Array(size) { FloatArray(size) { Math.random().toFloat() } }
                    val result = Array(size) { FloatArray(size) }
                    
                    for (i in 0 until size) {
                        for (j in 0 until size) {
                            var sum = 0f
                            for (k in 0 until size) {
                                sum += matrixA[i][k] * matrixB[k][j]
                            }
                            result[i][j] = sum
                        }
                    }
                }
            }
            
            // Artificial delay to make it feel like a deep scan
            delay(1500)
            
            // Heuristic converting standard math speed to LLM speed estimate
            // Time taken is usually around 50ms - 300ms depending on CPU.
            // A flagship phone might do it in 80ms. An old phone in 400ms.
            val estimatedLlmTime = (timeTaken / 100f) * 8f + 3f // Base 3 sec + scaling
            val finalEstimate = estimatedLlmTime.coerceIn(2.5f, 45f) // Cap limits
            
            _benchmarkResult.value = BenchmarkResult(
                isCapable = finalEstimate <= 30f,
                estimatedSeconds = finalEstimate
            )
            _benchmarkState.value = BenchmarkState.COMPLETED
        }
    }

    fun completeOnboarding(name: String, budget: Float) {
        viewModelScope.launch {
            preferencesManager.setUserName(name)
            preferencesManager.setMonthlyBudget(budget)
            preferencesManager.setOnboardingCompleted(true)
        }
    }
}
