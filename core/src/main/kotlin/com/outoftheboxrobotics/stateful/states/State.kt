package com.outoftheboxrobotics.stateful.states

import com.outoftheboxrobotics.stateful.statemachines.StateMachine

/**
 * Represents an individual state in a [StateMachine].
 *
 * Each State can hold a [value] representing some form of data associated with the individual states in a state
 * machine.
 *
 * @see StateRef
 */
interface State<out T> {
    val value: T

    /**
     * Runs whatever code is associated with this state.
     *
     * @return The next state to go to (can be the same one).
     */
    fun run(): State<T>
}