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
    private data class InvokeTask(val stateMachine: LinearStateMachine<*>) : LinearState

    private val endState = object : State<Unit> {
        override val value = Unit
        override fun run() = this
    }

    private val linearStates = mutableListOf<LinearState>()

    /**
     * Runs the enclosed lambda as a state.
     */
    fun task(block: () -> Unit) {
        linearStates.add(LinearTask(block))
    }

    /**
     * State that waits for the specified amount of time before continuing.
     *
     * Note: this is not safe to use in multiple copies of the same state machine running at the same time.
     */
    fun waitMillis(millis: Long) {
        linearStates.add(WaitTask(millis))
    }

    /**
     * Runs the given state machine until completion as a state.
     */
    fun runStateMachine(stateMachine: LinearStateMachine<*>) {
        linearStates.add(InvokeTask(stateMachine.createNew()))
    }

    internal fun build(): LinearStateMachine<Unit> {
        val state = linearStates.foldRight<_, State<Unit>>(endState) { state, acc ->
            when (state) {
                is LinearTask -> UnitState { state.task(); acc }

                is WaitTask -> object : UnitState {
                    var start: Long? = null

                    override fun run() = start?.let {
                        if (System.currentTimeMillis() - start!! > state.millis) acc.also { start = null }
                        else this
                    } ?: also { start = System.currentTimeMillis() }
                }

                is InvokeTask -> object : UnitState {
                    val s = state.stateMachine

                    override fun run() = if (!s.isFinished) also { s.update() } else acc
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