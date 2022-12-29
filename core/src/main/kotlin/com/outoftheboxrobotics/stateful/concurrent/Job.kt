package com.outoftheboxrobotics.stateful.concurrent

interface Job {
    val isFinished: Boolean

    fun updateJob()
}