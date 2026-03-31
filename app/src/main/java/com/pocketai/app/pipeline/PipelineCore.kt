package com.pocketai.app.pipeline

/**
 * Result of a pipeline stage execution.
 * Captures timing, success/failure, and any errors.
 * 
 * Phase 10 Re-architecture: Core abstraction for traceable pipeline.
 */
sealed class PipelineResult<out T> {
    abstract val stageName: String
    abstract val durationMs: Long
    
    /**
     * Stage completed successfully
     */
    data class Success<T>(
        override val stageName: String,
        override val durationMs: Long,
        val data: T
    ) : PipelineResult<T>()
    
    /**
     * Stage failed with an error
     */
    data class Failure(
        override val stageName: String,
        override val durationMs: Long,
        val error: String,
        val exception: Throwable? = null,
        val partialData: Any? = null  // Any partial data that was extracted before failure
    ) : PipelineResult<Nothing>()
    
    /**
     * Stage was skipped (e.g., fallback not needed)
     */
    data class Skipped(
        override val stageName: String,
        val reason: String
    ) : PipelineResult<Nothing>() {
        override val durationMs: Long = 0
    }
    
    fun isSuccess(): Boolean = this is Success
    fun isFailure(): Boolean = this is Failure
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw PipelineException(stageName, error, exception)
        is Skipped -> throw PipelineException(stageName, "Stage was skipped: $reason")
    }
}

/**
 * Exception thrown when pipeline stage fails
 */
class PipelineException(
    val stageName: String,
    message: String,
    cause: Throwable? = null
) : Exception("[$stageName] $message", cause)

/**
 * Interface for a single pipeline stage.
 * Each stage transforms input I to output O.
 */
interface PipelineStage<I, O> {
    val name: String
    
    /**
     * Process the input and return a result.
     * Implementations should not throw - all errors should be captured in PipelineResult.Failure
     */
    suspend fun process(input: I): PipelineResult<O>
}
