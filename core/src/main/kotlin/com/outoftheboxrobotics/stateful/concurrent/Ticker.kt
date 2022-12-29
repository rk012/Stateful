package com.outoftheboxrobotics.stateful.concurrent

class Ticker : Job {
    private val childJobs = mutableListOf<Job>()

    override var isFinished = false

    override fun updateJob() {
        childJobs.forEach { it.updateJob() }
        if (childJobs.all { it.isFinished }) isFinished = true
    }
}