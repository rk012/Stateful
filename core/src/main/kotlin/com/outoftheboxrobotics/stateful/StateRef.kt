package com.outoftheboxrobotics.stateful

import com.outoftheboxrobotics.stateful.builders.StateMachineBuilder

/**
 * Represents a [State] without associated code. Used by [StateMachineBuilder] defining states and declaring related
 * code later.
 */
class StateRef<T> internal constructor(override val value: T) : State<T> {
    internal var body: (State<T>.() -> State<T>)? = null

    override fun run(): State<T> = body!!()
}