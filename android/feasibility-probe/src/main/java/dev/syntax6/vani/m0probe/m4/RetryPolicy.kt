package dev.syntax6.vani.m0probe.m4

data class RetryDecision(val retry: Boolean, val attempt: Int, val backoffMillis: Long)

object M4RetryPolicy {
    fun decide(currentAttempt: Int, now: Long, expiresAt: Long): RetryDecision {
        require(currentAttempt >= 0)
        if (currentAttempt >= M4Protocol.MAX_RETRY_COUNT || now >= expiresAt) {
            return RetryDecision(false, currentAttempt, 0)
        }
        val backoff = minOf(8_000L, 250L shl currentAttempt)
        return if (now + backoff < expiresAt) RetryDecision(true, currentAttempt + 1, backoff)
        else RetryDecision(false, currentAttempt, 0)
    }
}
