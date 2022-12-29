package com.outoftheboxrobotics.stateful.concurrent

/**
 * Represents a completable job that can be updated.
 */
interface LinearJob : Awaitable {
    /**
     * Updates the job.
     */
    fun updateJob()
}