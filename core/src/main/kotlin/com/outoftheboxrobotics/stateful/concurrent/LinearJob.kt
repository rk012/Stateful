package com.outoftheboxrobotics.stateful.concurrent

interface LinearJob {
    val isFinished: Boolean

    fun updateJob()
}