package com.outoftheboxrobotics.stateful

/**
 * Represents a Finite State Machine.
 *
 * Each state machine holds a [currentState] representing the current [State] of the machine.
 *
 * @param initialState The initial state to start with
 *
 * @see LinearStateMachine
 */
open class StateMachine<T>(initialState: State<T>) {
    /**
     * The current [State] that will be run on next update.
     */
    var currentState = initialState
        private set

    /**
     * Updates the state machine.
     *
     * The code associated with the current state will be run once, and the returned state will become the current
     * state.
     */
    open fun update() {
        currentState = currentState.run()
    }
}