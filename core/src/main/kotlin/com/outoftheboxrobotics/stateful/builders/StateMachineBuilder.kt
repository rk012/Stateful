package com.outoftheboxrobotics.stateful.builders

import com.outoftheboxrobotics.stateful.states.State
import com.outoftheboxrobotics.stateful.statemachines.StateMachine
import com.outoftheboxrobotics.stateful.states.StateRef

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
    data class StateMatcher<T> internal constructor(internal val condition: (State<T>) -> Boolean)

    private data class StateResolverEntry<T>(val matcher: StateMatcher<T>, val body: State<T>.() -> State<T>)
    private data class StateMatcherEntry<T>(val matcher: StateMatcher<T>, val body: State<T>.() -> Unit)

    private val states = mutableListOf<StateRef<T>>()
    private val resolvers = mutableListOf<StateResolverEntry<T>>()
    private val matchers = mutableListOf<StateMatcherEntry<T>>()

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
     * Adds related code to get the next [State] when the receiver state is active.
     *
     * A matcher is generated, so earlier matchers take precedence over later ones.
     */
    fun StateRef<T>.resolveState(block: (@StateMachineDsl State<T>).() -> State<T>) {
        resolvers.add(StateResolverEntry(StateMatcher { this == it }, block))
    }

    /**
     * Adds related code to run when the receiver [State] is active.
     *
     * A matcher is generated, so earlier matchers take precedence over later ones.
     */
    fun StateRef<T>.alsoRun(block: (@StateMachineDsl State<T>).() -> Unit) {
        matchers.add(StateMatcherEntry(StateMatcher { this == it }, block))
    }

    /**
     * Returns a [StateMatcher] that can be used to build custom selectors for adding code to a [State].
     *
     * @see resolveState
     * @see alsoRun
     */
    fun where(predicate: (State<T>) -> Boolean) = StateMatcher(predicate)

    /**
     * Matcher that matches for all states.
     */
    val allStates = StateMatcher<T> { true }

    /**
     * Adds related code to get the next [State] when the receiver [StateMatcher] matches the current state.
     *
     * @see where
     */
    infix fun StateMatcher<T>.resolveState(block: (@StateMachineDsl State<T>).() -> State<T>) {
        resolvers.add(StateResolverEntry(this, block))
    }

    /**
     * Adds related code to run when the receiver [StateMatcher] matches the current state.
     *
     * @see where
     */
    infix fun StateMatcher<T>.alsoRun(block: (@StateMachineDsl State<T>).() -> Unit) {
        matchers.add(StateMatcherEntry(this, block))
    }

    internal fun build(): StateMachine<T> {
        if (!::startingState.isInitialized) throw IllegalArgumentException("Starting state not set")

        states.forEach { s ->
            resolvers.firstOrNull { it.matcher.condition(s) }.let {
                it ?: throw IllegalArgumentException("State body with ${s.value} not set")
            }.let { resolver ->
                val matchedCallbacks = matchers.filter { it.matcher.condition(s) }
                s.body = {
                    matchedCallbacks.forEach {
                        with(it) { body() }
                    }
                    with(resolver) { body() }
                }
            }
        }

        return StateMachine(startingState)
    }
}

/**
 * Dsl for building a [StateMachine].
 */
fun <T> buildStateMachine(block: StateMachineBuilder<T>.() -> Unit) = StateMachineBuilder<T>().apply(block).build()