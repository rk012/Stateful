package com.outoftheboxrobotics.stateful

import com.outoftheboxrobotics.stateful.builders.StateMachineBuilder

/**
 * Represents a [State] without associated code. Used by [StateMachineBuilder] defining states and declaring related
 * code later.
 */
class StateRef<T> internal constructor(override val value: T) : State<T> {
    override fun run() = throw IllegalStateException("StateRef body should not be invoked directly")
}