package com.outoftheboxrobotics.stateful.builders

import com.outoftheboxrobotics.stateful.concurrent.Awaitable
import com.outoftheboxrobotics.stateful.statemachines.LinearStateMachine
import com.outoftheboxrobotics.stateful.states.State
import com.outoftheboxrobotics.stateful.states.StateDataHandler
import com.outoftheboxrobotics.stateful.states.UnitState


@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
private annotation class LinearStateMachineDsl

/**
 * Dsl builder for creating a [LinearStateMachine].
 */
@LinearStateMachineDsl
class LinearStateMachineBuilder internal constructor() {
    private sealed interface LinearState
    private data class LinearTask(val task: () -> Unit) : LinearState
    private data class WaitTask(val millis: Long) : LinearState
    private data class WaitUntilTask(val handler: () -> Boolean) : LinearState
    private data class InvokeTask(val stateMachine: LinearStateMachine<*>) : LinearState
    private data class LoopTask(
        val condition: () -> Boolean,
        val task: LinearStateMachine<Unit>
    ) : LinearState
    private data class SingleLoopTask(
        val condition: () -> Boolean,
        val action: () -> Unit
    ) : LinearState

    /**
     * Builders for constructing if statements in the linear state machine builder.
     */
    class ConditionalTask internal constructor(
        internal val conditions: MutableList<Pair<() -> Boolean, LinearStateMachine<Unit>>> = mutableListOf()
    ) : LinearState {
        fun elif(condition: () -> Boolean, block: (@LinearStateMachineDsl LinearStateMachineBuilder).() -> Unit) = apply {
            conditions.add(condition to buildLinearStateMachine(block))
        }

        infix fun elseRun(block: (@LinearStateMachineDsl LinearStateMachineBuilder).() -> Unit) {
            conditions.add({ true } to buildLinearStateMachine(block))
        }
    }

    private data class LaunchTask(val stateMachine: LinearStateMachine<*>) : LinearState

    private val endState = object : UnitState() {
        override fun run() = also {
            activeStateMachine?.let { StateDataHandler.clearData(it) }
        }
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
     * State that waits until the specified condition is true before continuing.
     */
    fun waitUntil(condition: () -> Boolean) {
        linearStates.add(WaitUntilTask(condition))
    }

    /**
     * Runs the given state machine until completion as a state.
     */
    fun runStateMachine(stateMachine: LinearStateMachine<*>) {
        linearStates.add(InvokeTask(stateMachine.createNew()))
    }

    /**
     * Shorthand for `runStateMachine(buildLinearStateMachine {...})`
     *
     * Useful for defining async scopes for structured concurrency.
     */
    fun scope(block: (@LinearStateMachineDsl LinearStateMachineBuilder).() -> Unit) =
        runStateMachine(buildLinearStateMachine(block))

    /**
     * State that loops while the condition is true.
     */
    fun loopWhile(condition: () -> Boolean, body: (@LinearStateMachineDsl LinearStateMachineBuilder).() -> Unit) {
        linearStates.add(LoopTask(condition, buildLinearStateMachine(body)))
    }

    /**
     * Optimized loop state builder for repeating a single task that runs on every update.
     */
    fun loopTaskWhile(condition: () -> Boolean, body: () -> Unit) {
        linearStates.add(SingleLoopTask(condition, body))
    }

    /**
     * State builder for conditional execution (if statements).
     */
    fun runIf(condition: () -> Boolean, block: (@LinearStateMachineDsl LinearStateMachineBuilder).() -> Unit) =
        ConditionalTask(mutableListOf(condition to buildLinearStateMachine(block))).also {
            linearStates.add(it)
        }

    /**
     * Launches the given state machine in parallel with the current state machine.
     */
    fun launch(stateMachine: LinearStateMachine<*>): Awaitable = stateMachine.createNew().also {
        linearStates.add(LaunchTask(it))
    }

    /**
     * Shorthand for `launch(buildLinearStateMachine {...})`
     */
    fun launch(block: (@LinearStateMachineDsl LinearStateMachineBuilder).() -> Unit): Awaitable =
        launch(buildLinearStateMachine(block))

    /**
     * Waits for the given awaitable to finish before continuing.
     */
    fun await(awaitable: Awaitable) {
        linearStates.add(WaitUntilTask { awaitable.isFinished })
    }

    internal fun build(): LinearStateMachine<Unit> {
        val state = linearStates.foldRight<_, State<Unit>>(endState) { state, acc ->
            when (state) {
                is LinearTask -> unitState { state.task(); acc }

                is WaitTask -> object : UnitState() {
                    var start: Long? by stateVar(null)

                    override fun run() = start?.let {
                        if (System.currentTimeMillis() - it > state.millis) acc
                        else this
                    } ?: also { start = System.currentTimeMillis() }
                }

                is WaitUntilTask -> unitState {
                    if (state.handler()) acc else this
                }

                is InvokeTask -> object : UnitState() {
                    var s by stateVar(state.stateMachine)
                    var isStarted by stateVar(false)

                    override fun run() = when {
                        !isStarted -> {
                            isStarted = true
                            s = s.createNew()
                            updateStateMachine(s, acc)
                        }
                        else -> updateStateMachine(s, acc)
                    }
                }

                is LoopTask -> object : UnitState() {
                    var isRunning by stateVar(false)
                    var s by stateVar(state.task)

                    override fun run() = when {
                        !isRunning && !state.condition() -> acc
                        !s.isFinished -> also {
                            if (!isRunning) {
                                s = s.createNew()
                                isRunning = true
                            }
                            s.update()
                        }
                        state.condition() -> {
                            s = s.createNew()
                            also { s.update() }
                        }
                        else -> acc
                    }
                }

                is SingleLoopTask -> unitState {
                    if (state.condition()) also { state.action() }
                    else acc
                }

                is ConditionalTask -> object : UnitState() {
                    val branches = state.conditions.toList()
                    var s: LinearStateMachine<Unit>? by stateVar(null)

                    override fun run(): State<Unit> {
                        s ?: (
                                branches.firstOrNull { (cond, _) -> cond() }?.let { (_, l) -> s = l.createNew() }
                                ?: return acc
                            )

                        return updateStateMachine(s!!, acc)
                    }
                }

                is LaunchTask -> unitState {
                    (activeStateMachine as? LinearStateMachine)?.ticker?.launchJob(state.stateMachine)
                    acc
                }
            }
        }

        return LinearStateMachine(state, endState)
    }
}

private fun unitState(block: State<Unit>.() -> State<Unit>) = object : UnitState() {
    override fun run() = block()
}

private fun State<Unit>.updateStateMachine(targetStateMachine: LinearStateMachine<*>, nextState: State<Unit>) =
    targetStateMachine.also {
        it.update()
    }.let {
        if (it.isFinished) nextState else this
    }

/**
 * Dsl for building a [LinearStateMachine].
 */
fun buildLinearStateMachine(block: (@LinearStateMachineDsl LinearStateMachineBuilder).() -> Unit) =
    LinearStateMachineBuilder().apply(block).build()