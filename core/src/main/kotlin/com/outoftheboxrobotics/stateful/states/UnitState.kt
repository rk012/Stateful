package com.outoftheboxrobotics.stateful.states

/**
 * Convenience abstract class for states that have no data associated with them.
 */
abstract class UnitState : State<Unit>() {
    override val value
        get() = Unit
}