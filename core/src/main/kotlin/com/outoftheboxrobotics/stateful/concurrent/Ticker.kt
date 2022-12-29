package com.outoftheboxrobotics.stateful.concurrent

class Ticker : LinearJob {
    private val childJobs = mutableListOf<LinearJob>()

    override var isFinished = false

    override fun updateJob() {
        childJobs.forEach { it.updateJob() }
        if (childJobs.all { it.isFinished }) isFinished = true
    }
}