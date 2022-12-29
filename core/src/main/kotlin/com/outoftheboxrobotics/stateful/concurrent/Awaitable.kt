package com.outoftheboxrobotics.stateful.concurrent

/**
 * Represents a [LinearJob] that can be awaited on.
 */
interface Awaitable {
    /**
     * Whether the job is finished.
     */
    val isFinished: Boolean
}