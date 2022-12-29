package com.outoftheboxrobotics.stateful.statemachines

import com.outoftheboxrobotics.stateful.concurrent.LinearJob
import com.outoftheboxrobotics.stateful.states.State

/**
 * Represents a [StateMachine] that has a beginning and end [State].
 *
 * @param initialState The initial state to start with
 * @param endState The final state that will be reached. Note that this will only run once.
 */
class LinearStateMachine<T>(
    private val initialState: State<T>,
    private val endState: State<T>
) : StateMachine<T>(initialState), LinearJob {
    /**
     * Whether the State Machine has reached the end state.
     */
    override var isFinished = false
        private set

    /**
     * Creates copy of the state machine with the current state reset to the initial state.
     */
    internal fun createNew() = LinearStateMachine(initialState, endState)

    override fun update() {
        if (isFinished) return

        super.update()
        if (currentState == endState) {
            isFinished = true
            super.update()
        }
    }

    override fun updateJob() = update()
}