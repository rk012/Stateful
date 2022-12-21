package com.outoftheboxrobotics.stateful.builders

import com.outoftheboxrobotics.stateful.LinearStateMachine
import com.outoftheboxrobotics.stateful.State
import com.outoftheboxrobotics.stateful.UnitState

/**
 * Dsl builder for creating a [LinearStateMachine].
 */
@StateMachineDsl
class LinearStateMachineBuilder internal constructor() {
    private sealed interface LinearState
    private data class LinearTask(val task: () -> Unit) : LinearState
    private data class WaitTask(val millis: Long) : LinearState

    private val endState = object : State<Unit> {
        override val value = Unit
        override fun run() = this
    }

    private val linearStates = mutableListOf<LinearState>()

    fun task(block: () -> Unit) {
        linearStates.add(LinearTask(block))
    }

    fun waitMillis(millis: Long) {
        linearStates.add(WaitTask(millis))
    }

    internal fun build(): LinearStateMachine<Unit> {
        val state = linearStates.foldRight<_, State<Unit>>(endState) { state, acc ->
            when (state) {
                is LinearTask -> UnitState { state.task(); acc }
                is WaitTask -> object : UnitState {
                    var start: Long? = null

                    override fun run() = start?.let {
                        if (System.currentTimeMillis() - start!! > state.millis) acc
                        else this
                    } ?: also { start = System.currentTimeMillis() }
                }
            }
        }

        return LinearStateMachine(state, endState)
    }
}

/**
 * Dsl for building a [LinearStateMachine].
 */
fun buildLinearStateMachine(block: LinearStateMachineBuilder.() -> Unit) =
    LinearStateMachineBuilder().apply(block).build()