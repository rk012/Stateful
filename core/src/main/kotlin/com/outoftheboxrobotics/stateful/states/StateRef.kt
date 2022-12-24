package com.outoftheboxrobotics.stateful.states

import com.outoftheboxrobotics.stateful.builders.StateMachineBuilder

/**
 * Represents a [State] without associated code. Used by [StateMachineBuilder] defining states and declaring related
 * code later.
 */
class StateRef<T> internal constructor(override val value: T) : State<T>() {
    internal var body: (State<T>.() -> State<T>)? = null

    override fun run() = body?.invoke(this)
        ?: throw IllegalStateException("StateRef body should not be invoked directly")
}