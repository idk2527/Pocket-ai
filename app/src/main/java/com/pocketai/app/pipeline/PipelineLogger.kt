package com.pocketai.app.pipeline

import android.util.Log

/**
 * Structured logging for pipeline stages.
 * All pipeline operations log through here for consistent, traceable output.
 * 
 * Log format: [PIPELINE] STAGE_NAME → duration_ms → summary
 * 
 * Phase 10 Re-architecture: Centralized observability.
 */
object PipelineLogger {
    private const val TAG = "ReceiptPipeline"
    
    /**
     * Log a successful stage completion
     */
    fun stageSuccess(stageName: String, durationMs: Long, summary: String) {
        Log.i(TAG, "[PIPELINE] $stageName → ${durationMs}ms → $summary")
    }
    
    /**
     * Log a stage failure
     */
    fun stageError(stageName: String, durationMs: Long, error: String, exception: Throwable? = null) {
        Log.e(TAG, "[PIPELINE] $stageName → ${durationMs}ms → ERROR: $error", exception)
    }
    
    /**
     * Log a stage being skipped
     */
    fun stageSkipped(stageName: String, reason: String) {
        Log.d(TAG, "[PIPELINE] $stageName → SKIPPED: $reason")
    }
    
    /**
     * Log pipeline start
     */
    fun pipelineStart(inputSummary: String) {
        Log.i(TAG, "[PIPELINE] ════════ START ════════")
        Log.i(TAG, "[PIPELINE] Input: $inputSummary")
    }
    
    /**
     * Log pipeline completion with metrics
     */
    fun pipelineComplete(totalMs: Long, stageTimings: Map<String, Long>, success: Boolean) {
        val status = if (success) "SUCCESS" else "FAILED"
        Log.i(TAG, "[PIPELINE] ════════ $status ════════")
        Log.i(TAG, "[PIPELINE] Total: ${totalMs}ms")
        stageTimings.forEach { (stage, time) ->
            val pct = if (totalMs > 0) (time * 100 / totalMs) else 0
            Log.i(TAG, "[PIPELINE]   $stage: ${time}ms (${pct}%)")
        }
    }
    
    /**
     * Log raw data for debugging (verbose)
     */
    fun debug(stageName: String, message: String) {
        Log.d(TAG, "[PIPELINE] [$stageName] $message")
    }
    
    /**
     * Log the result of a pipeline stage
     */
    fun <T> logResult(result: PipelineResult<T>) {
        when (result) {
            is PipelineResult.Success -> {
                stageSuccess(
                    result.stageName,
                    result.durationMs,
                    result.data.toString().take(100)
                )
            }
            is PipelineResult.Failure -> {
                stageError(
                    result.stageName,
                    result.durationMs,
                    result.error,
                    result.exception
                )
            }
            is PipelineResult.Skipped -> {
                stageSkipped(result.stageName, result.reason)
            }
        }
    }
}

/**
 * Extension to measure and log a pipeline stage execution
 */
suspend fun <I, O> PipelineStage<I, O>.executeWithLogging(input: I): PipelineResult<O> {
    val startTime = System.currentTimeMillis()
    val result = process(input)
    PipelineLogger.logResult(result)
    return result
}
