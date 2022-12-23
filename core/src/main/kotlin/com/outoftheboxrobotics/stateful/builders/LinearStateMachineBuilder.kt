package com.outoftheboxrobotics.stateful.builders

import com.outoftheboxrobotics.stateful.statemachines.LinearStateMachine
import com.outoftheboxrobotics.stateful.states.State
import com.outoftheboxrobotics.stateful.states.UnitState

/**
 * Dsl builder for creating a [LinearStateMachine].
 */
@StateMachineDsl
class LinearStateMachineBuilder internal constructor() {
    private sealed interface LinearState
    private data class LinearTask(val task: () -> Unit) : LinearState
    private data class WaitTask(val millis: Long) : LinearState
    private data class InvokeTask(val stateMachine: LinearStateMachine<*>) : LinearState
    private data class LoopTask(
        val condition: () -> Boolean,
        val task: LinearStateMachineBuilder.() -> Unit
    ) : LinearState

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

    /**
     * State that loops while the condition is true.
     */
    fun loopWhile(condition: () -> Boolean, body: (@StateMachineDsl LinearStateMachineBuilder).() -> Unit) {
        linearStates.add(LoopTask(condition, body))
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

                is LoopTask -> object : UnitState {
                    var isStarted = false
                    var s = buildLinearStateMachine(state.task)

                    override fun run() =
                        if (!isStarted && !state.condition()) acc
                        else when {
                            !s.isFinished -> also { s.update() }
                            state.condition() -> {
                                s = s.createNew()
                                also { s.update() }
                            }
                            else -> {
                                s = s.createNew()
                                acc
                            }
                        }
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