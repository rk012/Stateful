package com.outoftheboxrobotics.stateful

/**
 * Convenience SAM interface for states that have no data associated with them.
 */
fun interface UnitState : State<Unit> {
    override val value
        get() = Unit
}