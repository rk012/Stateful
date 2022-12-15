package com.outoftheboxrobotics.stateful.builders

import com.outoftheboxrobotics.stateful.State
import com.outoftheboxrobotics.stateful.StateMachine
import com.outoftheboxrobotics.stateful.StateRef

/**
 * Dsl builder for creating a [StateMachine].
 */
@StateMachineDsl
class StateMachineBuilder<T> internal constructor() {
    private val states = mutableListOf<StateRef<T>>()

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
     */
    fun StateRef<T>.onRun(block: (@StateMachineDsl State<T>).() -> State<T>) { body = block }

    internal fun build(): StateMachine<T> {
        if (!::startingState.isInitialized) throw IllegalArgumentException("Starting state not set")

        states.forEach {
            if (it.body == null) throw IllegalArgumentException("State body with ${it.value} not set")
        }

        return StateMachine(startingState)
    }
}

/**
 * Dsl for building a [StateMachine].
 */
fun <T> buildStateMachine(block: StateMachineBuilder<T>.() -> Unit) = StateMachineBuilder<T>().apply(block).build()