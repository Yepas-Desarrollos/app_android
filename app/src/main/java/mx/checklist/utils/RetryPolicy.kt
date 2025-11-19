package mx.checklist.utils

import kotlinx.coroutines.delay

object RetryPolicy {
    suspend inline fun <T> withRetry(
        maxRetries: Int = 3,
        delayMs: Long = 1000,
        backoffMultiplier: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = delayMs
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    delay(currentDelay)
                    currentDelay = (currentDelay * backoffMultiplier).toLong()
                }
            }
        }

        throw lastException ?: Exception("Unknown error after $maxRetries retries")
    }
}

