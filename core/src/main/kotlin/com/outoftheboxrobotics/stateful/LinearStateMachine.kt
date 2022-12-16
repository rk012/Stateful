package com.outoftheboxrobotics.stateful

/**
 * Represents a [StateMachine] that has a beginning and end [State].
 *
 * @param initialState The initial state to start with
 * @param endState The final state that will be reached. Note that this will only run once.
 */
class LinearStateMachine<T>(
    private val initialState: State<T>,
    private val endState: State<T>
) : StateMachine<T>(initialState) {
    /**
     * Whether the State Machine has reached the end state.
     */
    var isFinished = false
        private set

    /**
     * Creates copy of the state machine with the current state reset to the initial state.
     */
    fun createNew() = LinearStateMachine(initialState, endState)

    override fun update() {
        if (isFinished) return

        if (currentState == endState) isFinished = true
        super.update()
    }
}