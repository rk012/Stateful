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

    class ConditionalTask internal constructor(
        internal val conditions: MutableList<Pair<() -> Boolean, LinearStateMachine<Unit>>> = mutableListOf()
    ) : LinearState {
        fun elif(condition: () -> Boolean, block: (@StateMachineDsl LinearStateMachineBuilder).() -> Unit) = apply {
            conditions.add(condition to buildLinearStateMachine(block))
        }

        infix fun elseRun(block: (@StateMachineDsl LinearStateMachineBuilder).() -> Unit) {
            conditions.add({ true } to buildLinearStateMachine(block))
        }
    }

    private val endState = object : UnitState() {
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

    fun runIf(condition: () -> Boolean, block: (@StateMachineDsl LinearStateMachineBuilder).() -> Unit) =
        ConditionalTask(mutableListOf(condition to buildLinearStateMachine(block))).also {
            linearStates.add(it)
        }

    internal fun build(): LinearStateMachine<Unit> {
        val state = linearStates.foldRight<_, State<Unit>>(endState) { state, acc ->
            when (state) {
                is LinearTask -> unitState { state.task(); acc }

                is WaitTask -> object : UnitState() {
                    var start: Long? = null

                    override fun run() = start?.let {
                        if (System.currentTimeMillis() - start!! > state.millis) acc.also { start = null }
                        else this
                    } ?: also { start = System.currentTimeMillis() }
                }

                is InvokeTask -> object : UnitState() {
                    val s = state.stateMachine

                    override fun run() = if (!s.isFinished) also { s.update() } else acc
                }

                is LoopTask -> object : UnitState() {
                    var isStarted = false
                    var s = buildLinearStateMachine(state.task)

                    override fun run() = when {
                        !isStarted && !state.condition() -> acc
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

                is ConditionalTask -> object : UnitState() {
                    var branches = state.conditions.toList()
                    var s: LinearStateMachine<Unit>? = null

                    override fun run(): State<Unit> {
                        s ?: (
                                branches.firstOrNull { (cond, _) -> cond() }?.let { (_, l) -> s = l.createNew() }
                                ?: return acc.also { s = null }
                            )

                        return s!!.let { lsm ->
                            if (!lsm.isFinished) also { lsm.update() }
                            else acc.also { s = null }
                        }
                    }
                }
            }
        }

        return LinearStateMachine(state, endState)
    }
}

internal fun unitState(block: () -> State<Unit>) = object : UnitState() {
    override fun run() = block()
}

/**
 * Dsl for building a [LinearStateMachine].
 */
fun buildLinearStateMachine(block: LinearStateMachineBuilder.() -> Unit) =
    LinearStateMachineBuilder().apply(block).build()