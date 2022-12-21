package com.outoftheboxrobotics.stateful.builders

import com.outoftheboxrobotics.stateful.State
import com.outoftheboxrobotics.stateful.StateMachine
import com.outoftheboxrobotics.stateful.StateRef

/**
 * Dsl builder for creating a [StateMachine].
 */
@StateMachineDsl
class StateMachineBuilder<T> internal constructor() {
    /**
     * Represents a conditional selector for adding code to a [State].
     *
     * @see StateMachineBuilder.where
     */
    @JvmInline
    value class StateMatcher<T> internal constructor(internal val matcher: (State<T>) -> Boolean)

    private data class StateMatchEntry<T>(val matcher: StateMatcher<T>, val body: State<T>.() -> State<T>)

    private val states = mutableListOf<StateRef<T>>()
    private val matchers = mutableListOf<StateMatchEntry<T>>()

    /**
     * Sets the starting state of the [StateMachine].
     */
    lateinit var startingState: State<T>

    /**
     * Returns a [StateRef] that can be used to build a [StateMachine].
     *
     * @param value The data associated with the [State]
     */
    fun createState(value: T) = StateRef(value).also { states.add(it) }

    /**
     * Adds related code to run when the receiver [State] is active.
     *
     * A matcher is generated, so earlier matchers take precedence over later ones.
     */
    fun StateRef<T>.onRun(block: (@StateMachineDsl State<T>).() -> State<T>) {
        matchers.add(StateMatchEntry(StateMatcher { this == it }, block))
    }

    /**
     * Returns a [StateMatcher] that can be used to build custom selectors for adding code to a [State].
     *
     * @see run
     */
    fun where(predicate: (State<T>) -> Boolean) = StateMatcher(predicate)

    /**
     * Matcher that matches for all states.
     */
    val allStates = StateMatcher<T> { true }

    /**
     * Adds related code to run when the receiver [StateMatcher] matches the current [State].
     *
     * @see where
     */
    infix fun StateMatcher<T>.run(block: (@StateMachineDsl State<T>).() -> State<T>) {
        matchers.add(StateMatchEntry(this, block))
    }

    internal fun build(): StateMachine<T> {
        if (!::startingState.isInitialized) throw IllegalArgumentException("Starting state not set")

        states.onEach { s ->
            matchers.firstOrNull { it.matcher.matcher(s) }.let {
                it ?: throw IllegalArgumentException("State body with ${s.value} not set")
            }.let {
                s.body = it.body
            }
        }

        return StateMachine(startingState)
    }
}

/**
 * Dsl for building a [StateMachine].
 */
fun <T> buildStateMachine(block: StateMachineBuilder<T>.() -> Unit) = StateMachineBuilder<T>().apply(block).build()