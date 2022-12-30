package com.outoftheboxrobotics.stateful.concurrent

import com.outoftheboxrobotics.stateful.statemachines.LinearStateMachine

/**
 * Tickers are used to run multiple [LinearJobs][LinearJob] with support for structured concurrency.
 */
class Ticker : LinearJob {
    private val childJobs = mutableListOf<LinearJob>()

    override var isFinished = false

    override fun updateJob() {
        childJobs.forEach { it.updateJob() }
        isFinished = childJobs.all { it.isFinished }
    }

    /**
     * Adds a [LinearJob] to the ticker. Usually this is a [LinearStateMachine] or another [Ticker].
     */
    fun launchJob(job: LinearJob) {
        childJobs.add(job)
    }
}