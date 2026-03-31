package com.pocketai.app.services

/**
 * Sealed class representing the result of an LLM inference operation.
 * Provides type-safe handling of success, error, and loading states.
 * 
 * Phase 9c: Added for robust result handling and debugging.
 */
sealed class InferenceResult<out T> {
    /**
     * Inference succeeded with a valid result.
     * @param data The parsed result
     * @param rawOutput The raw LLM output for debugging
     */
    data class Success<T>(
        val data: T,
        val rawOutput: String
    ) : InferenceResult<T>()
    
    /**
     * Inference failed with an error.
     * @param message Human-readable error message
     * @param rawOutput The raw LLM output if available (for debugging)
     * @param exception The underlying exception if any
     */
    data class Error(
        val message: String,
        val rawOutput: String? = null,
        val exception: Throwable? = null
    ) : InferenceResult<Nothing>()
    
    /**
     * Inference is in progress.
     * @param stage The current stage of processing
     */
    data class Loading(
        val stage: String = "Processing..."
    ) : InferenceResult<Nothing>()
    
    /**
     * Check if this result is successful.
     */
    fun isSuccess(): Boolean = this is Success
    
    /**
     * Get the data if successful, or null otherwise.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    /**
     * Get the error message if failed, or null otherwise.
     */
    fun errorOrNull(): String? = when (this) {
        is Error -> message
        else -> null
    }
    
    companion object {
        /**
         * Create a success result.
         */
        fun <T> success(data: T, rawOutput: String): InferenceResult<T> = 
            Success(data, rawOutput)
        
        /**
         * Create an error result.
         */
        fun error(message: String, rawOutput: String? = null, exception: Throwable? = null): InferenceResult<Nothing> = 
            Error(message, rawOutput, exception)
        
        /**
         * Create a loading result.
         */
        fun loading(stage: String = "Processing..."): InferenceResult<Nothing> = 
            Loading(stage)
    }
}
