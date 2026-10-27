package com.example.puzzlebubbles.domain

/** Monotonic recording time; UI recreation never creates a new deadline. */
data class RecordingDeadline(val startedAtMs: Long) {
    fun elapsed(nowMs: Long) = (nowMs - startedAtMs).coerceIn(0, LIMIT_MS)
    fun expired(nowMs: Long) = nowMs - startedAtMs >= LIMIT_MS
    companion object {
        const val LIMIT_MS = 5_000L
        // Native AAC output may include a small amount of encoder/container overhead.
        // This does not extend either the recorder setting or the UI deadline.
        const val MAX_OUTPUT_DURATION_MS = LIMIT_MS + 100L
    }
}
